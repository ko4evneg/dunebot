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
