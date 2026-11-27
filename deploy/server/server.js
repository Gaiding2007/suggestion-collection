'use strict';

/**
 * 建议收集 App 的后端接口
 *
 * 跑在自己的服务器上：只监听 127.0.0.1:3000，外网通过 nginx 的 /api/ 转发进来。
 * 数据库：PostgreSQL（deploy/server/schema.sql），连接串在环境变量 DATABASE_URL。
 *
 * 0.0.10 起登录交给公共账号服务（deploy/accounts，端口 3001）：
 *   - 账号、密码、注册登录都在那边，本服务不再存密码
 *   - 这里只校验它签发的 token（两边共用 API_SECRET），然后按 UUID 找本应用的成员
 *   - 成员表 members 只放「本应用里的这个人」：管理员身份、管理员备注
 */

const http = require('http');
const crypto = require('crypto');
const fs = require('fs');
const path = require('path');
const { Pool } = require('pg');

const PORT = Number(process.env.PORT || 3000);
const DATABASE_URL = process.env.DATABASE_URL;
const SECRET = process.env.API_SECRET || '';

// 图片存这儿，nginx 把 /var/www/suggestion 当站点根，所以上传目录是 /uploads/
const SITE_ROOT = process.env.SITE_ROOT || '/var/www/suggestion';
const UPLOAD_DIR = path.join(SITE_ROOT, 'uploads');
const MAX_IMAGES = 3;
const MAX_IMAGE_BYTES = 5 * 1024 * 1024; // 单张解码后不超过 5 MB

if (!DATABASE_URL) {
  console.error('缺少 DATABASE_URL，退出');
  process.exit(1);
}
if (!SECRET || SECRET.length < 32) {
  console.error('缺少 API_SECRET（至少 32 位），退出');
  process.exit(1);
}

const pool = new Pool({ connectionString: DATABASE_URL, max: 10 });
const q = (sql, params) => pool.query(sql, params);

// ---------------------------------------------------------------- 小工具

class HttpError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

const bad = (msg) => new HttpError(400, msg);
const unauth = (msg = '登录已失效，请重新登录') => new HttpError(401, msg);
const forbidden = (msg = '没有权限') => new HttpError(403, msg);
const notFound = (msg = '找不到这条数据') => new HttpError(404, msg);

function text(value, field, { min = 0, max = 100000 } = {}) {
  if (value === undefined || value === null) {
    if (min > 0) throw bad(`${field}不能为空`);
    return null;
  }
  const s = String(value).trim();
  if (s.length < min) throw bad(`${field}不能为空`);
  if (s.length > max) throw bad(`${field}最多 ${max} 个字`);
  return s.length ? s : null;
}

function verify(token) {
  if (typeof token !== 'string' || !token.includes('.')) return null;
  const [body, sig] = token.split('.');
  const want = crypto.createHmac('sha256', SECRET).update(body).digest('base64url');
  if (sig.length !== want.length) return null;
  if (!crypto.timingSafeEqual(Buffer.from(sig), Buffer.from(want))) return null;
  let parsed;
  try {
    parsed = JSON.parse(Buffer.from(body, 'base64url').toString('utf8'));
  } catch {
    return null;
  }
  if (!parsed.exp || parsed.exp < Math.floor(Date.now() / 1000)) return null;
  return parsed;
}

// ---------------------------------------------------------------- 行映射（保持和旧 Supabase 一样的字段名，App 端少改）

const memberRow = (r) => ({
  id: r.user_id,
  email: r.email,
  display_name: r.display_name,
  is_admin: r.is_admin,
  // 管理员给他写的备注：只有管理员调的管理员接口才会带上，普通接口不返回
  note: r.note || null,
  note_updated_at: r.note_updated_at || null,
  created_at: r.created_at,
});

const projectRow = (r) => ({
  id: r.id,
  name: r.name,
  description: r.description,
  created_at: r.created_at,
  is_archived: r.is_archived,
});

const suggestionRow = (r) => ({
  id: r.id,
  project_id: r.project_id,
  author_id: r.author_id,
  title: r.title,
  content: r.content,
  status: r.status,
  reject_reason: r.reject_reason,
  pushed_version: r.pushed_version,
  pushed_note: r.pushed_note,
  created_at: r.created_at,
  accepted_at: r.accepted_at,
  rejected_at: r.rejected_at,
  resolved_at: r.resolved_at,
  pushed_at: r.pushed_at,
  images: Array.isArray(r.images) ? r.images : [],
});

