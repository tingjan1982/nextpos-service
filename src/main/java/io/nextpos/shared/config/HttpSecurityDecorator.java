package io.nextpos.shared.config;

import io.nextpos.roles.data.Permission;
import io.nextpos.roles.data.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer;

import java.util.ArrayList;
import java.util.List;

public class HttpSecurityDecorator {

    private final HttpSecurity http;

    private final List<AuthorizationAccessDefinition> definitions = new ArrayList<>();

    private HttpSecurityDecorator(HttpSecurity http) {
        this.http = http;
    }

    public static HttpSecurityDecorator newInstance(HttpSecurity http) {
        return new HttpSecurityDecorator(http);
    }

    public HttpSecurityDecorator addAuthorization(String requestUrlPattern, UserRole.UserPermission userPermission, String fallbackRole) {
        definitions.add(AuthorizationAccessDefinition.create(requestUrlPattern, userPermission, fallbackRole));

        return this;
    }

    public HttpSecurityDecorator addAuthorization(HttpMethod httpMethod, String requestUrlPattern, UserRole.UserPermission userPermission, String fallbackRole) {
        definitions.add(AuthorizationAccessDefinition.create(httpMethod, requestUrlPattern, userPermission, fallbackRole));

        return this;
    }

    public void decorate() throws Exception {

        final ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry expressionInterceptUrlRegistry = http.authorizeRequests();

        definitions.forEach(d -> {
            final UserRole.UserPermission up = d.getUserPermission();
            final String permission = up.getOperation() == Permission.Operation.ALL ? up.toWildcardPermission() : up.toPermissionString();
            final String allOpPermission = UserRole.UserPermission.of(up.getPermission(), Permission.Operation.ALL).toPermissionString();
            final String attribute = String.format("#oauth2.hasScopeMatching('%s') or #oauth2.hasScope('%s') or hasAuthority('%s')", permission, allOpPermission, d.getFallbackRole());

            if (d.getHttpMethod() != null) {
                expressionInterceptUrlRegistry.antMatchers(d.getHttpMethod(), d.getRequestUrlPattern()).access(attribute);
            } else {
                expressionInterceptUrlRegistry.antMatchers(d.getRequestUrlPattern()).access(attribute);
            }
        });
    }

    @Data
    @AllArgsConstructor
    static class AuthorizationAccessDefinition {

        private HttpMethod httpMethod;

        private String requestUrlPattern;

        private UserRole.UserPermission userPermission;

        private String fallbackRole;

        static AuthorizationAccessDefinition create(String requestUrlPattern, UserRole.UserPermission userPermission, String fallbackRole) {
            return new AuthorizationAccessDefinition(null, requestUrlPattern, userPermission, fallbackRole);
        }

        static AuthorizationAccessDefinition create(HttpMethod httpMethod, String requestUrlPattern, UserRole.UserPermission userPermission, String fallbackRole) {
            return new AuthorizationAccessDefinition(httpMethod, requestUrlPattern, userPermission, fallbackRole);
        }

    }
}
