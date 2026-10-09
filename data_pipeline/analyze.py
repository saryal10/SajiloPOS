"""Analyze normalized SajiloPOS data with pandas and render matplotlib charts.

Reads:  data/raw/{products,transactions,sale_items}.csv   (see fetch_data.py)
Writes: data/processed/{daily,weekly,monthly,top_products,payment_mix,
                         restock,category_summary}.csv
        output/{revenue_trend,weekly_bars,payment_mix,top_products}.png
        data/processed/summary.json

Usage:
    python analyze.py
All knobs (velocity window, lead time, top-N) live in config.py / .env.
"""

from __future__ import annotations

import json
import math
import sys
from pathlib import Path

import matplotlib

matplotlib.use("Agg")  # headless-safe: no display server needed
import matplotlib.pyplot as plt
import pandas as pd

import config

# Brand palette shared with the Android app.
TEAL = "#0E7C63"
TEAL_DARK = "#0A5D4C"
VIVID = ["#0E7C63", "#6366F1", "#8B5CF6", "#EC4899", "#F97316", "#E9A13B", "#0EA5E9"]
GRID_COLOR = "#E7ECEF"
INK = "#12202B"
MUTED = "#63737F"

plt.rcParams.update(
    {
        "figure.facecolor": "white",
        "axes.facecolor": "white",
        "axes.edgecolor": GRID_COLOR,
        "axes.labelcolor": MUTED,
        "xtick.color": MUTED,
        "ytick.color": MUTED,
        "grid.color": GRID_COLOR,
        "font.size": 10,
    }
)


def load_raw() -> tuple[pd.DataFrame, pd.DataFrame, pd.DataFrame]:
    def read(name: str) -> pd.DataFrame:
        path = config.RAW_DIR / f"{name}.csv"
        if not path.exists():
            print(f"  ! {path.name} missing — run fetch_data.py first.")
            return pd.DataFrame()
        return pd.read_csv(path)

    products = read("products")
    transactions = read("transactions")
    lines = read("sale_items")
    for df, stamp in ((transactions, "timestamp_dt"), (lines, "timestamp_dt")):
        if not df.empty and stamp in df.columns:
            df[stamp] = pd.to_datetime(df[stamp], errors="coerce")
    return products, transactions, lines


def money(value: float) -> str:
    return f"{config.CURRENCY_SYMBOL} {value:,.0f}"


# --------------------------------------------------------------------------
# Aggregations
# --------------------------------------------------------------------------

def daily_frame(transactions: pd.DataFrame, lines: pd.DataFrame) -> pd.DataFrame:
    if transactions.empty:
        return pd.DataFrame(
            columns=["date", "revenue", "orders", "vat", "profit", "avg_ticket"]
        )
    tx = transactions.copy()
    tx["date"] = tx["timestamp_dt"].dt.date
    daily = (
        tx.groupby("date", as_index=False)
        .agg(revenue=("grandTotal", "sum"), orders=("id", "count"), vat=("vatTaxAmount", "sum"))
    )
    if not lines.empty and "timestamp_dt" in lines.columns:
        li = lines.copy()
        li["date"] = li["timestamp_dt"].dt.date
        profit = li.groupby("date", as_index=False).agg(
            profit=("lineTotal", lambda s: (s - li.loc[s.index, "costTotal"].fillna(0)).sum())
        )
        daily = daily.merge(profit, on="date", how="left")
    if "profit" not in daily.columns:
        daily["profit"] = 0.0
    daily["profit"] = daily["profit"].fillna(0.0)
    daily["avg_ticket"] = (daily["revenue"] / daily["orders"].replace(0, pd.NA)).fillna(0.0)
    # Gap-free series: a day with no sales is a zero, not a hole.
    full = pd.DataFrame({"date": pd.date_range(daily["date"].min(), daily["date"].max()).date})
    daily = full.merge(daily, on="date", how="left").fillna(
        {"revenue": 0.0, "orders": 0, "vat": 0.0, "profit": 0.0, "avg_ticket": 0.0}
    )
    daily["orders"] = daily["orders"].astype(int)
    return daily.sort_values("date").reset_index(drop=True)


