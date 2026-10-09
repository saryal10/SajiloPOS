"""DRF serializers. Field names are snake_case; the data-pipeline maps them
back onto the Android Room column layout (it is case/underscore insensitive).
Two explicit aliases — ``transactionId`` and ``productId`` on line items —
exist purely so the pipeline joins keep working.
"""

from datetime import datetime, timezone

from rest_framework import serializers

from .models import Product, SaleLineItem, SaleTransaction, Store


class FlexibleDateTimeField(serializers.DateTimeField):
    """Accepts ISO-8601 strings *and* epoch millis/seconds as the app sends."""

    def to_internal_value(self, value):
        if isinstance(value, bool):
            self.fail("invalid", format="datetime")
        if isinstance(value, (int, float)):
            number = float(value)
        elif isinstance(value, str) and value.strip():
            text = value.strip()
            try:
                number = float(text)
            except ValueError:
                number = None
        else:
            number = None
        if number is not None:
            if number > 1e12:
                number /= 1000.0
            if number > 1e9:
                return datetime.fromtimestamp(number, tz=timezone.utc)
            self.fail("invalid", format="datetime")
        return super().to_internal_value(value)


class StoreSerializer(serializers.ModelSerializer):
    class Meta:
        model = Store
        fields = [
            "id", "name", "pan_vat", "address", "phone",
            "currency", "vat_rate", "created_at",
        ]
        read_only_fields = fields


class ProductSerializer(serializers.ModelSerializer):
    class Meta:
        model = Product
        fields = [
            "id", "external_id", "name", "nepali_name", "barcode", "category",
            "price", "cost_price", "stock_quantity", "min_stock_threshold",
            "unit", "industry_mode", "destination_route", "is_kitchen_item",
            "updated_at",
        ]
        read_only_fields = ["id", "updated_at"]


class SaleLineItemSerializer(serializers.ModelSerializer):
    timestamp = FlexibleDateTimeField(required=False, allow_null=True)

    class Meta:
        model = SaleLineItem
        fields = [
            "id", "sale", "invoice_number", "product", "product_id_external",
            "product_name", "category", "quantity", "list_price", "unit_price",
            "line_total", "cost_total", "timestamp", "passenger_type", "notes",
        ]
        read_only_fields = ["id", "sale"]

    def to_representation(self, instance):
        """Add the camelCase aliases the data-pipeline joins on."""
        data = super().to_representation(instance)
        data["transactionId"] = instance.sale_id
        data["productId"] = instance.product_id_external
        data["invoiceNumber"] = instance.invoice_number
        return data


class SaleTransactionSerializer(serializers.ModelSerializer):
    timestamp = FlexibleDateTimeField()
    lines = SaleLineItemSerializer(many=True, required=False)

    class Meta:
        model = SaleTransaction
        fields = [
            "id", "external_id", "invoice_number", "timestamp", "subtotal",
            "discount_total", "service_charge", "vat_tax_amount", "grand_total",
            "payment_method", "payment_status", "transaction_ref",
            "industry_mode", "meta_info", "items_summary", "cash_tendered",
            "cash_change", "lines",
        ]
        read_only_fields = ["id"]


class RegisterSerializer(serializers.Serializer):
    username = serializers.CharField(max_length=150)
    password = serializers.CharField(write_only=True, min_length=8)
    store_name = serializers.CharField(max_length=200)
    pan_vat = serializers.CharField(max_length=64, required=False, allow_blank=True, default="")
    address = serializers.CharField(max_length=255, required=False, allow_blank=True, default="")
    phone = serializers.CharField(max_length=64, required=False, allow_blank=True, default="")
