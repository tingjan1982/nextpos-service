package io.nextpos.client.web;

import io.nextpos.client.service.ClientActivationService;
import io.nextpos.client.service.ClientActivationServiceImpl;
import io.nextpos.client.web.model.ResetClientPasswordRequest;
import io.nextpos.client.web.model.VerifyClientPasscodeRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import javax.validation.Valid;
import java.util.Map;

@Controller
@RequestMapping("/account")
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
