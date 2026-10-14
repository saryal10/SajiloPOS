"""SajiloPOS owner dashboard (Streamlit).

Reads the analyzed outputs in data/processed/ (produced by analyze.py) and the
PNG charts in output/. Run with:  streamlit run dashboard.py
(or simply ./start.sh from the data_pipeline folder).
"""

from __future__ import annotations

from pathlib import Path

import pandas as pd
import streamlit as st

import config

BASE = Path(__file__).resolve().parent
PROCESSED = config.PROCESSED_DIR
OUTPUT = config.OUTPUT_DIR

st.set_page_config(
    page_title="SajiloPOS · Owner Dashboard",
    page_icon="🏪",
    layout="wide",
)


# --------------------------------------------------------------------------
# Data loading
# --------------------------------------------------------------------------

@st.cache_data(show_spinner=False)
def load_csv(name: str) -> pd.DataFrame:
    path = PROCESSED / f"{name}.csv"
    if not path.exists():
        return pd.DataFrame()
    frame = pd.read_csv(path)
    for col in ("date",):
        if col in frame.columns:
            frame[col] = pd.to_datetime(frame[col], errors="coerce")
    return frame


@st.cache_data(show_spinner=False)
def load_summary() -> dict:
    import json

    path = PROCESSED / "summary.json"
    if not path.exists():
        return {}
    return json.loads(path.read_text(encoding="utf-8"))


def money(value) -> str:
    try:
        return f"{config.CURRENCY_SYMBOL} {float(value):,.0f}"
    except (TypeError, ValueError):
        return "—"


def image_if_exists(name: str):
    path = OUTPUT / name
    return str(path) if path.exists() else None


# --------------------------------------------------------------------------
# Header
# --------------------------------------------------------------------------

summary = load_summary()
daily = load_csv("daily")
weekly = load_csv("weekly")
monthly = load_csv("monthly")
top = load_csv("top_products")
mix = load_csv("payment_mix")
restock = load_csv("restock")
categories = load_csv("category_summary")

st.title("🏪 SajiloPOS · Owner Dashboard")
if summary:
    counts = summary.get("counts", {})
    st.caption(
        f"{counts.get('transactions', 0):,} sales · {counts.get('products', 0)} products · "
        f"{counts.get('sale_lines', 0):,} lines · generated {summary.get('generated_at', '?')}"
    )
else:
    st.warning(
        "No analysis found. Run `python fetch_data.py` then `python analyze.py` "
        "(or just `./start.sh`) first."
    )
    st.stop()

if daily.empty:
    st.info("No sales in the selected data yet — the tables below will fill up as you sell.")
    st.stop()

# --------------------------------------------------------------------------
# Sidebar filters
# --------------------------------------------------------------------------

st.sidebar.header("Filters")
min_date = daily["date"].min().date()
max_date = daily["date"].max().date()
start, end = st.sidebar.date_input(
    "Date range",
    value=(min_date, max_date),
    min_value=min_date,
    max_value=max_date,
)
if isinstance(start, (list, tuple)):
    start, end = start[0], start[-1]

categories_available = sorted(categories["category"].dropna().unique()) if not categories.empty else []
chosen_categories = st.sidebar.multiselect("Categories", categories_available, default=categories_available)

methods_available = sorted(mix["paymentMethod"].dropna().unique()) if not mix.empty else []
chosen_methods = st.sidebar.multiselect("Payment methods", methods_available, default=methods_available)

mask = (daily["date"].dt.date >= start) & (daily["date"].dt.date <= end)
range_daily = daily.loc[mask].copy()
range_revenue = float(range_daily["revenue"].sum())
range_orders = int(range_daily["orders"].sum())
range_profit = float(range_daily["profit"].sum())
range_vat = float(range_daily["vat"].sum())

st.sidebar.divider()
st.sidebar.caption(
    "Source: "
    + (
        f"API ({config.API_URL})"
        if config.API_URL
        else f"Room DB ({config.DB_PATH or 'not set'})"
    )
)

# --------------------------------------------------------------------------
# KPI row
# --------------------------------------------------------------------------

kpi1, kpi2, kpi3, kpi4 = st.columns(4)
kpi1.metric("Revenue", money(range_revenue), f"{range_orders:,} orders")
kpi2.metric("Avg ticket", money(range_revenue / range_orders) if range_orders else money(0))
kpi3.metric(
    "Gross profit",
    money(range_profit),
    f"{range_profit / range_revenue * 100:.1f}% margin" if range_revenue else "—",
)
kpi4.metric("VAT collected", money(range_vat))

# --------------------------------------------------------------------------
# Tabs
# --------------------------------------------------------------------------

overview_tab, sales_tab, products_tab, inventory_tab, vat_tab = st.tabs(
    ["Overview", "Sales", "Products", "Inventory", "VAT & Export"]
)

