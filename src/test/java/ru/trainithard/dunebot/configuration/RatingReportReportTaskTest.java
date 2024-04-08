package ru.trainithard.dunebot.configuration;

import com.itextpdf.text.DocumentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import ru.trainithard.dunebot.TestContextMock;
import ru.trainithard.dunebot.service.report.MonthlyRatingCalculationService;

import java.io.IOException;
import java.time.*;

import static org.mockito.Mockito.*;

@SpringBootTest
class RatingReportReportTaskTest extends TestContextMock {

    @Autowired
    private MonthlyRatingReportTask task;
    @MockBean
    private Clock clock;
    @MockBean
    private MonthlyRatingCalculationService service;

    @BeforeEach
    void beforeEach() {
        doReturn(ZoneId.of("UTC+3")).when(clock).getZone();
    }

    @Test
    void shouldRunServiceWhenTodayIsFirstDayOfTheMonth() throws DocumentException, IOException {
        LocalDateTime firstDay = LocalDate.of(2000, 10, 1).atTime(15, 0);
        Clock fixedClock = Clock.fixed(firstDay.toInstant(ZoneOffset.of("+03:00")), ZoneOffset.of("+03:00"));
        doReturn(fixedClock.instant()).when(clock).instant();

        task.run();

        verify(service, times(2)).storeAndSendMonthRating(any(), any(), any());
    }

    @Test
    void shouldNotRunServiceWhenTodayIsNotFirstDayOfTheMonth() throws DocumentException, IOException {
        LocalDateTime firstDay = LocalDate.of(2000, 10, 3).atTime(15, 0);
        Clock fixedClock = Clock.fixed(firstDay.toInstant(ZoneOffset.of("+03:00")), ZoneOffset.of("+03:00"));
        doReturn(fixedClock.instant()).when(clock).instant();

        task.run();

        verify(service, never()).storeAndSendMonthRating(any(), any(), any());
    }
}
