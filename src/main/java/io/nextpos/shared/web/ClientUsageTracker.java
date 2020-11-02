package io.nextpos.shared.web;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientService;
import io.nextpos.clienttracker.service.ClientUserTrackingService;
import io.nextpos.shared.config.SecurityConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class ClientUsageTracker extends HandlerInterceptorAdapter {

    private static final String CLIENT_DEVICE_IP = "X-Client-Device-Ip";

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientUsageTracker.class);

    private final ClientService clientService;

    private final ClientUserTrackingService clientUserTrackingService;


    @Autowired
    public ClientUsageTracker(ClientService clientService, ClientUserTrackingService clientUserTrackingService) {
        this.clientService = clientService;
        this.clientUserTrackingService = clientUserTrackingService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof OAuth2Authentication) {
            Client client = getClient(authentication);

            clientUserTrackingService.trackClientUser(client, authentication.getName());
            clientUserTrackingService.trackClientDevice(client, obtainDeviceIpAddress(request));
        }

        return true;
    }

    private Client getClient(Authentication authentication) {

        final OAuth2AuthenticationDetails details = (OAuth2AuthenticationDetails) authentication.getDetails();
        SecurityConfig.ExtraClaims extraClaims = (SecurityConfig.ExtraClaims) details.getDecodedDetails();
        final String clientId = extraClaims.getApplicationClientId();
        return clientService.getClientOrThrows(clientId);
    }

    private String obtainDeviceIpAddress(HttpServletRequest request) {
        final String clientDeviceIp = request.getHeader(CLIENT_DEVICE_IP);
        LOGGER.info("Obtained client device ip address: {}", clientDeviceIp);

        return clientDeviceIp;
    }
}
