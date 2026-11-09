package si.contactgroups.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

/** R-011-02, R-011-06, SR-06: one sheet, a header row, text cells only. */
class XlsxWriterTest {

  private static Workbook read(byte[] bytes) throws Exception {
    return new XSSFWorkbook(new ByteArrayInputStream(bytes));
  }

  @Test
  void writesOneSheetWithTheHeaderRowAndOneRowPerContact() throws Exception {
    byte[] file = XlsxWriter.write(List.of("A", "B"), List.of(List.of("1", ""), List.of("2", "x")));

    try (Workbook book = read(file)) {
      assertThat(book.getNumberOfSheets()).isEqualTo(1);
      Sheet sheet = book.getSheetAt(0);
      assertThat(sheet.getLastRowNum()).isEqualTo(2);
      assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("A");
      assertThat(sheet.getRow(2).getCell(1).getStringCellValue()).isEqualTo("x");
    }
  }

  @Test
  void everyCellIsATextCellEvenWhenTheValueLooksLikeAFormulaOrNumber() throws Exception {
    List<String> values = List.of("=1+2", "+386 40 123 456", "-1+1", "@x", "123");
    byte[] file = XlsxWriter.write(List.of("a", "b", "c", "d", "e"), List.of(values));

    try (Workbook book = read(file)) {
      var row = book.getSheetAt(0).getRow(1);
      for (int i = 0; i < values.size(); i++) {
        Cell cell = row.getCell(i);
        assertThat(cell.getCellType()).isEqualTo(CellType.STRING);
        assertThat(cell.getStringCellValue()).isEqualTo(values.get(i));
      }
    }
  }

  @Test
  void keepsALongTextInFullAndNonAsciiLetters() throws Exception {
    String notes = "ŠČŽ".repeat(1500);
    byte[] file = XlsxWriter.write(List.of("n"), List.of(List.of(notes)));

    try (Workbook book = read(file)) {
      assertThat(book.getSheetAt(0).getRow(1).getCell(0).getStringCellValue()).isEqualTo(notes);
    }
  }
}
