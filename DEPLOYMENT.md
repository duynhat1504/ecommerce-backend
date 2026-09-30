# Production deployment

## One-time server preparation

1. Point `API_DOMAIN` DNS to the server's public IPv4/IPv6 address and allow inbound TCP 80/443.
2. Copy `.env.example` to `.env` on the server and replace every placeholder with a production value. Generate a separate, long random value for `JWT_SECRET` and `REDIS_PASSWORD`.
3. In Google Cloud Console, register `https://<API_DOMAIN>/login/oauth2/code/google` as the authorized redirect URI.
4. If the frontend is on a different site, set `AUTH_REFRESH_COOKIE_SAME_SITE=None`; otherwise keep `Lax`.
5. If VNPay is enabled, configure all four `VNPAY_*` variables with production credentials and the public return URL.

## Deploy an image

Run this from the directory that contains `.env`, `docker-compose.yml`, `docker-compose.prod.yml`, and `Caddyfile`:

```sh
IMAGE_TAG=<git-commit-sha> docker compose -f docker-compose.yml -f docker-compose.prod.yml pull backend
IMAGE_TAG=<git-commit-sha> docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --remove-orphans
```

Verify the service after DNS and TLS have propagated:

```sh
curl --fail https://<API_DOMAIN>/actuator/health
```

## Backup and rollback

Before deploying a release that includes database migrations, run:

```sh
chmod +x scripts/backup-postgres.sh
./scripts/backup-postgres.sh
```

To roll back the application image, redeploy a previously successful `IMAGE_TAG`. Do not roll back database migrations without a separately reviewed database restore plan.

## GitHub Actions secrets

The deploy job requires these repository secrets:

- `DEPLOY_HOST`
- `DEPLOY_USER`
- `DEPLOY_SSH_KEY`
- `DEPLOY_PATH` (absolute path to this deployment directory on the server)

It also requires the repository variable `DOCKERHUB_USERNAME` and secret `DOCKERHUB_TOKEN`, which were already used by the image publishing job.
