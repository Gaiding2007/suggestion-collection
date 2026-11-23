#!/bin/bash
# 在服务器上部署接口服务（幂等：重复跑也安全）
set -eu
export DEBIAN_FRONTEND=noninteractive

echo "== 1. Node.js =="
if ! command -v node >/dev/null 2>&1; then
  apt-get install -y -qq nodejs npm >/dev/null 2>&1
fi
echo "node $(node -v)，npm $(npm -v)"

echo "== 2. 放代码 =="
mkdir -p /opt/suggestion-api
cp -f /root/upload/server.js /root/upload/package.json /root/upload/schema.sql /opt/suggestion-api/
cd /opt/suggestion-api

echo "== 3. 装依赖 =="
npm install --omit=dev --no-audit --no-fund --silent >/dev/null 2>&1
ls node_modules | head -5

echo "== 4. 建表 =="
DATABASE_URL=$(sed -n 's/^DATABASE_URL=//p' /root/.suggestion-db.env)
psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f /opt/suggestion-api/schema.sql >/dev/null
echo "tables: $(psql "$DATABASE_URL" -tAc "select string_agg(tablename, ', ' order by tablename) from pg_tables where schemaname='public'")"

echo "== 5. 环境变量 =="
if [ ! -f /etc/suggestion-api.env ]; then
  SECRET=$(openssl rand -hex 32)
  printf 'DATABASE_URL=%s\nAPI_SECRET=%s\nPORT=3000\n' "$DATABASE_URL" "$SECRET" > /etc/suggestion-api.env
  chmod 600 /etc/suggestion-api.env
  echo "已生成 /etc/suggestion-api.env"
else
  echo "/etc/suggestion-api.env 已存在，不覆盖"
fi

echo "== 6. systemd =="
cat > /etc/systemd/system/suggestion-api.service <<'UNIT'
[Unit]
Description=Suggestion Collection API
After=network.target postgresql.service

[Service]
Type=simple
EnvironmentFile=/etc/suggestion-api.env
WorkingDirectory=/opt/suggestion-api
ExecStart=/usr/bin/node /opt/suggestion-api/server.js
Restart=always
RestartSec=3
User=root

[Install]
WantedBy=multi-user.target
UNIT
systemctl daemon-reload
systemctl enable suggestion-api >/dev/null 2>&1
systemctl restart suggestion-api

sleep 2
echo "== 7. 自检 =="
systemctl is-active suggestion-api
curl -sS --max-time 10 http://127.0.0.1:3000/api/health; echo
journalctl -u suggestion-api -n 8 --no-pager
