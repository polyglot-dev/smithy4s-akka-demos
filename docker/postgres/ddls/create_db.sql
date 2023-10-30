
SELECT 'CREATE DATABASE service_database'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'service_database')\gexec

\c service_database postgres;
