package io.nextpos.ordermanagement.service;

import io.nextpos.client.data.Client;
import io.nextpos.datetime.data.ZonedDateRange;
import io.nextpos.notification.data.DynamicEmailDetails;
import io.nextpos.notification.data.NotificationDetails;
import io.nextpos.notification.service.NotificationService;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderRepository;
import io.nextpos.ordermanagement.data.Shift;
import io.nextpos.ordermanagement.data.ShiftRepository;
import io.nextpos.ordertransaction.data.ClosingShiftTransactionReport;
import io.nextpos.ordertransaction.data.OrderTransaction;
import io.nextpos.ordertransaction.service.OrderTransactionReportService;
import io.nextpos.shared.auth.AuthenticationHelper;
import io.nextpos.shared.exception.BusinessLogicException;
import io.nextpos.shared.exception.ObjectNotFoundException;
import io.nextpos.shared.exception.ShiftException;
import io.nextpos.shared.service.annotation.MongoTransaction;
import io.nextpos.shared.util.DateTimeUtil;
import io.nextpos.workingarea.data.SinglePrintInstruction;
import io.nextpos.workingarea.service.PrinterInstructionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@MongoTransaction
public class ShiftServiceImpl implements ShiftService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShiftServiceImpl.class);

    private final OrderTransactionReportService orderTransactionReportService;

    private final PrinterInstructionService printerInstructionService;

    private final NotificationService notificationService;

    private final ShiftRepository shiftRepository;

    private final OrderRepository orderRepository;

    private final AuthenticationHelper authenticationHelper;

    @Autowired
    public ShiftServiceImpl(final OrderTransactionReportService orderTransactionReportService, PrinterInstructionService printerInstructionService, NotificationService notificationService, final ShiftRepository shiftRepository, final OrderRepository orderRepository, AuthenticationHelper authenticationHelper) {
        this.orderTransactionReportService = orderTransactionReportService;
        this.printerInstructionService = printerInstructionService;
        this.notificationService = notificationService;
        this.shiftRepository = shiftRepository;
        this.orderRepository = orderRepository;
        this.authenticationHelper = authenticationHelper;
    }

    @Override
    public Shift openShift(final String clientId, BigDecimal openingBalance) {

        final Optional<Shift> activeShift = this.getActiveShift(clientId);

        if (activeShift.isPresent()) {
            LOGGER.info("There is already an active shift. Returning active shift directly: {}", activeShift.get());
            return activeShift.get();
        }

        final String currentUser = authenticationHelper.resolveCurrentUsername();
        final Shift shift = new Shift(clientId, new Date(), currentUser, openingBalance);

        return shiftRepository.save(shift);
    }

    @Override
    public void saveShift(Shift shift) {
        shiftRepository.save(shift);
    }

    @Override
    public ClosingShiftTransactionReport getClosingShiftReport(String clientId, String shiftId) {

        final Shift shift = getShift(shiftId);

        return orderTransactionReportService.getClosingShiftTransactionReport(shift);
    }

    @Override
    public Shift balanceClosingShift(String shiftId) {

        final Shift shift = getShift(shiftId);
        shift.balanceClosingShift(orderTransactionReportService::getClosingShiftTransactionReport);

        return shiftRepository.save(shift);
    }

    @Override
    public Shift initiateCloseShift(final String clientId) {

        final Shift shift = getCurrentShiftOrThrows(clientId);
        Shift.ShiftAction.INITIATE_CLOSE.checkShiftStatus(shift);

        if (orderRepository.countByClientIdAndStateIn(clientId, Order.OrderState.inflightStates()) > 0) {
            throw new BusinessLogicException("message.completeAllOrdersFirst", "Please complete all orders before closing shift.");
        }

        shift.initiateCloseShift(orderTransactionReportService::getClosingShiftTransactionReport);

        return shiftRepository.save(shift);
    }

    @Override
    public Shift closeShift(final String clientId, Shift.ClosingBalanceDetails cash, final Shift.ClosingBalanceDetails card) {

        final Shift shift = getCurrentShiftOrThrows(clientId);
        Shift.ShiftAction.CLOSE.checkShiftStatus(shift);

        final String currentUser = authenticationHelper.resolveCurrentUsername();
        shift.closeShift(currentUser, cash, card);

        return shiftRepository.save(shift);
    }

    @Override
    public Shift confirmCloseShift(final String clientId, final String closingRemark) {

        final Shift shift = getCurrentShiftOrThrows(clientId);
        Shift.ShiftAction.CONFIRM_CLOSE.checkShiftStatus(shift);

        shift.confirmCloseShift(closingRemark);

        return shiftRepository.save(shift);
    }

    @Override
    public Shift abortCloseShift(final String clientId) {

        final Shift shift = getCurrentShiftOrThrows(clientId);
        Shift.ShiftAction.ABORT_CLOSE.checkShiftStatus(shift);

        shift.abortCloseShift();
        return shiftRepository.save(shift);
    }

    @Override
    public Optional<Shift> getActiveShift(final String clientId) {
        return shiftRepository.findByClientIdAndShiftStatus(clientId, Shift.ShiftStatus.ACTIVE);
    }

    @Override
    public Optional<Shift> getMostRecentShift(final String clientId) {
        return shiftRepository.findFirstByClientIdOrderByCreatedDateDesc(clientId);
    }

    @Override
    public Shift getActiveShiftOrThrows(final String clientId) {

        return this.getActiveShift(clientId).orElseThrow(() -> {
            throw new ShiftException(clientId);
        });
    }

    @Override
    public Shift getShift(final String shiftId) {
        return shiftRepository.findById(shiftId).orElseThrow(() -> {
            throw new ObjectNotFoundException(shiftId, Shift.class);
        });
    }

    @Override
    public List<Shift> getShifts(final String clientId, final ZonedDateRange zonedDateRange) {

        return shiftRepository.findAllByClientIdAndStart_TimestampBetween(clientId, zonedDateRange.getFromDate(), zonedDateRange.getToDate(), Sort.by(Sort.Order.desc("start.timestamp")));
    }

    @Override
    public CompletableFuture<NotificationDetails> sendShiftReport(Client client, String shiftId, String emailAddress) {

        final Shift shift = this.getShift(shiftId);

        if (!shift.getShiftStatus().isFinalState()) {
            throw new BusinessLogicException("message.shiftNotClosed", "Please close the shift before sending the shift report");
        }

        DynamicEmailDetails notificationDetails = new DynamicEmailDetails(shift.getClientId(), emailAddress, "d-a1b0553668d34b2f84a4cd1c2e1689d6");
        notificationDetails.addTemplateData("client", client.getClientName());
        notificationDetails.addTemplateData("shiftOpenDate", DateTimeUtil.formatDateTime(shift.getStart().toLocalDateTime(client.getZoneId())));
        notificationDetails.addTemplateData("shiftCloseDate", DateTimeUtil.formatDateTime(shift.getEnd().toLocalDateTime(client.getZoneId())));
        final ClosingShiftTransactionReport closingShiftReport = shift.getEnd().getClosingShiftReport();

        final Shift.ClosingBalanceDetails cashBalance = shift.getEnd().getClosingBalance(OrderTransaction.PaymentMethod.CASH);

        closingShiftReport.getShiftTotal(OrderTransaction.PaymentMethod.CASH).ifPresent(t -> {
            notificationDetails.addTemplateData("cashTotal", t.getOrderTotal());
            notificationDetails.addTemplateData("openBalance", shift.getStart().getBalance());
            notificationDetails.addTemplateData("actualCashTotal", cashBalance.getClosingBalance());
            notificationDetails.addTemplateData("cashDifference", cashBalance.getDifference());
            notificationDetails.addTemplateData("cashReason", cashBalance.getUnbalanceReason());
        });

        final Shift.ClosingBalanceDetails cardBalance = shift.getEnd().getClosingBalance(OrderTransaction.PaymentMethod.CARD);

        closingShiftReport.getShiftTotal(OrderTransaction.PaymentMethod.CARD).ifPresent(t -> {
            notificationDetails.addTemplateData("cardTotal", t.getOrderTotal());
            notificationDetails.addTemplateData("actualCardTotal", cardBalance.getClosingBalance());
            notificationDetails.addTemplateData("cardDifference", cardBalance.getDifference());
            notificationDetails.addTemplateData("cardReason", cardBalance.getUnbalanceReason());
        });

        notificationDetails.addTemplateData("shiftTotal", closingShiftReport.getOneOrderSummary().getOrderTotal());
        notificationDetails.addTemplateData("closingRemark", shift.getEnd().getClosingRemark());
        notificationDetails.addTemplateData("orderCount", closingShiftReport.getTotalOrderCount());
        notificationDetails.addTemplateData("deletedOrderCount", closingShiftReport.getOrderCount(Order.OrderState.DELETED).getOrderCount());
        notificationDetails.addTemplateData("discountTotal", closingShiftReport.getOneOrderSummary().getDiscount());
        notificationDetails.addTemplateData("serviceChargeTotal", closingShiftReport.getOneOrderSummary().getDiscount());
        notificationDetails.addTemplateData("deletedLineItems", shift.getDeletedLineItems());

        return notificationService.sendNotification(notificationDetails);
    }

    @Override
    public SinglePrintInstruction printShiftReport(Client client, String shiftId) {

        final Shift shift = this.getShift(shiftId);
        return printerInstructionService.createShiftReportPrintInstruction(client, shift);
    }

    private Shift getCurrentShiftOrThrows(String clientId) {
        return shiftRepository.findFirstByClientIdOrderByCreatedDateDesc(clientId).orElseThrow(() -> {
            throw new ObjectNotFoundException(clientId, Shift.class);
        });
    }
}