/**
 * 按 token 里的 UUID 找本应用的成员；第一次见到这个人时建一行。
 * 本应用还没有管理员时，第一个来的人自动成为管理员（新库开局用）。
 */
async function ensureMember(claims) {
  const id = String(claims.sub || claims.uid || '');
  if (!id) throw unauth();
  const email = typeof claims.email === 'string' ? claims.email : '';
  const name = typeof claims.name === 'string' ? claims.name : '';

  const found = await q('select * from members where user_id = $1', [id]);
  if (found.rowCount) {
    const m = found.rows[0];
    if ((email && email !== m.email) || (name && name !== (m.display_name || ''))) {
      const upd = await q(
        `update members set
           email = coalesce(nullif($2, ''), email),
           display_name = coalesce(nullif($3, ''), display_name),
           last_seen_at = now()
         where user_id = $1 returning *`,
        [id, email, name],
      );
      return upd.rows[0];
    }
    return m;
  }

  const admins = await q('select 1 from members where is_admin limit 1');
  const firstEver = admins.rowCount === 0;
  const ins = await q(
    `insert into members (user_id, email, display_name, is_admin, last_seen_at)
     values ($1, $2, $3, $4, now())
     on conflict (user_id) do update set email = excluded.email
     returning *`,
    [id, email || `${id}@unknown`, name || null, firstEver],
  );
  return ins.rows[0];
}

async function hasAccess(userId, projectId) {
  const r = await q('select 1 from project_access where user_id = $1 and project_id = $2', [
    userId,
    projectId,
  ]);
  return r.rowCount > 0;
}

async function loadSuggestion(id) {
  const r = await q('select * from suggestions where id = $1', [id]);
  if (!r.rowCount) throw notFound('这条建议不存在');
  return r.rows[0];
}

// ---------------------------------------------------------------- 图片（0.0.9）

const IMAGE_PATH_RE = /^uploads\/[0-9a-f]{24}\.(jpg|png)$/;

function ensureUploadDir() {
  fs.mkdirSync(UPLOAD_DIR, { recursive: true });
}

/** 校验并规范化客户端传来的图片路径数组；没传返回 null（表示「这一项不改」） */
function imageList(value) {
  if (value === undefined || value === null) return null;
  if (!Array.isArray(value)) throw bad('图片格式不对');
  if (value.length > MAX_IMAGES) throw bad(`一条建议最多 ${MAX_IMAGES} 张图`);
  const out = [];
  for (const item of value) {
    const p = String(item || '').trim();
    if (!IMAGE_PATH_RE.test(p)) throw bad('图片路径不对');
    if (!out.includes(p)) out.push(p);
  }
  return out;
}

function deleteImageFiles(paths) {
  for (const p of paths || []) {
    if (!IMAGE_PATH_RE.test(String(p))) continue;
    fs.unlink(path.join(SITE_ROOT, String(p)), () => {});
  }
}

async function hUpload(req) {
  const raw = String(req.body.data || '');
  if (!raw) throw bad('没有收到图片');

  let buf;
  try {
    buf = Buffer.from(raw.replace(/^data:image\/[a-z+.-]+;base64,/i, ''), 'base64');
  } catch {
    throw bad('图片数据是坏的');
  }
  if (!buf.length) throw bad('图片是空的');
  if (buf.length > MAX_IMAGE_BYTES) throw bad('图片太大了，换一张小一点的');

  // 只认 JPEG / PNG 的文件头，不信任客户端给的后缀
  let ext = null;
  if (buf[0] === 0xff && buf[1] === 0xd8) ext = 'jpg';
  else if (buf[0] === 0x89 && buf[1] === 0x50 && buf[2] === 0x4e && buf[3] === 0x47) ext = 'png';
  if (!ext) throw bad('只支持 JPG 或 PNG 图片');

  ensureUploadDir();
  const name = crypto.randomBytes(12).toString('hex') + '.' + ext;
  fs.writeFileSync(path.join(UPLOAD_DIR, name), buf);
  return { path: 'uploads/' + name, size: buf.length };
}

// ---------------------------------------------------------------- 接口实现

async function hHealth() {
  await q('select 1');
  return { ok: true };
}

async function hMe(req, user) {
  return { user: memberRow(user) };
}

