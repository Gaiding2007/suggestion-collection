'use strict';

/**
 * 把 Supabase 导出的 JSON（/root/migration/*.json）灌进自己的 PostgreSQL。
 * 用法：DATABASE_URL=... node import.js /root/migration
 * 说明：老账号先标成未认领（claimed=false），用户第一次「注册」同一邮箱时认领，
 *      这样历史建议的 author_id 不用改。
 */

const fs = require('fs');
const path = require('path');
const { Pool } = require('pg');

const dir = process.argv[2] || '/root/migration';
const pool = new Pool({ connectionString: process.env.DATABASE_URL });

const read = (name) => {
  const p = path.join(dir, `${name}.json`);
  if (!fs.existsSync(p)) return [];
  return JSON.parse(fs.readFileSync(p, 'utf8'));
};

async function main() {
  const profiles = read('profiles');
  const projects = read('projects');
  const access = read('project_access');
  const suggestions = read('suggestions');

  console.log(`读入：profiles=${profiles.length} projects=${projects.length} access=${access.length} suggestions=${suggestions.length}`);

  const client = await pool.connect();
  const stats = { users: 0, projects: 0, access: 0, suggestions: 0, skipped: [] };

  try {
    await client.query('begin');

    for (const p of profiles) {
      await client.query(
        `insert into users (id, email, display_name, is_admin, claimed, created_at)
         values ($1, $2, $3, $4, false, coalesce($5, now()))
         on conflict (id) do update
           set email = excluded.email,
               display_name = excluded.display_name,
               is_admin = excluded.is_admin`,
        [p.id, p.email, p.display_name, !!p.is_admin, p.created_at],
      );
      stats.users += 1;
    }

    const userIds = new Set(
      (await client.query('select id from users')).rows.map((r) => r.id),
    );

    for (const p of projects) {
      await client.query(
        `insert into projects (id, name, description, created_by, created_at, is_archived)
         values ($1, $2, $3, $4, coalesce($5, now()), $6)
         on conflict (id) do update
           set name = excluded.name,
               description = excluded.description,
               is_archived = excluded.is_archived`,
        [
          p.id,
          p.name,
          p.description || null,
          userIds.has(p.created_by) ? p.created_by : null,
          p.created_at,
          !!p.is_archived,
        ],
      );
      stats.projects += 1;
    }

    const projectIds = new Set(
      (await client.query('select id from projects')).rows.map((r) => r.id),
    );

    for (const a of access) {
      if (!projectIds.has(a.project_id) || !userIds.has(a.user_id)) {
        stats.skipped.push(`access ${a.project_id}/${a.user_id}`);
        continue;
      }
      await client.query(
        `insert into project_access (project_id, user_id, granted_at)
         values ($1, $2, coalesce($3, now()))
         on conflict do nothing`,
        [a.project_id, a.user_id, a.granted_at],
      );
      stats.access += 1;
    }

    const admin = await client.query(
      'select id from users where is_admin = true order by created_at limit 1',
    );
    const fallbackAuthor = admin.rowCount ? admin.rows[0].id : null;

    for (const s of suggestions) {
      if (!projectIds.has(s.project_id)) {
        stats.skipped.push(`suggestion ${s.id}（项目不存在）`);
        continue;
      }
      const author = userIds.has(s.author_id) ? s.author_id : fallbackAuthor;
      if (!author) {
        stats.skipped.push(`suggestion ${s.id}（作者不存在且没有管理员兜底）`);
        continue;
      }
      await client.query(
        `insert into suggestions (
            id, project_id, author_id, title, content, status, reject_reason,
            pushed_version, pushed_note, handled_by, created_at,
            accepted_at, rejected_at, resolved_at, pushed_at)
         values ($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,coalesce($11, now()),$12,$13,$14,$15)
         on conflict (id) do update
           set status = excluded.status,
               reject_reason = excluded.reject_reason,
               pushed_version = excluded.pushed_version,
               pushed_note = excluded.pushed_note,
               accepted_at = excluded.accepted_at,
               rejected_at = excluded.rejected_at,
               resolved_at = excluded.resolved_at,
               pushed_at = excluded.pushed_at,
               handled_by = excluded.handled_by`,
        [
          s.id,
          s.project_id,
          author,
          s.title,
          s.content || null,
          s.status,
          s.reject_reason || null,
          s.pushed_version || null,
          s.pushed_note || null,
          userIds.has(s.handled_by) ? s.handled_by : null,
          s.created_at,
          s.accepted_at,
          s.rejected_at,
          s.resolved_at,
          s.pushed_at,
        ],
      );
      stats.suggestions += 1;
    }

    await client.query('commit');
  } catch (err) {
    await client.query('rollback');
    throw err;
  } finally {
    client.release();
  }

  console.log('导入完成：', stats);
  const counts = await pool.query(`
    select
      (select count(*) from users) as users,
      (select count(*) from projects) as projects,
      (select count(*) from project_access) as access,
      (select count(*) from suggestions) as suggestions,
      (select count(*) from users where is_admin) as admins,
      (select count(*) from users where claimed) as claimed
  `);
  console.log('库里现在：', counts.rows[0]);
  await pool.end();
}

main().catch((err) => {
  console.error('导入失败：', err);
  process.exit(1);
});
