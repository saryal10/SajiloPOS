#!/usr/bin/env bash
#
# SajiloPOS backend API — one command for local development.
#
#   ./start.sh                 migrate -> create admin (if new) -> runserver
#   ./start.sh --no-server      migrate only (CI / containers)
#   ./start.sh --reinstall      rebuild the virtualenv from scratch
#
# Configuration lives in .env (copy .env.example first). Without POSTGRES_*
# variables the API runs on a local SQLite file — no infrastructure needed.
set -euo pipefail

cd "$(dirname "$0")"

NO_SERVER=0
REINSTALL=0

while [ $# -gt 0 ]; do
  case "$1" in
    --no-server) NO_SERVER=1; shift ;;
    --reinstall) REINSTALL=1; shift ;;
    -h|--help) sed -n '2,9p' "$0"; exit 0 ;;
    *) echo "Unknown option: $1 (try --help)"; exit 1 ;;
  esac
done

if ! command -v python3 >/dev/null 2>&1; then
  echo "ERROR: python3 is required (3.10+)." >&2
  exit 1
fi

if [ "$REINSTALL" -eq 1 ] && [ -d .venv ]; then
  echo "Removing old virtualenv..."
  rm -rf .venv
fi
if [ ! -x .venv/bin/python ]; then
  echo "Creating virtualenv (.venv)..."
  python3 -m venv .venv
fi
PY=.venv/bin/python

if [ ! -f .venv/.installed ] || [ "$REINSTALL" -eq 1 ]; then
  echo "Installing dependencies..."
  "$PY" -m pip install --upgrade pip -q
  "$PY" -m pip install -q -r requirements.txt
  touch .venv/.installed
fi

echo "== Migrating database =="
"$PY" manage.py migrate --noinput -v 0

if [ ! -f .venv/.admin_created ] && [ "${DJANGO_SUPERUSER:-}" != "" ]; then
  echo "Creating superuser $DJANGO_SUPERUSER..."
  "$PY" manage.py createsuperuser --noinput \
    --username "$DJANGO_SUPERUSER" --email "${DJANGO_SUPERUSER_EMAIL:-admin@example.com}" || true
  touch .venv/.admin_created
fi

if [ "$NO_SERVER" -eq 1 ]; then
  echo "Done (server not started)."
  exit 0
fi

echo "== Starting API at http://127.0.0.1:8000 (Ctrl+C to stop) =="
exec "$PY" manage.py runserver 127.0.0.1:8000
