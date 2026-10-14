# How to run the SajiloPOS data pipeline

This guide takes you from zero to analyzed shop data and a live owner
dashboard. All commands run from this folder (`data_pipeline/`).

---

## 1. What you need

| Requirement | Details |
|---|---|
| Computer | macOS, Windows, or Linux |
| Python | **3.10 or newer** (`python3 --version` to check) |
| pip + venv | Ship with Python (Debian/Ubuntu may need `sudo apt install python3-venv`) |
| A data source | **Either** the API URL + store API key, **or** a Room database file (see step 3) |

> Dependencies (`requirements.txt`): pandas, matplotlib, seaborn, streamlit,
> requests, python-dotenv. Installed automatically into `.venv/` by `start.sh`.

---

## 2. Get the code

```bash
git clone <your-repo-url>
cd SajiloPOS/data_pipeline
```

---

## 3. Point it at data (pick ONE source)

```bash
cp .env.example .env   # then edit .env in any text editor
```

**Option A — backend API** (best for automation):

```bash
SAJILO_API_URL=https://pos.example.com
SAJILO_API_KEY=sk_live_...
```

**Option B — Room database file** (best for the offline-first reality, where the
truth lives on the device):

```bash
adb pull /data/data/com.aistudio.sajilopos.npqzr/databases/sajilo_pos_database ./sajilo_pos_database.db
# .env:
SAJILO_DB_PATH=./sajilo_pos_database.db
```

(The `adb pull` path needs a rooted device or a debuggable build; otherwise
export/share the DB from inside the app. `adb` lives in your Android SDK's
`platform-tools` folder.)

> If `SAJILO_API_URL` is set, API mode wins. `.env` holds secrets and is
> **git-ignored** — never commit it. CLI flags (`--source`, `--db`,
> `--api-url`, `--api-key`) override `.env` for one-off runs.

---

## 4. Run everything (recommended)

```bash
./start.sh
```

This creates `.venv/`, installs requirements (first run takes a few minutes),
fetches data → analyzes it → opens the dashboard in your browser. Variants:

```bash
./start.sh --no-dashboard   # fetch + analyze only (cron jobs, CI)
./start.sh --reinstall       # rebuild the virtualenv from scratch
./start.sh --db /path/to.db  # one-off Room file, ignores .env source
./start.sh --help            # all options
```

Prefer manual steps? `start.sh` does exactly this:

```bash
python3 -m venv .venv
.venv/bin/pip install -r requirements.txt
.venv/bin/python fetch_data.py    # → data/raw/*.csv
.venv/bin/python analyze.py       # → data/processed/*.csv + output/*.png
.venv/bin/python -m streamlit run dashboard.py
```

---

## 5. What you get

| Output | Contents |
|---|---|
| `data/raw/` | Normalized `products.csv`, `transactions.csv`, `sale_items.csv` (exact Room layout) |
| `data/processed/` | `daily.csv`, `weekly.csv`, `monthly.csv`, `top_products.csv`, `payment_mix.csv`, `restock.csv`, `category_summary.csv`, `summary.json` |
| `output/` | `revenue_trend.png`, `weekly_bars.png`, `payment_mix.png`, `top_products.png` |
| Dashboard tabs | Overview · Sales · Products · Inventory · VAT & Export (every table downloadable as CSV) |

Analysis knobs live in `.env`: `SAJILO_VELOCITY_WINDOW_DAYS` (14),
`SAJILO_RESTOCK_LEAD_DAYS` (7), `SAJILO_TOP_N` (10), and more — see `.env.example`.

---

## 6. Troubleshooting

| Symptom | Fix |
|---|---|
| `python3: command not found` / venv fails | Install Python 3.10+ (and `python3-venv` on Debian/Ubuntu) |
| `pip install` is slow / fails | Upgrade pip (the script does this automatically); needs internet on first run |
| “no data source configured” | Fill in `.env`: either `SAJILO_API_URL` + `SAJILO_API_KEY`, or `SAJILO_DB_PATH` |
| API 401 | Wrong/expired key, or the server expects the other header style — try `SAJILO_API_AUTH=x-api-key` (default is `bearer`) |
| API unreachable | Is the server running? For local runs: `cd ../api && ./start.sh` |
| “not a SajiloPOS database” | The `.db` file must contain `products`, `transactions`, `sale_items` tables — re-pull from the device |
| Empty dashboard / “no sales yet” | Normal with a fresh shop — tables fill as sales sync; run `./start.sh --no-dashboard` and inspect `data/processed/summary.json` |
| Port 8501 busy | Stop the other Streamlit instance, or run with `--server.port 8502` appended after `dashboard.py` |

---

## 7. Cheat sheet

```bash
./start.sh                      # everything → dashboard in browser
./start.sh --no-dashboard       # fetch + analyze only
.venv/bin/python fetch_data.py --help    # source/override options
.venv/bin/python analyze.py     # re-run analysis on existing raw data
.venv/bin/python -m streamlit run dashboard.py   # dashboard only
```

Questions or problems? Open an issue in the repo with your OS, Python version,
and the exact error text (redact API keys).
