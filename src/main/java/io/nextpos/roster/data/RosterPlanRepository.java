package io.nextpos.roster.data;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface RosterPlanRepository extends MongoRepository<RosterPlan, String> {
}
