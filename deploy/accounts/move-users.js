'use strict';

// 一次性迁移：把「建议收集」库里的 users 搬进公共账号库 accounts。
// 保留原来的 UUID，所以历史项目权限、建议归属都不用改。
//
// 用法（在服务器上，两个库的连接串从各自的环境变量文件里取）：
//   SRC_DATABASE_URL=... DST_DATABASE_URL=... NODE_PATH=/opt/accounts-api/node_modules node move-users.js

const { Client } = require('pg');

(async () => {
  const srcUrl = process.env.SRC_DATABASE_URL;
  const dstUrl = process.env.DST_DATABASE_URL;
  if (!srcUrl || !dstUrl) {
    console.error('需要 SRC_DATABASE_URL 和 DST_DATABASE_URL');
    process.exit(1);
  }

  const src = new Client({ connectionString: srcUrl });
  const dst = new Client({ connectionString: dstUrl });
  await src.connect();
  await dst.connect();

  const { rows } = await src.query(
    'select id, email, password_hash, display_name, created_at from users order by created_at',
  );
  console.log('源库用户数:', rows.length);

  let moved = 0;
  for (const r of rows) {
    await dst.query(
      `insert into accounts (id, email, password_hash, display_name, created_at)
       values ($1, $2, $3, $4, $5)
       on conflict (id) do update set
         email = excluded.email,
         password_hash = coalesce(accounts.password_hash, excluded.password_hash),
         display_name = coalesce(accounts.display_name, excluded.display_name)`,
      [r.id, r.email, r.password_hash, r.display_name, r.created_at],
    );
    moved += 1;
    console.log('  -', r.email, r.password_hash ? '(有密码)' : '(无密码)');
  }

  const total = await dst.query('select count(*)::int as n from accounts');
  console.log('目标库账号数:', total.rows[0].n, '/ 本次处理:', moved);

  await src.end();
  await dst.end();
})().catch((err) => {
  console.error('迁移失败:', err.message);
  process.exit(1);
});
