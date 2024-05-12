ALTER TABLE SETTINGS
    RENAME TO APP_SETTINGS;

CREATE TABLE USER_SETTINGS
(
    ID           BIGSERIAL PRIMARY KEY,
    PLAYER_ID    BIGINT REFERENCES PLAYERS (ID),
    SETTING_NAME VARCHAR     NOT NULL,
    VALUE        VARCHAR     NOT NULL,
    CREATED_AT   TIMESTAMPTZ NOT NULL,
    UPDATED_AT   TIMESTAMPTZ
)
