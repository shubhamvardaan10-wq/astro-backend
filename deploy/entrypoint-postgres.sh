#!/bin/bash
set -e

BINDIR=$(ls -d /usr/lib/postgresql/*/bin | head -n 1)
export PATH="$BINDIR:$PATH"

if [ ! -s "$PGDATA/PG_VERSION" ]; then
    mkdir -p "$PGDATA"
    chown -R postgres:postgres "$PGDATA"
    chmod 700 "$PGDATA"
    gosu postgres initdb -D "$PGDATA" --auth-local=trust --auth-host=md5
    echo "host all all 0.0.0.0/0 md5" >> "$PGDATA/pg_hba.conf"
    echo "listen_addresses='*'" >> "$PGDATA/postgresql.conf"
    gosu postgres pg_ctl -D "$PGDATA" -o "-c listen_addresses=''" -w start
    gosu postgres psql -v ON_ERROR_STOP=1 --username postgres <<-EOSQL
        CREATE USER $POSTGRES_USER WITH SUPERUSER PASSWORD '$POSTGRES_PASSWORD';
        CREATE DATABASE $POSTGRES_DB OWNER $POSTGRES_USER;
        GRANT ALL PRIVILEGES ON DATABASE $POSTGRES_DB TO $POSTGRES_USER;
EOSQL
    gosu postgres pg_ctl -D "$PGDATA" -m fast -w stop
fi

exec gosu postgres postgres -D "$PGDATA"
