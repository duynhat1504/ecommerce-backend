#!/usr/bin/env sh
set -eu

backup_dir="${BACKUP_DIR:-./backups}"
timestamp="$(date -u +%Y%m%dT%H%M%SZ)"

mkdir -p "$backup_dir"

docker compose exec -T postgres sh -c \
  "PGPASSWORD=\"\$POSTGRES_PASSWORD\" pg_dump \
    --username=\"\$POSTGRES_USER\" \
    --format=custom \
    --file=\"/tmp/ecommerce-${timestamp}.dump\" \
    \"\$POSTGRES_DB\""

container_id="$(docker compose ps -q postgres)"
docker cp "${container_id}:/tmp/ecommerce-${timestamp}.dump" \
  "${backup_dir}/ecommerce-${timestamp}.dump"
docker compose exec -T postgres rm -f "/tmp/ecommerce-${timestamp}.dump"

printf 'Backup created: %s\n' "${backup_dir}/ecommerce-${timestamp}.dump"
