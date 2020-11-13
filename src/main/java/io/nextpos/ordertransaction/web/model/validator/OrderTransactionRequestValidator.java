package io.nextpos.ordertransaction.web.model.validator;

import io.micrometer.core.instrument.util.StringUtils;
import io.nextpos.ordertransaction.data.OrderTransaction;
import io.nextpos.ordertransaction.web.model.OrderTransactionRequest;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class OrderTransactionRequestValidator implements ConstraintValidator<ValidOrderTransactionRequest, OrderTransactionRequest> {

    @Override
    public void initialize(final ValidOrderTransactionRequest constraintAnnotation) {

    }

    @Override
    public boolean isValid(final OrderTransactionRequest value, final ConstraintValidatorContext context) {

        try {
            context.disableDefaultConstraintViolation();
            OrderTransaction.BillType billType = OrderTransaction.BillType.valueOf(value.getBillType());

            if (billType == OrderTransaction.BillType.SPLIT && (value.getSettleAmount() == null)) {
                final String errorMsg = "Need to specify a settleAmount that  when billType is SPLIT.";
                context.buildConstraintViolationWithTemplate(errorMsg).addPropertyNode("settleAmount").addConstraintViolation();

                return false;
            }

            return checkInvoiceOptions(value, context);

        } catch (Exception e) {
            final String errorMsg = "Cannot detect a valid billType.";
            context.buildConstraintViolationWithTemplate(errorMsg).addBeanNode().addConstraintViolation();
            return false;
        }
    }

    private boolean checkInvoiceOptions(OrderTransactionRequest request, ConstraintValidatorContext context) {

        if (request.getCarrierType() != null && StringUtils.isBlank(request.getCarrierId())) {
            String errorMsg = "carrierId is required when carrierType is specified";
            context.buildConstraintViolationWithTemplate(errorMsg).addPropertyNode("carrierId").addConstraintViolation();

            return false;
        }

        return true;
    }
}
