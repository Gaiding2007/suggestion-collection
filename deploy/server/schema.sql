-- 自己的 PostgreSQL 结构（从 Supabase 搬过来时用）
-- 用法：psql "$DATABASE_URL" -f schema.sql
-- 和 supabase/schema.sql 的区别：没有 RLS、没有 auth.users，
-- 权限全部由接口服务（server.js）判断，密码用 scrypt 存在 users 表。

create extension if not exists pgcrypto;

create table if not exists users (
  id uuid primary key default gen_random_uuid(),
  email text not null,
  password_hash text,
  display_name text,
  is_admin boolean not null default false,
  claimed boolean not null default true,
  created_at timestamptz not null default now()
);
create unique index if not exists users_email_lower_key on users (lower(email));

create table if not exists projects (
  id uuid primary key default gen_random_uuid(),
  name text not null check (char_length(btrim(name)) between 1 and 30),
  description text check (description is null or char_length(description) <= 100),
  created_by uuid references users(id) on delete set null,
  created_at timestamptz not null default now(),
  is_archived boolean not null default false
);

create table if not exists project_access (
  project_id uuid not null references projects(id) on delete cascade,
  user_id uuid not null references users(id) on delete cascade,
  granted_at timestamptz not null default now(),
  primary key (project_id, user_id)
);

create table if not exists suggestions (
  id uuid primary key default gen_random_uuid(),
  project_id uuid not null references projects(id) on delete cascade,
  author_id uuid not null references users(id) on delete cascade,
  title text not null check (char_length(btrim(title)) between 1 and 50),
  content text check (content is null or char_length(content) <= 1000),
  status text not null default 'pending'
    check (status in ('pending', 'accepted', 'rejected', 'resolved', 'pushed')),
  reject_reason text check (reject_reason is null or char_length(reject_reason) <= 400),
  pushed_version text check (pushed_version is null or char_length(pushed_version) <= 30),
  pushed_note text check (pushed_note is null or char_length(pushed_note) <= 200),
  handled_by uuid references users(id) on delete set null,
  created_at timestamptz not null default now(),
  accepted_at timestamptz,
  rejected_at timestamptz,
  resolved_at timestamptz,
  pushed_at timestamptz
);

create index if not exists suggestions_project_idx on suggestions (project_id, created_at);
create index if not exists suggestions_author_idx on suggestions (author_id);
create index if not exists suggestions_status_idx on suggestions (status);

-- 0.0.9：建议可以带图片。存的是相对下载站根目录的路径，例如 ["uploads/ab12cd34.jpg"]
-- 文件本体放在 /var/www/suggestion/uploads/ 下，由 nginx 直接返回（权限在接口层校验）。
alter table suggestions add column if not exists images jsonb not null default '[]'::jsonb;
