package io.nextpos.client.data;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class ClientActivationResult {

    private final String clientId;

    private final ActivationStatus activationStatus;

    private String clientActivationLink;

    public enum ActivationStatus {
        ACTIVATED, EXPIRED, FAILED
    }
}
