package io.nextpos.reporting.service;

import io.nextpos.client.data.Client;
import io.nextpos.datetime.data.ZonedDateRange;
import io.nextpos.timecard.data.UserTimeCard;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.List;

public interface SpreadsheetService {

    String CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    Workbook createUserTimeCardSpreadsheet(Client client, ZonedDateRange dateRange, List<UserTimeCard> userTimeCards);

    void sendWorkBookNotification(Client client, String email, ZonedDateRange dateRange, Workbook workbook);
}
