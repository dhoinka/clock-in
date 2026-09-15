default: dev

# Start the services needed for local development
dev:
    docker compose -f docker-compose.dev.yml up -d
