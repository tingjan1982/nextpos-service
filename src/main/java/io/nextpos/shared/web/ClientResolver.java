package io.nextpos.shared.web;

import io.micrometer.core.instrument.util.StringUtils;
import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientService;
import io.nextpos.shared.exception.ClientNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
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

        request.setAttribute("req-client", client);

        filterChain.doFilter(request, response);

    }
}
