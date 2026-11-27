-- 公共账号服务（accounts-api）的库结构
--
-- 这个库和具体应用的库完全分开：
--   accounts  只放「账号」本身（邮箱、密码哈希、显示名）
--   apps      放「哪些应用接入了这套账号」，方便以后按应用区分
-- 应用自己的数据（比如建议收集里的管理员身份、备注、项目权限）不放这里。

create extension if not exists pgcrypto;

create table if not exists accounts (
  id uuid primary key default gen_random_uuid(),
  email text not null,
  password_hash text,
  display_name text,
  is_disabled boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

-- 同一个邮箱只能有一个账号（大小写不敏感）
create unique index if not exists accounts_email_key on accounts (lower(email));

create table if not exists apps (
  id uuid primary key default gen_random_uuid(),
  slug text not null unique,
  name text not null,
  created_at timestamptz not null default now()
);

insert into apps (slug, name) values ('suggestion-collection', '建议收集')
on conflict (slug) do nothing;
