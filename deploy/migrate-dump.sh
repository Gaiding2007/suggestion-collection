#!/bin/bash
# 从 Supabase 把四张表导成 JSON（在服务器上跑）
#
# 用法：SUPABASE_REF=xxx SUPABASE_SECRET=sb_secret_xxx bash deploy/migrate-dump.sh
#
# 注意：secret key 有全库读写权限，别写死在这个文件里，也别提交进 git。
#       迁移完成、确认新后端数据对得上之后，去 Supabase 后台把这个项目整个删掉。
set -u
REF="${SUPABASE_REF:-}"
KEY="${SUPABASE_SECRET:-}"
if [ -z "$REF" ] || [ -z "$KEY" ]; then
  echo "缺少 SUPABASE_REF 或 SUPABASE_SECRET 环境变量"
  exit 1
fi
BASE="https://${REF}.supabase.co"
OUT="${OUT_DIR:-/root/migration}"
mkdir -p "$OUT"

dump() {
  local table="$1"
  local extra="$2"
  local file="$OUT/${table}.json"
  code=$(curl -sS -o "$file" -w '%{http_code}' --max-time 60 \
    -H "apikey: $KEY" -H "Authorization: Bearer $KEY" \
    -H "Accept: application/json" \
    "${BASE}/rest/v1/${table}?select=*${extra}")
  size=$(wc -c < "$file")
  echo "${table}: HTTP ${code}, ${size} bytes"
}

dump profiles ""
dump projects ""
dump project_access ""
dump suggestions ""

echo "--- counts ---"
for t in profiles projects project_access suggestions; do
  n=$(python3 -c "import json,sys;print(len(json.load(open('$OUT/$t.json'))))" 2>/dev/null || echo "?")
  echo "$t rows = $n"
done

echo "--- head of suggestions ---"
head -c 400 "$OUT/suggestions.json"
echo
