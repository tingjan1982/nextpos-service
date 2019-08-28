package io.nextpos.shared.auth;

import io.nextpos.shared.exception.GeneralApplicationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Component;

@Component
public class OAuth2HelperImpl implements OAuth2Helper {

    public String getCurrentPrincipal() {

        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication instanceof UsernamePasswordAuthenticationToken) {
            return ((User) authentication.getPrincipal()).getUsername();

        } else if (authentication instanceof OAuth2Authentication) {
            final OAuth2Authentication oauth2Authentication = (OAuth2Authentication) authentication;
            final UsernamePasswordAuthenticationToken userAuthentication = (UsernamePasswordAuthenticationToken) oauth2Authentication.getUserAuthentication();

            return ((String) userAuthentication.getPrincipal());
        }

        throw new GeneralApplicationException("Current principal cannot be found");
    }
}
