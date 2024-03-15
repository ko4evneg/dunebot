package ru.trainithard.dunebot.service.report;

import com.itextpdf.text.DocumentException;
import ru.trainithard.dunebot.model.ModType;

import java.io.IOException;
import java.nio.file.Path;
import java.time.YearMonth;

public interface MonthlyRatingCalculationService {
    void storeAndSendMonthRating(YearMonth month, ModType modType, Path dir) throws DocumentException, IOException;
}
