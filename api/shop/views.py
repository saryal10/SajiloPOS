"""API views. Every store-scoped view resolves exactly one Store via
``get_request_store()`` — a store API key implies its own store, while user
tokens/sessions must name an owned store explicitly.
"""

from datetime import datetime, timedelta, timezone as dt_timezone

from django.contrib.auth import authenticate
from django.contrib.auth.models import User
from django.db import transaction
from django.db.models import Avg, Count, F, Sum
from django.utils import timezone
from rest_framework import status
from rest_framework.authtoken.models import Token
from rest_framework.exceptions import PermissionDenied, ValidationError
from rest_framework.permissions import AllowAny
from rest_framework.response import Response
from rest_framework.views import APIView

from .auth import IsStoreAuthenticated
from .models import Product, SaleLineItem, SaleTransaction, Store
from .serializers import (
    ProductSerializer,
    RegisterSerializer,
    SaleLineItemSerializer,
    SaleTransactionSerializer,
    StoreSerializer,
)


def get_request_store(request) -> Store:
    """Resolve the single store this request may touch."""
    auth = getattr(request, "auth", None)
    if isinstance(auth, Store):
        return auth
    store_id = request.headers.get("X-Store-ID") or request.query_params.get("store_id")
    if not store_id:
        raise PermissionDenied(
            "User credentials require an explicit store: pass ?store_id= or X-Store-ID."
        )
    try:
        store = Store.objects.select_related("owner").get(pk=store_id)
    except (Store.DoesNotExist, ValueError):
        raise PermissionDenied("Unknown store.")
    user = request.user
    if not (user.is_staff or store.owner_id == user.pk):
        raise PermissionDenied("You do not own this store.")
    return store


def parse_since(value: str | None):
    """Accept 'YYYY-MM-DD', full ISO datetimes, or a bare day count."""
    if not value:
        return None
    value = value.strip()
    if value.isdigit():
        return timezone.now() - timedelta(days=int(value))
    try:
        moment = datetime.fromisoformat(value.replace("Z", "+00:00"))
    except ValueError:
        raise ValidationError({"since": "Use YYYY-MM-DD, ISO datetime, or a day count."})
    if moment.tzinfo is None:
        moment = moment.replace(tzinfo=dt_timezone.utc)
    return moment


# --------------------------------------------------------------------------
# Public
# --------------------------------------------------------------------------

class HealthView(APIView):
    permission_classes = [AllowAny]
    throttle_scope = None

    def get(self, request):
        return Response({"status": "ok", "service": "sajilopos-api"})


class RegisterView(APIView):
    permission_classes = [AllowAny]

    def post(self, request):
        serializer = RegisterSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        data = serializer.validated_data
        if User.objects.filter(username=data["username"]).exists():
            raise ValidationError({"username": "Username is taken."})
        user = User.objects.create_user(username=data["username"], password=data["password"])
        store = Store.objects.create(
            name=data["store_name"],
            owner=user,
            pan_vat=data.get("pan_vat", ""),
            address=data.get("address", ""),
            phone=data.get("phone", ""),
        )
        token = Token.objects.create(user=user)
        return Response(
            {
                "token": token.key,
                "store_id": store.pk,
                "api_key": store.api_key,
                "warning": "Store this API key now — it is shown only here and on rotation.",
            },
            status=status.HTTP_201_CREATED,
        )


class LoginView(APIView):
    permission_classes = [AllowAny]

    def post(self, request):
        username = (request.data.get("username") or "").strip()
        password = request.data.get("password") or ""
        user = authenticate(request, username=username, password=password)
        if user is None:
            return Response(
                {"detail": "Invalid credentials."}, status=status.HTTP_401_UNAUTHORIZED
            )
        token, _ = Token.objects.get_or_create(user=user)
        return Response({"token": token.key})


# --------------------------------------------------------------------------
# Store self-service
# --------------------------------------------------------------------------

