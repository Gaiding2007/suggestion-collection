-- ============================================================
-- 建议收集 v0.0.6 数据库结构（Supabase / PostgreSQL）
--
-- 用法：
--   1. 打开 https://supabase.com/dashboard/project/pnnyujqkkdehfxnmomww
--   2. 左侧点 SQL Editor → New query
--   3. 把本文件从第一行到最后一行整段粘贴进去 → Run
--
-- 这个脚本可以重复执行，不会报错，也不会清空已有数据。
-- ============================================================


-- ---------- 1. 账号资料 ----------
create table if not exists public.profiles (
  id uuid primary key references auth.users (id) on delete cascade,
  email text not null default '',
  display_name text,
  is_admin boolean not null default false,
  created_at timestamptz not null default now()
);

-- 有人在 App 里注册时，自动插入一条 profiles（不用客户端自己写）
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.profiles (id, email, display_name)
  values (
    new.id,
    coalesce(new.email, ''),
    split_part(coalesce(new.email, '用户'), '@', 1)
  )
  on conflict (id) do update set email = excluded.email;
  return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
after insert on auth.users
for each row execute function public.handle_new_user();


-- ---------- 2. 项目 ----------
create table if not exists public.projects (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  description text,
  created_by uuid references public.profiles (id) on delete set null,
  created_at timestamptz not null default now(),
  is_archived boolean not null default false,
  constraint projects_name_len check (char_length(name) between 1 and 30),
  constraint projects_desc_len check (description is null or char_length(description) <= 100)
);


-- ---------- 3. 项目权限（谁能进哪个项目） ----------
create table if not exists public.project_access (
  project_id uuid not null references public.projects (id) on delete cascade,
  user_id uuid not null references public.profiles (id) on delete cascade,
  granted_at timestamptz not null default now(),
  primary key (project_id, user_id)
);


-- ---------- 4. 建议 ----------
create table if not exists public.suggestions (
  id uuid primary key default gen_random_uuid(),
  project_id uuid not null references public.projects (id) on delete cascade,
  author_id uuid not null references public.profiles (id) on delete cascade,
  title text not null,
  content text,
  status text not null default 'pending',
  reject_reason text,
  pushed_version text,
  pushed_note text,
  handled_by uuid references public.profiles (id) on delete set null,
  created_at timestamptz not null default now(),
  accepted_at timestamptz,
  rejected_at timestamptz,
  resolved_at timestamptz,
  pushed_at timestamptz,
  constraint suggestions_status_chk
    check (status in ('pending', 'accepted', 'rejected', 'resolved', 'pushed')),
  constraint suggestions_title_len check (char_length(title) between 1 and 50),
  constraint suggestions_content_len check (content is null or char_length(content) <= 1000),
  constraint suggestions_reason_len check (reject_reason is null or char_length(reject_reason) <= 400),
  constraint suggestions_version_len check (pushed_version is null or char_length(pushed_version) <= 30)
);

create index if not exists suggestions_project_created_idx
  on public.suggestions (project_id, created_at);
create index if not exists suggestions_created_idx
  on public.suggestions (created_at);
create index if not exists project_access_user_idx
  on public.project_access (user_id);


-- ---------- 5. 权限判断函数（security definer，避免 RLS 互相递归） ----------
create or replace function public.is_admin()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select coalesce((select p.is_admin from public.profiles p where p.id = auth.uid()), false);
$$;

create or replace function public.has_project_access(p uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1 from public.project_access pa
    where pa.project_id = p and pa.user_id = auth.uid()
  );
$$;


-- ---------- 6. 行级安全（RLS） ----------
alter table public.profiles enable row level security;
alter table public.projects enable row level security;
alter table public.project_access enable row level security;
alter table public.suggestions enable row level security;

-- profiles：自己看自己，管理员看全部；只有管理员能改
drop policy if exists profiles_select on public.profiles;
create policy profiles_select on public.profiles
  for select to authenticated
  using (id = auth.uid() or public.is_admin());

drop policy if exists profiles_admin_update on public.profiles;
create policy profiles_admin_update on public.profiles
  for update to authenticated
  using (public.is_admin())
  with check (public.is_admin());

-- projects：管理员全部，普通用户只看被授权的
drop policy if exists projects_select on public.projects;
create policy projects_select on public.projects
  for select to authenticated
  using (public.is_admin() or public.has_project_access(id));

drop policy if exists projects_admin_insert on public.projects;
create policy projects_admin_insert on public.projects
  for insert to authenticated
  with check (public.is_admin());

drop policy if exists projects_admin_update on public.projects;
create policy projects_admin_update on public.projects
  for update to authenticated
  using (public.is_admin())
  with check (public.is_admin());

-- 彻底删项目：只有管理员能删。删项目会连带删掉里面的建议和授权（外键 on delete cascade）
drop policy if exists projects_admin_delete on public.projects;
create policy projects_admin_delete on public.projects
  for delete to authenticated
  using (public.is_admin());

