"""Shared configuration for the SajiloPOS data pipeline.

Everything is driven by environment variables (see .env.example), with
command-line flags in fetch_data.py able to override them. No secrets are
ever hardcoded here.
"""

from __future__ import annotations

import os
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent
DATA_DIR = BASE_DIR / "data"
RAW_DIR = DATA_DIR / "raw"
PROCESSED_DIR = DATA_DIR / "processed"
OUTPUT_DIR = BASE_DIR / "output"


def load_dotenv(path: Path | None = None) -> None:
    """Minimal .env loader (avoids a hard dependency for config import)."""
    env_file = path or (BASE_DIR / ".env")
    if not env_file.exists():
        return
    for raw_line in env_file.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, _, value = line.partition("=")
        key, value = key.strip(), value.strip().strip("'\"")
        os.environ.setdefault(key, value)


load_dotenv()


def _env(name: str, default: str = "") -> str:
    return os.environ.get(name, default).strip()


# --- Data source -----------------------------------------------------------
# API mode wins when a URL is configured; otherwise a local Room database
# file (pulled from the device with adb) is used.
API_URL: str = _env("SAJILO_API_URL")
API_KEY: str = _env("SAJILO_API_KEY")
API_AUTH_STYLE: str = _env("SAJILO_API_AUTH", "bearer").lower()  # bearer | x-api-key
API_SINCE_DAYS: int = int(_env("SAJILO_API_SINCE_DAYS", "365") or 365)
API_TIMEOUT_S: int = int(_env("SAJILO_API_TIMEOUT_S", "30") or 30)

DB_PATH: str = _env("SAJILO_DB_PATH")

# --- Analysis knobs (mirror the Android app's AnalyticsEngine) ------------
VELOCITY_WINDOW_DAYS: int = int(_env("SAJILO_VELOCITY_WINDOW_DAYS", "14") or 14)
RESTOCK_LEAD_TIME_DAYS: int = int(_env("SAJILO_RESTOCK_LEAD_DAYS", "7") or 7)
WATCH_STOCK_LEVEL: int = int(_env("SAJILO_WATCH_STOCK_LEVEL", "10") or 10)
TOP_N_PRODUCTS: int = int(_env("SAJILO_TOP_N", "10") or 10)
WEEKLY_CHART_WEEKS: int = int(_env("SAJILO_WEEKLY_CHART_WEEKS", "12") or 12)

CURRENCY_SYMBOL: str = _env("SAJILO_CURRENCY", "रू")
# ASCII-only fallback for matplotlib (DejaVu Sans has no Devanagari glyphs).
CHART_CURRENCY: str = _env("SAJILO_CHART_CURRENCY", "Rs")


def resolve_source(explicit: str = "auto") -> str:
    """Return 'api' or 'db'. Raises a helpful error if nothing is configured."""
    if explicit in ("api", "db"):
        if explicit == "api" and not API_URL:
            raise SystemExit(
                "ERROR: --source api needs SAJILO_API_URL (see .env.example)."
            )
        if explicit == "db" and not DB_PATH:
            raise SystemExit(
                "ERROR: --source db needs SAJILO_DB_PATH (see .env.example). "
                "Tip: adb pull /data/data/com.aistudio.sajilopos.npqzr/databases/sajilo_pos_database ./sajilo_pos_database.db"
            )
        return explicit
    if API_URL:
        return "api"
    if DB_PATH:
        return "db"
    raise SystemExit(
        "ERROR: no data source configured.\n"
        "  Option A (API):  set SAJILO_API_URL + SAJILO_API_KEY in data_pipeline/.env\n"
        "  Option B (file): set SAJILO_DB_PATH to a Room database file in data_pipeline/.env\n"
        "See data_pipeline/.env.example for details."
    )


def ensure_dirs() -> None:
    RAW_DIR.mkdir(parents=True, exist_ok=True)
    PROCESSED_DIR.mkdir(parents=True, exist_ok=True)
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
