package io.nextpos.shared.auth;

public interface AuthenticationHelper {

    String resolveCurrentClientId();

    String resolveCurrentUsername();
}
