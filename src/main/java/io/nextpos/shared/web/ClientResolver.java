package io.nextpos.shared.web;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientService;
import io.nextpos.shared.config.SecurityConfig;
import io.nextpos.shared.exception.ClientAccountException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class ClientResolver extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientResolver.class);

    public static final String REQ_ATTR_CLIENT = "req-client";

    private static final String MDC_CLIENT_ID = "client.id";

    private final ClientService clientService;


    @Autowired
    public ClientResolver(final ClientService clientService) {
        this.clientService = clientService;
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain) throws ServletException, IOException {

        final Client client = resolveClient();
        request.setAttribute(REQ_ATTR_CLIENT, client);

        try {
            MDC.put(MDC_CLIENT_ID, client.getId());
            filterChain.doFilter(request, response);

        } finally {
            MDC.remove(MDC_CLIENT_ID);
        }

    }

    private Client resolveClient() {
        final OAuth2Authentication authentication = (OAuth2Authentication) SecurityContextHolder.getContext().getAuthentication();
        final OAuth2AuthenticationDetails oAuth2AuthenticationDetails = (OAuth2AuthenticationDetails) authentication.getDetails();
        final SecurityConfig.ExtraClaims extraClaims = (SecurityConfig.ExtraClaims) oAuth2AuthenticationDetails.getDecodedDetails();

        final String clientId = extraClaims.getApplicationClientId();
        final Client client = clientService.getClientOrThrows(clientId);

        if (client.getStatus() == Client.Status.INACTIVE) {
            throw new ClientAccountException("Specified account is not active. Please contact customer service for more details", client);
        }

        return client;
    }
}
