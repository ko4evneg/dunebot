CREATE TABLE IF NOT EXISTS PLAYERS
(
    ID               BIGSERIAL PRIMARY KEY,
    EXTERNAL_ID      BIGINT      NOT NULL,
    EXTERNAL_CHAT_ID BIGINT      NOT NULL,
    STEAM_NAME       VARCHAR     NOT NULL,
    FIRST_NAME       VARCHAR     NOT NULL,
    LAST_NAME        VARCHAR,
    EXTERNAL_NAME    VARCHAR,
    CREATED_AT       TIMESTAMPTZ NOT NULL,
    UPDATED_AT       TIMESTAMPTZ,
    CONSTRAINT UNQ_TELEGRAM_ID UNIQUE (EXTERNAL_ID),
    CONSTRAINT UNQ_STEAM_NAME UNIQUE (STEAM_NAME)
);

CREATE TABLE IF NOT EXISTS MATCHES
(
    ID                         BIGSERIAL PRIMARY KEY,
    EXTERNAL_POLL_ID           VARCHAR,
    OWNER_ID                   BIGINT REFERENCES PLAYERS (ID),
    POSITIVE_ANSWERS_COUNT     INTEGER     NOT NULL DEFAULT 0,
    SUBMITS_COUNT              INTEGER     NOT NULL DEFAULT 0,
    IS_FINISHED                BOOLEAN     NOT NULL DEFAULT FALSE,
    IS_ONSUBMIT                BOOLEAN     NOT NULL DEFAULT FALSE,
    MOD_TYPE                   VARCHAR     NOT NULL,
    EXTERNAL_MESSAGE_ID        BIGINT,
    EXTERNAL_REPLY_ID          BIGINT,
    EXTERNAL_CHAT_ID           VARCHAR,
    EXTERNAL_SUBMIT_MESSAGE_ID BIGINT,
    EXTERNAL_SUBMIT_REPLY_ID   BIGINT,
    EXTERNAL_SUBMIT_CHAT_ID    VARCHAR,
    CREATED_AT                 TIMESTAMPTZ NOT NULL,
    UPDATED_AT                 TIMESTAMPTZ,
    CONSTRAINT UNQ_TELEGRAM_POLL_ID UNIQUE (EXTERNAL_POLL_ID)
);

CREATE TABLE IF NOT EXISTS MATCH_PLAYERS
(
    ID                  BIGSERIAL PRIMARY KEY,
    MATCH_ID            BIGINT REFERENCES MATCHES (ID),
    PLAYER_ID           BIGINT REFERENCES PLAYERS (ID),
    EXTERNAL_MESSAGE_ID BIGINT,
    EXTERNAL_REPLY_ID   BIGINT,
    EXTERNAL_CHAT_ID    VARCHAR,
    PLACE               INTEGER,
    CANDIDATE_PLACE     INTEGER,
    CREATED_AT          TIMESTAMPTZ NOT NULL,
    UPDATED_AT          TIMESTAMPTZ,
    CONSTRAINT UNQ_MATCH_PLAYER UNIQUE (MATCH_ID, PLAYER_ID)
);
