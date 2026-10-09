"""Authentication for the SajiloPOS API.

Two credentials are accepted, in order:

1. Store API key — ``Authorization: Bearer sk_live_…`` (or ``X-API-Key: …``).
   This is what the Android app and the data-pipeline use. It authenticates as
   the store *owner* and carries the Store as ``request.auth``, so every view
   is automatically scoped to exactly one store.
2. DRF token / session — for staff dashboards and the browsable API / admin.

Unauthenticated access is rejected everywhere except the explicitly public
endpoints (health check, register, login).
"""

from rest_framework import authentication, exceptions, permissions

from .models import Store


def _extract_key(request):
    header = request.headers.get("Authorization", "")
    if header.lower().startswith("bearer "):
        return header[7:].strip()
    return (request.headers.get("X-API-Key") or "").strip()


class StoreAPIKeyAuthentication(authentication.BaseAuthentication):
    def authenticate(self, request):
        key = _extract_key(request)
        if not key:
            return None
        try:
            store = Store.objects.select_related("owner").get(api_key=key)
        except Store.DoesNotExist:
            raise exceptions.AuthenticationFailed("Invalid store API key.")
        if not store.owner.is_active:
            raise exceptions.AuthenticationFailed("Store owner is disabled.")
        return (store.owner, store)

    def authenticate_header(self, request):
        return 'Bearer realm="sajilopos"'


class IsStoreAuthenticated(permissions.BasePermission):
    """Any authenticated credential (store key, token or session)."""

    message = "Authentication required."

    def has_permission(self, request, view):
        user = getattr(request, "user", None)
        return bool(user and user.is_authenticated)
