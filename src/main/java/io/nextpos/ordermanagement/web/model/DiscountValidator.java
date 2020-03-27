package io.nextpos.ordermanagement.web.model;

import io.nextpos.merchandising.data.OrderLevelOffer;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.math.BigDecimal;
import java.util.Arrays;

public class DiscountValidator implements ConstraintValidator<ValidDiscount, DiscountRequest> {

    @Override
    public void initialize(final ValidDiscount constraintAnnotation) {

    }

    @Override
    public boolean isValid(final DiscountRequest value, final ConstraintValidatorContext context) {

        try {
            context.disableDefaultConstraintViolation();

            final OrderLevelOffer.GlobalOrderDiscount globalOrderDiscount = OrderLevelOffer.GlobalOrderDiscount.valueOf(value.getOrderDiscount());

            if (globalOrderDiscount == OrderLevelOffer.GlobalOrderDiscount.ENTER_DISCOUNT) {
                final BigDecimal discount = value.getDiscount();

                if (discount.compareTo(BigDecimal.ZERO) <= 0 || discount.compareTo(BigDecimal.valueOf(100)) > 0) {
                    final String errorMsg = "Discount needs to be in between 1 and 100";
                    context.buildConstraintViolationWithTemplate(errorMsg)
                            .addPropertyNode("discount").addConstraintViolation();
                    return false;
                }
            }

        } catch (Exception e) {
            final String errorMsg = "Invalid global discount type. Valid values are " + Arrays.toString(OrderLevelOffer.GlobalOrderDiscount.values());
            context.buildConstraintViolationWithTemplate(errorMsg).addPropertyNode("orderDiscount").addConstraintViolation();
            return false;
        }

        return true;
    }
}
