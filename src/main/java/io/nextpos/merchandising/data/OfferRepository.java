package io.nextpos.merchandising.data;

import io.nextpos.client.data.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Date;
import java.util.stream.Stream;

public interface OfferRepository extends JpaRepository<Offer, String> {

    @Query("select o from io.nextpos.merchandising.data.Offer o where o.client = ?1 and o.effectiveDetails.active = true and " +
            "o.triggerType = ?2 and " +
            "(o.effectiveDetails.startDate is null or o.effectiveDetails.startDate <= ?3) and " +
            "(o.effectiveDetails.endDate is null or o.effectiveDetails.endDate >= ?3)")
    Stream<Offer> findActiveOffers(Client client, Offer.TriggerType triggerType, Date date);
}
