from django.contrib import admin

from .models import Product, SaleLineItem, SaleTransaction, Store


@admin.register(Store)
class StoreAdmin(admin.ModelAdmin):
    list_display = ("name", "owner", "pan_vat", "created_at")
    search_fields = ("name", "pan_vat")
    readonly_fields = ("api_key", "created_at")


@admin.register(Product)
class ProductAdmin(admin.ModelAdmin):
    list_display = ("name", "store", "barcode", "category", "price", "stock_quantity")
    list_filter = ("store", "category")
    search_fields = ("name", "barcode")


class SaleLineItemInline(admin.TabularInline):
    model = SaleLineItem
    extra = 0
    readonly_fields = ("product_name", "quantity", "line_total")


@admin.register(SaleTransaction)
class SaleTransactionAdmin(admin.ModelAdmin):
    list_display = ("invoice_number", "store", "timestamp", "grand_total", "payment_method")
    list_filter = ("store", "payment_method")
    search_fields = ("invoice_number", "transaction_ref")
    inlines = [SaleLineItemInline]


@admin.register(SaleLineItem)
class SaleLineItemAdmin(admin.ModelAdmin):
    list_display = ("product_name", "store", "quantity", "line_total", "timestamp")
    list_filter = ("store",)
    search_fields = ("product_name", "invoice_number")
