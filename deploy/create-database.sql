SELECT 'CREATE DATABASE openlinkhub_iot'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'openlinkhub_iot')\gexec
