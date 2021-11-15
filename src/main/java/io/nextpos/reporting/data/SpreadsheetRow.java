package io.nextpos.reporting.data;

import lombok.Data;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import java.util.ArrayList;
import java.util.List;

@Data
public class SpreadsheetRow {

    private List<String> values = new ArrayList<>();

    public void addValue(String value) {
        values.add(value);
    }

    public void populateRowData(Row row) {

        for (int i = 0; i < values.size(); i++) {
            final Cell cell = row.createCell(i);
            cell.setCellValue(values.get(i));
        }
    }
}
