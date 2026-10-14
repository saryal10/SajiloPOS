# How to run the SajiloPOS backend API

This guide takes you from zero to the Django API serving requests, with either
SQLite (zero setup) or PostgreSQL. All commands run from this folder (`api/`).

---

## 1. What you need

| Requirement | Details |
|---|---|
| Computer | macOS, Windows, or Linux |
| Python | **3.10 or newer** (`python3 --version` to check) |
| pip + venv | Ship with Python (Debian/Ubuntu may need `sudo apt install python3-venv`) |
| Docker (optional) | Only needed if you want PostgreSQL via `docker compose`; SQLite needs nothing |

> Dependencies are `Django>=4.2`, `djangorestframework>=3.14`,
> `psycopg[binary]>=3.1` (see `requirements.txt`).

---

## 2. Get the code

```bash
git clone <your-repo-url>
cd SajiloPOS/api
```

---

## 3. Configure (one minute)

```bash
cp .env.example .env
```

Open `.env` in any text editor. To start immediately, change nothing — without
`POSTGRES_*` variables the API uses a local `db.sqlite3` file.

For PostgreSQL, set:

```bash
POSTGRES_DB=sajilopos
POSTGRES_USER=sajilopos
POSTGRES_PASSWORD=<pick-a-strong-password>
POSTGRES_HOST=127.0.0.1
POSTGRES_PORT=5432
```

> `.env` holds secrets and is **git-ignored** — never commit it. Only `.env.example` is committed.

---

## 4. Start the server (recommended)

```bash
./start.sh
```

This creates `.venv/`, installs requirements (first run only), runs migrations,
and serves at `http://127.0.0.1:8000`. Useful variants:

```bash
./start.sh --no-server   # migrate only (CI / containers)
./start.sh --reinstall   # rebuild the virtualenv from scratch
./start.sh --help        # all options
```

Prefer manual steps? The script does exactly this:

```bash
python3 -m venv .venv
.venv/bin/pip install -r requirements.txt
.venv/bin/python manage.py migrate
.venv/bin/python manage.py runserver 127.0.0.1:8000
```

To create an admin user for `/admin/`:

```bash
.venv/bin/python manage.py createsuperuser
```

---

## 5. Use PostgreSQL (production-like)

```bash
docker compose up -d db        # starts Postgres 16-alpine with a persisted volume
```

Then set the four `POSTGRES_*` variables in `.env` and re-run `./start.sh`
(migrations run automatically). To stop the database: `docker compose down`
(add `-v` only if you truly want to delete its data).

---

## 6. Verify it works (5-minute test script)

```bash
# 1. health — no auth needed
curl http://127.0.0.1:8000/api/v1/health/

# 2. register a shop (save the api_key — shown once)
curl -X POST http://127.0.0.1:8000/api/v1/auth/register/ \
  -H 'Content-Type: application/json' \
  -d '{"username":"ram","password":"pass12345","store_name":"Himalayan Mart"}'

# 3. push a product (replace sk_live_… with your key)
export KEY=sk_live_...
curl -X POST http://127.0.0.1:8000/api/v1/products/ \
  -H 'Content-Type: application/json' -H "Authorization: Bearer $KEY" \
  -d '{"name":"Wai Wai Noodles","barcode":"8901234001","price":25,"cost_price":18,"stock_quantity":120}'

# 4. record a sale (epoch-millis timestamp, nested lines)
curl -X POST http://127.0.0.1:8000/api/v1/sales/ \
  -H 'Content-Type: application/json' -H "Authorization: Bearer $KEY" \
  -d '{"invoice_number":"INV-20260101-0001","timestamp":1767225600000,
       "subtotal":25,"vat_tax_amount":3.25,"grand_total":28.25,
       "payment_method":"CASH","payment_status":"PAID_CASH",
       "lines":[{"product_name":"Wai Wai Noodles","quantity":1,
                 "list_price":25,"unit_price":25,"line_total":25,"cost_total":18}]}'

# 5. per-store analytics
curl http://127.0.0.1:8000/api/v1/analytics/summary/ \
  -H "Authorization: Bearer $KEY"
```

Posting the same invoice number twice returns the existing sale (`200`, no
duplicate, no double stock decrement) — replay the step-4 command to see it.

Connect the analytics dashboard: in `../data_pipeline/.env` set
`SAJILO_API_URL=http://127.0.0.1:8000` and `SAJILO_API_KEY=<your key>`,
then `cd ../data_pipeline && ./start.sh`.

---

## 7. Run the tests

```bash
.venv/bin/python manage.py test    # 23 API tests, SQLite, no Postgres needed
```

Covered: registration/login, bad credentials, anonymous rejection, bad-key
rejection, cross-store isolation (list + detail), key rotation killing the old
key, `X-API-Key` header, bulk upsert dedupe, product CRUD, low-stock filter,
sale linking + stock decrement, idempotent replay, epoch-millis timestamps,
list filters, line-alias endpoints, analytics totals + windows, throttle config,
100-sale bulk perf smoke.

---

## 8. Troubleshooting

| Symptom | Fix |
|---|---|
| `python3: command not found` / venv fails | Install Python 3.10+ and (Debian/Ubuntu) `python3-venv` |
| `pip install` fails on `psycopg` | It ships a binary wheel; upgrade pip first: `.venv/bin/pip install --upgrade pip`. SQLite-only dev doesn't import it at runtime, but it must still install |
| `connection refused` on Postgres | Is the container up? `docker compose ps`. Check `POSTGRES_*` match the compose file |
| `relation does not exist` errors | You pointed at a fresh Postgres DB without migrating: run `./start.sh --no-server` once |
| 401 on every call | Wrong/expired key, or you need the `X-API-Key` style — set `SAJILO_API_AUTH` server-side? No: send `Authorization: Bearer <key>` (or `X-API-Key: <key>`); rotate a fresh key via `POST /stores/regenerate-key/` with the old one |
| 429 Too Many Requests | You hit a throttle (`anon 100/day`, `user 2000/day`, `ingest 120/minute`); raise them in `.env` (`API_THROTTLE_*`) |
| Port 8000 busy | Stop the other server, or run `.venv/bin/python manage.py runserver 127.0.0.1:8001` |

---

## 9. Cheat sheet

```bash
./start.sh                        # venv → migrate → serve :8000
./start.sh --no-server             # migrate only
.venv/bin/python manage.py test   # 23 API tests
.venv/bin/python manage.py createsuperuser   # admin login
docker compose up -d db           # Postgres 16
docker compose down               # stop Postgres (data kept in volume)
```

Questions or problems? Open an issue in the repo with your OS, Python version,
and the exact error text (redact API keys).
