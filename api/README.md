# SajiloPOS backend API

Django + Django REST Framework backend for the SajiloPOS Android app. It stores
each shop's catalog and sales in PostgreSQL (SQLite for local dev), isolates
stores by API key, and serves the exact endpoints the
[`data_pipeline`](../data_pipeline/) analytics dashboard consumes.

```bash
cd api
cp .env.example .env   # no Postgres needed to start
./start.sh             # migrate -> run at http://127.0.0.1:8000
```

> `start.sh` is executable (`chmod +x` already applied).

## Features

1. **Owner auth** — `POST /api/v1/auth/register/` creates a user + store in one
   call and returns a login token **and** the store API key (shown once);
   `POST /api/v1/auth/login/` returns the token.
2. **Store API keys** — the mobile app and the pipeline authenticate with
   `Authorization: Bearer sk_live_…` (or `X-API-Key`). Keys can be rotated via
   `POST /api/v1/stores/regenerate-key/`; the old key dies immediately.
3. **Multi-store isolation** — every row carries a store FK; API-key callers can
   only ever see their own store (covered by tests, including 404-not-403 on
   cross-store detail access). Token/session users name a store explicitly.
4. **Product catalog CRUD** — `GET/POST /api/v1/products/` (search, category,
   `low_stock=1` filters) with **bulk upsert** matched on barcode, else Android
   id; `GET/PATCH/DELETE /api/v1/products/<id>/`.
5. **Sales ingestion** — `POST /api/v1/sales/` accepts one sale, a list, or
   `{"sales": [...]}` with nested `lines`; **idempotent on
   `(store, invoice_number)`**; accepts epoch-millis timestamps like the app
   sends; links lines to catalog products and decrements stock (floored at 0).
6. **Filtered listings** — `GET /api/v1/sales/` (`?since=` date/ISO/days,
   `?payment_method=`), `GET /api/v1/sale-items/` (plus `/sale_items/`,
   `/lines/` aliases), `GET /api/v1/transactions/` alias — the exact shapes the
   data_pipeline expects.
7. **Per-store analytics** — `GET /api/v1/analytics/summary/` returns totals,
   payment mix, top-5 products, low-stock list and inventory value.
8. **Rate limiting** — DRF throttles: `anon 100/day`, `user 2000/day`,
   `ingest 120/minute` on product/sale writes (all overridable via `.env`).
9. **Django admin** — stores, products, sales (with inline lines) ready to browse.

## Quick tour (local server)

```bash
# 1. health
curl http://127.0.0.1:8000/api/v1/health/

# 2. register a shop (save the api_key — shown once)
curl -X POST http://127.0.0.1:8000/api/v1/auth/register/ \
  -H 'Content-Type: application/json' \
  -d '{"username":"ram","password":"pass12345","store_name":"Himalayan Mart"}'

# 3. push a product (Bearer <api_key>)
curl -X POST http://127.0.0.1:8000/api/v1/products/ \
  -H 'Content-Type: application/json' -H 'Authorization: Bearer sk_live_...' \
  -d '{"name":"Wai Wai Noodles","barcode":"8901234001","price":25,"cost_price":18,"stock_quantity":120}'

# 4. record a sale (epoch millis timestamp, nested lines)
curl -X POST http://127.0.0.1:8000/api/v1/sales/ \
  -H 'Content-Type: application/json' -H 'Authorization: Bearer sk_live_...' \
  -d '{"invoice_number":"INV-20260101-0001","timestamp":1767225600000,
       "subtotal":25,"vat_tax_amount":3.25,"grand_total":28.25,
       "payment_method":"CASH","payment_status":"PAID_CASH",
       "lines":[{"product_name":"Wai Wai Noodles","quantity":1,
                 "list_price":25,"unit_price":25,"line_total":25,"cost_total":18}]}'

# 5. per-store analytics
curl http://127.0.0.1:8000/api/v1/analytics/summary/ \
  -H 'Authorization: Bearer sk_live_...'
```

Point the dashboard at it from `../data_pipeline/.env`:

```bash
SAJILO_API_URL=http://127.0.0.1:8000
SAJILO_API_KEY=sk_live_...
```

then `cd ../data_pipeline && ./start.sh`.

## PostgreSQL (production)

```bash
docker compose up -d db
# .env:
# POSTGRES_DB=sajilopos
# POSTGRES_USER=sajilopos
# POSTGRES_PASSWORD=<secret>
./start.sh --no-server   # runs migrations against Postgres
./start.sh               # serve
```

Without `POSTGRES_*` variables the API uses a local `db.sqlite3` file —
`shop/tests.py` runs against SQLite (`python manage.py test`).

## Files

| File | Purpose |
|---|---|
| `start.sh` | One-command dev runner (venv → migrate → serve) |
| `config/` | Django project: `settings.py` (env-driven DB + throttles), `urls.py` |
| `shop/models.py` | `Store`, `Product`, `SaleTransaction`, `SaleLineItem` (Room-mirrored fields) |
| `shop/auth.py` | `StoreAPIKeyAuthentication` + `IsStoreAuthenticated` |
| `shop/serializers.py` | Validation, epoch-millis timestamps, pipeline-compatible aliases |
| `shop/views.py` | Auth, catalog, idempotent ingest, listings, analytics |
| `shop/admin.py` | Admin for all four models |
| `shop/tests.py` | 23 API tests (auth, isolation, ingest, filters, analytics, perf smoke) |
| `docker-compose.yml` | Postgres 16 for production-like runs |
| `requirements.txt` | `Django`, `djangorestframework`, `psycopg[binary]` |

`.venv/`, `db.sqlite3`, `__pycache__/` and `.env` are git-ignored.
