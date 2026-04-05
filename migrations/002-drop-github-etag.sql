--liquibase formatted sql
--changeset daniel-konenkov:002-drop-github-etag

alter table github_tracking_state
    drop column if exists etag;
