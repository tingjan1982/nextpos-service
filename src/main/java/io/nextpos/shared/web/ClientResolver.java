package io.nextpos.shared.web;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientService;
import io.nextpos.shared.config.SecurityConfig;
import io.nextpos.shared.exception.ClientNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final String CLIENT_ID = "x-client-id";

    private final ClientService clientService;


    @Autowired
    public ClientResolver(final ClientService clientService) {
        this.clientService = clientService;
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain) throws ServletException, IOException {

        request.setAttribute(REQ_ATTR_CLIENT, resolveClient());

        filterChain.doFilter(request, response);

    }

    private Client resolveClient() {
        final OAuth2Authentication authentication = (OAuth2Authentication) SecurityContextHolder.getContext().getAuthentication();
        final OAuth2AuthenticationDetails oAuth2AuthenticationDetails = (OAuth2AuthenticationDetails) authentication.getDetails();
        final SecurityConfig.ExtraClaims extraClaims = (SecurityConfig.ExtraClaims) oAuth2AuthenticationDetails.getDecodedDetails();

        final String clientId = extraClaims.getApplicationClientId();
        return clientService.getClient(clientId).orElseThrow(() -> {
            throw new ClientNotFoundException("Client cannot be found: " + clientId);
        });
    }
}
