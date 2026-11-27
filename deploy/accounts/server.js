'use strict';

// 公共账号服务：一套账号给多个 App 共用。
// 只做「账号」这件事：注册、登录、续期、改密码、列出接入的应用。
// 具体身份（比如谁是建议收集的管理员）由各 App 自己的库决定。

const http = require('http');
const crypto = require('crypto');
const { Pool } = require('pg');

const PORT = Number(process.env.PORT || 3001);
const DATABASE_URL = process.env.DATABASE_URL;
const SECRET = process.env.API_SECRET || '';
const ACCESS_TTL = 12 * 60 * 60; // 12 小时
const REFRESH_TTL = 60 * 24 * 60 * 60; // 60 天
const MIN_PASSWORD = 6;

if (!DATABASE_URL) {
  console.error('缺少 DATABASE_URL，退出');
  process.exit(1);
}
if (!SECRET || SECRET.length < 32) {
  console.error('缺少 API_SECRET（至少 32 位，必须和各应用一致），退出');
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

function normEmail(value) {
  const e = String(value || '').trim().toLowerCase();
  if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(e)) throw bad('邮箱格式不对');
  return e;
}

function hashPassword(pw) {
  const salt = crypto.randomBytes(16).toString('hex');
  const hash = crypto.scryptSync(pw, salt, 64).toString('hex');
  return `scrypt$${salt}$${hash}`;
}

function checkPassword(pw, stored) {
  if (!stored) return false;
  const [alg, salt, hash] = stored.split('$');
  if (alg !== 'scrypt' || !salt || !hash) return false;
  const calc = crypto.scryptSync(pw, salt, 64);
  const want = Buffer.from(hash, 'hex');
  return calc.length === want.length && crypto.timingSafeEqual(calc, want);
}

function sign(payload, ttl) {
  const body = Buffer.from(
    JSON.stringify({ ...payload, exp: Math.floor(Date.now() / 1000) + ttl }),
  ).toString('base64url');
  const sig = crypto.createHmac('sha256', SECRET).update(body).digest('base64url');
  return `${body}.${sig}`;
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

// ---------------------------------------------------------------- 行映射

const accountRow = (r) => ({
  id: r.id,
  email: r.email,
  display_name: r.display_name,
});

function sessionOf(account) {
  return {
    access_token: sign(
      { sub: account.id, email: account.email, name: account.display_name, typ: 'access' },
      ACCESS_TTL,
    ),
    refresh_token: sign({ sub: account.id, typ: 'refresh' }, REFRESH_TTL),
    expires_in: ACCESS_TTL,
    user: accountRow(account),
  };
}

// ---------------------------------------------------------------- 接口

async function hHealth() {
  const r = await q('select count(*)::int as n from accounts');
  return { ok: true, accounts: r.rows[0].n };
}

async function hRegister(req) {
  const email = normEmail(req.body.email);
  const pw = String(req.body.password || '');
  if (pw.length < MIN_PASSWORD) throw bad(`密码至少 ${MIN_PASSWORD} 位，再短容易被猜到`);
  const displayName = text(req.body.display_name, '称呼', { max: 30 }) || email.split('@')[0];

  const found = await q('select * from accounts where lower(email) = $1', [email]);
  let account;
  if (found.rowCount) {
    const old = found.rows[0];
    if (old.is_disabled) throw forbidden('这个账号被停用了');
    if (old.password_hash) throw new HttpError(409, '这个邮箱已经注册过了，直接登录就行');
    // 从旧系统搬过来的账号还没有密码：这次注册等于认领它，UUID 不变，历史数据都还在
    const upd = await q(
      `update accounts set password_hash = $1,
         display_name = coalesce(nullif(display_name, ''), $2), updated_at = now()
       where id = $3 returning *`,
      [hashPassword(pw), displayName, old.id],
    );
    account = upd.rows[0];
  } else {
    const ins = await q(
      `insert into accounts (email, password_hash, display_name) values ($1, $2, $3) returning *`,
      [email, hashPassword(pw), displayName],
    );
    account = ins.rows[0];
  }
  return sessionOf(account);
}

async function hLogin(req) {
  const email = normEmail(req.body.email);
  const pw = String(req.body.password || '');
  const r = await q('select * from accounts where lower(email) = $1', [email]);
  if (!r.rowCount) throw unauth('邮箱或密码不对');
  const account = r.rows[0];
  if (account.is_disabled) throw forbidden('这个账号被停用了');
  if (!account.password_hash) {
    throw new HttpError(409, '这个账号是搬过来的，还没设新密码：请点「注册」，用同一个邮箱设置一次密码');
  }
  if (!checkPassword(pw, account.password_hash)) throw unauth('邮箱或密码不对');
  return sessionOf(account);
}

async function hRefresh(req, account) {
  return sessionOf(account);
}

async function hMe(req, account) {
  return { user: accountRow(account) };
}

async function hChangePassword(req, account) {
  const oldPw = String(req.body.old_password || '');
  const newPw = String(req.body.new_password || '');
  if (newPw.length < MIN_PASSWORD) throw bad(`新密码至少 ${MIN_PASSWORD} 位`);
  if (account.password_hash && !checkPassword(oldPw, account.password_hash)) {
    throw unauth('原来的密码不对');
  }
  await q('update accounts set password_hash = $1, updated_at = now() where id = $2', [
    hashPassword(newPw),
    account.id,
  ]);
  return { ok: true };
}

async function hApps() {
  const r = await q('select slug, name, created_at from apps order by created_at');
  return { apps: r.rows };
}

// ---------------------------------------------------------------- 路由

const routes = [
  ['GET', /^\/accounts\/health$/, hHealth, { auth: false }],
  ['POST', /^\/accounts\/auth\/register$/, hRegister, { auth: false }],
  ['POST', /^\/accounts\/auth\/login$/, hLogin, { auth: false }],
  ['POST', /^\/accounts\/auth\/refresh$/, hRefresh, { auth: true, tokenType: 'refresh' }],
  ['GET', /^\/accounts\/me$/, hMe, { auth: true }],
  ['PUT', /^\/accounts\/me\/password$/, hChangePassword, { auth: true }],
  ['GET', /^\/accounts\/apps$/, hApps, { auth: true }],
];

// ---------------------------------------------------------------- 请求处理

function readBody(req, maxBytes = 64 * 1024) {
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
  const path = url.pathname.replace(/\/+$/, '') || '/accounts';

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
      : await readBody(req, matched.options.maxBody || 64 * 1024);

  let account = null;
  if (matched.options.auth) {
    const header = req.headers.authorization || '';
    const token = header.startsWith('Bearer ') ? header.slice(7) : '';
    const claims = verify(token);
    if (!claims) throw unauth();
    const want = matched.options.tokenType || 'access';
    if (claims.typ !== want) throw unauth();
    const r = await q('select * from accounts where id = $1', [claims.sub]);
    if (!r.rowCount) throw unauth();
    account = r.rows[0];
    if (account.is_disabled) throw forbidden('这个账号被停用了');
  }

  return send(res, 200, await matched.handler(req, account, matched.params));
}

const server = http.createServer((req, res) => {
  handle(req, res).catch((err) => {
    const status = err instanceof HttpError ? err.status : 500;
    if (status >= 500) console.error('[500]', req.method, req.url, err);
    send(res, status, { error: err.message || '服务器出错了' });
  });
});

server.listen(PORT, '127.0.0.1', () => {
  console.log(`账号服务已启动：http://127.0.0.1:${PORT}/accounts/health`);
});
