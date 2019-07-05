package io.nextpos.shared.web;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientService;
import io.nextpos.shared.config.SecurityConfig;
import io.nextpos.shared.exception.ClientNotFoundException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
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
import java.util.Optional;

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

        final String clientId = request.getHeader(CLIENT_ID);

        if (StringUtils.isBlank(clientId)) {
            throw new ClientNotFoundException("Client id is missing in the header: x-client-id");
        }

        final Optional<Client> clientOptional = clientService.getClient(clientId);

        final Client client = clientOptional.orElseThrow(() -> {
            throw new ClientNotFoundException("Client cannot be found: " + clientId);
        });

        checkClientCredentials(client);

        request.setAttribute(REQ_ATTR_CLIENT, client);

        filterChain.doFilter(request, response);

    }

    private void checkClientCredentials(Client client) {

        final OAuth2Authentication authentication = (OAuth2Authentication) SecurityContextHolder.getContext().getAuthentication();
        final OAuth2AuthenticationDetails oAuth2AuthenticationDetails = (OAuth2AuthenticationDetails) authentication.getDetails();
        final SecurityConfig.ExtraClaims extraClaims = (SecurityConfig.ExtraClaims) oAuth2AuthenticationDetails.getDecodedDetails();

        final boolean accessCheck = StringUtils.equals(client.getId(), extraClaims.getClientId());
        LOGGER.info("Access control check on client access token against header: {}", accessCheck);

        if (!accessCheck) {
            throw new AccessDeniedException(String.format("Value in header %s does not match client id in the token", CLIENT_ID));
        }
    }
}
