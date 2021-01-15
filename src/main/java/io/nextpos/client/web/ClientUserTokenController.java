package io.nextpos.client.web;

import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientService;
import io.nextpos.shared.exception.BusinessLogicException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.common.exceptions.InvalidClientException;
import org.springframework.security.oauth2.common.exceptions.UnsupportedGrantTypeException;
import org.springframework.security.oauth2.provider.*;
import org.springframework.security.oauth2.provider.password.ResourceOwnerPasswordTokenGranter;
import org.springframework.security.oauth2.provider.request.DefaultOAuth2RequestFactory;
import org.springframework.security.oauth2.provider.request.DefaultOAuth2RequestValidator;
import org.springframework.security.oauth2.provider.token.AuthorizationServerTokenServices;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

/**
 * Part of the code is copied from TokenEndpoint to create oauth2 access token.
 */
@RestController
@RequestMapping("/clientUserTokens")
public class ClientUserTokenController {

    private final ClientDetailsService clientDetailsService;

    private final OAuth2RequestFactory oAuth2RequestFactory;

    private final TokenGranter tokenGranter;

    private final OAuth2RequestValidator oAuth2RequestValidator;

    private final ClientService clientService;


    @Autowired
    public ClientUserTokenController(ClientDetailsService clientDetailsService, AuthenticationManager authenticationManager, AuthorizationServerTokenServices tokenServices, ClientService clientService) {
        this.clientDetailsService = clientDetailsService;
        this.oAuth2RequestFactory = new DefaultOAuth2RequestFactory(clientDetailsService);
        this.clientService = clientService;
        this.tokenGranter = new ResourceOwnerPasswordTokenGranter(authenticationManager, tokenServices, clientDetailsService, this.oAuth2RequestFactory);
        this.oAuth2RequestValidator = new DefaultOAuth2RequestValidator();
    }

    @PostMapping
    public ResponseEntity<OAuth2AccessToken> createClientUserAccessToken(Principal principal,
                                                                         @RequestParam("password") String password) {

        if (!(principal instanceof Authentication)) {
            throw new InsufficientAuthenticationException(
                    "There is no client authentication. Try adding an appropriate authentication filter.");
        }

        String clientId = getClientId(principal);
        final Client client = clientService.getClientByUsername(clientId).orElseThrow(() -> {
            throw new BusinessLogicException("message.clientNotFound", "Client cannot be resolved by value: " + clientId);
        });

        final String username = clientService.getClientUsernameByPassword(client, password);

        final Map<String, String> parameters = new HashMap<>();
        parameters.put("grant_type", "password");
        parameters.put("username", username);
        parameters.put("password", password);

        ClientDetails authenticatedClient = clientDetailsService.loadClientByClientId(clientId);
        TokenRequest tokenRequest = oAuth2RequestFactory.createTokenRequest(parameters, authenticatedClient);

        if (clientId != null && !clientId.equals("")) {
            // Only validate the client details if a client authenticated during this
            // request.
            if (!clientId.equals(tokenRequest.getClientId())) {
                // double check to make sure that the client ID in the token request is the same as that in the
                // authenticated client
                throw new InvalidClientException("Given client ID does not match authenticated client");
            }
        }
        if (authenticatedClient != null) {
            oAuth2RequestValidator.validateScope(tokenRequest, authenticatedClient);
        }

        OAuth2AccessToken token = tokenGranter.grant(tokenRequest.getGrantType(), tokenRequest);

        if (token == null) {
            throw new UnsupportedGrantTypeException("Unsupported grant type: " + tokenRequest.getGrantType());
        }

        return getResponse(token);

    }

    protected String getClientId(Principal principal) {
        Authentication client = (Authentication) principal;
        if (!client.isAuthenticated()) {
            throw new InsufficientAuthenticationException("The client is not authenticated.");
        }
        String clientId = client.getName();
        if (client instanceof OAuth2Authentication) {
            // Might be a client and user combined authentication
            clientId = ((OAuth2Authentication) client).getOAuth2Request().getClientId();
        }
        return clientId;
    }

    private ResponseEntity<OAuth2AccessToken> getResponse(OAuth2AccessToken accessToken) {

        HttpHeaders headers = new HttpHeaders();
        headers.set("Cache-Control", "no-store");
        headers.set("Pragma", "no-cache");
        headers.set("Content-Type", "application/json;charset=UTF-8");

        return new ResponseEntity<>(accessToken, headers, HttpStatus.OK);
    }
}