-- project_access：用户看自己的授权，管理员全部可读写
drop policy if exists project_access_select on public.project_access;
create policy project_access_select on public.project_access
  for select to authenticated
  using (user_id = auth.uid() or public.is_admin());

drop policy if exists project_access_admin_insert on public.project_access;
create policy project_access_admin_insert on public.project_access
  for insert to authenticated
  with check (public.is_admin());

drop policy if exists project_access_admin_delete on public.project_access;
create policy project_access_admin_delete on public.project_access
  for delete to authenticated
  using (public.is_admin());

-- suggestions：管理员全部；普通用户只读被授权项目里的；改状态只有管理员
drop policy if exists suggestions_select on public.suggestions;
create policy suggestions_select on public.suggestions
  for select to authenticated
  using (public.is_admin() or public.has_project_access(project_id));

drop policy if exists suggestions_admin_update on public.suggestions;
create policy suggestions_admin_update on public.suggestions
  for update to authenticated
  using (public.is_admin())
  with check (public.is_admin());


-- ---------- 7. 提建议的函数 ----------
-- 普通用户没有直接 insert 的权限，只能通过这个函数提交：
-- 函数里检查项目权限，并强制 author_id = 当前登录用户。
create or replace function public.create_suggestion(
  p_project_id uuid,
  p_title text,
  p_content text default null
)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
  new_id uuid;
begin
  if auth.uid() is null then
    raise exception '请先登录';
  end if;
  if not public.has_project_access(p_project_id) then
    raise exception '你没有这个项目的权限';
  end if;
  if p_title is null or char_length(btrim(p_title)) = 0 then
    raise exception '标题不能为空';
  end if;
  if char_length(btrim(p_title)) > 50 then
    raise exception '标题最多 50 个字';
  end if;
  if p_content is not null and char_length(p_content) > 1000 then
    raise exception '内容最多 1000 个字';
  end if;

  insert into public.suggestions (project_id, author_id, title, content)
  values (
    p_project_id,
    auth.uid(),
    btrim(p_title),
    nullif(btrim(coalesce(p_content, '')), '')
  )
  returning id into new_id;

  return new_id;
end;
$$;


-- ---------- 8. 修改自己还没被处理的建议 ----------
-- 提交人只能改自己提的、状态还是 pending 的建议，而且只能改标题和内容：
-- 状态、驳回理由这些字段碰不到，避免用户自己把建议改成「已接纳」。
create or replace function public.update_suggestion(
  p_id uuid,
  p_title text,
  p_content text default null
)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  affected int;
begin
  if auth.uid() is null then
    raise exception '请先登录';
  end if;
  if p_title is null or char_length(btrim(p_title)) = 0 then
    raise exception '标题不能为空';
  end if;
  if char_length(btrim(p_title)) > 50 then
    raise exception '标题最多 50 个字';
  end if;
  if p_content is not null and char_length(p_content) > 1000 then
    raise exception '内容最多 1000 个字';
  end if;

  update public.suggestions
     set title = btrim(p_title),
         content = nullif(btrim(coalesce(p_content, '')), '')
   where id = p_id
     and author_id = auth.uid()
     and status = 'pending';

  get diagnostics affected = row_count;
  if affected = 0 then
    raise exception '只能修改自己提的、还没被处理的建议';
  end if;
end;
$$;


-- ---------- 9. 删掉自己还没被处理的建议 ----------
-- 和被处理的界线一样：状态还是 pending 才能删。删了就是删了，数据库里查不回来。
create or replace function public.delete_suggestion(p_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  affected int;
begin
  if auth.uid() is null then
    raise exception '请先登录';
  end if;

  delete from public.suggestions
   where id = p_id
     and author_id = auth.uid()
     and status = 'pending';

  get diagnostics affected = row_count;
  if affected = 0 then
    raise exception '只能删除自己提的、还没被处理的建议';
  end if;
end;
$$;


-- ---------- 10. 授权 ----------
grant usage on schema public to anon, authenticated;
grant select on all tables in schema public to authenticated;
grant insert, update, delete on all tables in schema public to authenticated;

revoke all on function public.is_admin() from public;
revoke all on function public.has_project_access(uuid) from public;
revoke all on function public.create_suggestion(uuid, text, text) from public;
revoke all on function public.update_suggestion(uuid, text, text) from public;
revoke all on function public.delete_suggestion(uuid) from public;
grant execute on function public.is_admin() to authenticated;
grant execute on function public.has_project_access(uuid) to authenticated;
grant execute on function public.create_suggestion(uuid, text, text) to authenticated;
grant execute on function public.update_suggestion(uuid, text, text) to authenticated;
grant execute on function public.delete_suggestion(uuid) to authenticated;


-- ============================================================
-- 最后一步：把管理员设成管理员
--
-- 顺序很重要：先在自己手机上用管理员邮箱注册一个账号，
-- 再把下面这一句取消注释（删掉行首的 --）并单独执行一次。
-- ============================================================
-- update public.profiles set is_admin = true where email = '1412759076@qq.com';
