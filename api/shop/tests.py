"""API tests: auth, multi-store isolation, idempotent ingest, analytics.

Run with SQLite (no Postgres needed):
    DB_ENGINE=sqlite python manage.py test
"""

import time

from django.contrib.auth.models import User
from rest_framework.test import APITestCase

from .models import Product, SaleTransaction, Store


def make_store(username="owner", store_name="Himalayan Mart"):
    user = User.objects.create_user(username=username, password="pass12345")
    store = Store.objects.create(name=store_name, owner=user)
    return user, store


def auth_headers(key):
    return {"HTTP_AUTHORIZATION": f"Bearer {key}"}


SALE_PAYLOAD = {
    "invoice_number": "INV-20260101-0001",
    "timestamp": 1767225600000,  # epoch millis, like the Android app sends
    "subtotal": 260.0,
    "discount_total": 0.0,
    "service_charge": 0.0,
    "vat_tax_amount": 33.8,
    "grand_total": 293.8,
    "payment_method": "CASH",
    "payment_status": "PAID_CASH",
    "transaction_ref": "CASH-REG-1",
    "industry_mode": "RETAIL",
    "meta_info": "Counter 1",
    "items_summary": "1x Wai Wai",
    "cash_tendered": 300.0,
    "cash_change": 6.2,
    "lines": [
        {
            "product_name": "Wai Wai Noodles",
            "category": "Snacks",
            "quantity": 1,
            "list_price": 260.0,
            "unit_price": 260.0,
            "line_total": 260.0,
            "cost_total": 200.0,
        }
    ],
}


class AuthTests(APITestCase):
    def test_register_creates_user_store_and_key(self):
        response = self.client.post(
            "/api/v1/auth/register/",
            {"username": "ram", "password": "pass12345", "store_name": "Ram Store"},
            format="json",
        )
        self.assertEqual(response.status_code, 201)
        self.assertIn("token", response.data)
        self.assertIn("api_key", response.data)
        self.assertTrue(response.data["api_key"].startswith("sk_live_"))
        self.assertTrue(Store.objects.filter(name="Ram Store").exists())

    def test_register_rejects_duplicate_username(self):
        make_store(username="ram")
        response = self.client.post(
            "/api/v1/auth/register/",
            {"username": "ram", "password": "pass12345", "store_name": "Other"},
            format="json",
        )
        self.assertEqual(response.status_code, 400)

    def test_login_returns_token(self):
        user, _ = make_store(username="sita")
        response = self.client.post(
            "/api/v1/auth/login/",
            {"username": "sita", "password": "pass12345"},
            format="json",
        )
        self.assertEqual(response.status_code, 200)
        from rest_framework.authtoken.models import Token

        self.assertEqual(response.data["token"], Token.objects.get(user=user).key)

    def test_login_rejects_bad_password(self):
        make_store(username="sita")
        response = self.client.post(
            "/api/v1/auth/login/",
            {"username": "sita", "password": "wrong"},
            format="json",
        )
        self.assertEqual(response.status_code, 401)

    def test_unauthenticated_requests_are_rejected(self):
        self.assertEqual(self.client.get("/api/v1/products/").status_code, 401)
        self.assertEqual(self.client.get("/api/v1/sales/").status_code, 401)
        self.assertEqual(self.client.get("/api/v1/analytics/summary/").status_code, 401)

    def test_bad_api_key_is_rejected(self):
        response = self.client.get(
            "/api/v1/products/", **auth_headers("sk_live_bogus")
        )
        self.assertEqual(response.status_code, 401)

    def test_health_needs_no_auth(self):
        response = self.client.get("/api/v1/health/")
        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.data["status"], "ok")


