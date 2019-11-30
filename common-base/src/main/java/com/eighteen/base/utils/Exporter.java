package com.eighteen.base.utils;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.List;

/**
 * Created by wangwei.
 * Date: 2019/10/4
 * Time: 17:46
 */
public interface Exporter<T> {
    default void export(HttpServletResponse response, List<T> list, String[] titles, String fileName, ExportType type, String charset) throws IOException {
        byte[] bytes = new byte[0];
        bytes = getBytes(list, titles, type, charset, bytes);
        response.setContentType("application/x-msdownload");
        response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
        response.setContentLength(bytes.length);
        response.getOutputStream().write(bytes);
        response.getOutputStream().flush();
        response.getOutputStream().close();
    }

    default void export(File file, List<T> list, String[] titles, ExportType type, String charsetname) {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            byte[] bytes = new byte[0];
            bytes = getBytes(list, titles, type, charsetname, bytes);
            fos.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    default byte[] getBytes(List<T> list, String[] titles, ExportType type, String charset, byte[] bytes) throws IOException {
        switch (type) {
            case EXCEL:
            case XLS:
                bytes = toExcel(list, titles);
                break;
            case CSV:
                bytes = toCsv(list, titles, charset);
                break;
        }
        return bytes;
    }

    default byte[] toExcel(List<T> list, String[] titles) throws IOException {
        SXSSFWorkbook workbook = new SXSSFWorkbook();

//        CellStyle cellStyle = workbook.createCellStyle();
//        cellStyle.setFillForegroundColor(IndexedColors.BLUE.index);
//
//        Font font = workbook.createFont();
//        font.setFontName("黑体");
//        font.setBold(true);
//        font.setFontHeightInPoints((short) 30);
//
//        cellStyle.setFont(font);
        SXSSFSheet sheet = workbook.createSheet();

        Row row = sheet.createRow(0);
        for (int i = 0; i < titles.length; i++) {
//            row.setRowStyle(cellStyle);
            row.createCell(i, CellType.STRING).setCellValue(titles[i]);
        }

        for (int i = 0; i < list.size(); i++) {
            T t = list.get(i);
            Row data = sheet.createRow(i + 1);
            writeRow(t, data);
        }
        sheet.flushRows();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        return out.toByteArray();
    }

    default byte[] toCsv(List<T> list, String[] titles) throws IOException {
        return toCsv(list, titles, "GBK");
    }

    default byte[] toCsv(List<T> list, String[] titles, String charsetName) throws IOException {
        CSVFormat csvFormat = CSVFormat.DEFAULT.withHeader(titles);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             OutputStreamWriter osw = new OutputStreamWriter(out, charsetName);
             CSVPrinter csvPrinter = new CSVPrinter(osw, csvFormat)) {

            for (T t : list) {
                writeRow(t, csvPrinter);
            }
            csvPrinter.flush();
            return out.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    void writeRow(T t, Object row) throws IOException;

    default String filename(String name, ExportType type) {
        String suffix = "";
        switch (type) {
            case EXCEL:
                suffix = ".xlsx";
                break;
            case CSV:
                suffix = ".csv";
                break;
            case TEXT:
                suffix = ".txt";
                break;
            case XLS:
                suffix = ".xls";
        }
        return name + suffix;
    }

    enum ExportType {
        EXCEL, XLS, CSV, TEXT;

        public <T> void export(List<T> list, Object out, Consumer<T, Object> consumer, String... titles) {
            export(list, out, consumer, "GBK", "", titles);
        }

        public <T> void export(List<T> list, Object out, Consumer<T, Object> consumer, String name, String... titles) {
            export(list, out, consumer, "GBK", name, titles);
        }

        public <T> void export(List<T> list, Object out, Consumer<T, Object> consumer, String charsetname, String filename, String... titles) {
            Exporter<T> export = consumer::accept;
            try {
                if (out instanceof File) export.export((File) out, list, titles, this, charsetname);
                if (out instanceof HttpServletResponse)
                    export.export((HttpServletResponse) out, list, titles, export.filename(filename, this), this, charsetname);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
