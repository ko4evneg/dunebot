DROP TABLE IF EXISTS MATCH_PLAYERS;
DROP TABLE IF EXISTS MATCHES;
DROP TABLE IF EXISTS USER_SETTINGS;
DROP TABLE IF EXISTS PLAYERS;
DROP TABLE IF EXISTS EXTERNAL_MESSAGES;
DROP TABLE IF EXISTS SETTINGS;
DROP TABLE IF EXISTS LEADERS;
DROP TABLE IF EXISTS DUNEBOT_TASKS;

CREATE TABLE IF NOT EXISTS EXTERNAL_MESSAGES
(
    ID         BIGSERIAL PRIMARY KEY,
    DTYPE      VARCHAR,
    MESSAGE_ID BIGINT,
    CHAT_ID    BIGINT,
    REPLY_ID   BIGINT,
    POLL_ID    VARCHAR,
    CREATED_AT TIMESTAMP WITH TIME ZONE NOT NULL,
    UPDATED_AT TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS PLAYERS
(
    ID                  BIGSERIAL PRIMARY KEY,
    EXTERNAL_ID         BIGINT                   NOT NULL,
    EXTERNAL_CHAT_ID    BIGINT                   NOT NULL,
    STEAM_NAME          VARCHAR                  NOT NULL,
    FIRST_NAME          VARCHAR                  NOT NULL,
    LAST_NAME           VARCHAR                  NOT NULL,
    EXTERNAL_FIRST_NAME VARCHAR                  NOT NULL,
    EXTERNAL_NAME       VARCHAR,
    IS_GUEST            BOOLEAN DEFAULT FALSE,
    IS_CHAT_BLOCKED BOOLEAN DEFAULT FALSE,
    CREATED_AT          TIMESTAMP WITH TIME ZONE NOT NULL,
    UPDATED_AT          TIMESTAMP WITH TIME ZONE,
    CONSTRAINT UNQ_EXTERNAL_ID UNIQUE (EXTERNAL_ID),
    CONSTRAINT UNQ_STEAM_NAME UNIQUE (STEAM_NAME)
);

CREATE INDEX IDX_STEAM_NAME ON PLAYERS (STEAM_NAME);

CREATE TABLE IF NOT EXISTS LEADERS
(
    ID         BIGSERIAL PRIMARY KEY,
    NAME       VARCHAR                  NOT NULL,
    MOD_TYPE   VARCHAR                  NOT NULL,
    CREATED_AT TIMESTAMP WITH TIME ZONE NOT NULL,
    UPDATED_AT TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS MATCHES
(
    ID                     BIGSERIAL PRIMARY KEY,
    EXTERNAL_POLL_ID       BIGINT REFERENCES EXTERNAL_MESSAGES (ID),
    EXTERNAL_START_ID      BIGINT REFERENCES EXTERNAL_MESSAGES (ID),
    OWNER_ID               BIGINT REFERENCES PLAYERS (ID),
    SUBMITTER_ID BIGINT REFERENCES PLAYERS (ID),
    POSITIVE_ANSWERS_COUNT INTEGER                  NOT NULL DEFAULT 0,
    SUBMITS_RETRY_COUNT    INTEGER                  NOT NULL DEFAULT 0,
    STATE                  VARCHAR                  NOT NULL,
    SCREENSHOT_PATH VARCHAR,
    FINISH_DATE            DATE,
    MOD_TYPE               VARCHAR                  NOT NULL,
    CREATED_AT             TIMESTAMP WITH TIME ZONE NOT NULL,
    UPDATED_AT             TIMESTAMP WITH TIME ZONE,
    CONSTRAINT UNQ_TELEGRAM_POLL_ID UNIQUE (EXTERNAL_POLL_ID)
);

CREATE TABLE IF NOT EXISTS MATCH_PLAYERS
(
    ID              BIGSERIAL PRIMARY KEY,
    MATCH_ID        BIGINT REFERENCES MATCHES (ID),
    PLAYER_ID       BIGINT REFERENCES PLAYERS (ID),
    LEADER          BIGINT REFERENCES LEADERS (ID) NULL,
    PLACE           INTEGER,
    CANDIDATE_PLACE INTEGER,
    CREATED_AT      TIMESTAMP WITH TIME ZONE       NOT NULL,
    UPDATED_AT      TIMESTAMP WITH TIME ZONE,
    CONSTRAINT UNQ_MATCH_PLAYER UNIQUE (MATCH_ID, PLAYER_ID)
);

CREATE TABLE IF NOT EXISTS APP_SETTINGS
(
    ID         BIGSERIAL PRIMARY KEY,
    KEY VARCHAR UNIQUE NOT NULL,
    VALUE      VARCHAR,
    CREATED_AT TIMESTAMP WITH TIME ZONE NOT NULL,
    UPDATED_AT TIMESTAMP WITH TIME ZONE
);

CREATE TABLE USER_SETTINGS
(
    ID         BIGSERIAL PRIMARY KEY,
    PLAYER_ID  BIGINT REFERENCES PLAYERS (ID),
    KEY        VARCHAR                  NOT NULL,
    VALUE      VARCHAR                  NOT NULL,
    CREATED_AT TIMESTAMP WITH TIME ZONE NOT NULL,
    UPDATED_AT TIMESTAMP WITH TIME ZONE
);

CREATE TABLE DUNEBOT_TASKS
(
    ID         BIGSERIAL PRIMARY KEY,
    TASK_TYPE  VARCHAR                  NOT NULL,
    ENTITY_ID  BIGINT,
    STATUS     VARCHAR                  NOT NULL,
    START_TIME TIMESTAMP WITH TIME ZONE NOT NULL,
    CREATED_AT TIMESTAMP WITH TIME ZONE NOT NULL,
    UPDATED_AT TIMESTAMP WITH TIME ZONE
)
