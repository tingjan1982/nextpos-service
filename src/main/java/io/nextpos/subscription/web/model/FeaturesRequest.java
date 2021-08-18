package io.nextpos.subscription.web.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FeaturesRequest {

    private String feature;

    private boolean enabled;
}