async function hHome(req, user) {
  if (user.is_admin) {
    const [p, u, s, a] = await Promise.all([
      q('select * from projects order by created_at'),
      q('select * from members order by created_at'),
      q('select * from suggestions order by created_at'),
      q('select project_id, user_id from project_access'),
    ]);
    const members = u.rows.map(memberRow);
    return {
      projects: p.rows.map(projectRow),
      members,
      // 老版本的 App 只认 users 这个字段，先留着当别名，等大家都升级上去再删
      users: members,
      suggestions: s.rows.map(suggestionRow),
      access: a.rows,
    };
  }
  const [p, s] = await Promise.all([
    q(
      `select p.* from projects p
         join project_access a on a.project_id = p.id
        where a.user_id = $1 order by p.created_at`,
      [user.id],
    ),
    q(
      `select s.* from suggestions s
         join project_access a on a.project_id = s.project_id
        where a.user_id = $1 order by s.created_at`,
      [user.id],
    ),
  ]);
  return {
    projects: p.rows.map(projectRow),
    members: [],
    users: [],
    suggestions: s.rows.map(suggestionRow),
    access: [],
  };
}

async function hCreateProject(req, user) {
  if (!user.is_admin) throw forbidden();
  const name = text(req.body.name, '项目名称', { min: 1, max: 30 });
  const description = text(req.body.description, '项目说明', { max: 100 });
  const r = await q(
    'insert into projects (name, description, created_by) values ($1, $2, $3) returning *',
    [name, description, user.id],
  );
  return { project: projectRow(r.rows[0]) };
}

async function hDeleteProject(req, user, params) {
  if (!user.is_admin) throw forbidden();
  // 先把项目里建议带的图片路径记下来，删完库再删文件（否则文件会变成孤儿）
  const imgs = await q('select images from suggestions where project_id = $1', [params.id]);
  const r = await q('delete from projects where id = $1 returning id', [params.id]);
  if (!r.rowCount) throw notFound('项目不存在，或者数据库没同意删除');
  for (const row of imgs.rows) {
    deleteImageFiles(Array.isArray(row.images) ? row.images : []);
  }
  return { ok: true, id: r.rows[0].id };
}

async function hSetAccess(req, user, params) {
  if (!user.is_admin) throw forbidden();
  const projectId = params.id;
  const userId = String(req.body.user_id || '');
  const granted = req.body.granted === true;
  const p = await q('select 1 from projects where id = $1', [projectId]);
  if (!p.rowCount) throw notFound('项目不存在');
  const u = await q('select 1 from members where user_id = $1', [userId]);
  if (!u.rowCount) throw notFound('用户不存在');
  if (granted) {
    await q(
      'insert into project_access (project_id, user_id) values ($1, $2) on conflict do nothing',
      [projectId, userId],
    );
  } else {
    await q('delete from project_access where project_id = $1 and user_id = $2', [
      projectId,
      userId,
    ]);
  }
  return { ok: true, project_id: projectId, user_id: userId, granted };
}

/**
 * 管理员给某个用户写备注。备注只在这个接口的返回里出现，
 * 普通用户的 /home、/me 都不会带别人的备注。
 */
async function hSetMemberNote(req, user, params) {
  if (!user.is_admin) throw forbidden();
  const note = text(req.body.note, '备注', { max: 300 });
  const r = await q(
    `update members set
       note = $1::text,
       note_updated_at = case when $1::text is null then null else now() end
     where user_id = $2 returning *`,
    [note, params.id],
  );
  if (!r.rowCount) throw notFound('这个用户不存在');
  return { member: memberRow(r.rows[0]) };
}

async function hCreateSuggestion(req, user) {
  const projectId = String(req.body.project_id || '');
  const title = text(req.body.title, '标题', { min: 1, max: 50 });
  const content = text(req.body.content, '内容', { max: 1000 });
  const images = imageList(req.body.images) || [];
  if (!user.is_admin && !(await hasAccess(user.id, projectId))) {
    throw forbidden('你还没有这个项目的权限，找管理员开通一下');
  }
  const p = await q('select 1 from projects where id = $1', [projectId]);
  if (!p.rowCount) throw notFound('项目不存在');
  const r = await q(
    `insert into suggestions (project_id, author_id, title, content, images)
     values ($1, $2, $3, $4, $5::jsonb) returning *`,
    [projectId, user.id, title, content, JSON.stringify(images)],
  );
  return { suggestion: suggestionRow(r.rows[0]) };
}

