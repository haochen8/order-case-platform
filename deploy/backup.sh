#!/usr/bin/env bash
# Run from any directory. Creates both dumps with restrictive permissions.
set -euo pipefail
cd "$(dirname "$0")"
umask 077
stamp=$(date -u +%Y%m%dT%H%M%SZ)
backup_dir="../backups/$stamp"
mkdir -p ../backups
# Refuse to overwrite a backup if two runs start in the same second.
mkdir "$backup_dir"
compose=(docker compose --env-file .env -f compose.yml)
"${compose[@]}" exec -T app-db pg_dump -U caseuser -d casedb -Fc > "$backup_dir/cases.dump.partial"
mv "$backup_dir/cases.dump.partial" "$backup_dir/cases.dump"
"${compose[@]}" exec -T identity-db pg_dump -U keycloak -d keycloak -Fc > "$backup_dir/identity.dump.partial"
mv "$backup_dir/identity.dump.partial" "$backup_dir/identity.dump"
# A dump is not proven until restored. Encrypt and copy this directory off-host.
printf 'Created %s (restore verification and encrypted off-host copy still required)\n' "$backup_dir"