class StoreMeView(APIView):
    permission_classes = [IsStoreAuthenticated]

    def get(self, request):
        return Response(StoreSerializer(get_request_store(request)).data)


class RegenerateKeyView(APIView):
    permission_classes = [IsStoreAuthenticated]

    def post(self, request):
        store = get_request_store(request)
        new_key = store.rotate_api_key()
        return Response(
            {
                "api_key": new_key,
                "warning": "The old key stops working immediately. Update the app and dashboard.",
            }
        )


# --------------------------------------------------------------------------
# Products
# --------------------------------------------------------------------------

class ProductListView(APIView):
    permission_classes = [IsStoreAuthenticated]
    throttle_scope = "ingest"

    def get(self, request):
        store = get_request_store(request)
        queryset = Product.objects.filter(store=store)
        search = (request.query_params.get("search") or "").strip()
        if search:
            queryset = queryset.filter(name__icontains=search)
        category = (request.query_params.get("category") or "").strip()
        if category:
            queryset = queryset.filter(category__iexact=category)
        if (request.query_params.get("low_stock") or "").lower() in ("1", "true", "yes"):
            queryset = queryset.filter(stock_quantity__lte=F("min_stock_threshold"))
        return self._paginate(queryset.order_by("name"), request, ProductSerializer)

    def post(self, request):
        """Create one product, or bulk-upsert a list (matched on barcode, else external_id)."""
        store = get_request_store(request)
        payload = request.data.get("products", request.data) if isinstance(request.data, dict) else request.data
        many = isinstance(payload, list)
        items = payload if many else [payload]
        results = []
        with transaction.atomic():
            for item in items:
                serializer = ProductSerializer(data=item)
                serializer.is_valid(raise_exception=True)
                data = dict(serializer.validated_data)
                product = self._upsert(store, data)
                results.append(ProductSerializer(product).data)
        return Response(
            results if many else results[0],
            status=status.HTTP_201_CREATED,
        )

    @staticmethod
    def _upsert(store: Store, data: dict) -> Product:
        barcode = (data.get("barcode") or "").strip()
        external_id = data.get("external_id")
        product = None
        if barcode:
            product = Product.objects.filter(store=store, barcode=barcode).first()
        if product is None and external_id is not None:
            product = Product.objects.filter(store=store, external_id=external_id).first()
        if product is None:
            return Product.objects.create(store=store, **data)
        for field, value in data.items():
            setattr(product, field, value)
        product.save()
        return product

    @staticmethod
    def _paginate(queryset, request, serializer_class):
        from rest_framework.pagination import PageNumberPagination

        paginator = PageNumberPagination()
        paginator.page_size_query_param = None
        page = paginator.paginate_queryset(queryset, request)
        if page is not None:
            return paginator.get_paginated_response(serializer_class(page, many=True).data)
        return Response(serializer_class(queryset, many=True).data)


class ProductDetailView(APIView):
    permission_classes = [IsStoreAuthenticated]
    throttle_scope = "ingest"

    def _get(self, request, pk: int) -> Product:
        store = get_request_store(request)
        try:
            return Product.objects.get(store=store, pk=pk)
        except (Product.DoesNotExist, ValueError):
            from django.http import Http404

            raise Http404("Product not found.")

    def get(self, request, pk: int):
        return Response(ProductSerializer(self._get(request, pk)).data)

    def patch(self, request, pk: int):
        product = self._get(request, pk)
        serializer = ProductSerializer(product, data=request.data, partial=True)
        serializer.is_valid(raise_exception=True)
        serializer.save()
        return Response(serializer.data)

    def delete(self, request, pk: int):
        self._get(request, pk).delete()
        return Response(status=status.HTTP_204_NO_CONTENT)


# --------------------------------------------------------------------------
# Sales ingestion + listing
# --------------------------------------------------------------------------