def resample_frame(daily: pd.DataFrame, rule: str, label: str) -> pd.DataFrame:
    if daily.empty:
        return pd.DataFrame(columns=["period", "revenue", "orders", "vat", "profit", "avg_ticket"])
    frame = daily.copy()
    frame["date"] = pd.to_datetime(frame["date"])
    grouped = (
        frame.set_index("date")
        .resample(rule)
        .agg({"revenue": "sum", "orders": "sum", "vat": "sum", "profit": "sum"})
        .reset_index()
    )
    grouped["avg_ticket"] = (grouped["revenue"] / grouped["orders"].replace(0, pd.NA)).fillna(0.0)
    if rule.startswith("W"):
        grouped["period"] = "w/o " + grouped["date"].dt.strftime("%b %d")
    else:
        grouped["period"] = grouped["date"].dt.strftime("%b %Y")
    grouped = grouped.drop(columns=["date"])
    print(f"  {label}: {len(grouped)} buckets")
    return grouped[["period", "revenue", "orders", "vat", "profit", "avg_ticket"]]


def top_products(lines: pd.DataFrame, top_n: int) -> pd.DataFrame:
    cols = ["productName", "category", "units", "revenue", "profit", "share"]
    if lines.empty:
        return pd.DataFrame(columns=cols)
    grouped = (
        lines.groupby(["productName", "category"], as_index=False)
        .agg(units=("quantity", "sum"), revenue=("lineTotal", "sum"), cost=("costTotal", "sum"))
    )
    grouped["profit"] = (grouped["revenue"] - grouped["cost"].fillna(0)).round(2)
    total = grouped["revenue"].sum()
    grouped["share"] = (grouped["revenue"] / total * 100).round(1) if total else 0.0
    return grouped.sort_values("revenue", ascending=False).head(top_n)[cols].reset_index(drop=True)


def payment_mix(transactions: pd.DataFrame) -> pd.DataFrame:
    cols = ["paymentMethod", "revenue", "orders", "share"]
    if transactions.empty:
        return pd.DataFrame(columns=cols)
    grouped = (
        transactions.groupby("paymentMethod", as_index=False)
        .agg(revenue=("grandTotal", "sum"), orders=("id", "count"))
    )
    total = grouped["revenue"].sum()
    grouped["share"] = (grouped["revenue"] / total * 100).round(1) if total else 0.0
    return grouped.sort_values("revenue", ascending=False)[cols].reset_index(drop=True)


def restock_table(
    products: pd.DataFrame, lines: pd.DataFrame, velocity_days: int, lead_days: int
) -> pd.DataFrame:
    cols = [
        "productId", "name", "unit", "stock", "min_threshold",
        "avg_daily_units", "days_of_cover", "suggested_qty", "urgency", "stock_value",
    ]
    if products.empty:
        return pd.DataFrame(columns=cols)
    velocity: dict = {}
    if not lines.empty and "timestamp_dt" in lines.columns:
        cutoff = pd.Timestamp.now().normalize() - pd.Timedelta(days=velocity_days)
        recent = lines[lines["timestamp_dt"] >= cutoff]
        velocity = recent.groupby("productId")["quantity"].sum().to_dict()
    rows = []
    for _, row in products.iterrows():
        units = float(velocity.get(row["id"], 0.0))
        avg = units / max(velocity_days, 1)
        stock = float(row["stockQuantity"] or 0)
        cover = (stock / avg) if avg > 0 else math.inf
        target = math.ceil(avg * lead_days)
        suggested = max(0, target - int(stock))
        if stock <= 0:
            urgency = "OUT_OF_STOCK"
        elif suggested > 0 and avg > 0:
            urgency = "STOCKOUT_SOON"
        elif stock <= config.WATCH_STOCK_LEVEL:
            urgency = "WATCH"
        else:
            urgency = "PLENTY"
        rows.append(
            {
                "productId": row["id"],
                "name": row["name"],
                "unit": row.get("unit", "pcs"),
                "stock": int(stock),
                "min_threshold": int(row["minStockThreshold"] or 0),
                "avg_daily_units": round(avg, 2),
                "days_of_cover": round(cover, 1) if cover != math.inf else float("inf"),
                "suggested_qty": suggested,
                "urgency": urgency,
                "stock_value": round(float(row["costPrice"] or 0) * stock, 2),
            }
        )
    table = pd.DataFrame(rows, columns=cols)
    order = {"OUT_OF_STOCK": 0, "STOCKOUT_SOON": 1, "WATCH": 2, "PLENTY": 3}
    table["cover_sort"] = table["days_of_cover"].replace(float("inf"), 1e9)
    table = table.sort_values(
        ["urgency", "cover_sort"], key=lambda s: s.map(order) if s.name == "urgency" else s
    ).drop(columns=["cover_sort"])
    return table.reset_index(drop=True)


# --------------------------------------------------------------------------
# Charts
# --------------------------------------------------------------------------

