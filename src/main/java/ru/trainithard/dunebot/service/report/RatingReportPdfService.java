package ru.trainithard.dunebot.service.report;

import ru.trainithard.dunebot.model.ModType;

import java.time.LocalDate;

public interface RatingReportPdfService {
    RatingReportPdf createPlayersReport(LocalDate from, LocalDate to, ModType modType, String reportName);

    RatingReportPdf createLeadersReport(LocalDate from, LocalDate to, ModType modType, String reportName);
}
