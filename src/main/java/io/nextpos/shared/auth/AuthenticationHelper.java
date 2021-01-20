package io.nextpos.shared.auth;

import io.nextpos.shared.exception.GeneralApplicationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationHelper {

    public String resolveCurrentClientId() {
        return resolveAuthenticationValueStrategy().clientId();
    }

    public String resolveCurrentUsername() {
        return resolveAuthenticationValueStrategy().username();
    }

    private AuthenticationValueStrategy resolveAuthenticationValueStrategy() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof UsernamePasswordAuthenticationToken) {
            return new UsernamePasswordAuthTokenStrategy(authentication);
        } else if (authentication instanceof OAuth2Authentication) {
            return new OAuth2Strategy(authentication);
        }

        throw new GeneralApplicationException("Should not reach here. Current authentication class type is not supported: " + authentication);
    }

    private interface AuthenticationValueStrategy {

        String username();

        String clientId();
    }

    private static class UsernamePasswordAuthTokenStrategy implements AuthenticationValueStrategy {

        private final UsernamePasswordAuthenticationToken authentication;

        private UsernamePasswordAuthTokenStrategy(Authentication authentication) {
            this.authentication = (UsernamePasswordAuthenticationToken) authentication;
        }

        @Override
        public String username() {
            return ((User) authentication.getPrincipal()).getUsername();
        }
        @Override
        public String clientId() {
            return ((User) authentication.getPrincipal()).getUsername();
        }

    }

    private static class OAuth2Strategy implements AuthenticationValueStrategy {

        private final OAuth2Authentication authentication;

        public OAuth2Strategy(Authentication authentication) {
            this.authentication = ((OAuth2Authentication) authentication);
        }

        @Override
        public String username() {
            final UsernamePasswordAuthenticationToken userAuthentication = (UsernamePasswordAuthenticationToken) authentication.getUserAuthentication();
            return (String) userAuthentication.getPrincipal();
        }

        @Override
        public String clientId() {
            return authentication.getOAuth2Request().getClientId();
        }
    }
}
