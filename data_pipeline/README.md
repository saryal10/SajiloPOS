# SajiloPOS data pipeline

Offline-friendly analytics for the SajiloPOS Android app: pull the collected
sales data, analyze it with pandas, chart it with matplotlib, and explore it
in a Streamlit dashboard. One command runs everything:

```bash
cd data_pipeline
cp .env.example .env   # then fill in ONE source (API or DB file)
./start.sh
```

> **Why is the dashboard file called `dashboard.py` and not `streamlit.py`?**
> A file named `streamlit.py` would shadow the installed `streamlit` package on
> `import streamlit` and crash immediately. `dashboard.py` avoids that trap.

## How it works

```
┌──────────────┐   ┌──────────────┐   ┌───────────────┐   ┌──────────────┐
│ API  ─┐      │   │ data/raw/    │   │ analyze.py    │   │ dashboard.py │
│  SAJILO_│fetch_│──▶│ products,    │──▶│ pandas +      │──▶│ Streamlit:   │
│  API_URL│data.py│   │ transactions,│   │ matplotlib    │   │ KPIs, tabs,  │
│  + KEY ─┘      │   │ sale_items   │   │ 7 CSVs+4 PNGs │   │ CSV downloads│
│ DB file ─┘     │   │ (Room layout)│   │ + summary.json│   │              │
└──────────────┘   └──────────────┘   └───────────────┘   └──────────────┘
```

Both sources normalize to the **exact Room column layout** of the app
(`products` / `transactions` / `sale_items`), so analysis is identical either way.

## Reference backend

This repo now ships the backend the API mode talks to: [`../api/`](../api/)
(Django + DRF + PostgreSQL, multi-store, token + store-key auth, rate limits).
Point the pipeline at a local run with `SAJILO_API_URL=http://127.0.0.1:8000`
— the endpoint shapes were verified against each other end to end.

## Data sources

**Option A — backend API** (best for automation). Set in `.env`:

```bash
SAJILO_API_URL=https://pos.example.com
SAJILO_API_KEY=sk_live_...
SAJILO_API_AUTH=bearer        # or: x-api-key
```

The client sends the key as an `Authorization: Bearer …` header (or `X-API-Key`)
and tries, in order, `/api/v1/products`, `/api/v1/sales` (then `/transactions`),
`/api/v1/sale-items` (then `/sale_items`, `/lines`), passing `?since=YYYY-MM-DD`.
JSON may be a bare list or an object wrapping one (`data`/`items`/`results`/…);
camelCase keys are accepted and mapped to the Room columns. Missing tables are
fine — related metrics simply show as empty with a note.

**Option B — Room database file** (best for the offline-first reality: the
truth lives on the device). Pull it off a phone, then point at it:

```bash
adb pull /data/data/com.aistudio.sajilopos.npqzr/databases/sajilo_pos_database ./sajilo_pos_database.db
# .env:
SAJILO_DB_PATH=./sajilo_pos_database.db
```

(The exact `adb pull` path needs a rooted device or a debuggable build;
otherwise export/share the DB from inside the app.)

If `SAJILO_API_URL` is set, API mode wins. CLI flags override `.env`:
`--source api|db`, `--db PATH`, `--api-url URL`, `--api-key KEY`.

## What gets analyzed

- **Daily / weekly / monthly** revenue, orders, VAT, profit, avg ticket
  (gap-free series — quiet days are zeros, not holes)
- **Top products** by revenue with units, profit and share
- **Payment mix** (Cash / eSewa / Fonepay / Khalti) with shares
- **Restock predictions** mirroring the app's `AnalyticsEngine`: 14-day sales
  velocity → days of cover → suggested order qty (`OUT_OF_STOCK`,
  `STOCKOUT_SOON`, `WATCH`, `PLENTY`)
- **VAT summary** (13% Nepal VAT) and per-category revenue / stock value

Outputs: `data/processed/*.csv` + `summary.json`, charts in `output/*.png`.

## Dashboard

`./start.sh` opens it automatically. Sidebar filters (date range, categories,
payment methods) drive KPI cards and five tabs: **Overview, Sales, Products,
Inventory, VAT & Export** — every table has a CSV download button. Pure static
file serving is not enough; run it with:
`./.venv/bin/python -m streamlit run dashboard.py`.

## Files

| File | Purpose |
|---|---|
| `start.sh` | One-command runner: venv → install → fetch → analyze → dashboard |
| `config.py` | `.env` loading, source resolution, shared paths/knobs |
| `fetch_data.py` | API + Room readers → normalized `data/raw/*.csv` |
| `analyze.py` | pandas aggregations + matplotlib charts → `data/processed/`, `output/` |
| `dashboard.py` | Streamlit owner dashboard (tabs, filters, downloads) |
| `.env.example` | Copy to `.env` and fill in (never commit `.env`) |
| `requirements.txt` | pandas, matplotlib, seaborn, streamlit, requests, python-dotenv |

`data/`, `output/`, `.venv/` and `.env` are git-ignored — only code and docs
are committed.
