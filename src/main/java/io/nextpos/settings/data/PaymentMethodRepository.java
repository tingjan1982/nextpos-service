package io.nextpos.settings.data;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, String> {

    Optional<PaymentMethod> findByPaymentKey(String paymentKey);
}