class StoreIsolationTests(APITestCase):
    def setUp(self):
        _, self.store_a = make_store("owner_a", "Shop A")
        _, self.store_b = make_store("owner_b", "Shop B")
        Product.objects.create(store=self.store_b, name="Secret Item", price=10, stock_quantity=1)

    def test_store_cannot_see_other_store_products(self):
        response = self.client.get("/api/v1/products/", **auth_headers(self.store_a.api_key))
        self.assertEqual(response.status_code, 200)
        names = [p["name"] for p in response.data["results"]]
        self.assertNotIn("Secret Item", names)

    def test_store_cannot_fetch_other_store_product_detail(self):
        other = Product.objects.get(store=self.store_b)
        response = self.client.get(
            f"/api/v1/products/{other.pk}/", **auth_headers(self.store_a.api_key)
        )
        self.assertEqual(response.status_code, 404)

    def test_regenerate_key_invalidates_old_key(self):
        old = self.store_a.api_key
        response = self.client.post(
            "/api/v1/stores/regenerate-key/", **auth_headers(old)
        )
        self.assertEqual(response.status_code, 200)
        new = response.data["api_key"]
        self.assertNotEqual(old, new)
        self.assertEqual(
            self.client.get("/api/v1/products/", **auth_headers(old)).status_code, 401
        )
        self.assertEqual(
            self.client.get("/api/v1/products/", **auth_headers(new)).status_code, 200
        )

    def test_x_api_key_header_also_works(self):
        response = self.client.get(
            "/api/v1/products/", HTTP_X_API_KEY=self.store_a.api_key
        )
        self.assertEqual(response.status_code, 200)


class ProductTests(APITestCase):
    def setUp(self):
        _, self.store = make_store()

    def headers(self):
        return auth_headers(self.store.api_key)

    def test_bulk_upsert_matches_on_barcode(self):
        payload = [
            {"name": "Wai Wai", "barcode": "8901234001", "price": 25, "stock_quantity": 10},
            {"name": "Wai Wai", "barcode": "8901234001", "price": 30, "stock_quantity": 12},
        ]
        response = self.client.post("/api/v1/products/", payload, format="json", **self.headers())
        self.assertEqual(response.status_code, 201)
        self.assertEqual(Product.objects.filter(store=self.store).count(), 1)
        product = Product.objects.get(store=self.store)
        self.assertEqual(product.price, 30)
        self.assertEqual(product.stock_quantity, 12)

    def test_product_crud(self):
        created = self.client.post(
            "/api/v1/products/",
            {"name": "Oil", "barcode": "111", "price": 260, "cost_price": 225, "stock_quantity": 5},
            format="json",
            **self.headers(),
        )
        self.assertEqual(created.status_code, 201)
        pk = created.data["id"] if isinstance(created.data, dict) else created.data[0]["id"]
        patched = self.client.patch(
            f"/api/v1/products/{pk}/", {"price": 270}, format="json", **self.headers()
        )
        self.assertEqual(patched.data["price"], 270)
        deleted = self.client.delete(f"/api/v1/products/{pk}/", **self.headers())
        self.assertEqual(deleted.status_code, 204)

    def test_low_stock_filter(self):
        Product.objects.create(store=self.store, name="Low", stock_quantity=2, min_stock_threshold=5)
        Product.objects.create(store=self.store, name="Fine", stock_quantity=50, min_stock_threshold=5)
        response = self.client.get("/api/v1/products/?low_stock=1", **self.headers())
        names = [p["name"] for p in response.data["results"]]
        self.assertEqual(names, ["Low"])


