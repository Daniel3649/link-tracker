--liquibase formatted sql
--changeset daniel-konenkov:001-init-schema

create table if not exists telegram_chat (
    chat_id bigint primary key
);

create table if not exists tracked_link (
    id bigserial primary key,
    url text not null,
    link_type varchar(32) not null,
    github_owner text,
    github_repo text,
    stackoverflow_question_id bigint,
    constraint chk_tracked_link_type
        check (link_type in ('GITHUB', 'STACKOVERFLOW')),
    constraint chk_tracked_link_key_shape
        check (
            (
                link_type = 'GITHUB'
                and github_owner is not null
                and github_repo is not null
                and stackoverflow_question_id is null
            )
            or
            (
                link_type = 'STACKOVERFLOW'
                and github_owner is null
                and github_repo is null
                and stackoverflow_question_id is not null
            )
        )
);

create table if not exists subscription (
    id bigserial primary key,
    chat_id bigint not null references telegram_chat (chat_id) on delete cascade,
    link_id bigint not null references tracked_link (id) on delete cascade,
    constraint uq_subscription_chat_link unique (chat_id, link_id)
);

create table if not exists subscription_tag (
    subscription_id bigint not null references subscription (id) on delete cascade,
    tag text not null,
    constraint pk_subscription_tag primary key (subscription_id, tag)
);

create table if not exists github_tracking_state (
    link_id bigint primary key references tracked_link (id) on delete cascade,
    etag text,
    last_activity_id bigint
);

create table if not exists stackoverflow_tracking_state (
    link_id bigint primary key references tracked_link (id) on delete cascade,
    last_creation_date_epoch_sec bigint not null default 0,
    last_event_key text,
    next_check_at timestamptz,
    last_question_activity_date_epoch_sec bigint not null default 0
);

create unique index if not exists uq_tracked_link_github_key
    on tracked_link (lower(github_owner), lower(github_repo))
    where link_type = 'GITHUB';

create unique index if not exists uq_tracked_link_stackoverflow_key
    on tracked_link (stackoverflow_question_id)
    where link_type = 'STACKOVERFLOW';

create index if not exists idx_tracked_link_url
    on tracked_link (url);

create index if not exists idx_subscription_chat_id
    on subscription (chat_id);

create index if not exists idx_subscription_link_id
    on subscription (link_id);

create index if not exists idx_subscription_tag_subscription_id
    on subscription_tag (subscription_id);

create index if not exists idx_stackoverflow_tracking_state_next_check_at
    on stackoverflow_tracking_state (next_check_at);
