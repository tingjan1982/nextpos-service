package io.nextpos.ordermanagement.service;

import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderRepository;
import io.nextpos.ordermanagement.data.Shift;
import io.nextpos.ordermanagement.data.ShiftRepository;
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

    private final ShiftRepository shiftRepository;

    private final OrderRepository orderRepository;

    private final OAuth2Helper oAuth2Helper;

    @Autowired
    public ShiftServiceImpl(final ShiftRepository shiftRepository, final OrderRepository orderRepository, final OAuth2Helper oAuth2Helper) {
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
    public Shift createInterimBalance(final String clientId, BigDecimal interimBalanceAmount) {

        final Shift shift = getActiveShiftOrThrows(clientId);
        final String currentUser = oAuth2Helper.getCurrentPrincipal();

        final Shift.ShiftDetails interimBalance = new Shift.ShiftDetails(new Date(), currentUser, interimBalanceAmount);
        shift.addInterimBalance(interimBalance);

        return shiftRepository.save(shift);
    }

    @Override
    public Shift closeShift(final String clientId, BigDecimal closingBalance) {

        final Shift shift = getActiveShiftOrThrows(clientId);

        if (orderRepository.countByClientIdAndStateIn(clientId, Order.OrderState.inflightStates()) > 0) {
            throw new BusinessLogicException("Please complete all orders before closing shift.");
        }

        final String currentUser = oAuth2Helper.getCurrentPrincipal();
        shift.closeShift(currentUser, closingBalance);

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
}