async function hUpdateSuggestion(req, user, params) {
  const row = await loadSuggestion(params.id);
  if (row.author_id !== user.id) throw forbidden('只能改自己提的建议');
  if (row.status !== 'pending') throw forbidden('这条已经被处理过了，不能再改');
  const title = text(req.body.title, '标题', { min: 1, max: 50 });
  const content = text(req.body.content, '内容', { max: 1000 });
  const oldImages = Array.isArray(row.images) ? row.images : [];
  const images = imageList(req.body.images);
  const nextImages = images === null ? oldImages : images;
  const r = await q(
    'update suggestions set title = $1, content = $2, images = $3::jsonb where id = $4 returning *',
    [title, content, JSON.stringify(nextImages), row.id],
  );
  if (images !== null) {
    for (const gone of oldImages.filter((p) => !nextImages.includes(p))) deleteImageFiles([gone]);
  }
  return { suggestion: suggestionRow(r.rows[0]) };
}

async function hDeleteSuggestion(req, user, params) {
  const row = await loadSuggestion(params.id);
  if (row.author_id !== user.id) throw forbidden('只能删自己提的建议');
  if (row.status !== 'pending') throw forbidden('这条已经被处理过了，删不了');
  await q('delete from suggestions where id = $1', [row.id]);
  deleteImageFiles(Array.isArray(row.images) ? row.images : []);
  return { ok: true, id: row.id };
}

async function hAccept(req, user, params) {
  if (!user.is_admin) throw forbidden();
  await loadSuggestion(params.id);
  const r = await q(
    `update suggestions set status = 'accepted', accepted_at = now(),
        rejected_at = null, reject_reason = null, resolved_at = null, pushed_at = null,
        pushed_version = null, pushed_note = null, handled_by = $2
      where id = $1 returning *`,
    [params.id, user.id],
  );
  return { suggestion: suggestionRow(r.rows[0]) };
}

async function hReject(req, user, params) {
  if (!user.is_admin) throw forbidden();
  const reason = text(req.body.reason, '驳回理由', { min: 1, max: 400 });
  await loadSuggestion(params.id);
  const r = await q(
    `update suggestions set status = 'rejected', reject_reason = $2, rejected_at = now(),
        accepted_at = null, resolved_at = null, pushed_at = null,
        pushed_version = null, pushed_note = null, handled_by = $3
      where id = $1 returning *`,
    [params.id, reason, user.id],
  );
  return { suggestion: suggestionRow(r.rows[0]) };
}

async function hResolve(req, user, params) {
  if (!user.is_admin) throw forbidden();
  const row = await loadSuggestion(params.id);
  if (!['accepted', 'resolved'].includes(row.status)) throw bad('只有已接纳的建议才能标记为解决');
  const r = await q(
    `update suggestions set status = 'resolved', resolved_at = now(), handled_by = $2
      where id = $1 returning *`,
    [params.id, user.id],
  );
  return { suggestion: suggestionRow(r.rows[0]) };
}

async function hPush(req, user, params) {
  if (!user.is_admin) throw forbidden();
  const version = text(req.body.version, '版本号', { min: 1, max: 30 });
  const note = text(req.body.note, '推送说明', { max: 200 });
  const row = await loadSuggestion(params.id);
  if (!['accepted', 'resolved', 'pushed'].includes(row.status)) {
    throw bad('只有已接纳或已解决的建议才能推送');
  }
  const r = await q(
    `update suggestions set status = 'pushed', pushed_at = now(), pushed_version = $2,
        pushed_note = $3, handled_by = $4
      where id = $1 returning *`,
    [params.id, version, note, user.id],
  );
  return { suggestion: suggestionRow(r.rows[0]) };
}

async function hReopen(req, user, params) {
  if (!user.is_admin) throw forbidden();
  await loadSuggestion(params.id);
  const r = await q(
    `update suggestions set status = 'accepted', accepted_at = now(), resolved_at = null,
        pushed_at = null, pushed_version = null, pushed_note = null,
        rejected_at = null, reject_reason = null, handled_by = $2
      where id = $1 returning *`,
    [params.id, user.id],
  );
  return { suggestion: suggestionRow(r.rows[0]) };
}

// ---------------------------------------------------------------- 路由

