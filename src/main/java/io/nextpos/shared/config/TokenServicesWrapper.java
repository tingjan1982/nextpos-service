package io.nextpos.shared.config;

import io.nextpos.shared.service.annotation.ChainedTransaction;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.DefaultTokenServices;

/**
 * This class solves a transaction issue because DefaultTokenServices.createAccessToken method
 * is annotated with @Transactional without specifying a transaction manager. The application
 * has three TransactionManager and thus will fail.
 *
 * We override the method with @ChainedTransaction to allow Spring to get the correct transaction manager.
 */
public class TokenServicesWrapper extends DefaultTokenServices {

    @ChainedTransaction
    @Override
    public OAuth2AccessToken createAccessToken(OAuth2Authentication authentication) throws AuthenticationException {
        return super.createAccessToken(authentication);
    }
}
