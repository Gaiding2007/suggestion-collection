#!/usr/bin/env bash
# 在服务器上执行：把公共账号服务装好（幂等，可以重复跑）
# 用法：bash /root/upload/accounts-deploy.sh
set -euo pipefail

APP_DIR=/opt/accounts-api
ENV_FILE=/etc/accounts-api.env
SRC_DIR=/root/upload
NGINX_SITE=/etc/nginx/sites-enabled/suggestion
DB_ROLE=accounts
DB_NAME=accounts

echo "== 1. 目录 =="
mkdir -p "$APP_DIR"
cp -f "$SRC_DIR/accounts-server.js" "$APP_DIR/server.js"
cp -f "$SRC_DIR/accounts-schema.sql" "$APP_DIR/schema.sql"
cp -f "$SRC_DIR/accounts-package.json" "$APP_DIR/package.json"
cd "$APP_DIR"
npm install --omit=dev --silent
echo "deps-ok"

echo "== 2. 账号库（角色 + 数据库）=="
if [ -f "$ENV_FILE" ]; then
  DB_PASS=$(sed -n 's/^DATABASE_URL=postgres:\/\/accounts:\([^@]*\)@.*/\1/p' "$ENV_FILE")
else
  DB_PASS=""
fi
if [ -z "${DB_PASS:-}" ]; then
  DB_PASS=$(openssl rand -hex 18)
fi

sudo -u postgres psql -v ON_ERROR_STOP=1 -tAc \
  "select 1 from pg_roles where rolname = '$DB_ROLE'" | grep -q 1 || \
  sudo -u postgres psql -v ON_ERROR_STOP=1 -c \
  "create role $DB_ROLE login password '$DB_PASS'"

# 密码可能和已存在角色不一致，统一改成这一次的
sudo -u postgres psql -v ON_ERROR_STOP=1 -c \
  "alter role $DB_ROLE with login password '$DB_PASS'" >/dev/null

sudo -u postgres psql -v ON_ERROR_STOP=1 -tAc \
  "select 1 from pg_database where datname = '$DB_NAME'" | grep -q 1 || \
  sudo -u postgres psql -v ON_ERROR_STOP=1 -c \
  "create database $DB_NAME owner $DB_ROLE"

echo "== 3. 环境变量 =="
if [ ! -f "$ENV_FILE" ]; then
  API_SECRET=$(sed -n 's/^API_SECRET=//p' /etc/suggestion-api.env)
  if [ -z "$API_SECRET" ]; then
    echo "拿不到 API_SECRET，退出"
    exit 1
  fi
  cat > "$ENV_FILE" <<EOF
DATABASE_URL=postgres://$DB_ROLE:$DB_PASS@127.0.0.1:5432/$DB_NAME
API_SECRET=$API_SECRET
PORT=3001
EOF
  chmod 600 "$ENV_FILE"
  echo "env-written"
else
  # 已有配置时只确保密码和数据库对得上
  if ! grep -q "^DATABASE_URL=postgres://$DB_ROLE:$DB_PASS@" "$ENV_FILE"; then
    API_SECRET=$(sed -n 's/^API_SECRET=//p' "$ENV_FILE")
    cat > "$ENV_FILE" <<EOF
DATABASE_URL=postgres://$DB_ROLE:$DB_PASS@127.0.0.1:5432/$DB_NAME
API_SECRET=$API_SECRET
PORT=3001
EOF
    chmod 600 "$ENV_FILE"
  fi
  echo "env-kept"
fi

echo "== 4. 建表 =="
set -a
# shellcheck disable=SC1090
. "$ENV_FILE"
set +a
psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f "$APP_DIR/schema.sql" >/dev/null
psql "$DATABASE_URL" -tAc "select count(*) from accounts" | sed 's/^/accounts=/'
psql "$DATABASE_URL" -tAc "select string_agg(slug, ',') from apps" | sed 's/^/apps=/'

echo "== 5. systemd =="
cat > /etc/systemd/system/accounts-api.service <<'UNIT'
[Unit]
Description=Shared Accounts API
After=network.target postgresql.service

[Service]
Type=simple
EnvironmentFile=/etc/accounts-api.env
WorkingDirectory=/opt/accounts-api
ExecStart=/usr/bin/node /opt/accounts-api/server.js
Restart=always
RestartSec=3
User=root

[Install]
WantedBy=multi-user.target
UNIT
systemctl daemon-reload
systemctl enable accounts-api >/dev/null 2>&1 || true
systemctl restart accounts-api
sleep 1
systemctl is-active accounts-api

echo "== 6. nginx /accounts/ =="
python3 - "$NGINX_SITE" <<'PY'
import sys
path = sys.argv[1]
text = open(path, encoding='utf-8').read()
if 'location /accounts/' in text:
    print('nginx-kept')
    sys.exit(0)
block = '''    # 公共账号服务：转给本机 3001 的 Node 服务（deploy/accounts/server.js）
    location /accounts/ {
        proxy_pass http://127.0.0.1:3001;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 30s;
        proxy_connect_timeout 10s;
        client_max_body_size 64k;
    }

'''
marker = '    # 后端接口：转给本机 3000 的 Node 服务'
idx = text.find(marker)
if idx < 0:
    print('nginx-marker-missing')
    sys.exit(1)
open(path, 'w', encoding='utf-8').write(text[:idx] + block + text[idx:])
print('nginx-patched')
PY
nginx -t
systemctl reload nginx

echo "== 7. 自检 =="
curl -sS -m 5 http://127.0.0.1:3001/accounts/health; echo
curl -sS -m 5 -o /dev/null -w 'public=%{http_code}\n' http://127.0.0.1:8080/accounts/health
echo "done"
