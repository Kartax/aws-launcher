# Kartax AWS Launcher

Be sure to provide following environment variables:

+ AWS_ACCESS_KEY_ID
+ AWS_SECRET_ACCESS_KEY
+ AWS_REGION
+ AWS_BUDGET_ACCOUNT_ID
+ AWS_BUDGET_NAME

## docker run
```
docker run -d -p 8080:8080 \
  -e AWS_ACCESS_KEY_ID="my-key" \
  -e AWS_SECRET_ACCESS_KEY="my-secret" \
  -e AWS_REGION="eu-central-1" \
  -e AWS_BUDGET_ACCOUNT_ID="12345678" \
  -e AWS_BUDGET_NAME="my budget" \
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
      - AWS_BUDGET_ACCOUNT_ID="12345678
      - AWS_BUDGET_NAME="my budget"
    ports:
      - "8080:8080"

```
