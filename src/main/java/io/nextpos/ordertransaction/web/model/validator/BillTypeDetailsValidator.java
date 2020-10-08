package io.nextpos.ordertransaction.web.model.validator;

import io.nextpos.ordertransaction.data.OrderTransaction;
import io.nextpos.ordertransaction.web.model.OrderTransactionRequest;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class BillTypeDetailsValidator implements ConstraintValidator<ValidBillTypeDetails, OrderTransactionRequest> {

    @Override
    public void initialize(final ValidBillTypeDetails constraintAnnotation) {

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

            return true;

        } catch (Exception e) {
            final String errorMsg = "Cannot detect a valid billType.";
            context.buildConstraintViolationWithTemplate(errorMsg).addBeanNode().addConstraintViolation();
            return false;
        }
    }
}
