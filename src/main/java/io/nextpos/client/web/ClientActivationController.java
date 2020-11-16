package io.nextpos.client.web;

import io.nextpos.client.data.ClientActivationResult;
import io.nextpos.client.service.ClientActivationService;
import io.nextpos.client.service.ClientService;
import io.nextpos.client.web.model.ResetClientPasswordRequest;
import io.nextpos.client.web.model.VerifyClientPasscodeRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Controller
@RequestMapping("/account")
public class ClientActivationController {

    private final ClientActivationService clientActivationService;

    private final ClientService clientService;

    @Autowired
    public ClientActivationController(final ClientActivationService clientActivationService, ClientService clientService) {
        this.clientActivationService = clientActivationService;
        this.clientService = clientService;
    }

    @GetMapping("/activateaccount")
    public ModelAndView activateClientAccount(@RequestParam("activationToken") String encodedToken) {

        final ClientActivationResult result = clientActivationService.activateClient(encodedToken);
        final HashMap<String, Object> model = new HashMap<>();
        model.put("activationStatus", result.getActivationStatus());

        if (result.getClientActivationLink() != null) {
            model.put("clientActivationLink", result.getClientActivationLink());
        }

        return new ModelAndView("clientActivationResult", model);
    }

    @GetMapping("/resendClientActivation")
    public ModelAndView resendClientActivation(@RequestParam("clientId") String clientId) {

        AtomicBoolean clientFound = new AtomicBoolean(true);
        clientService.getClient(clientId).ifPresentOrElse(clientActivationService::sendActivationNotification, () -> clientFound.set(false));

        return new ModelAndView("resendClientActivation", Map.of("clientFound", clientFound.get()));
    }

    @GetMapping("/sendResetPasscode")
    @ResponseBody
    public void sendResetPasscode(@RequestParam("clientEmail") String clientEmail) {

        clientActivationService.sendResetPasscode(clientEmail);
    }

    @PostMapping("/verifyResetPasscode")
    @ResponseBody
    public boolean verifyResetPasscode(@Valid @RequestBody VerifyClientPasscodeRequest request) {

        return clientActivationService.verifyResetPasscode(request.getClientEmail(), request.getPasscode());
    }

    @PostMapping("/resetClientPassword")
    @ResponseBody
    public void resetClientPassword(@Valid @RequestBody ResetClientPasswordRequest request) {

        clientActivationService.resetClientPassword(request.getClientEmail(), request.getPassword());
    }
}
