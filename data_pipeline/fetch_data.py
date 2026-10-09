"""Fetch SajiloPOS data from the backend API or a local Room database file.

Both paths normalize into the exact Room column layout used by the Android
app (tables: products / transactions / sale_items) and write
data/raw/{products,transactions,sale_items}.csv. Everything downstream
(analyze.py, dashboard.py) only ever reads those normalized CSVs, so the
analysis is identical no matter where the data came from.

Usage:
    python fetch_data.py [--source api|db] [--db PATH] [--api-url URL] [--api-key KEY]
"""

from __future__ import annotations

import argparse
import sqlite3
import sys
from datetime import datetime, timedelta, timezone
from pathlib import Path

import pandas as pd
import requests

import config

# Exact Room column layout from
# app/src/main/java/com/example/data/model/PosModels.kt
PRODUCT_COLUMNS = [
    "id", "name", "nepaliName", "barcode", "category", "price",
    "costPrice", "stockQuantity", "minStockThreshold", "unit",
    "industryMode", "destinationRoute", "isKitchenItem",
]
TRANSACTION_COLUMNS = [
    "id", "invoiceNumber", "timestamp", "subtotal", "discountTotal",
    "serviceCharge", "vatTaxAmount", "grandTotal", "paymentMethod",
    "paymentStatus", "transactionRef", "industryMode", "metaInfo",
    "itemsSummary", "cashTendered", "cashChange",
]
LINE_ITEM_COLUMNS = [
    "id", "transactionId", "invoiceNumber", "productId", "productName",
    "category", "quantity", "listPrice", "unitPrice", "lineTotal",
    "costTotal", "timestamp", "passengerType", "notes",
]

NUMERIC_COLUMNS = {
    "products": ["id", "price", "costPrice", "stockQuantity", "minStockThreshold"],
    "transactions": [
        "id", "subtotal", "discountTotal", "serviceCharge",
        "vatTaxAmount", "grandTotal", "cashTendered", "cashChange",
    ],
    "sale_items": [
        "id", "transactionId", "productId", "quantity", "listPrice",
        "unitPrice", "lineTotal", "costTotal",
    ],
}


# --------------------------------------------------------------------------
# Normalization helpers
# --------------------------------------------------------------------------

def normalize_frame(df: pd.DataFrame, table: str) -> pd.DataFrame:
    """Rename API-style keys to Room columns, add missing columns as NA."""
    df = df.copy()
    # Accept camelCase/snake_case variants coming from a JSON API.
    renames = {}
    wanted = {
        "products": PRODUCT_COLUMNS,
        "transactions": TRANSACTION_COLUMNS,
        "sale_items": LINE_ITEM_COLUMNS,
    }[table]
    lowered = {c.lower().replace("_", ""): c for c in df.columns}
    for col in wanted:
        key = col.lower().replace("_", "")
        if col not in df.columns and key in lowered:
            renames[lowered[key]] = col
    if renames:
        df = df.rename(columns=renames)
    for col in wanted:
        if col not in df.columns:
            df[col] = pd.NA
    df = df[wanted]
    for col in NUMERIC_COLUMNS[table]:
        df[col] = pd.to_numeric(df[col], errors="coerce")
    return df


def normalize_timestamp(series: pd.Series) -> pd.Series:
    """Accept ms epoch, s epoch, or ISO strings -> naive local datetimes."""
    def parse_one(value):
        if value is None or (isinstance(value, float) and pd.isna(value)):
            return pd.NaT
        if isinstance(value, (int, float)):
            value = float(value)
            if value > 1e12:
                value = value / 1000.0
            if value > 1e9:
                return datetime.fromtimestamp(value)
            return pd.NaT
        text = str(value).strip()
        if not text:
            return pd.NaT
        try:
            number = float(text)
            return parse_one(number)
        except ValueError:
            pass
        try:
            moment = datetime.fromisoformat(text.replace("Z", "+00:00"))
            if moment.tzinfo is not None:
                moment = moment.astimezone().replace(tzinfo=None)
            return moment
        except ValueError:
            return pd.NaT

    return pd.to_datetime(series.map(parse_one), errors="coerce")


# --------------------------------------------------------------------------
# API source
# --------------------------------------------------------------------------

def _auth_headers() -> dict:
    if not config.API_KEY:
        raise SystemExit(
            "ERROR: SAJILO_API_KEY is empty. Put your key in data_pipeline/.env "
            "(see .env.example). The key is sent as an HTTP header and is never logged."
        )
    if config.API_AUTH_STYLE == "x-api-key":
        return {"X-API-Key": config.API_KEY}
    return {"Authorization": f"Bearer {config.API_KEY}"}


def _get_json(session: requests.Session, url: str, params: dict) -> list:
    try:
        response = session.get(url, params=params, timeout=config.API_TIMEOUT_S)
    except requests.RequestException as exc:
        raise SystemExit(f"ERROR: could not reach the API at {url}: {exc}")
    if response.status_code == 401:
        raise SystemExit(
            "ERROR: the API rejected the key (HTTP 401). Check SAJILO_API_KEY, "
            "or set SAJILO_API_AUTH=x-api-key if your server expects that header."
        )
    if response.status_code == 404:
        print(f"  ! endpoint not found (404), treating as empty: {url}")
        return []
    try:
        response.raise_for_status()
    except requests.HTTPError as exc:
        raise SystemExit(f"ERROR: API request failed: {exc}")
    try:
        payload = response.json()
    except ValueError:
        raise SystemExit(f"ERROR: API did not return JSON: {url}")
    if isinstance(payload, dict):
        for key in ("data", "items", "results", "sales", "products"):
            if isinstance(payload.get(key), list):
                return payload[key]
        return [payload]
    if isinstance(payload, list):
        return payload
    raise SystemExit(f"ERROR: unexpected API response shape from {url}")


