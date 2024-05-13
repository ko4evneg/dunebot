package ru.trainithard.dunebot.service.telegram.command.processor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.model.MatchState;
import ru.trainithard.dunebot.model.ModType;
import ru.trainithard.dunebot.model.UserSettingKey;
import ru.trainithard.dunebot.model.messaging.ChatType;
import ru.trainithard.dunebot.service.messaging.dto.MessageDto;
import ru.trainithard.dunebot.service.telegram.command.Command;
import ru.trainithard.dunebot.service.telegram.command.CommandMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
class HostCommandProcessorTest extends TestContextMock {
    @Autowired
    private HostCommandProcessor processor;

    @BeforeEach
    void beforeEach() {
        jdbcTemplate.execute("insert into players (id, external_id, external_chat_id, steam_name, first_name, last_name, external_first_name, created_at) " +
                             "values (10000, 12345, 9000, 'st_pl', 'name', 'l1', 'e1', '2010-10-10') ");
        jdbcTemplate.execute("insert into matches (id, owner_id, mod_type, state, positive_answers_count, created_at) " +
                             "values (10000, 10000, '" + ModType.CLASSIC + "', '" + MatchState.NEW + "', 1, '2010-10-10') ");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                             "values (10000, 10000, 10000, '2010-10-10')");
        jdbcTemplate.execute("insert into user_settings (id, player_id, key, value, created_at) " +
                             "values (10000, 10000, '" + UserSettingKey.HOST + "', 'srv/psw', '2010-10-10')");
        jdbcTemplate.execute("insert into app_settings (id, key, value, created_at) values (10000, 'CHAT_ID', 'strVal', '2010-01-02')");
        jdbcTemplate.execute("insert into app_settings (id, key, value, created_at) values (10001, 'TOPIC_ID_CLASSIC', '5', '2010-01-02')");
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("delete from match_players where match_id in (select id from matches where id between 10000 and 10001)");
        jdbcTemplate.execute("delete from matches where id between 10000 and 10001");
        jdbcTemplate.execute("delete from user_settings where id = 10000");
        jdbcTemplate.execute("delete from players where id = 10000");
        jdbcTemplate.execute("delete from app_settings where id between 10000 and 10001");
    }

    @Test
    void shouldSendMessageOnCommandWhenAllDataPresented() {
        processor.process(getCommandMessage());

        ArgumentCaptor<MessageDto> messageDtoCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService, times(1)).sendMessageAsync(messageDtoCaptor.capture());
        MessageDto messageDto = messageDtoCaptor.getValue();

        assertThat(messageDto)
                .extracting(MessageDto::getChatId, MessageDto::getTopicId, MessageDto::getText)
                .containsExactly("strVal", 5, """
                        Игрок name \\(st\\_pl\\) l1 предлагает свой сервер для *матча 10000*\\.
                        Сервер: srv/psw""");
    }

    @Test
    void shouldSelectLatestMatchForHosting() {
        jdbcTemplate.execute("insert into matches (id, owner_id, mod_type, state, positive_answers_count, created_at) " +
                             "values (10001, 10000, '" + ModType.CLASSIC + "', '" + MatchState.NEW + "', 1, '2010-10-11') ");
        jdbcTemplate.execute("insert into match_players (id, match_id, player_id, created_at) " +
                             "values (10001, 10001, 10000, '2010-10-11')");

        processor.process(getCommandMessage());

        ArgumentCaptor<MessageDto> messageDtoCaptor = ArgumentCaptor.forClass(MessageDto.class);
        verify(messagingService, times(1)).sendMessageAsync(messageDtoCaptor.capture());
        MessageDto messageDto = messageDtoCaptor.getValue();

        assertThat(messageDto)
                .extracting(MessageDto::getChatId, MessageDto::getText)
                .containsExactly("9000", """
                        Игрок name \\(st\\_pl\\) l1 предлагает свой сервер для *матча 10001*\\.
                        Сервер: srv/psw""");
    }

    @Test
    void shouldReturnHostCommand() {
        Command actualCommand = processor.getCommand();

        assertThat(actualCommand).isEqualTo(Command.HOST);
    }

    private CommandMessage getCommandMessage() {
        Chat chat = new Chat();
        chat.setId(9000L);
        chat.setType(ChatType.PRIVATE.getValue());

        User user = new User();
        user.setId(12345L);

        Message message = new Message();
        message.setMessageId(10000);
        message.setText("/host");
        message.setChat(chat);
        message.setFrom(user);

        return CommandMessage.getMessageInstance(message);
    }
}