const routes = [
  ['GET', /^\/api\/health$/, hHealth, { auth: false }],
  ['GET', /^\/api\/me$/, hMe, { auth: true }],
  ['GET', /^\/api\/home$/, hHome, { auth: true }],
  ['PUT', /^\/api\/members\/(?<id>[0-9a-f-]{36})\/note$/, hSetMemberNote, { auth: true }],
  ['POST', /^\/api\/projects$/, hCreateProject, { auth: true }],
  ['DELETE', /^\/api\/projects\/(?<id>[0-9a-f-]{36})$/, hDeleteProject, { auth: true }],
  ['PUT', /^\/api\/projects\/(?<id>[0-9a-f-]{36})\/access$/, hSetAccess, { auth: true }],
  ['POST', /^\/api\/suggestions$/, hCreateSuggestion, { auth: true }],
  ['POST', /^\/api\/upload$/, hUpload, { auth: true, maxBody: 8 * 1024 * 1024 }],
  ['PATCH', /^\/api\/suggestions\/(?<id>[0-9a-f-]{36})$/, hUpdateSuggestion, { auth: true }],
  ['DELETE', /^\/api\/suggestions\/(?<id>[0-9a-f-]{36})$/, hDeleteSuggestion, { auth: true }],
  ['POST', /^\/api\/suggestions\/(?<id>[0-9a-f-]{36})\/accept$/, hAccept, { auth: true }],
  ['POST', /^\/api\/suggestions\/(?<id>[0-9a-f-]{36})\/reject$/, hReject, { auth: true }],
  ['POST', /^\/api\/suggestions\/(?<id>[0-9a-f-]{36})\/resolve$/, hResolve, { auth: true }],
  ['POST', /^\/api\/suggestions\/(?<id>[0-9a-f-]{36})\/push$/, hPush, { auth: true }],
  ['POST', /^\/api\/suggestions\/(?<id>[0-9a-f-]{36})\/reopen$/, hReopen, { auth: true }],
];

function readBody(req, maxBytes = 256 * 1024) {
  return new Promise((resolve, reject) => {
    let size = 0;
    const chunks = [];
    req.on('data', (c) => {
      size += c.length;
      if (size > maxBytes) {
        reject(bad('请求内容太大了'));
        req.destroy();
        return;
      }
      chunks.push(c);
    });
    req.on('end', () => {
      if (!chunks.length) return resolve({});
      try {
        resolve(JSON.parse(Buffer.concat(chunks).toString('utf8')));
      } catch {
        reject(bad('请求格式不对'));
      }
    });
    req.on('error', reject);
  });
}

function send(res, status, payload) {
  const body = JSON.stringify(payload);
  res.writeHead(status, {
    'Content-Type': 'application/json; charset=utf-8',
    'Content-Length': Buffer.byteLength(body),
    'Cache-Control': 'no-store',
  });
  res.end(body);
}

async function handle(req, res) {
  const url = new URL(req.url, 'http://127.0.0.1');
  const path = url.pathname.replace(/\/+$/, '') || '/api';

  let matched = null;
  for (const [method, pattern, handler, options] of routes) {
    if (method !== req.method) continue;
    const m = pattern.exec(path);
    if (m) {
      matched = { handler, options, params: m.groups || {} };
      break;
    }
  }
  if (!matched) return send(res, 404, { error: '没有这个接口' });

  req.body =
    req.method === 'GET' || req.method === 'DELETE'
      ? {}
      : await readBody(req, matched.options.maxBody || 256 * 1024);

  let user = null;
  if (matched.options.auth) {
    const header = req.headers.authorization || '';
    const token = header.startsWith('Bearer ') ? header.slice(7) : '';
    const claims = verify(token);
    if (!claims) throw unauth();
    const want = matched.options.tokenType || 'access';
    if (claims.typ !== want) throw unauth();
    // token 是公共账号服务签的，这里只按 UUID 找本应用的成员（没有就建一行）
    const member = await ensureMember(claims);
    user = { ...member, id: member.user_id };
  }

  const result = await matched.handler(req, user, matched.params);
  return send(res, 200, result);
}

const server = http.createServer((req, res) => {
  handle(req, res).catch((err) => {
    const status = err instanceof HttpError ? err.status : 500;
    if (status >= 500) console.error('[500]', req.method, req.url, err);
    send(res, status, { error: err.message || '服务器出错了' });
  });
});

server.listen(PORT, '127.0.0.1', () => {
  console.log(`建议收集接口已启动：http://127.0.0.1:${PORT}/api/health`);
});
