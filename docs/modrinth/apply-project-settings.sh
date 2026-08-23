#!/usr/bin/env bash
# Apply the canonical Modrinth project metadata in this directory to the live project.
#
# This only touches project-level settings (description, body, categories, links,
# license, environment). It never uploads a version and never changes publication
# status; use the tagged release workflow for that.
#
# Usage: MODRINTH_TOKEN=mrp_... docs/modrinth/apply-project-settings.sh
set -euo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
: "${MODRINTH_TOKEN:?MODRINTH_TOKEN must be set (do not commit it)}"

project_id="$(python3 -c "import json;print(json.load(open('$here/project-settings.json'))['project_id'])")"

payload="$(python3 - "$here" <<'PY'
import json, sys
here = sys.argv[1]
settings = json.load(open(f"{here}/project-settings.json"))
body = open(f"{here}/description.md").read()
summary = open(f"{here}/summary.txt").read().strip()
print(json.dumps({
    "title": settings["title"],
    "description": summary,
    "body": body,
    "categories": settings["categories"],
    "additional_categories": settings["additional_categories"],
    "client_side": settings["client_side"],
    "server_side": settings["server_side"],
    "license_id": settings["license_id"],
    "license_url": settings["license_url"],
    "source_url": settings["source_url"],
    "issues_url": settings["issues_url"],
}))
PY
)"

code="$(curl -sS -o /tmp/modrinth-patch-response -w '%{http_code}' \
  -X PATCH "https://api.modrinth.com/v2/project/${project_id}" \
  -H "Authorization: ${MODRINTH_TOKEN}" \
  -H "Content-Type: application/json" \
  -H "User-Agent: BiomeTunes/release-tooling (github.com/BiomeTunes/datapack)" \
  -d "${payload}")"

if [ "$code" != "204" ]; then
  echo "Modrinth PATCH failed with HTTP ${code}:" >&2
  cat /tmp/modrinth-patch-response >&2
  exit 1
fi
echo "Applied project settings to ${project_id}."

# The icon is a separate endpoint (raw image body, not JSON).
icon="$(python3 -c "import json;print(json.load(open('$here/project-settings.json'))['icon'])")"
icon_path="$here/../../$icon"
code="$(curl -sS -o /tmp/modrinth-icon-response -w '%{http_code}' \
  -X PATCH "https://api.modrinth.com/v2/project/${project_id}/icon?ext=png" \
  -H "Authorization: ${MODRINTH_TOKEN}" \
  -H "Content-Type: image/png" \
  -H "User-Agent: BiomeTunes/release-tooling (github.com/BiomeTunes/datapack)" \
  --data-binary "@${icon_path}")"

if [ "$code" != "204" ]; then
  echo "Modrinth icon upload failed with HTTP ${code}:" >&2
  cat /tmp/modrinth-icon-response >&2
  exit 1
fi
echo "Applied icon from ${icon}."
