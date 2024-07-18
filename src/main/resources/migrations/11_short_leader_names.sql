ALTER TABLE LEADERS
    ADD COLUMN SHORT_NAME VARCHAR;

UPDATE LEADERS
SET SHORT_NAME = 'Ильбан'
WHERE NAME = 'Граф Ильбан Ричез';
UPDATE LEADERS
SET SHORT_NAME = 'Тессия'
WHERE NAME = 'Тессия Верниус';
UPDATE LEADERS
SET SHORT_NAME = 'Барон'
WHERE NAME = 'Барон Владимир Харконен';
UPDATE LEADERS
SET SHORT_NAME = 'Юна'
WHERE NAME = 'Принцесса Юна Моритани';
UPDATE LEADERS
SET SHORT_NAME = 'Раббан'
WHERE NAME = 'Глоссу "Зверь" Раббан';
UPDATE LEADERS
SET SHORT_NAME = 'Лето'
WHERE NAME = 'Герцог Лето Атрейдес';
UPDATE LEADERS
SET SHORT_NAME = 'Пол'
WHERE NAME = 'Пол Атрейдес';
UPDATE LEADERS
SET SHORT_NAME = 'Хундро'
WHERE NAME = 'Виконт Хундро Моритани';
UPDATE LEADERS
SET SHORT_NAME = 'Елена'
WHERE NAME = 'Елена Ричез';
UPDATE LEADERS
SET SHORT_NAME = 'Арманд'
WHERE NAME = 'Эрцгерцог Арманд Эказ';
UPDATE LEADERS
SET SHORT_NAME = 'Ариана'
WHERE NAME = 'Графиня Ариана Торвальд';
UPDATE LEADERS
SET SHORT_NAME = 'Ромбур'
WHERE NAME = 'Принц Ромбур';
UPDATE LEADERS
SET SHORT_NAME = 'Мемнон'
WHERE NAME = 'Граф Мемнон Торвальд';
UPDATE LEADERS
SET SHORT_NAME = 'Илеса'
WHERE NAME = 'Илеса Эказ';
UPDATE LEADERS
SET SHORT_NAME = NAME
WHERE MOD_TYPE != 'CLASSIC';

ALTER TABLE LEADERS
    ALTER COLUMN SHORT_NAME SET NOT NULL;
