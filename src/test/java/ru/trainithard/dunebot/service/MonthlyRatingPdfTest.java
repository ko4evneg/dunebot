package ru.trainithard.dunebot.service;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfReader;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class MonthlyRatingPdfTest {
    @Test
    void shouldBuildCorrectPdf() throws IOException, DocumentException {
        MonthlyRatingPdf monthlyRatingPdf = new MonthlyRatingPdf("РЕЙТИНГ 10.23", List.of(
                List.of("1", "Супер пупер длинное имя (ну прям ппц какое длинное)", "231", "25", "35", "45", "99", "0.19", "99.99%"),
                List.of("2", "Хехе", "1", "58", "1", "9", "12", "0.1", "0.99%"),
                List.of("3", "Имя Фам", "20", "452", "74", "12", "55", "0.31", "1%"),
                List.of("4", "сир Ланцелот", "50", "12", "35", "1", "40", "0.32", "5%"),
                List.of("5", "симпл димпл", "1000", "201", "35", "14", "99", "0.56", "50%"),
                List.of("6", "ПОПЫТ", "75", "1", "20", "45", "8", "0.34", "97%")
        ));

        PdfReader reader = new PdfReader(monthlyRatingPdf.getPdfBytes());
        PdfReader referenceReader = new PdfReader("src/test/resources/pdf/monthly_rate_example_1.pdf");
        byte[] actualBytes = reader.getPageContent(1);
        byte[] expectedBytes = referenceReader.getPageContent(1);

        assertArrayEquals(expectedBytes, actualBytes);
    }
}
