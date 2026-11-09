package si.contactgroups.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Writes the one sheet of an Export file. Every cell is set with {@code setCellValue(String)}, so
 * it is a text cell and never a formula (R-011-06, SR-06).
 */
final class XlsxWriter {

  private XlsxWriter() {}

  static byte[] write(List<String> header, List<List<String>> rows) {
    try (XSSFWorkbook book = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = book.createSheet();
      writeRow(sheet.createRow(0), header);
      for (int i = 0; i < rows.size(); i++) {
        writeRow(sheet.createRow(i + 1), rows.get(i));
      }
      book.write(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static void writeRow(Row row, List<String> cells) {
    for (int i = 0; i < cells.size(); i++) {
      row.createCell(i).setCellValue(cells.get(i));
    }
  }
}
