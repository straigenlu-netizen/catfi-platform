create table if not exists agent_accounts (
  username varchar(80) primary key,
  password_hash varchar(100) not null,
  role varchar(40) not null,
  enabled boolean not null default true,
  created_at timestamptz not null default now()
);

insert into agent_accounts(username,password_hash,role,enabled) values
('AgentCRM1','$2y$12$J0/g9NcvNVRzRFpzivoTIual./sytoft2tne0jo38g/funKtluoYW','ROLE_AGENT',true)
on conflict (username) do nothing;

create table if not exists agent_audit_events (
  id bigserial primary key,
  username varchar(80) not null,
  event_type varchar(40) not null,
  resource varchar(255) not null,
  bucket_start timestamptz null,
  event_count integer not null default 1,
  first_at timestamptz not null,
  last_at timestamptz not null
);
create unique index if not exists uq_agent_audit_read_bucket
  on agent_audit_events(username,event_type,resource,bucket_start) where bucket_start is not null;
create index if not exists ix_agent_audit_last_at on agent_audit_events(last_at);
