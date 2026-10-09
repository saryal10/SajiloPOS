#!/usr/bin/env bash
#
# SajiloPOS data pipeline — one command to rule it all.
#
#   ./start.sh                  fetch -> analyze -> launch dashboard
#   ./start.sh --no-dashboard   fetch -> analyze only (headless/CI)
#   ./start.sh --reinstall      rebuild the virtualenv from scratch
#   ./start.sh --db PATH        use a Room .db file (overrides .env)
#   ./start.sh --api-url URL    use the API (overrides .env; needs SAJILO_API_KEY)
#
# Configuration lives in .env (copy .env.example first). The API key is read
# from the environment or .env — never pass it on the command line in shared
# terminals if you can avoid it.
set -euo pipefail

cd "$(dirname "$0")"

NO_DASHBOARD=0
REINSTALL=0
EXTRA_ARGS=()

while [ $# -gt 0 ]; do
  case "$1" in
    --no-dashboard) NO_DASHBOARD=1; shift ;;
    --reinstall) REINSTALL=1; shift ;;
    --db) EXTRA_ARGS+=(--db "$2"); shift 2 ;;
    --api-url) EXTRA_ARGS+=(--api-url "$2"); shift 2 ;;
    --api-key) EXTRA_ARGS+=(--api-key "$2"); shift 2 ;;
    -h|--help) sed -n '2,14p' "$0"; exit 0 ;;
    *) echo "Unknown option: $1 (try --help)"; exit 1 ;;
  esac
done

if ! command -v python3 >/dev/null 2>&1; then
  echo "ERROR: python3 is required (3.10+). See README.md." >&2
  exit 1
fi

# --- virtualenv ------------------------------------------------------------
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
  echo "Installing dependencies (first run takes a few minutes)..."
  "$PY" -m pip install --upgrade pip -q
  "$PY" -m pip install -q -r requirements.txt
  touch .venv/.installed
fi

# --- load .env (optional) --------------------------------------------------
if [ -f .env ]; then
  set -a
  # shellcheck disable=SC1091
  . ./.env
  set +a
fi

# --- pipeline --------------------------------------------------------------
echo "== Step 1/2: fetching data =="
"$PY" fetch_data.py "${EXTRA_ARGS[@]}"

echo "== Step 2/2: analyzing =="
"$PY" analyze.py

if [ "$NO_DASHBOARD" -eq 1 ]; then
  echo "Done. Charts in ./output/, tables in ./data/processed/."
  exit 0
fi

echo "== Launching dashboard (Ctrl+C to stop) =="
exec "$PY" -m streamlit run dashboard.py
