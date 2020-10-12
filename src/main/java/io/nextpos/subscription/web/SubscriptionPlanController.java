package io.nextpos.subscription.web;

import io.nextpos.subscription.data.SubscriptionPlan;
import io.nextpos.subscription.service.SubscriptionService;
import io.nextpos.subscription.web.model.SubscriptionPlanRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/admin/subscriptionPlans")
public class SubscriptionPlanController {

    private final SubscriptionService subscriptionService;

    @Autowired
    public SubscriptionPlanController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping
    public SubscriptionPlan createSubscriptionPlan(@Valid @RequestBody SubscriptionPlanRequest request) {

        SubscriptionPlan subscriptionPlan = fromSubscriptionPlanRequest(request);

        return subscriptionService.saveSubscriptionPlan(subscriptionPlan);
    }

    private SubscriptionPlan fromSubscriptionPlanRequest(SubscriptionPlanRequest request) {

        final SubscriptionPlan subscriptionPlan = new SubscriptionPlan(request.getCountryCode(), request.getPlanGroup(), request.getPlanName());
        request.getPlanPrices().forEach((planPeriod, price) -> subscriptionPlan.addPlanPrice(planPeriod, new SubscriptionPlan.PlanPrice(price)));
        subscriptionPlan.setRestrictedFeatures(request.getRestrictedFeatures());

        return subscriptionPlan;
    }

    @GetMapping
    public List<SubscriptionPlan> getSubscriptionPlans(@RequestParam("country") String countryCode) {

        return subscriptionService.getSubscriptionPlans(countryCode);
    }

    @GetMapping("/{id}")
    public SubscriptionPlan getSubscription(@PathVariable String id) {

        return subscriptionService.getSubscription(id);
    }

    @PostMapping("/{id}")
    public SubscriptionPlan updateSubscriptionPlan(@PathVariable String id,
                                                   @Valid @RequestBody SubscriptionPlanRequest request) {

        final SubscriptionPlan subscription = subscriptionService.getSubscription(id);
        updateFromRequest(subscription, request);

        return subscriptionService.saveSubscriptionPlan(subscription);
    }

    private void updateFromRequest(SubscriptionPlan subscriptionPlan, SubscriptionPlanRequest request) {

        subscriptionPlan.setPlanName(request.getPlanName());
        subscriptionPlan.setPlanGroup(request.getPlanGroup());
        subscriptionPlan.setRestrictedFeatures(request.getRestrictedFeatures());

        subscriptionPlan.getPlanPrices().clear();
        request.getPlanPrices().forEach((planPeriod, price) -> subscriptionPlan.addPlanPrice(planPeriod, new SubscriptionPlan.PlanPrice(price)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSubscriptionPlan(@PathVariable String id) {

        final SubscriptionPlan subscription = subscriptionService.getSubscription(id);
        subscriptionService.deleteSubscriptionPlan(subscription);
    }
}