def _link_product(store: Store, line: dict):
    """Match a sale line to a catalog product via Android id, then barcode."""
    external = line.get("productId")
    if external is None:
        external = line.get("product_id_external")
    if external is not None:
        product = Product.objects.filter(store=store, external_id=external).first()
        if product:
            return product, external
    barcode = (line.get("barcode") or "").strip()
    if barcode:
        product = Product.objects.filter(store=store, barcode=barcode).first()
        if product:
            return product, product.external_id
    return None, external


def _ingest_sale(store: Store, payload: dict) -> tuple[SaleTransaction, bool]:
    serializer = SaleTransactionSerializer(data=payload)
    serializer.is_valid(raise_exception=True)
    data = dict(serializer.validated_data)
    lines = data.pop("lines", []) or []
    invoice = (data.get("invoice_number") or "").strip()
    if not invoice:
        raise ValidationError({"invoice_number": "Invoice number is required."})

    with transaction.atomic():
        sale, created = SaleTransaction.objects.get_or_create(
            store=store,
            invoice_number=invoice,
            defaults={**data, "timestamp": data.get("timestamp") or timezone.now()},
        )
        if not created:
            return sale, False
        # Linking keys (productId / barcode) ride along on the raw payload;
        # validated values always win for the stored fields.
        raw_input_lines = payload.get("lines", []) or []
        for raw, raw_input in zip(lines, raw_input_lines):
            lookup = dict(raw_input) if isinstance(raw_input, dict) else {}
            lookup.update(raw)
            product, external = _link_product(store, lookup)
            timestamp = raw.get("timestamp") or sale.timestamp
            SaleLineItem.objects.create(
                sale=sale,
                store=store,
                invoice_number=invoice,
                product=product,
                product_id_external=external,
                product_name=raw.get("product_name", "") or (product.name if product else ""),
                category=raw.get("category", "") or (product.category if product else ""),
                quantity=int(raw.get("quantity", 1) or 1),
                list_price=float(raw.get("list_price", 0) or 0),
                unit_price=float(raw.get("unit_price", 0) or 0),
                line_total=float(raw.get("line_total", 0) or 0),
                cost_total=float(raw.get("cost_total", 0) or 0),
                timestamp=timestamp,
                passenger_type=raw.get("passenger_type", "Regular") or "Regular",
                notes=raw.get("notes", "") or "",
            )
            if product:
                product.stock_quantity = max(0, product.stock_quantity - int(raw.get("quantity", 1) or 1))
                product.save(update_fields=["stock_quantity"])
    return sale, True


class SaleListView(APIView):
    permission_classes = [IsStoreAuthenticated]
    throttle_scope = "ingest"

    def get(self, request):
        store = get_request_store(request)
        queryset = SaleTransaction.objects.filter(store=store).prefetch_related("lines")
        since = parse_since(request.query_params.get("since") or request.query_params.get("days"))
        if since:
            queryset = queryset.filter(timestamp__gte=since)
        method = (request.query_params.get("payment_method") or "").strip()
        if method:
            queryset = queryset.filter(payment_method__iexact=method)
        return ProductListView._paginate(
            queryset.order_by("-timestamp"), request, SaleTransactionSerializer
        )

    def post(self, request):
        """Ingest one sale or a bulk list. Idempotent on (store, invoice_number)."""
        store = get_request_store(request)
        data = request.data
        if isinstance(data, dict) and isinstance(data.get("sales"), list):
            payloads = data["sales"]
            many = True
        elif isinstance(data, list):
            payloads = data
            many = True
        elif isinstance(data, dict):
            payloads = [data]
            many = False
        else:
            raise ValidationError("Body must be a sale object, a list, or {\"sales\": [...]}.")

        results, created_any = [], False
        for payload in payloads:
            sale, created = _ingest_sale(store, payload)
            created_any = created_any or created
            results.append(SaleTransactionSerializer(sale).data)
        return Response(
            results if many else results[0],
            status=status.HTTP_201_CREATED if created_any else status.HTTP_200_OK,
        )


