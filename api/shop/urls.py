from django.urls import path

from . import views

urlpatterns = [
    # Public
    path("health/", views.HealthView.as_view(), name="health"),
    path("auth/register/", views.RegisterView.as_view(), name="register"),
    path("auth/login/", views.LoginView.as_view(), name="login"),
    # Store self-service
    path("stores/me/", views.StoreMeView.as_view(), name="store-me"),
    path("stores/regenerate-key/", views.RegenerateKeyView.as_view(), name="regenerate-key"),
    # Catalog
    path("products/", views.ProductListView.as_view(), name="product-list"),
    path("products/<int:pk>/", views.ProductDetailView.as_view(), name="product-detail"),
    # Sales (transactions/ is an alias kept for pipeline compatibility)
    path("sales/", views.SaleListView.as_view(), name="sale-list"),
    path("sales/<int:pk>/", views.SaleDetailView.as_view(), name="sale-detail"),
    path("transactions/", views.SaleListView.as_view(), name="transaction-list"),
    path("transactions/<int:pk>/", views.SaleDetailView.as_view(), name="transaction-detail"),
    # Line items (sale_items/ and lines/ are aliases for pipeline compatibility)
    path("sale-items/", views.SaleLineListView.as_view(), name="sale-item-list"),
    path("sale_items/", views.SaleLineListView.as_view(), name="sale-item-list-alt"),
    path("lines/", views.SaleLineListView.as_view(), name="line-list"),
    # Analytics
    path("analytics/summary/", views.AnalyticsSummaryView.as_view(), name="analytics-summary"),
]
