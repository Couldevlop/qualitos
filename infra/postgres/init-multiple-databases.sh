#!/bin/bash
# Permet de créer plusieurs bases au démarrage du container Postgres officiel.
# Inspiré du pattern documenté ici : https://github.com/docker-library/postgres/issues/108
#
# Variable d'env attendue : POSTGRES_MULTIPLE_DATABASES="db1,db2,db3"

set -e
set -u

if [ -z "${POSTGRES_MULTIPLE_DATABASES:-}" ]; then
    echo "POSTGRES_MULTIPLE_DATABASES not set, skipping multi-db init."
    exit 0
fi

create_database() {
    local database=$1
    echo "  Creating database '$database'…"
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
        CREATE DATABASE "$database";
        GRANT ALL PRIVILEGES ON DATABASE "$database" TO "$POSTGRES_USER";
EOSQL
}

echo "Multiple database creation requested: $POSTGRES_MULTIPLE_DATABASES"
for db in $(echo "$POSTGRES_MULTIPLE_DATABASES" | tr ',' ' '); do
    create_database "$db"
done
echo "Multiple databases created."