class SaleDetailView(APIView):
    permission_classes = [IsStoreAuthenticated]

    def get(self, request, pk: int):
        store = get_request_store(request)
        try:
            sale = SaleTransaction.objects.prefetch_related("lines").get(store=store, pk=pk)
        except (SaleTransaction.DoesNotExist, ValueError):
            from django.http import Http404

            raise Http404("Sale not found.")
        return Response(SaleTransactionSerializer(sale).data)


class SaleLineListView(APIView):
    permission_classes = [IsStoreAuthenticated]

    def get(self, request):
        store = get_request_store(request)
        queryset = SaleLineItem.objects.filter(store=store).select_related("sale", "product")
        since = parse_since(request.query_params.get("since") or request.query_params.get("days"))
        if since:
            queryset = queryset.filter(timestamp__gte=since)
        return ProductListView._paginate(
            queryset.order_by("-timestamp", "-id"), request, SaleLineItemSerializer
        )


# --------------------------------------------------------------------------
# Analytics
# --------------------------------------------------------------------------

class AnalyticsSummaryView(APIView):
    permission_classes = [IsStoreAuthenticated]

    def get(self, request):
        store = get_request_store(request)
        since = parse_since(request.query_params.get("since") or request.query_params.get("days"))
        sales = SaleTransaction.objects.filter(store=store)
        lines = SaleLineItem.objects.filter(store=store)
        if since:
            sales = sales.filter(timestamp__gte=since)
            lines = lines.filter(timestamp__gte=since)

        totals = sales.aggregate(
            revenue=Sum("grand_total"),
            vat=Sum("vat_tax_amount"),
            orders=Count("id"),
        )
        revenue = float(totals["revenue"] or 0)
        orders = int(totals["orders"] or 0)
        vat = float(totals["vat"] or 0)
        line_sums = lines.aggregate(
            line_total=Sum("line_total"), cost_total=Sum("cost_total")
        )
        profit = float((line_sums["line_total"] or 0) - (line_sums["cost_total"] or 0))

        mix_rows = list(
            sales.values("payment_method")
            .annotate(revenue=Sum("grand_total"), orders=Count("id"))
            .order_by("-revenue")
        )
        for row in mix_rows:
            row["revenue"] = float(row["revenue"] or 0)
            row["share"] = round(row["revenue"] / revenue * 100, 1) if revenue else 0.0

        top_rows = list(
            lines.values("product_name")
            .annotate(units=Sum("quantity"), revenue=Sum("line_total"))
            .order_by("-revenue")[:5]
        )
        for row in top_rows:
            row["units"] = int(row["units"] or 0)
            row["revenue"] = float(row["revenue"] or 0)

        low_qs = Product.objects.filter(
            store=store, stock_quantity__lte=F("min_stock_threshold")
        ).order_by("stock_quantity")[:50]
        low_stock = [
            {
                "id": p.pk,
                "name": p.name,
                "stock": p.stock_quantity,
                "min_threshold": p.min_stock_threshold,
                "suggested_qty": max(0, p.min_stock_threshold * 2 - p.stock_quantity),
            }
            for p in low_qs
        ]
        stock_value = (
            Product.objects.filter(store=store).aggregate(
                value=Sum(F("cost_price") * F("stock_quantity"))
            )["value"]
            or 0
        )

        return Response(
            {
                "store": {"id": store.pk, "name": store.name},
                "range": {"since": since.isoformat() if since else None},
                "totals": {
                    "revenue": round(revenue, 2),
                    "orders": orders,
                    "avg_ticket": round(revenue / orders, 2) if orders else 0.0,
                    "vat_collected": round(vat, 2),
                    "gross_profit": round(profit, 2),
                    "margin_pct": round(profit / revenue * 100, 1) if revenue else 0.0,
                },
                "payment_mix": mix_rows,
                "top_products": top_rows,
                "low_stock": {"count": len(low_stock), "items": low_stock},
                "inventory_value": round(float(stock_value), 2),
            }
        )
