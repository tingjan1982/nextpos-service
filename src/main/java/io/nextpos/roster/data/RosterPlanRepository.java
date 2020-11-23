package io.nextpos.roster.data;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface RosterPlanRepository extends MongoRepository<RosterPlan, String> {

    List<RosterPlan> findAllByClientId(String clientId);
}
