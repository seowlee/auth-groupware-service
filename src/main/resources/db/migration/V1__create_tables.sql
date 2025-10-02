-- UUID 함수는 initdb에서 pgcrypto 설치했으므로 바로 사용 가능

-- 팀(teams)
CREATE TABLE IF NOT EXISTS groupware.teams
(
    id         BIGSERIAL PRIMARY KEY,
    kr_name    VARCHAR(100) NOT NULL UNIQUE,
    en_name    VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    created_by VARCHAR(50) DEFAULT 'system',
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    updated_by VARCHAR(50) DEFAULT 'system'
);

-- 사용자(users)
CREATE TABLE IF NOT EXISTS groupware.users
(
    id           BIGSERIAL PRIMARY KEY,
    user_uuid    UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    username     VARCHAR(100) NOT NULL,
    password     TEXT,
    email        VARCHAR(255) NOT NULL,
    phone_number VARCHAR(32),
    kakao_sub    VARCHAR(100),
    first_name   VARCHAR(50),
    last_name    VARCHAR(50),
    joined_date  DATE         NOT NULL        DEFAULT CURRENT_DATE,
    year_number  INTEGER      NOT NULL        DEFAULT 1,
    role         VARCHAR(20)  NOT NULL,
    status       VARCHAR(20)  NOT NULL        DEFAULT 'ACTIVE',
    team_id      BIGINT REFERENCES groupware.teams (id),
    created_at   TIMESTAMPTZ                  DEFAULT NOW(),
    created_by   VARCHAR(50)                  DEFAULT 'system',
    updated_at   TIMESTAMPTZ                  DEFAULT NOW(),
    updated_by   VARCHAR(50)                  DEFAULT 'system',
    CONSTRAINT uq_users_username UNIQUE (username, status),
    CONSTRAINT uq_users_email UNIQUE (email, status),
    CONSTRAINT uq_users_phone UNIQUE (phone_number, status),
    CONSTRAINT uq_users_kakao_sub UNIQUE (kakao_sub)
);

-- 연차 신청(leaves)
CREATE TABLE IF NOT EXISTS groupware.leaves
(
    id                BIGSERIAL PRIMARY KEY,
    user_id           BIGINT REFERENCES groupware.users (id) ON DELETE CASCADE,
    start_dt          TIMESTAMPTZ NOT NULL,
    end_dt            TIMESTAMPTZ NOT NULL,
    leave_type        VARCHAR(30) NOT NULL,
    status            VARCHAR(20) NOT NULL,
    reason            TEXT,
    calendar_event_id VARCHAR(512),
    applied_at        TIMESTAMPTZ   DEFAULT CURRENT_TIMESTAMP,
    used_days         NUMERIC(6, 3) DEFAULT 0.000,
    created_at        TIMESTAMPTZ   DEFAULT NOW(),
    created_by        VARCHAR(50)   DEFAULT 'system',
    updated_at        TIMESTAMPTZ   DEFAULT NOW(),
    updated_by        VARCHAR(50)   DEFAULT 'system',
    CONSTRAINT uq_leaves_range UNIQUE (user_id, start_dt, end_dt)
);

-- 개인별 연차 통계(leave_balances)
CREATE TABLE IF NOT EXISTS groupware.leave_balances
(
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT REFERENCES groupware.users (id) ON DELETE CASCADE,
    leave_type      VARCHAR(30) NOT NULL,
    year_number     INT         NOT NULL DEFAULT 1,
    total_allocated NUMERIC(6, 3),
    used            NUMERIC(6, 3)        DEFAULT 0.000,
    created_at      TIMESTAMPTZ          DEFAULT NOW(),
    created_by      VARCHAR(50)          DEFAULT 'system',
    updated_at      TIMESTAMPTZ          DEFAULT NOW(),
    updated_by      VARCHAR(50)          DEFAULT 'system',
    CONSTRAINT uq_leave_balances UNIQUE (user_id, leave_type, year_number)
);

-- 공휴일(public_holidays)
CREATE TABLE IF NOT EXISTS groupware.public_holidays
(
    id           BIGSERIAL PRIMARY KEY,
    holiday_date DATE                      NOT NULL,
    holiday_name VARCHAR(100)              NOT NULL,
    year         INTEGER                   NOT NULL,
    seq          SMALLINT                  NOT NULL DEFAULT 1,
    created_at   TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    created_by   VARCHAR(50) DEFAULT 'system',
    updated_at   TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_by   VARCHAR(50) DEFAULT 'system',
    CONSTRAINT uq_public_holidays UNIQUE (holiday_date, seq)
);


-- 감사 로그(audit_logs)
CREATE TABLE IF NOT EXISTS groupware.audit_logs
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       REFERENCES groupware.users (id) ON DELETE SET NULL,
    ip_address VARCHAR(45),
    action     VARCHAR(255) NOT NULL,
    status     VARCHAR(50),
    detail     TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    created_by VARCHAR(50) DEFAULT 'system',
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    updated_by VARCHAR(50) DEFAULT 'system'
);

-- M365 토큰(m365_oauth_token)
CREATE TABLE IF NOT EXISTS groupware.m365_oauth_token
(
    id                   BIGSERIAL PRIMARY KEY,
    token_key            VARCHAR(64) NOT NULL UNIQUE, -- 단일 테넌트용 "TENANT_DELEGATED" 등 키
    refresh_token_cipher TEXT        NOT NULL,        -- 암호문(평문 저장 금지)
    tenant_id            VARCHAR(64),
    client_id            VARCHAR(64),
    scope                TEXT,
    connected_by_uuid    VARCHAR(64),
    connected_by_name    VARCHAR(100),
    created_at           TIMESTAMPTZ DEFAULT NOW(),
    created_by           VARCHAR(50) DEFAULT 'system',
    updated_at           TIMESTAMPTZ DEFAULT NOW(),
    updated_by           VARCHAR(50) DEFAULT 'system'
);
