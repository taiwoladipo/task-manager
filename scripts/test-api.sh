#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
TASKS_URL="$BASE_URL/api/tasks"
PROJECTS_URL="$BASE_URL/api/projects"

HTTP_STATUS=""
HTTP_ETAG=""
HTTP_BODY=""

log() {
  printf "\n==> %s\n" "$1"
}

fail() {
  printf "\nERROR: %s\n" "$1" >&2
  exit 1
}

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || fail "Missing required command: $1"
}

extract_first_id() {
  local json="$1"
  local compact
  compact="$(printf '%s' "$json" | tr -d '\n\r')"
  printf '%s' "$compact" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4
}

extract_first_project_id() {
  local json="$1"
  local compact
  compact="$(printf '%s' "$json" | tr -d '\n\r')"
  printf '%s' "$compact" | grep -o '"projects":\[{"id":"[^"]*"' | head -1 | cut -d'"' -f6
}

request() {
  local method="$1"
  local url="$2"
  local body="${3:-}"
  local header_name="${4:-}"
  local header_value="${5:-}"

  local hdr_file body_file
  hdr_file="$(mktemp)"
  body_file="$(mktemp)"

  local curl_cmd=(curl -sS -X "$method" "$url" -D "$hdr_file" -o "$body_file")
  if [[ -n "$header_name" ]]; then
    curl_cmd+=( -H "$header_name: $header_value" )
  fi
  if [[ -n "$body" ]]; then
    curl_cmd+=( -H "Content-Type: application/json" -d "$body" )
  fi

  "${curl_cmd[@]}"

  HTTP_STATUS="$(awk 'NR==1 {print $2}' "$hdr_file")"
  HTTP_ETAG="$(awk 'BEGIN{IGNORECASE=1} /^ETag:/ {sub(/\r$/, "", $2); print $2}' "$hdr_file" | tail -1)"
  HTTP_BODY="$(cat "$body_file")"

  rm -f "$hdr_file" "$body_file"
}

require_cmd curl

log "GET /api/tasks"
request GET "$TASKS_URL"
[[ "$HTTP_STATUS" == "200" ]] || fail "GET /api/tasks failed with status $HTTP_STATUS"

log "GET /api/projects"
request GET "$PROJECTS_URL"
[[ "$HTTP_STATUS" == "200" ]] || fail "GET /api/projects failed with status $HTTP_STATUS"

DEFAULT_PROJECT_ID="$(extract_first_project_id "$HTTP_BODY")"
[[ -n "$DEFAULT_PROJECT_ID" ]] || fail "No project found in GET /api/projects"

log "POST /api/projects"
project_name="API Test Project $(date +%s)"
project_payload="{\"name\":\"$project_name\"}"
request POST "$PROJECTS_URL" "$project_payload"
[[ "$HTTP_STATUS" == "201" ]] || fail "POST /api/projects failed with status $HTTP_STATUS"

NEW_PROJECT_ID="$(extract_first_id "$HTTP_BODY")"
[[ -n "$NEW_PROJECT_ID" ]] || fail "Project creation response did not contain id"

log "POST /api/tasks"
stamp="$(date +%s)"
create_payload="{\"title\":\"API Test Task $stamp\",\"author\":\"CLI Tester\",\"description\":\"Created by scripts/test-api.sh\",\"project\":{\"id\":\"$DEFAULT_PROJECT_ID\"},\"status\":\"pending\"}"
request POST "$TASKS_URL" "$create_payload"
[[ "$HTTP_STATUS" == "201" ]] || fail "POST /api/tasks failed with status $HTTP_STATUS"

TASK_ID="$(extract_first_id "$HTTP_BODY")"
[[ -n "$TASK_ID" ]] || fail "Task creation response did not contain id"

log "GET /api/tasks/{id} (expect ETag)"
request GET "$TASKS_URL/$TASK_ID"
[[ "$HTTP_STATUS" == "200" ]] || fail "GET /api/tasks/{id} failed with status $HTTP_STATUS"
[[ -n "$HTTP_ETAG" ]] || fail "GET /api/tasks/{id} did not return ETag"
ORIGINAL_ETAG="$HTTP_ETAG"

log "PUT /api/tasks/{id} with matching ETag"
update_payload="{\"title\":\"API Test Task $stamp\",\"author\":\"CLI Tester\",\"description\":\"Updated by scripts/test-api.sh\",\"project\":{\"id\":\"$DEFAULT_PROJECT_ID\"},\"status\":\"in-progress\"}"
request PUT "$TASKS_URL/$TASK_ID" "$update_payload" "ETag" "$ORIGINAL_ETAG"
[[ "$HTTP_STATUS" == "200" ]] || fail "PUT /api/tasks/{id} with valid ETag failed with status $HTTP_STATUS"
[[ -n "$HTTP_ETAG" ]] || fail "PUT /api/tasks/{id} did not return ETag"
[[ "$HTTP_ETAG" != "$ORIGINAL_ETAG" ]] || fail "PUT /api/tasks/{id} returned unchanged ETag"

log "PUT /api/tasks/{id} with stale ETag (expect 409)"
stale_payload="{\"title\":\"API Test Task $stamp\",\"author\":\"CLI Tester\",\"description\":\"Stale update attempt\",\"project\":{\"id\":\"$DEFAULT_PROJECT_ID\"},\"status\":\"completed\"}"
request PUT "$TASKS_URL/$TASK_ID" "$stale_payload" "ETag" "$ORIGINAL_ETAG"
[[ "$HTTP_STATUS" == "409" ]] || fail "PUT with stale ETag expected 409, got $HTTP_STATUS"

log "GET /api/tasks?status=pending"
request GET "$TASKS_URL?status=pending"
[[ "$HTTP_STATUS" == "200" ]] || fail "GET /api/tasks?status=pending failed with status $HTTP_STATUS"

log "DELETE /api/tasks/{id}"
request DELETE "$TASKS_URL/$TASK_ID"
[[ "$HTTP_STATUS" == "200" ]] || fail "DELETE /api/tasks/{id} failed with status $HTTP_STATUS"

log "All endpoint checks passed"
printf "Created project id: %s\n" "$NEW_PROJECT_ID"
printf "Created/deleted task id: %s\n" "$TASK_ID"


