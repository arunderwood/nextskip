# Database Configuration

NextSkip uses PostgreSQL 18 for persistent data storage.

> **Adding a new data source?** See [PERSISTENCE_REFERENCE.md](PERSISTENCE_REFERENCE.md) for reference examples.

## Local Development

### Docker Compose (Recommended)

```bash
# Start PostgreSQL
docker-compose up -d

# Verify it's running
docker-compose ps

# View logs
docker-compose logs -f postgres

# Stop
docker-compose down

# Stop and remove data
docker-compose down -v
```

### Manual PostgreSQL

If you prefer to run PostgreSQL locally without Docker:

```bash
# Create database and user
createdb nextskip
createuser nextskip -P  # Set password: nextskip
psql -c "GRANT ALL PRIVILEGES ON DATABASE nextskip TO nextskip;"
```

### Connection Details

| Setting  | Default Value                             |
| -------- | ----------------------------------------- |
| Host     | localhost                                 |
| Port     | 5432                                      |
| Database | nextskip                                  |
| Username | nextskip                                  |
| Password | nextskip                                  |
| JDBC URL | jdbc:postgresql://localhost:5432/nextskip |

## Running the Application

```bash
# Start database
docker-compose up -d

# Start application
./gradlew bootRun
```

## Liquibase Migrations

Migrations are in `src/main/resources/db/changelog/migrations/`.

### Naming Convention

```
XXX-description.yaml
```

Where `XXX` is a zero-padded sequence number (001, 002, etc.).

### Viewing Migration Status

```bash
./gradlew liquibaseStatus
```

### Rolling Back (Development Only)

```bash
./gradlew liquibaseRollbackCount -PliquibaseCommandValue=1
```

### Generating Diff (Future Entity Changes)

After adding/modifying JPA entities:

```bash
docker-compose up -d
./gradlew diffChangeLog
```

Review generated changelog in `src/main/resources/db/changelog/`.

## Render Deployment

### PostgreSQL Setup on Render

1. Go to [Render Dashboard](https://dashboard.render.com/)
2. Click **New** > **PostgreSQL**
3. Configure:
   - **Name**: `nextskip-db`
   - **Database**: `nextskip`
   - **User**: (auto-generated)
   - **Region**: Same as web service (e.g., Oregon)
   - **Plan**: Starter ($7/mo) or higher
4. Click **Create Database**
5. Copy the **Internal Database URL** (for same-region services)

### Environment Variables

Set these in your Render web service:

| Variable          | Source                                                                    |
| ----------------- | ------------------------------------------------------------------------- |
| `DATABASE_URL`    | Internal Database URL from Render (modify to JDBC format, add sslmode)    |
| `DATABASE_USERNAME` | Username from Render PostgreSQL dashboard                                |
| `DATABASE_PASSWORD` | Password from Render PostgreSQL dashboard                                |

**DATABASE_URL format** (convert from Render's Internal URL):

Render provides: `postgres://user:pass@host:5432/dbname`

Convert to JDBC: `jdbc:postgresql://host:5432/dbname?sslmode=require`

### SSL Configuration

Render PostgreSQL requires SSL. The connection string must include `?sslmode=require`.

If using the Internal Database URL (recommended for same-region), SSL is handled automatically.

## Testing

### Integration Tests

Testcontainers automatically provisions PostgreSQL for tests:

```bash
./gradlew test --tests DatabaseHealthIntegrationTest
```

No Docker Compose required for tests - Testcontainers handles container lifecycle.

### Verifying Database Health

The `/actuator/health` endpoint includes database status:

```bash
curl http://localhost:8080/actuator/health | jq '.components.db'
```

## Troubleshooting

### Connection Refused

1. Verify PostgreSQL is running: `docker-compose ps`
2. Check port 5432 is not in use: `lsof -i :5432`
3. Restart container: `docker-compose restart postgres`

### Migration Failures

1. Check Liquibase logs in application output
2. Verify `db.changelog-master.yaml` syntax
3. Check for conflicting changesets

### Render Connection Issues

1. Verify Internal Database URL (not External)
2. Confirm `?sslmode=require` in JDBC URL
3. Check Render PostgreSQL instance is not suspended (Starter plan suspends after inactivity)

### Test Failures with Testcontainers

1. Ensure Docker is running
2. Check Docker has sufficient resources
3. Verify `@ActiveProfiles("test")` is present on test class
