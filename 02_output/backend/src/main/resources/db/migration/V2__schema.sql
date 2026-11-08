-- Baseline schema (docs/02_specification.md "Data model and validation", ADR-02, ADR-03).
-- Times are timestamptz stored in UTC (AR-10). ${app_user} is the restricted application role.

CREATE TABLE app_user (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    firebase_uid  varchar(128) NOT NULL UNIQUE,
    email         varchar(255) NOT NULL UNIQUE,
    display_name  varchar(100) NOT NULL CHECK (char_length(display_name) >= 1),
    created_at    timestamptz  NOT NULL DEFAULT now()
);

CREATE TABLE contact_group (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name        varchar(100) NOT NULL CHECK (char_length(btrim(name)) >= 1),
    created_at  timestamptz  NOT NULL DEFAULT now(),
    deleted_at  timestamptz
);

CREATE TABLE member (
    group_id   uuid        NOT NULL REFERENCES contact_group (id) ON DELETE CASCADE,
    user_id    uuid        NOT NULL REFERENCES app_user (id),
    role       varchar(10) NOT NULL CHECK (role IN ('OWNER', 'EDITOR', 'VIEWER')),
    joined_at  timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (group_id, user_id)
);
CREATE INDEX member_user_idx ON member (user_id);

CREATE TABLE invitation (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id    uuid         NOT NULL REFERENCES contact_group (id) ON DELETE CASCADE,
    email       varchar(255) NOT NULL,
    role        varchar(10)  NOT NULL CHECK (role IN ('OWNER', 'EDITOR', 'VIEWER')),
    status      varchar(10)  NOT NULL CHECK (status IN ('PENDING', 'ACCEPTED', 'CANCELLED', 'EXPIRED')),
    created_at  timestamptz  NOT NULL DEFAULT now(),
    expires_at  timestamptz  NOT NULL,
    created_by  uuid         NOT NULL,
    CHECK (expires_at > created_at)
);
CREATE INDEX invitation_group_idx ON invitation (group_id, status);
CREATE INDEX invitation_email_idx ON invitation (lower(email), status);

CREATE TABLE contact (
    id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id         uuid          NOT NULL REFERENCES contact_group (id) ON DELETE CASCADE,
    version          integer       NOT NULL DEFAULT 1 CHECK (version >= 1),
    first_name       varchar(255),
    last_name        varchar(255),
    email            varchar(255),
    phone            varchar(30),
    company          varchar(255),
    job_title        varchar(255),
    street           varchar(255),
    postal_code      varchar(255),
    city             varchar(255),
    country          char(2),
    notes            varchar(5000),
    first_name_norm  varchar(255),
    last_name_norm   varchar(255),
    company_norm     varchar(255),
    email_norm       varchar(255),
    search_norm      varchar(1300),
    created_at       timestamptz   NOT NULL DEFAULT now(),
    updated_at       timestamptz   NOT NULL DEFAULT now(),
    created_by       uuid          NOT NULL,
    deleted_at       timestamptz
);
CREATE INDEX contact_list_idx ON contact (group_id, deleted_at, last_name_norm, first_name_norm);
CREATE INDEX contact_email_idx ON contact (group_id, email_norm);

CREATE TABLE tag (
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id   uuid         NOT NULL REFERENCES contact_group (id) ON DELETE CASCADE,
    name       varchar(50)  NOT NULL CHECK (char_length(name) >= 1),
    name_norm  varchar(200) NOT NULL
);
CREATE UNIQUE INDEX tag_group_name_uq ON tag (group_id, lower(name));

CREATE TABLE contact_tag (
    contact_id  uuid NOT NULL REFERENCES contact (id) ON DELETE CASCADE,
    tag_id      uuid NOT NULL REFERENCES tag (id) ON DELETE CASCADE,
    PRIMARY KEY (contact_id, tag_id)
);
CREATE INDEX contact_tag_tag_idx ON contact_tag (tag_id);

CREATE TABLE attribute (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    contact_id  uuid          NOT NULL REFERENCES contact (id) ON DELETE CASCADE,
    key         varchar(50)   NOT NULL CHECK (char_length(key) >= 1),
    value       varchar(255)  NOT NULL CHECK (char_length(value) >= 1),
    value_norm  varchar(1020) NOT NULL
);
CREATE UNIQUE INDEX attribute_contact_key_uq ON attribute (contact_id, lower(key));

-- Append-only audit trail (BR-03, AR-09, ADR-08). user_id and contact_id have no foreign key on purpose.
CREATE TABLE history_entry (
    id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id     uuid         NOT NULL REFERENCES contact_group (id) ON DELETE CASCADE,
    occurred_at  timestamptz  NOT NULL DEFAULT now(),
    user_id      uuid,
    user_name    varchar(100) NOT NULL,
    user_email   varchar(255),
    action       varchar(40)  NOT NULL CHECK (action IN (
        'CONTACT_CREATED', 'CONTACT_EDITED', 'CONTACT_DELETED', 'CONTACT_DELETED_FOREVER',
        'CONTACT_RESTORED', 'TAG_ADDED', 'TAG_REMOVED', 'TAG_RENAMED', 'TAG_DELETED',
        'ATTRIBUTE_ADDED', 'ATTRIBUTE_CHANGED', 'ATTRIBUTE_REMOVED', 'CONTACTS_MERGED',
        'CONTACTS_EXPORTED', 'MEMBER_JOINED', 'ROLE_CHANGED', 'MEMBER_REMOVED',
        'INVITATION_CREATED', 'INVITATION_CANCELLED', 'VERSION_RESTORED')),
    contact_id   uuid,
    details      jsonb        NOT NULL DEFAULT '{}'::jsonb
);
CREATE INDEX history_group_time_idx ON history_entry (group_id, occurred_at DESC);
CREATE INDEX history_contact_time_idx ON history_entry (contact_id, occurred_at DESC);

CREATE TABLE contact_version (
    contact_id  uuid        NOT NULL REFERENCES contact (id) ON DELETE CASCADE,
    number      integer     NOT NULL CHECK (number >= 1),
    created_at  timestamptz NOT NULL DEFAULT now(),
    user_id     uuid        NOT NULL,
    snapshot    jsonb       NOT NULL,
    PRIMARY KEY (contact_id, number)
);

CREATE TABLE duplicate_dismissal (
    id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id     uuid        NOT NULL REFERENCES contact_group (id) ON DELETE CASCADE,
    contact_key  varchar(64) NOT NULL,
    created_at   timestamptz NOT NULL DEFAULT now(),
    created_by   uuid        NOT NULL,
    UNIQUE (group_id, contact_key)
);

CREATE TABLE duplicate_dismissal_contact (
    dismissal_id  uuid NOT NULL REFERENCES duplicate_dismissal (id) ON DELETE CASCADE,
    contact_id    uuid NOT NULL REFERENCES contact (id) ON DELETE CASCADE,
    PRIMARY KEY (dismissal_id, contact_id)
);

-- Privileges of the application role (ADR-03): no DDL, History append-only.
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
GRANT USAGE ON SCHEMA public TO "${app_user}";
GRANT SELECT, INSERT, UPDATE, DELETE ON
    app_user, contact_group, member, invitation, contact, tag, contact_tag, attribute,
    contact_version, duplicate_dismissal, duplicate_dismissal_contact
    TO "${app_user}";
GRANT SELECT, INSERT ON history_entry TO "${app_user}";
GRANT UPDATE (details, user_email) ON history_entry TO "${app_user}";