with overview_tab:
    st.subheader("Revenue trend")
    trend_img = image_if_exists("revenue_trend.png")
    if trend_img:
        st.image(trend_img, use_container_width=True)
    col_a, col_b = st.columns([3, 2])
    with col_a:
        st.subheader("Top products")
        if top.empty:
            st.info("No item sales yet.")
        else:
            view = top if not chosen_categories else top[top["category"].isin(chosen_categories)]
            st.dataframe(
                view[["productName", "category", "units", "revenue", "profit", "share"]]
                .rename(
                    columns={
                        "productName": "Item",
                        "category": "Category",
                        "units": "Units",
                        "revenue": "Revenue",
                        "profit": "Profit",
                        "share": "Share %",
                    }
                ),
                use_container_width=True,
                hide_index=True,
            )
    with col_b:
        st.subheader("Payment mix")
        mix_img = image_if_exists("payment_mix.png")
        if mix_img:
            st.image(mix_img, use_container_width=True)
        if not mix.empty:
            view = mix if not chosen_methods else mix[mix["paymentMethod"].isin(chosen_methods)]
            for _, row in view.iterrows():
                st.write(f"**{row['paymentMethod']}** — {money(row['revenue'])} ({row['share']}%)")

with sales_tab:
    st.subheader("Daily breakdown")
    show = range_daily.rename(
        columns={
            "date": "Date",
            "revenue": "Revenue",
            "orders": "Orders",
            "vat": "VAT",
            "profit": "Profit",
            "avg_ticket": "Avg ticket",
        }
    )
    show["Date"] = pd.to_datetime(show["Date"]).dt.date.astype(str)
    st.dataframe(show.sort_values("Date", ascending=False), use_container_width=True, hide_index=True)
    st.subheader("Weekly revenue")
    week_img = image_if_exists("weekly_bars.png")
    if week_img:
        st.image(week_img, use_container_width=True)
    if not monthly.empty:
        with st.expander("Monthly table"):
            st.dataframe(monthly, use_container_width=True, hide_index=True)

with products_tab:
    st.subheader("Best sellers")
    top_img = image_if_exists("top_products.png")
    if top_img:
        st.image(top_img, use_container_width=True)
    if not categories.empty:
        st.subheader("Revenue by category")
        st.dataframe(
            categories.rename(
                columns={
                    "category": "Category",
                    "products": "SKUs",
                    "units": "Units",
                    "revenue": "Revenue",
                    "stock_value": "Stock value",
                }
            ),
            use_container_width=True,
            hide_index=True,
        )

with inventory_tab:
    needs = restock[restock["suggested_qty"] > 0] if not restock.empty else restock
    c1, c2, c3 = st.columns(3)
    c1.metric("SKUs", f"{len(restock):,}" if not restock.empty else "0")
    c2.metric(
        "Stock value (cost)",
        money(restock["stock_value"].sum()) if not restock.empty else money(0),
    )
    c3.metric("Need restock", f"{len(needs):,}")
    st.subheader("Stock levels")
    if restock.empty:
        st.info("No products found. Check the catalog sync.")
    else:
        view = restock if not chosen_categories else restock.merge(
            top[["productName", "category"]].drop_duplicates()
            if not top.empty and "category" in top.columns
            else pd.DataFrame(columns=["productName", "category"]),
            left_on="name",
            right_on="productName",
            how="left",
        )
        # Keep every row even when the join finds no category.
        if "category" not in view.columns:
            view["category"] = ""
        view = view[view["category"].isin(chosen_categories)] if chosen_categories else view
        st.dataframe(
            view[
                ["name", "unit", "stock", "min_threshold", "days_of_cover",
                 "suggested_qty", "urgency", "stock_value"]
            ].rename(
                columns={
                    "name": "Item",
                    "unit": "Unit",
                    "stock": "Stock",
                    "min_threshold": "Min",
                    "days_of_cover": "Days cover",
                    "suggested_qty": "Order",
                    "urgency": "Status",
                    "stock_value": "Value",
                }
            ),
            use_container_width=True,
            hide_index=True,
        )
        st.download_button(
            "Download restock list (CSV)",
            data=view.to_csv(index=False).encode("utf-8"),
            file_name="restock.csv",
            mime="text/csv",
        )

with vat_tab:
    st.subheader("VAT summary (13% Nepal VAT)")
    taxable = float(range_daily["revenue"].sum() - range_daily["vat"].sum())
    v1, v2, v3 = st.columns(3)
    v1.metric("Taxable sales", money(taxable))
    v2.metric("VAT collected", money(range_vat))
    v3.metric(
        "Effective rate",
        f"{range_vat / taxable * 100:.1f}%" if taxable else "—",
    )
    st.caption("PAN/VAT number and invoice sequences live on each receipt in the app.")
    st.subheader("Export")
    for name, label in [
        ("daily", "Daily sales"),
        ("weekly", "Weekly sales"),
        ("monthly", "Monthly sales"),
        ("top_products", "Top products"),
        ("payment_mix", "Payment mix"),
        ("restock", "Restock list"),
    ]:
        frame = load_csv(name)
        if not frame.empty:
            st.download_button(
                f"Download {label} (CSV)",
                data=frame.to_csv(index=False).encode("utf-8"),
                file_name=f"{name}.csv",
                mime="text/csv",
                key=f"dl_{name}",
            )
