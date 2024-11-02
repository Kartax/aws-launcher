# Kartax AWS Launcher

Be sure to provide following environment variables:

+ AWS_ACCESS_KEY_ID
+ AWS_SECRET_ACCESS_KEY
+ AWS_REGION

## docker run
```
docker run -d -p 8080:8080 \
  -e AWS_ACCESS_KEY_ID="my-key" \
  -e AWS_SECRET_ACCESS_KEY="my-secret" \
  -e AWS_REGION="eu-central-1" \
  ghcr.io/<username>/<repository>:latest
```

## docker compose
```
version: '3.8'

services:
  app:
    image: ghcr.io/<username>/<repository>:latest
    environment:
      - AWS_ACCESS_KEY_ID="my-key"
      - AWS_SECRET_ACCESS_KEY="my-secret"
      - AWS_REGION="eu-central-1"
    ports:
      - "8080:8080"

```
