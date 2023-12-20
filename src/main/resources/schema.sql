CREATE TABLE IF NOT EXISTS PLAYERS
(
    ID          BIGSERIAL PRIMARY KEY,
    TELEGRAM_ID BIGINT UNIQUE  NOT NULL,
    STEAM_NAME  VARCHAR UNIQUE NOT NULL,
    FIRST_NAME  VARCHAR        NOT NULL,
    LAST_NAME   VARCHAR,
    USER_NAME   VARCHAR,
    CREATED_AT  TIMESTAMPTZ    NOT NULL,
    UPDATED_AT  TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS MATCHES
(
    ID                       BIGSERIAL PRIMARY KEY,
    TELEGRAM_POLL_ID         VARCHAR UNIQUE,
    TELEGRAM_MESSAGE_ID      BIGINT UNIQUE,
    OWNER_ID                 BIGINT REFERENCES PLAYERS (ID),
    REGISTERED_PLAYERS_COUNT INTEGER     NOT NULL,
    IS_FINISHED              BOOLEAN     NOT NULL DEFAULT FALSE,
    MOD_TYPE                 VARCHAR     NOT NULL,
    CREATED_AT               TIMESTAMPTZ NOT NULL,
    UPDATED_AT               TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS MATCH_PLAYERS
(
    ID         BIGSERIAL PRIMARY KEY,
    MATCH_ID   BIGINT REFERENCES MATCHES (ID),
    PLAYER_ID  BIGINT REFERENCES PLAYERS (ID),
    PLACE      INTEGER,
    CREATED_AT TIMESTAMPTZ NOT NULL,
    UPDATED_AT TIMESTAMPTZ,
    CONSTRAINT UNQ_MATCH_PLAYER UNIQUE (MATCH_ID, PLAYER_ID)
);
