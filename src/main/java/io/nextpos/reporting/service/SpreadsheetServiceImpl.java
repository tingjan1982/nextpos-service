package io.nextpos.reporting.service;

import io.nextpos.client.data.Client;
import io.nextpos.datetime.data.ZonedDateRange;
import io.nextpos.notification.data.DynamicEmailDetails;
import io.nextpos.notification.service.NotificationService;
import io.nextpos.reporting.data.SpreadsheetRow;
import io.nextpos.shared.exception.BusinessLogicException;
import io.nextpos.shared.util.DateTimeUtil;
import io.nextpos.timecard.data.UserTimeCard;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.bson.types.Binary;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SpreadsheetServiceImpl implements SpreadsheetService {

    private final NotificationService notificationService;

    public SpreadsheetServiceImpl(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Override
    public Workbook createUserTimeCardSpreadsheet(Client client, ZonedDateRange dateRange, List<UserTimeCard> userTimeCards) {

        final SpreadsheetRow criteria = new SpreadsheetRow();
        criteria.addValue("Date Range");
        criteria.addValue(DateTimeUtil.formatDate(client.getZoneId(), dateRange.getFromDate()));
        criteria.addValue(DateTimeUtil.formatDate(client.getZoneId(), dateRange.getToDate()));

        final List<SpreadsheetRow> timeCardRows = new ArrayList<>();
        final List<SpreadsheetRow> summaryRows = new ArrayList<>();

        timeCardRows.add(criteria);
        timeCardRows.add(new SpreadsheetRow());

        summaryRows.add(criteria);
        summaryRows.add(new SpreadsheetRow());

        final SpreadsheetRow tcHeader = new SpreadsheetRow();
        tcHeader.addValue("Name");
        tcHeader.addValue("Date");
        tcHeader.addValue("Start Time");
        tcHeader.addValue("End Time");
        tcHeader.addValue("Hour(s)");
        tcHeader.addValue("Minute(s)");
        tcHeader.addValue("Actual Hour(s)");
        tcHeader.addValue("Actual Minute(s)");
        timeCardRows.add(tcHeader);

        SpreadsheetRow summaryHeader = new SpreadsheetRow();
        summaryHeader.addValue("Name");
        summaryHeader.addValue("Hour(s)");
        summaryHeader.addValue("Minute(s)");
        summaryHeader.addValue("Actual Hour(s)");
        summaryHeader.addValue("Actual Minute(s)");
        summaryRows.add(summaryHeader);

        final Map<String, List<UserTimeCard>> groupedTimeCards = userTimeCards.stream()
                .collect(Collectors.groupingBy(UserTimeCard::getNickname));

        groupedTimeCards.forEach((name, tcs) -> {
            Duration total = Duration.ZERO;
            Duration actualTotal = Duration.ZERO;

            for (final UserTimeCard tc : tcs) {
                final SpreadsheetRow row = new SpreadsheetRow();
                row.addValue(tc.getNickname());
                row.addValue(DateTimeUtil.formatDate(client.getZoneId(), tc.getClockIn(), "YYYY-MM-dd"));
                row.addValue(DateTimeUtil.formatDate(client.getZoneId(), tc.getClockIn(), "HH:mm"));
                row.addValue(DateTimeUtil.formatDate(client.getZoneId(), tc.getClockOut(), "HH:mm"));
                final Duration duration = tc.getWorkingDuration();

                total = total.plus(duration);

                row.addValue(String.valueOf(duration.toHours()));
                row.addValue(String.valueOf(duration.toMinutesPart()));
                row.addValue(String.valueOf(tc.getActualWorkingHours()));
                row.addValue(String.valueOf(tc.getActualWorkingMinutes()));

                actualTotal = actualTotal.plusHours(tc.getActualWorkingHours());
                actualTotal = actualTotal.plusMinutes(tc.getActualWorkingMinutes());

                timeCardRows.add(row);
            }

            timeCardRows.add(new SpreadsheetRow());

            final SpreadsheetRow summaryRow = new SpreadsheetRow();
            summaryRow.addValue(name);
            summaryRow.addValue(String.valueOf(total.toHours()));
            summaryRow.addValue(String.valueOf(total.toMinutesPart()));
            summaryRow.addValue(String.valueOf(actualTotal.toHours()));
            summaryRow.addValue(String.valueOf(actualTotal.toMinutesPart()));
            summaryRows.add(summaryRow);
        });

        Workbook wb = new XSSFWorkbook();
        createSpreadsheet(wb, "Time Cards", timeCardRows);
        createSpreadsheet(wb, "Summary", summaryRows);

        return wb;
    }

    private void createSpreadsheet(Workbook wb, String sheetName, List<SpreadsheetRow> dataRows) {

        final Sheet sheet = wb.createSheet(sheetName);

        for (int i = 0; i < dataRows.size(); i++) {
            final SpreadsheetRow spreadsheetRow = dataRows.get(i);
            final Row row = sheet.createRow(i);
            spreadsheetRow.populateRowData(row);
        }

        for (int i = 0; i < 8; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    @Override
    public void sendWorkBookNotification(Client client, String email, ZonedDateRange dateRange, Workbook workbook) {

        final DynamicEmailDetails emailDetails = new DynamicEmailDetails(client.getId(), email, "d-f16f5ac4b1bc497aad0d0f200770f526");
        emailDetails.addTemplateData("client", client.getClientName());

        String formattedDateRange = dateRange.getFormattedFromDate() +
                " ~ " +
                dateRange.getFormattedToDate();

        emailDetails.addTemplateData("dateRange", formattedDateRange);

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            workbook.write(bos);
            emailDetails.setAttachment(new Binary(bos.toByteArray()));
            emailDetails.setContentType(SpreadsheetService.CONTENT_TYPE);
            emailDetails.setFilename("userTimeCardReport.xlsx");

        } catch (Exception e) {
            throw new BusinessLogicException("Error converting workbook to byte array");
        }

        notificationService.sendSimpleNotification(emailDetails);
    }
}
