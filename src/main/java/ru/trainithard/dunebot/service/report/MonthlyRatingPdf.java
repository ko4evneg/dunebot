package ru.trainithard.dunebot.service.report;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.util.List;

class MonthlyRatingPdf {
    private static final BaseColor TABLE_HEADER_BACKGROUND_COLOR = new BaseColor(180, 200, 200);
    private static final BaseColor EVEN_COLUMN_BACKGROUND_COLOR = new BaseColor(240, 250, 250);
    private static final List<String> TABLE_HEADER_COLUMNS_NAMES = List.of("Место", "Имя (Никнейм) Фамилия",
            "Кол-во партий", "1-х мест", "2-х мест", "3-х мест", "4-х мест", "Коэф. эффективности", "Коэф. побед");
    private static final float[] COLUMN_WIDTHS = new float[]{8, 30, 8, 6, 6, 6, 6, 10, 10};
    private static final Font TABLE_CELL_FONT = FontFactory.getFont("fonts/arial.ttf", "cp1251", 12f, Font.FontStyle.NORMAL.ordinal(), BaseColor.BLACK);

    private final Document document = new Document();
    private final String documentHeader;
    private final List<List<String>> tableRows;

    public MonthlyRatingPdf(String documentHeader, List<List<String>> tableRows) {
        this.documentHeader = documentHeader;
        this.tableRows = tableRows;
    }

    public byte[] getPdfBytes() throws DocumentException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, outputStream);

        document.open();
        addHeader();
        addTable();
        document.close();

        return outputStream.toByteArray();
    }

    private void addHeader() throws DocumentException {
        Paragraph headerParagraph = new Paragraph();
        headerParagraph.setAlignment(Element.ALIGN_CENTER);
        headerParagraph.setSpacingAfter(20f);

        Font headerFont = FontFactory.getFont("fonts/arial.ttf", "cp1251", 22, Font.FontStyle.BOLD.ordinal(), BaseColor.BLACK);
        Chunk chunk = new Chunk(documentHeader, headerFont);

        headerParagraph.add(chunk);
        document.add(headerParagraph);
    }

    private void addTable() throws DocumentException {
        Paragraph tableParagraph = new Paragraph();
        tableParagraph.setAlignment(Element.ALIGN_CENTER);

        PdfPTable table = new PdfPTable(COLUMN_WIDTHS);
        table.setWidthPercentage(95f);
        addTableHeader(table);
        addTableRows(table);

        tableParagraph.add(table);
        document.add(tableParagraph);
    }

    private void addTableHeader(PdfPTable table) {
        for (String columnTitle : TABLE_HEADER_COLUMNS_NAMES) {
            PdfPCell header = new PdfPCell(new Phrase(columnTitle, TABLE_CELL_FONT));
            header.setBackgroundColor(TABLE_HEADER_BACKGROUND_COLOR);
            alignCenter(header);
            table.addCell(header);
        }
    }

    private void addTableRows(PdfPTable table) {
        int cellIndex = 1;
        for (List<String> row : tableRows) {
            for (String cellValue : row) {
                PdfPCell cell = new PdfPCell(new Phrase(cellValue, TABLE_CELL_FONT));
                if (cellIndex % COLUMN_WIDTHS.length == 2) {
                    alignLeft(cell);
                } else {
                    alignCenter(cell);
                }
                if ((cellIndex - 1) / COLUMN_WIDTHS.length % 2 != 0) {
                    cell.setBackgroundColor(EVEN_COLUMN_BACKGROUND_COLOR);
                }
                table.addCell(cell);

                cellIndex++;
            }
        }
    }

    private void alignCenter(PdfPCell cell) {
        cell.setUseAscender(true);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
    }

    private void alignLeft(PdfPCell cell) {
        cell.setUseAscender(true);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
    }
}
