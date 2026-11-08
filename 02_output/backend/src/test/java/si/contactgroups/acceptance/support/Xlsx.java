package si.contactgroups.acceptance.support;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Read-back of an exported {@code .xlsx} (contract {@code xlsx-export.schema.json}): sheet count,
 * header, rows as text and whether any cell is not a text cell.
 */
public record Xlsx(int sheetCount, List<String> header, List<List<String>> rows, List<String> nonText) {

  public static Xlsx read(byte[] bytes) {
    try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
      Sheet sheet = workbook.getSheetAt(0);
      List<List<String>> all = new ArrayList<>();
      List<String> nonText = new ArrayList<>();
      int width = 0;
      for (Row row : sheet) {
        width = Math.max(width, row.getLastCellNum());
      }
      for (int r = 0; r <= sheet.getLastRowNum(); r++) {
        Row row = sheet.getRow(r);
        List<String> values = new ArrayList<>();
        for (int c = 0; c < width; c++) {
          Cell cell = row == null ? null : row.getCell(c);
          if (cell == null || cell.getCellType() == CellType.BLANK) {
            values.add("");
          } else if (cell.getCellType() == CellType.STRING) {
            values.add(cell.getStringCellValue());
          } else {
            nonText.add(cell.getAddress().formatAsString() + ":" + cell.getCellType());
            values.add(
                cell.getCellType() == CellType.FORMULA ? "=" + cell.getCellFormula() : cell.toString());
          }
        }
        all.add(values);
      }
      List<String> header = all.isEmpty() ? List.of() : all.get(0);
      List<List<String>> rows = all.size() <= 1 ? List.of() : all.subList(1, all.size());
      return new Xlsx(workbook.getNumberOfSheets(), header, rows, nonText);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** The value of the named column in a row. */
  public String cell(List<String> row, String column) {
    int index = header.indexOf(column);
    if (index < 0) {
      throw new IllegalArgumentException("no column " + column + " in " + header);
    }
    return index < row.size() ? row.get(index) : "";
  }

  /** The values of one column over all rows. */
  public List<String> column(String column) {
    return rows.stream().map(row -> cell(row, column)).toList();
  }
}
