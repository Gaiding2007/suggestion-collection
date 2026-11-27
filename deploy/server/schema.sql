-- 自己的 PostgreSQL 结构（从 Supabase 搬过来时用）
-- 用法：psql "$DATABASE_URL" -f schema.sql
-- 和 supabase/schema.sql 的区别：没有 RLS、没有 auth.users，
-- 权限全部由接口服务（server.js）判断。
--
-- 0.0.10 起：账号（邮箱、密码）搬去公共账号服务（deploy/accounts）的独立库，
-- 这里只保留一份「本应用里的这个人」——members 表，用同一个 UUID 和账号库对上。
-- 管理员身份、管理员备注、项目权限都属于本应用，放在这里。

create extension if not exists pgcrypto;

create table if not exists members (
  user_id uuid primary key,
  email text not null,
  display_name text,
  is_admin boolean not null default false,
  -- 管理员给这个人写的备注，只有管理员看得到；客户端不会把备注发给普通用户
  note text check (note is null or char_length(note) <= 300),
  note_updated_at timestamptz,
  created_at timestamptz not null default now(),
  last_seen_at timestamptz
);

create table if not exists projects (
  id uuid primary key default gen_random_uuid(),
  name text not null check (char_length(btrim(name)) between 1 and 30),
  description text check (description is null or char_length(description) <= 100),
  created_by uuid,
  created_at timestamptz not null default now(),
  is_archived boolean not null default false
);

create table if not exists project_access (
  project_id uuid not null references projects(id) on delete cascade,
  user_id uuid not null references members(user_id) on delete cascade,
  granted_at timestamptz not null default now(),
  primary key (project_id, user_id)
);

create table if not exists suggestions (
  id uuid primary key default gen_random_uuid(),
  project_id uuid not null references projects(id) on delete cascade,
  author_id uuid not null references members(user_id) on delete cascade,
  title text not null check (char_length(btrim(title)) between 1 and 50),
  content text check (content is null or char_length(content) <= 1000),
  status text not null default 'pending'
    check (status in ('pending', 'accepted', 'rejected', 'resolved', 'pushed')),
  reject_reason text check (reject_reason is null or char_length(reject_reason) <= 400),
  pushed_version text check (pushed_version is null or char_length(pushed_version) <= 30),
  pushed_note text check (pushed_note is null or char_length(pushed_note) <= 200),
  handled_by uuid,
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
