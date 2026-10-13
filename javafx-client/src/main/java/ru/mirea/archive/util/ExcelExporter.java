package ru.mirea.archive.util;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ru.mirea.archive.model.DocumentRequest;
import ru.mirea.archive.model.User;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ExcelExporter {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private ExcelExporter() {
    }

    public static String exportToExcel(List<User> users, List<DocumentRequest> requests, String filePath)
            throws IOException {

        File target = new File(filePath);
        if (target.getParentFile() != null) {
            target.getParentFile().mkdirs();
        }

        try (Workbook workbook = new XSSFWorkbook()) {

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            Sheet userSheet = workbook.createSheet("Пользователи");
            String[] userHeaders = {"ID", "ФИО", "Email", "Телефон", "Отдел", "Дата регистрации"};
            Row userHeaderRow = userSheet.createRow(0);
            for (int i = 0; i < userHeaders.length; i++) {
                Cell cell = userHeaderRow.createCell(i);
                cell.setCellValue(userHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (User u : users) {
                Row row = userSheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(u.getId());
                row.createCell(1).setCellValue(u.getFullName());
                row.createCell(2).setCellValue(u.getEmail());
                row.createCell(3).setCellValue(u.getPhone() != null ? u.getPhone() : "");
                row.createCell(4).setCellValue(u.getDepartment() != null ? u.getDepartment() : "");
                row.createCell(5).setCellValue(u.getCreatedAt() != null ? u.getCreatedAt().format(FMT) : "");
            }

            for (int i = 0; i < userHeaders.length; i++) {
                userSheet.autoSizeColumn(i);
            }

            Sheet reqSheet = workbook.createSheet("Запросы на документы");
            String[] reqHeaders = {"ID", "Пользователь", "Документ", "Описание", "Категория",
                    "Статус", "Приоритет", "Создан", "Обновлен"};
            Row reqHeaderRow = reqSheet.createRow(0);
            for (int i = 0; i < reqHeaders.length; i++) {
                Cell cell = reqHeaderRow.createCell(i);
                cell.setCellValue(reqHeaders[i]);
                cell.setCellStyle(headerStyle);
            }

            rowIdx = 1;
            for (DocumentRequest r : requests) {
                Row row = reqSheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(r.getId());
                row.createCell(1).setCellValue(r.getUserName() != null ? r.getUserName() : String.valueOf(r.getUserId()));
                row.createCell(2).setCellValue(r.getDocumentTitle());
                row.createCell(3).setCellValue(r.getDescription() != null ? r.getDescription() : "");
                row.createCell(4).setCellValue(r.getCategory());
                row.createCell(5).setCellValue(r.getStatus() != null ? r.getStatus().getDisplayName() : "");
                row.createCell(6).setCellValue(r.getPriority() != null ? r.getPriority().getDisplayName() : "");
                row.createCell(7).setCellValue(r.getCreatedAt() != null ? r.getCreatedAt().format(FMT) : "");
                row.createCell(8).setCellValue(r.getUpdatedAt() != null ? r.getUpdatedAt().format(FMT) : "");
            }

            for (int i = 0; i < reqHeaders.length; i++) {
                reqSheet.autoSizeColumn(i);
            }

            try (FileOutputStream fos = new FileOutputStream(target)) {
                workbook.write(fos);
            }
        }

        return target.getAbsolutePath();
    }

    public static String exportUsersToCsv(List<User> users, String filePath) throws IOException {
        File target = new File(filePath);
        if (target.getParentFile() != null) {
            target.getParentFile().mkdirs();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("ID;ФИО;Email;Телефон;Отдел;Дата регистрации\n");

        for (User u : users) {
            sb.append(u.getId()).append(";");
            sb.append(escapeCsv(u.getFullName())).append(";");
            sb.append(escapeCsv(u.getEmail())).append(";");
            sb.append(escapeCsv(u.getPhone() != null ? u.getPhone() : "")).append(";");
            sb.append(escapeCsv(u.getDepartment() != null ? u.getDepartment() : "")).append(";");
            sb.append(u.getCreatedAt() != null ? u.getCreatedAt().format(FMT) : "").append("\n");
        }

        try (FileOutputStream fos = new FileOutputStream(target)) {
            fos.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
            fos.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        }

        return target.getAbsolutePath();
    }

    public static String exportRequestsToCsv(List<DocumentRequest> requests, String filePath) throws IOException {
        File target = new File(filePath);
        if (target.getParentFile() != null) {
            target.getParentFile().mkdirs();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("ID;Пользователь;Документ;Описание;Категория;Статус;Приоритет;Создан;Обновлен\n");

        for (DocumentRequest r : requests) {
            sb.append(r.getId()).append(";");
            sb.append(escapeCsv(r.getUserName() != null ? r.getUserName() : String.valueOf(r.getUserId()))).append(";");
            sb.append(escapeCsv(r.getDocumentTitle())).append(";");
            sb.append(escapeCsv(r.getDescription() != null ? r.getDescription() : "")).append(";");
            sb.append(escapeCsv(r.getCategory())).append(";");
            sb.append(r.getStatus() != null ? r.getStatus().getDisplayName() : "").append(";");
            sb.append(r.getPriority() != null ? r.getPriority().getDisplayName() : "").append(";");
            sb.append(r.getCreatedAt() != null ? r.getCreatedAt().format(FMT) : "").append(";");
            sb.append(r.getUpdatedAt() != null ? r.getUpdatedAt().format(FMT) : "").append("\n");
        }

        try (FileOutputStream fos = new FileOutputStream(target)) {
            fos.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
            fos.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        }

        return target.getAbsolutePath();
    }

    public static String exportToCsv(List<DocumentRequest> requests, String filePath) throws IOException {
        return exportRequestsToCsv(requests, filePath);
    }

    private static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(";") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
