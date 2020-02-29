package io.nextpos.ordermanagement.service;

import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderRepository;
import io.nextpos.ordermanagement.data.Shift;
import io.nextpos.ordermanagement.data.ShiftRepository;
import io.nextpos.ordertransaction.service.OrderTransactionReportService;
import io.nextpos.shared.auth.OAuth2Helper;
import io.nextpos.shared.exception.BusinessLogicException;
import io.nextpos.shared.exception.ShiftException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Optional;

@Service
@Transactional
public class ShiftServiceImpl implements ShiftService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShiftServiceImpl.class);

    private final OrderTransactionReportService orderTransactionReportService;

    private final ShiftRepository shiftRepository;

    private final OrderRepository orderRepository;

    private final OAuth2Helper oAuth2Helper;

    @Autowired
    public ShiftServiceImpl(final OrderTransactionReportService orderTransactionReportService, final ShiftRepository shiftRepository, final OrderRepository orderRepository, final OAuth2Helper oAuth2Helper) {
        this.orderTransactionReportService = orderTransactionReportService;
        this.shiftRepository = shiftRepository;
        this.orderRepository = orderRepository;
        this.oAuth2Helper = oAuth2Helper;
    }

    @Override
    public Shift openShift(final String clientId, BigDecimal openingBalance) {

        final Optional<Shift> activeShift = this.getActiveShift(clientId);

        if (activeShift.isPresent()) {
            LOGGER.info("There is already an active shift. Returning active shift directly: {}", activeShift.get());
            return activeShift.get();
        }

        final String currentUser = oAuth2Helper.getCurrentPrincipal();
        final Shift shift = new Shift(clientId, new Date(), currentUser, openingBalance);

        return shiftRepository.save(shift);
    }

    @Override
    public Shift initiateCloseShift(final String clientId) {

        final Shift shift = getActiveShiftOrThrows(clientId);

        if (orderRepository.countByClientIdAndStateIn(clientId, Order.OrderState.inflightStates()) > 0) {
            throw new BusinessLogicException("Please complete all orders before closing shift.");
        }

        shift.initiateCloseShift(orderTransactionReportService::getClosingShiftTransactionReport);

        return shiftRepository.save(shift);
    }

    @Override
    public Shift closeShift(final String clientId, Shift.ClosingBalanceDetails cash, final Shift.ClosingBalanceDetails card) {

        final Shift shift = getShiftByStatusOrThrows(clientId, Shift.ShiftStatus.CLOSING).orElseThrow(() -> {
            throw new BusinessLogicException("Please initiate close shift first.");
        });

        final String currentUser = oAuth2Helper.getCurrentPrincipal();
        shift.closeShift(currentUser, cash, card);

        return shiftRepository.save(shift);
    }

    @Override
    public Shift confirmCloseShift(final String clientId, final String closingRemark) {

        final Shift shift = getShiftByStatusOrThrows(clientId, Shift.ShiftStatus.CONFIRM_CLOSE).orElseThrow(() -> {
            throw new BusinessLogicException("Please close shift first.");
        });

        shift.confirmCloseShift(closingRemark);

        return shiftRepository.save(shift);
    }

    @Override
    public Shift abortCloseShift(final String clientId) {

        final Shift shift = getShiftByStatusOrThrows(clientId, Shift.ShiftStatus.CONFIRM_CLOSE).orElseThrow(() -> {
            throw new BusinessLogicException("Cannot abort a shift that is not in confirm closing status");
        });

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

    private Optional<Shift> getShiftByStatusOrThrows(String clientId, Shift.ShiftStatus shiftStatus) {
        return shiftRepository.findByClientIdAndShiftStatus(clientId, shiftStatus);
    }
}