def fetch_from_api() -> dict[str, pd.DataFrame]:
    base = config.API_URL.rstrip("/")
    print(f"Fetching from API: {base}")
    session = requests.Session()
    session.headers.update(_auth_headers())
    session.headers.update({"Accept": "application/json"})
    since = (datetime.now(timezone.utc) - timedelta(days=config.API_SINCE_DAYS)).date().isoformat()

    endpoints = [
        # (endpoint path, table name, query params)
        ("products", "products", {}),
        ("sales", "transactions", {"since": since}),
        ("transactions", "transactions", {"since": since}),
        ("sale-items", "sale_items", {"since": since}),
        ("sale_items", "sale_items", {"since": since}),
        ("lines", "sale_items", {"since": since}),
    ]
    tables: dict[str, pd.DataFrame] = {
        "products": pd.DataFrame(),
        "transactions": pd.DataFrame(),
        "sale_items": pd.DataFrame(),
    }
    for path, table, params in endpoints:
        if not tables[table].empty:
            continue  # first matching endpoint per table wins
        try:
            rows = _get_json(session, f"{base}/api/v1/{path}", params)
        except SystemExit as exc:
            # A missing-table endpoint (404) is tolerated; auth errors are fatal.
            if "404" in str(exc) or "empty" in str(exc):
                rows = []
            else:
                raise
        if rows:
            tables[table] = pd.DataFrame(rows)
            print(f"  {table}: {len(rows)} rows from /api/v1/{path}")
    return tables


# --------------------------------------------------------------------------
# Room database source
# --------------------------------------------------------------------------

def fetch_from_db(db_path: str | None = None) -> dict[str, pd.DataFrame]:
    path = Path(db_path or config.DB_PATH).expanduser()
    if not path.exists():
        raise SystemExit(
            f"ERROR: database file not found: {path}\n"
            "Pull it from a device first:\n"
            "  adb pull /data/data/com.aistudio.sajilopos.npqzr/databases/sajilo_pos_database ./sajilo_pos_database.db\n"
            "(needs a rooted device or a debuggable/run-as capable build)"
        )
    print(f"Reading Room database: {path}")
    connection = sqlite3.connect(f"file:{path}?mode=ro", uri=True)
    try:
        available = {
            row[0]
            for row in connection.execute(
                "SELECT name FROM sqlite_master WHERE type='table'"
            ).fetchall()
        }
        expected = {"products": "products", "transactions": "transactions", "sale_items": "sale_items"}
        missing = [t for t in expected if t not in available]
        if missing:
            raise SystemExit(
                f"ERROR: {path} is not a SajiloPOS database "
                f"(missing tables: {', '.join(missing)})."
            )
        tables = {
            table: pd.read_sql_query(f"SELECT * FROM {table}", connection)
            for table in expected
        }
    finally:
        connection.close()
    for table, df in tables.items():
        print(f"  {table}: {len(df)} rows")
    return tables


# --------------------------------------------------------------------------
# Main
# --------------------------------------------------------------------------

def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Fetch SajiloPOS data (API or Room DB).")
    parser.add_argument("--source", choices=["auto", "api", "db"], default="auto")
    parser.add_argument("--db", default=None, help="Path to a Room .db file (overrides SAJILO_DB_PATH).")
    parser.add_argument("--api-url", default=None, help="API base URL (overrides SAJILO_API_URL).")
    parser.add_argument("--api-key", default=None, help="API key (overrides SAJILO_API_KEY; prefer .env).")
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> None:
    args = parse_args(argv)
    if args.api_url:
        config.API_URL = args.api_url
    if args.api_key:
        config.API_KEY = args.api_key
    if args.db:
        config.DB_PATH = args.db

    source = config.resolve_source(args.source)
    config.ensure_dirs()

    if source == "api":
        raw = fetch_from_api()
    else:
        raw = fetch_from_db()

    wanted = {
        "products": PRODUCT_COLUMNS,
        "transactions": TRANSACTION_COLUMNS,
        "sale_items": LINE_ITEM_COLUMNS,
    }
    for table, columns in wanted.items():
        df = normalize_frame(raw.get(table, pd.DataFrame()), table)
        if table in ("transactions", "sale_items") and "timestamp" in df.columns:
            # Parse first (ISO strings, epoch s/ms all accepted), then store
            # epoch millis so the CSV matches the Room column layout exactly.
            parsed = normalize_timestamp(df["timestamp"])
            df["timestamp_dt"] = parsed
            df["timestamp"] = parsed.map(
                lambda moment: int(moment.timestamp() * 1000) if pd.notna(moment) else pd.NA
            )
        out = config.RAW_DIR / f"{table}.csv"
        df.to_csv(out, index=False)
        print(f"Wrote {out} ({len(df)} rows, {len(columns)} columns)")

    if raw["transactions"].empty:
        print("WARNING: no transactions found — analysis will show empty results.")
    if raw["sale_items"].empty:
        print("NOTE: no sale_items found — profit and velocity metrics will be limited.")


if __name__ == "__main__":
    main(sys.argv[1:])
