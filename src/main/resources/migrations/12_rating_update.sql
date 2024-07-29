CREATE TABLE LEADER_RATINGS
(
    ID                 BIGSERIAL PRIMARY KEY,
    LEADER_ID          BIGINT REFERENCES LEADERS (ID) NOT NULL,
    RATING_DATE        DATE                           NOT NULL,
    MATCHES_COUNT      INT                            NOT NULL,
    EFFICIENCY         DOUBLE PRECISION               NOT NULL,
    WIN_RATE           DOUBLE PRECISION               NOT NULL,
    FIRST_PLACE_COUNT  INT                            NOT NULL,
    SECOND_PLACE_COUNT INT                            NOT NULL,
    THIRD_PLACE_COUNT  INT                            NOT NULL,
    FOURTH_PLACE_COUNT INT                            NOT NULL,
    CREATED_AT         TIMESTAMP WITH TIME ZONE       NOT NULL,
    UPDATED_AT         TIMESTAMP WITH TIME ZONE
);

CREATE TABLE PLAYER_RATINGS
(
    ID                    BIGSERIAL PRIMARY KEY,
    PLAYER_ID             BIGINT REFERENCES PLAYERS (ID) NOT NULL,
    RATING_DATE           DATE                           NOT NULL,
    MATCHES_COUNT         INT                            NOT NULL,
    EFFICIENCY            DOUBLE PRECISION               NOT NULL,
    WIN_RATE              DOUBLE PRECISION               NOT NULL,
    FIRST_PLACE_COUNT     INT                            NOT NULL,
    SECOND_PLACE_COUNT    INT                            NOT NULL,
    THIRD_PLACE_COUNT     INT                            NOT NULL,
    FOURTH_PLACE_COUNT    INT                            NOT NULL,
    CURRENT_STRIKE_LENGTH INT                            NOT NULL,
    MAX_STRIKE_LENGTH     INT                            NOT NULL,
    IS_PREVIOUSLY_WON     BOOLEAN                        NOT NULL,
    CREATED_AT            TIMESTAMP WITH TIME ZONE       NOT NULL,
    UPDATED_AT            TIMESTAMP WITH TIME ZONE
);


CREATE TABLE META_DATA
(
    ID         BIGSERIAL PRIMARY KEY,
    TYPE       VARCHAR                  NOT NULL,
    VALUE      VARCHAR,
    CREATED_AT TIMESTAMP WITH TIME ZONE NOT NULL,
    UPDATED_AT TIMESTAMP WITH TIME ZONE
)