class SaleIngestTests(APITestCase):
    def setUp(self):
        _, self.store = make_store()
        self.product = Product.objects.create(
            store=self.store,
            external_id=7,
            name="Wai Wai Noodles",
            barcode="8901234001",
            price=260.0,
            cost_price=200.0,
            stock_quantity=10,
        )

    def headers(self):
        return auth_headers(self.store.api_key)

    def test_ingest_links_product_and_decrements_stock(self):
        payload = dict(SALE_PAYLOAD)
        payload["lines"] = [dict(SALE_PAYLOAD["lines"][0], productId=7)]
        response = self.client.post("/api/v1/sales/", payload, format="json", **self.headers())
        self.assertEqual(response.status_code, 201, response.content[:300])
        sale = SaleTransaction.objects.get(store=self.store)
        self.assertEqual(sale.lines.count(), 1)
        self.assertEqual(sale.lines.first().product_id, self.product.pk)
        self.product.refresh_from_db()
        self.assertEqual(self.product.stock_quantity, 9)

    def test_ingest_is_idempotent_on_invoice_number(self):
        payload = dict(SALE_PAYLOAD)
        payload["lines"] = [dict(SALE_PAYLOAD["lines"][0], productId=7)]
        for _ in range(2):
            response = self.client.post(
                "/api/v1/sales/", payload, format="json", **self.headers()
            )
            self.assertIn(response.status_code, (200, 201))
        self.assertEqual(SaleTransaction.objects.filter(store=self.store).count(), 1)
        self.product.refresh_from_db()
        self.assertEqual(self.product.stock_quantity, 10 - 1)  # decremented exactly once

    def test_epoch_millis_timestamp_accepted(self):
        response = self.client.post(
            "/api/v1/sales/", dict(SALE_PAYLOAD), format="json", **self.headers()
        )
        self.assertEqual(response.status_code, 201)
        sale = SaleTransaction.objects.get(store=self.store)
        self.assertEqual(sale.timestamp.year, 2026)

    def test_sales_list_filters(self):
        self.client.post("/api/v1/sales/", dict(SALE_PAYLOAD), format="json", **self.headers())
        other = dict(SALE_PAYLOAD, invoice_number="INV-20260101-0002", payment_method="ESEWA")
        self.client.post("/api/v1/sales/", other, format="json", **self.headers())
        cash = self.client.get("/api/v1/sales/?payment_method=cash", **self.headers())
        self.assertEqual(len(cash.data["results"]), 1)
        recent = self.client.get("/api/v1/sales/?days=4000", **self.headers())
        self.assertEqual(len(recent.data["results"]), 2)
        old = self.client.get("/api/v1/sales/?days=0", **self.headers())
        self.assertEqual(len(old.data["results"]), 0)

    def test_line_alias_endpoints_agree(self):
        self.client.post("/api/v1/sales/", dict(SALE_PAYLOAD), format="json", **self.headers())
        for path in ("/api/v1/sale-items/", "/api/v1/sale_items/", "/api/v1/lines/"):
            response = self.client.get(path, **self.headers())
            self.assertEqual(response.status_code, 200, path)
            self.assertEqual(len(response.data["results"]), 1, path)
            row = response.data["results"][0]
            self.assertIn("transactionId", row)
            self.assertIn("productId", row)


class AnalyticsTests(APITestCase):
    def setUp(self):
        _, self.store = make_store()
        self.product = Product.objects.create(
            store=self.store, name="Wai Wai", price=260.0, cost_price=200.0,
            stock_quantity=8, min_stock_threshold=10,
        )
        payload = dict(SALE_PAYLOAD)
        payload["lines"] = [dict(SALE_PAYLOAD["lines"][0], productId=None)]
        self.client.post("/api/v1/sales/", payload, format="json",
                         **auth_headers(self.store.api_key))

    def test_summary_totals(self):
        response = self.client.get(
            "/api/v1/analytics/summary/", **auth_headers(self.store.api_key)
        )
        self.assertEqual(response.status_code, 200)
        totals = response.data["totals"]
        self.assertAlmostEqual(totals["revenue"], 293.8)
        self.assertEqual(totals["orders"], 1)
        self.assertAlmostEqual(totals["vat_collected"], 33.8)
        self.assertAlmostEqual(totals["gross_profit"], 60.0)
        mix = {row["payment_method"]: row for row in response.data["payment_mix"]}
        self.assertIn("CASH", mix)
        self.assertEqual(response.data["low_stock"]["count"], 1)
        self.assertGreater(response.data["inventory_value"], 0)

    def test_summary_respects_since(self):
        response = self.client.get(
            "/api/v1/analytics/summary/?days=0", **auth_headers(self.store.api_key)
        )
        self.assertEqual(response.data["totals"]["orders"], 0)


class ThrottleConfigTests(APITestCase):
    def test_ingest_scope_is_configured(self):
        from django.conf import settings

        self.assertIn("ingest", settings.REST_FRAMEWORK["DEFAULT_THROTTLE_RATES"])
        # hammering without auth must eventually be throttled, not crash
        statuses = {
            self.client.get("/api/v1/products/").status_code for _ in range(5)
        }
        self.assertTrue(statuses <= {401, 429})


class PerformanceSmokeTests(APITestCase):
    def test_bulk_ingest_100_sales(self):
        _, store = make_store(username="bulk")
        headers = auth_headers(store.api_key)
        payloads = []
        base = int(time.time() * 1000)
        for i in range(100):
            sale = dict(SALE_PAYLOAD)
            sale["invoice_number"] = f"BULK-{i:04d}"
            sale["timestamp"] = base - i * 1000
            payloads.append(sale)
        started = time.monotonic()
        response = self.client.post("/api/v1/sales/", payloads, format="json", **headers)
        elapsed = time.monotonic() - started
        self.assertEqual(response.status_code, 201)
        self.assertEqual(SaleTransaction.objects.filter(store=store).count(), 100)
        print(f"\n100-sale bulk ingest took {elapsed:.2f}s")
