
SELECT 'CREATE DATABASE service_database_test'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'service_database_test')\gexec

\c service_database_test postgres;
