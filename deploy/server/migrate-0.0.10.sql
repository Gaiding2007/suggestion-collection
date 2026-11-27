-- 0.0.10 的一次性迁移：账号搬去公共账号服务后，本应用只留 members
--
-- 顺序很重要：
--   1) 先跑 deploy/accounts/deploy-server.sh 装好账号服务
--   2) 再跑 deploy/accounts/move-users.js 把 users 复制进账号库（保留 UUID）
--   3) 最后跑这个文件：把 users 变成 members，并把外键指过去，然后删掉 users
--
-- 用法：psql "$DATABASE_URL" -v ON_ERROR_STOP=1 -f migrate-0.0.10.sql
-- 可以重复执行（约束先 drop 再 add）。

begin;

create table if not exists members (
  user_id uuid primary key,
  email text not null,
  display_name text,
  is_admin boolean not null default false,
  note text check (note is null or char_length(note) <= 300),
  note_updated_at timestamptz,
  created_at timestamptz not null default now(),
  last_seen_at timestamptz
);

-- 老用户原样搬进来：同一个 UUID、同样的管理员身份
insert into members (user_id, email, display_name, is_admin, created_at)
select id, email, display_name, is_admin, created_at from users
on conflict (user_id) do nothing;

-- 外键从 users 挪到 members
alter table projects drop constraint if exists projects_created_by_fkey;
alter table project_access drop constraint if exists project_access_user_id_fkey;
alter table suggestions drop constraint if exists suggestions_author_id_fkey;
alter table suggestions drop constraint if exists suggestions_handled_by_fkey;

alter table project_access
  add constraint project_access_user_id_fkey
  foreign key (user_id) references members (user_id) on delete cascade;

alter table suggestions
  add constraint suggestions_author_id_fkey
  foreign key (author_id) references members (user_id) on delete cascade;

-- 账号数据已经在账号库里了，这张表不再留
drop table if exists users;

commit;
