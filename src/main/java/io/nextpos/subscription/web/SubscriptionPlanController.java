package io.nextpos.subscription.web;

import io.nextpos.settings.data.CountrySettings;
import io.nextpos.settings.service.SettingsService;
import io.nextpos.subscription.data.SubscriptionPlan;
import io.nextpos.subscription.service.SubscriptionPlanService;
import io.nextpos.subscription.web.model.SubscriptionPlanRequest;
import io.nextpos.subscription.web.model.SubscriptionPlanResponse;
import io.nextpos.subscription.web.model.SubscriptionPlansResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/subscriptionPlans")
public class SubscriptionPlanController {

    private final SubscriptionPlanService subscriptionPlanService;

    private final SettingsService settingsService;

    @Autowired
    public SubscriptionPlanController(SubscriptionPlanService subscriptionPlanService, SettingsService settingsService) {
        this.subscriptionPlanService = subscriptionPlanService;
        this.settingsService = settingsService;
    }

    @PostMapping
    public SubscriptionPlan createSubscriptionPlan(@Valid @RequestBody SubscriptionPlanRequest request) {

        SubscriptionPlan subscriptionPlan = fromSubscriptionPlanRequest(request);

        return subscriptionPlanService.saveSubscriptionPlan(subscriptionPlan);
    }

    private SubscriptionPlan fromSubscriptionPlanRequest(SubscriptionPlanRequest request) {

        final CountrySettings countrySettings = settingsService.getCountrySettings(request.getCountryCode());
        final SubscriptionPlan subscriptionPlan = new SubscriptionPlan(request.getCountryCode(), request.getPlanGroup(), request.getPlanName(), countrySettings);
        subscriptionPlan.setDescription(request.getDescription());

        request.getPlanPrices().forEach((planPeriod, price) -> subscriptionPlan.addPlanPrice(planPeriod, new SubscriptionPlan.PlanPrice(price)));

        subscriptionPlan.updateSubscriptionLimit(request.getUserLimit(), request.getDeviceLimit(), request.getRestrictedFeatures());

        return subscriptionPlan;
    }

    @GetMapping
    public SubscriptionPlansResponse getSubscriptionPlans(@RequestParam("country") String countryCode) {

        final List<SubscriptionPlanResponse> results = subscriptionPlanService.getSubscriptionPlans(countryCode).stream()
                .map(SubscriptionPlanResponse::new)
                .collect(Collectors.toList());

        return new SubscriptionPlansResponse(results);
    }

    @GetMapping("/{id}")
    public SubscriptionPlanResponse getSubscription(@PathVariable String id) {

        return new SubscriptionPlanResponse(subscriptionPlanService.getSubscription(id));
    }

    @PostMapping("/{id}")
    public SubscriptionPlanResponse updateSubscriptionPlan(@PathVariable String id,
                                                   @Valid @RequestBody SubscriptionPlanRequest request) {

        final SubscriptionPlan subscription = subscriptionPlanService.getSubscription(id);
        updateFromRequest(subscription, request);

        final SubscriptionPlan updated = subscriptionPlanService.saveSubscriptionPlan(subscription);
        return new SubscriptionPlanResponse(updated);
    }

    private void updateFromRequest(SubscriptionPlan subscriptionPlan, SubscriptionPlanRequest request) {

        subscriptionPlan.setPlanName(request.getPlanName());
        subscriptionPlan.setDescription(request.getDescription());
        subscriptionPlan.setPlanGroup(request.getPlanGroup());
        subscriptionPlan.updateSubscriptionLimit(request.getUserLimit(), request.getDeviceLimit(), request.getRestrictedFeatures());
        subscriptionPlan.getPlanPrices().clear();
        request.getPlanPrices().forEach((planPeriod, price) -> subscriptionPlan.addPlanPrice(planPeriod, new SubscriptionPlan.PlanPrice(price)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSubscriptionPlan(@PathVariable String id) {

        final SubscriptionPlan subscription = subscriptionPlanService.getSubscription(id);
        subscriptionPlanService.deleteSubscriptionPlan(subscription);
    }
}
