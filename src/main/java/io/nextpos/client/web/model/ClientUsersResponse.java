package io.nextpos.client.web.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ClientUsersResponse {

    private List<ClientUserResponse> users;
}
