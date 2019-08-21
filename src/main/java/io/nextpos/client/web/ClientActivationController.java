package io.nextpos.client.web;

import io.nextpos.client.service.ClientActivationService;
import io.nextpos.client.service.ClientActivationServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

@Controller
public class ClientActivationController {

    private final ClientActivationService clientActivationService;

    @Autowired
    public ClientActivationController(final ClientActivationService clientActivationService) {
        this.clientActivationService = clientActivationService;
    }

    @GetMapping("/activateaccount")
    public ModelAndView activateAccount(@RequestParam("activationToken") String encodedToken) {

        final ClientActivationServiceImpl.ActivationStatus activationStatus = clientActivationService.activateClient(encodedToken);

        return new ModelAndView("clientActivation", Map.of("activationStatus", activationStatus));
    }
}
