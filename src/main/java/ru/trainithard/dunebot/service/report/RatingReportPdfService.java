package ru.trainithard.dunebot.service.report;

import ru.trainithard.dunebot.model.ModType;

import java.time.LocalDate;

public interface RatingReportPdfService {
    RatingReportPdf createRating(LocalDate from, LocalDate to, ModType modType, String reportName);
}
