"""Database models for the SajiloPOS backend.

Field names mirror the Android Room entities (snake_cased) so the mobile app
and the data-pipeline can sync without a translation layer:

  products      <-> ProductItem      (app/.../data/model/PosModels.kt)
  transactions  <-> SaleTransaction
  sale_items    <-> SaleLineItem

Every business row belongs to exactly one Store. API access is scoped through
the store's api_key (see shop.auth), so stores can never see each other.
"""

import secrets

from django.conf import settings
from django.db import models


def generate_api_key() -> str:
    return "sk_live_" + secrets.token_urlsafe(32)


class Store(models.Model):
    name = models.CharField(max_length=200)
    owner = models.ForeignKey(
        settings.AUTH_USER_MODEL, on_delete=models.CASCADE, related_name="stores"
    )
    api_key = models.CharField(max_length=80, unique=True, default=generate_api_key)
    pan_vat = models.CharField(max_length=64, blank=True, default="")
    address = models.CharField(max_length=255, blank=True, default="")
    phone = models.CharField(max_length=64, blank=True, default="")
    currency = models.CharField(max_length=8, blank=True, default="रू")
    vat_rate = models.FloatField(default=13.0)
    created_at = models.DateTimeField(auto_now_add=True)

    def __str__(self) -> str:  # pragma: no cover - display helper
        return f"{self.name} (#{self.pk})"

    def rotate_api_key(self) -> str:
        self.api_key = generate_api_key()
        self.save(update_fields=["api_key"])
        return self.api_key


class Product(models.Model):
    store = models.ForeignKey(Store, on_delete=models.CASCADE, related_name="products")
    external_id = models.BigIntegerField(null=True, blank=True)
    name = models.CharField(max_length=200)
    nepali_name = models.CharField(max_length=200, blank=True, default="")
    barcode = models.CharField(max_length=64, blank=True, default="", db_index=True)
    category = models.CharField(max_length=100, blank=True, default="")
    price = models.FloatField(default=0.0)
    cost_price = models.FloatField(default=0.0)
    stock_quantity = models.IntegerField(default=0)
    min_stock_threshold = models.IntegerField(default=5)
    unit = models.CharField(max_length=16, blank=True, default="pcs")
    industry_mode = models.CharField(max_length=32, blank=True, default="RETAIL")
    destination_route = models.CharField(max_length=255, blank=True, default="")
    is_kitchen_item = models.BooleanField(default=False)
    updated_at = models.DateTimeField(auto_now=True)

    class Meta:
        ordering = ["name"]

    def __str__(self) -> str:  # pragma: no cover - display helper
        return f"{self.name} [{self.store_id}]"


class SaleTransaction(models.Model):
    store = models.ForeignKey(Store, on_delete=models.CASCADE, related_name="sales")
    external_id = models.BigIntegerField(null=True, blank=True)
    invoice_number = models.CharField(max_length=64, db_index=True)
    timestamp = models.DateTimeField(db_index=True)
    subtotal = models.FloatField(default=0.0)
    discount_total = models.FloatField(default=0.0)
    service_charge = models.FloatField(default=0.0)
    vat_tax_amount = models.FloatField(default=0.0)
    grand_total = models.FloatField(default=0.0)
    payment_method = models.CharField(max_length=32, blank=True, default="CASH", db_index=True)
    payment_status = models.CharField(max_length=64, blank=True, default="")
    transaction_ref = models.CharField(max_length=128, blank=True, default="")
    industry_mode = models.CharField(max_length=32, blank=True, default="RETAIL")
    meta_info = models.CharField(max_length=255, blank=True, default="")
    items_summary = models.TextField(blank=True, default="")
    cash_tendered = models.FloatField(default=0.0)
    cash_change = models.FloatField(default=0.0)
    created_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ["-timestamp"]
        constraints = [
            models.UniqueConstraint(
                fields=["store", "invoice_number"], name="unique_invoice_per_store"
            )
        ]

    def __str__(self) -> str:  # pragma: no cover - display helper
        return f"{self.invoice_number} [{self.store_id}]"


class SaleLineItem(models.Model):
    sale = models.ForeignKey(
        SaleTransaction, on_delete=models.CASCADE, related_name="lines"
    )
    store = models.ForeignKey(Store, on_delete=models.CASCADE, related_name="sale_lines")
    invoice_number = models.CharField(max_length=64, blank=True, default="", db_index=True)
    product = models.ForeignKey(
        Product, on_delete=models.SET_NULL, null=True, blank=True, related_name="sale_lines"
    )
    product_id_external = models.BigIntegerField(null=True, blank=True)
    product_name = models.CharField(max_length=200, blank=True, default="")
    category = models.CharField(max_length=100, blank=True, default="")
    quantity = models.IntegerField(default=1)
    list_price = models.FloatField(default=0.0)
    unit_price = models.FloatField(default=0.0)
    line_total = models.FloatField(default=0.0)
    cost_total = models.FloatField(default=0.0)
    timestamp = models.DateTimeField(null=True, blank=True)
    passenger_type = models.CharField(max_length=32, blank=True, default="Regular")
    notes = models.TextField(blank=True, default="")

    class Meta:
        ordering = ["id"]

    def __str__(self) -> str:  # pragma: no cover - display helper
        return f"{self.quantity}x {self.product_name} [{self.store_id}]"
