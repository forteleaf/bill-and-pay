#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE EXTENSION IF NOT EXISTS ltree;
    
    CREATE DATABASE billpay;
EOSQL

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "billpay" <<-EOSQL
    CREATE EXTENSION IF NOT EXISTS ltree;
EOSQL

echo "PostgreSQL initialization completed"
