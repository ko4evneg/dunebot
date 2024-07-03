-- ADD MOCK PLAYERS
INSERT INTO PLAYERS (ID, external_id, external_chat_id, STEAM_NAME, FIRST_NAME, last_name, external_first_name,
                     CREATED_AT)
VALUES (1001, 193506611, '193506611', 'Ko4evneG1', 'Alex1', 'l1', 'player1', '2010-01-10');
INSERT INTO PLAYERS (ID, external_id, external_chat_id, STEAM_NAME, FIRST_NAME, last_name, external_first_name,
                     CREATED_AT)
VALUES (1002, 193506612, '193506612', 'Ko4evneG2', 'Alex2', 'l2', 'player1', '2010-01-10');
INSERT INTO PLAYERS (ID, external_id, external_chat_id, STEAM_NAME, FIRST_NAME, last_name, external_first_name,
                     CREATED_AT)
VALUES (1003, 193506613, '193506613', 'Ko4evneG3', 'Alex3', 'l4', 'player2', '2010-01-10');
INSERT INTO PLAYERS (ID, external_id, external_chat_id, STEAM_NAME, FIRST_NAME, last_name, external_first_name,
                     CREATED_AT)
VALUES (1004, 193506614, '193506614', 'Ko4evneG4', 'Alex4', 'l34', 'player3', '2010-01-10');
update matches
set positive_answers_count = 5
where id = 36;

-- ADD POSITIVE VOTES TO THE LAST CREATEDMATCH
DO
$$
    DECLARE
        match_id INT;
    BEGIN
        select max(id) into match_id from matches;
        -- MOCK VOTING
        delete from match_players where id between 1001 and 1004;
        insert into match_players (id, match_id, player_id, created_at) values (1001, match_id, 1001, '2020-10-10');
        insert into match_players (id, match_id, player_id, created_at) values (1002, match_id, 1002, '2020-10-10');
        insert into match_players (id, match_id, player_id, created_at) values (1003, match_id, 1003, '2020-10-10');
        update matches set positive_answers_count = 3 where id = match_id;
    END
$$;
