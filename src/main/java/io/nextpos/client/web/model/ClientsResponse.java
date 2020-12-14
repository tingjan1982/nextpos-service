package io.nextpos.client.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ClientsResponse {

    private List<ClientResponse> results;
}
