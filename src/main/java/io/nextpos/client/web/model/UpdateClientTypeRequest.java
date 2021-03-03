package io.nextpos.client.web.model;

import io.nextpos.client.data.Client;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UpdateClientTypeRequest {

    private Client.ClientType clientType;
}
