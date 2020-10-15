package io.nextpos.subscription.data;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface SubscriptionPaymentInstructionRepository extends MongoRepository<SubscriptionPaymentInstruction, String> {

    Optional<SubscriptionPaymentInstruction> findByCountryCode(String countryCode);
}
