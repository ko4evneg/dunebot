CREATE TABLE IF NOT EXISTS EXTERNAL_MESSAGES
(
    ID         BIGSERIAL PRIMARY KEY,
    DTYPE      VARCHAR,
    MESSAGE_ID BIGINT,
    CHAT_ID    BIGINT,
    REPLY_ID   BIGINT,
    POLL_ID    VARCHAR,
    CREATED_AT TIMESTAMPTZ NOT NULL,
    UPDATED_AT TIMESTAMPTZ
);

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
    ID                     BIGSERIAL PRIMARY KEY,
    EXTERNAL_POLL_ID       BIGINT REFERENCES EXTERNAL_MESSAGES (ID),
    EXTERNAL_START_ID      BIGINT REFERENCES EXTERNAL_MESSAGES (ID),
    OWNER_ID               BIGINT REFERENCES PLAYERS (ID),
    POSITIVE_ANSWERS_COUNT INTEGER     NOT NULL DEFAULT 0,
    SUBMITS_COUNT          INTEGER     NOT NULL DEFAULT 0,
    SUBMITS_RETRY_COUNT    INTEGER     NOT NULL DEFAULT 0,
    STATE                  VARCHAR     NOT NULL,
    IS_ONSUBMIT            BOOLEAN     NOT NULL DEFAULT FALSE,
    HAS_ONSUBMIT_PHOTO     BOOLEAN     NOT NULL DEFAULT FALSE,
    MOD_TYPE               VARCHAR     NOT NULL,
    CREATED_AT             TIMESTAMPTZ NOT NULL,
    UPDATED_AT             TIMESTAMPTZ,
    CONSTRAINT UNQ_TELEGRAM_POLL_ID UNIQUE (EXTERNAL_POLL_ID)
);

CREATE TABLE IF NOT EXISTS MATCH_PLAYERS
(
    ID                 BIGSERIAL PRIMARY KEY,
    MATCH_ID           BIGINT REFERENCES MATCHES (ID),
    PLAYER_ID          BIGINT REFERENCES PLAYERS (ID),
    EXTERNAL_SUBMIT_ID BIGINT REFERENCES EXTERNAL_MESSAGES (ID),
    PLACE              INTEGER,
    CANDIDATE_PLACE    INTEGER,
    CREATED_AT         TIMESTAMPTZ NOT NULL,
    UPDATED_AT         TIMESTAMPTZ,
    CONSTRAINT UNQ_MATCH_PLAYER UNIQUE (MATCH_ID, PLAYER_ID)
);
