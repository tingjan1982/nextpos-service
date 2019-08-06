package io.nextpos.ordertransaction.web.model.validator;

import io.nextpos.ordertransaction.data.OrderTransaction;
import io.nextpos.ordertransaction.web.model.OrderTransactionRequest;
import org.springframework.util.CollectionUtils;

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

            if (billType == OrderTransaction.BillType.SPLIT && (value.getSplitWith() == null || value.getSplitWith() < 2)) {
                final String errorMsg = "Need to be a positive integer and greater than 1 when billType is SPLIT.";
                context.buildConstraintViolationWithTemplate(errorMsg).addPropertyNode("splitWith").addConstraintViolation();

                return false;
            } else if (billType == OrderTransaction.BillType.CUSTOM && CollectionUtils.isEmpty(value.getBillLineItems())) {
                final String errorMsg = "Bill line items cannot be empty when billType is CUSTOM.";
                context.buildConstraintViolationWithTemplate(errorMsg).addPropertyNode("billLineItems").addConstraintViolation();

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