def chart_revenue_trend(daily: pd.DataFrame, path: Path) -> None:
    fig, ax = plt.subplots(figsize=(10, 4.2))
    if daily.empty:
        ax.text(0.5, 0.5, "No sales data yet", ha="center", color=MUTED)
    else:
        labels = pd.to_datetime(daily["date"]).dt.strftime("%b %d")
        ax.plot(labels, daily["revenue"], color=TEAL, linewidth=2.2, label="Daily revenue")
        if len(daily) >= 7:
            ax.plot(
                labels,
                daily["revenue"].rolling(7, min_periods=1).mean(),
                color=VIVID[5],
                linewidth=1.4,
                linestyle="--",
                label="7-day average",
            )
        ax.fill_between(range(len(daily)), daily["revenue"], alpha=0.10, color=TEAL)
        peak = daily["revenue"].idxmax()
        ax.scatter([labels.iloc[peak]], [daily["revenue"].iloc[peak]], color=VIVID[3], s=46, zorder=5)
        ax.set_xticks(range(0, len(daily), max(1, len(daily) // 8)))
        ax.legend(frameon=False)
    ax.set_title("Daily revenue", color=INK, fontweight="bold", loc="left")
    ax.set_ylabel(f"Revenue ({config.CHART_CURRENCY})")
    ax.grid(axis="y", linestyle="--", alpha=0.7)
    fig.tight_layout()
    fig.savefig(path, dpi=150)
    plt.close(fig)


def chart_weekly_bars(weekly: pd.DataFrame, path: Path) -> None:
    fig, ax = plt.subplots(figsize=(10, 4.2))
    if weekly.empty:
        ax.text(0.5, 0.5, "No sales data yet", ha="center", color=MUTED)
    else:
        frame = weekly.tail(config.WEEKLY_CHART_WEEKS)
        bars = ax.bar(frame["period"], frame["revenue"], color=TEAL, edgecolor=TEAL_DARK, linewidth=0.8)
        ax.bar_label(bars, fmt=lambda v: f"{v:,.0f}", fontsize=8, color=MUTED)
        plt.setp(ax.get_xticklabels(), rotation=30, ha="right")
    ax.set_title(f"Weekly revenue (last {config.WEEKLY_CHART_WEEKS} weeks)", color=INK, fontweight="bold", loc="left")
    ax.set_ylabel(f"Revenue ({config.CHART_CURRENCY})")
    ax.grid(axis="y", linestyle="--", alpha=0.7)
    fig.tight_layout()
    fig.savefig(path, dpi=150)
    plt.close(fig)


def chart_payment_mix(mix: pd.DataFrame, path: Path) -> None:
    fig, ax = plt.subplots(figsize=(5.2, 4.2))
    if mix.empty or mix["revenue"].sum() == 0:
        ax.text(0.5, 0.5, "No sales data yet", ha="center", color=MUTED)
    else:
        colors = [VIVID[i % len(VIVID)] for i in range(len(mix))]
        wedges, _, autotexts = ax.pie(
            mix["revenue"],
            colors=colors,
            startangle=90,
            counterclock=False,
            wedgeprops={"width": 0.45, "edgecolor": "white", "linewidth": 2},
            autopct=lambda p: f"{p:.0f}%" if p >= 5 else "",
            pctdistance=0.78,
            textprops={"fontsize": 9, "color": INK},
        )
        ax.legend(wedges, mix["paymentMethod"], loc="center", bbox_to_anchor=(0.5, -0.12),
                  ncol=2, frameon=False, fontsize=9)
        ax.set_title("Revenue by payment method", color=INK, fontweight="bold")
    fig.tight_layout()
    fig.savefig(path, dpi=150, bbox_inches="tight")
    plt.close(fig)


def chart_top_products(top: pd.DataFrame, path: Path) -> None:
    fig, ax = plt.subplots(figsize=(8.5, max(2.6, 0.55 * max(len(top), 1) + 1.2)))
    if top.empty:
        ax.text(0.5, 0.5, "No sales data yet", ha="center", color=MUTED)
    else:
        frame = top.iloc[::-1]
        bars = ax.barh(frame["productName"], frame["revenue"], color=TEAL, edgecolor=TEAL_DARK, linewidth=0.8)
        ax.bar_label(bars, fmt=lambda v: f"{v:,.0f}", fontsize=8, color=MUTED, padding=4)
    ax.set_title(f"Top {config.TOP_N_PRODUCTS} products by revenue", color=INK, fontweight="bold", loc="left")
    ax.set_xlabel(f"Revenue ({config.CHART_CURRENCY})")
    ax.grid(axis="x", linestyle="--", alpha=0.7)
    fig.tight_layout()
    fig.savefig(path, dpi=150)
    plt.close(fig)


# --------------------------------------------------------------------------
# Main
# --------------------------------------------------------------------------

def main() -> None:
    config.ensure_dirs()
    products, transactions, lines = load_raw()
    print(f"Loaded: {len(products)} products, {len(transactions)} sales, {len(lines)} lines")

    daily = daily_frame(transactions, lines)
    weekly = resample_frame(daily, "W-MON", "weekly")
    monthly = resample_frame(daily, "MS", "monthly")
    top = top_products(lines, config.TOP_N_PRODUCTS)
    mix = payment_mix(transactions)
    restock = restock_table(products, lines, config.VELOCITY_WINDOW_DAYS, config.RESTOCK_LEAD_TIME_DAYS)

    categories = pd.DataFrame(columns=["category", "products", "units", "revenue", "stock_value"])
    if not products.empty:
        units_by_cat = lines.groupby("category")["quantity"].sum() if not lines.empty else pd.Series(dtype=float)
        revenue_by_cat = lines.groupby("category")["lineTotal"].sum() if not lines.empty else pd.Series(dtype=float)
        categories = pd.DataFrame(
            {
                "category": products["category"].dropna().unique(),
            }
        )
        categories["products"] = categories["category"].map(products["category"].value_counts())
        categories["units"] = categories["category"].map(units_by_cat).fillna(0).astype(int)
        categories["revenue"] = categories["category"].map(revenue_by_cat).fillna(0).round(2)
        categories["stock_value"] = categories["category"].map(
            products.assign(stock_value=products["costPrice"].fillna(0) * products["stockQuantity"].fillna(0))
            .groupby("category")["stock_value"].sum()
        ).fillna(0).round(2)
        categories = categories.sort_values("revenue", ascending=False).reset_index(drop=True)

    daily.to_csv(config.PROCESSED_DIR / "daily.csv", index=False)
    weekly.to_csv(config.PROCESSED_DIR / "weekly.csv", index=False)
    monthly.to_csv(config.PROCESSED_DIR / "monthly.csv", index=False)
    top.to_csv(config.PROCESSED_DIR / "top_products.csv", index=False)
    mix.to_csv(config.PROCESSED_DIR / "payment_mix.csv", index=False)
    restock.to_csv(config.PROCESSED_DIR / "restock.csv", index=False)
    categories.to_csv(config.PROCESSED_DIR / "category_summary.csv", index=False)

    chart_revenue_trend(daily, config.OUTPUT_DIR / "revenue_trend.png")
    chart_weekly_bars(weekly, config.OUTPUT_DIR / "weekly_bars.png")
    chart_payment_mix(mix, config.OUTPUT_DIR / "payment_mix.png")
    chart_top_products(top, config.OUTPUT_DIR / "top_products.png")
    print("Wrote 7 CSVs + 4 charts")

    total_revenue = float(transactions["grandTotal"].sum()) if not transactions.empty else 0.0
    total_orders = int(len(transactions))
    total_vat = float(transactions["vatTaxAmount"].sum()) if not transactions.empty else 0.0
    line_profit = float((lines["lineTotal"].fillna(0) - lines["costTotal"].fillna(0)).sum()) if not lines.empty else 0.0
    summary = {
        "generated_at": pd.Timestamp.now().isoformat(timespec="seconds"),
        "currency": config.CURRENCY_SYMBOL,
        "counts": {
            "products": int(len(products)),
            "transactions": total_orders,
            "sale_lines": int(len(lines)),
        },
        "totals": {
            "revenue": round(total_revenue, 2),
            "vat_collected": round(total_vat, 2),
            "gross_profit": round(line_profit, 2),
            "margin_pct": round(line_profit / total_revenue * 100, 1) if total_revenue else 0.0,
            "avg_ticket": round(total_revenue / total_orders, 2) if total_orders else 0.0,
        },
        "inventory": {
            "stock_value_at_cost": round(float(restock["stock_value"].sum()), 2) if not restock.empty else 0.0,
            "needs_restock": int((restock["suggested_qty"] > 0).sum()) if not restock.empty else 0,
            "out_of_stock": int((restock["urgency"] == "OUT_OF_STOCK").sum()) if not restock.empty else 0,
        },
        "windows": {
            "velocity_days": config.VELOCITY_WINDOW_DAYS,
            "lead_days": config.RESTOCK_LEAD_TIME_DAYS,
        },
    }
    with open(config.PROCESSED_DIR / "summary.json", "w", encoding="utf-8") as handle:
        json.dump(summary, handle, indent=2, ensure_ascii=False)
    print(
        f"Summary: {money(total_revenue)} revenue · {total_orders} orders · "
        f"{money(line_profit)} profit · {money(total_vat)} VAT"
    )


if __name__ == "__main__":
    sys.exit(main() or 0)
