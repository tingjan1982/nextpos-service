package io.nextpos.client.web;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientSetting;
import io.nextpos.client.service.ClientSettingsService;
import io.nextpos.client.web.model.ClientSettingRequest;
import io.nextpos.client.web.model.ClientSettingResponse;
import io.nextpos.client.web.model.ClientSettingsResponse;
import io.nextpos.client.web.model.UpdateClientSettingRequest;
import io.nextpos.shared.exception.ObjectNotFoundException;
import io.nextpos.shared.web.ClientResolver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/clients/me/settings")
public class ClientSettingsController {

    private final ClientSettingsService clientSettingsService;

    @Autowired
    public ClientSettingsController(final ClientSettingsService clientSettingsService) {
        this.clientSettingsService = clientSettingsService;
    }

    @PostMapping
    public ClientSettingResponse createClientSettings(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client, @Valid @RequestBody ClientSettingRequest clientSettingRequest) {

        final ClientSetting clientSetting = fromClientSettingsRequest(client, clientSettingRequest);
        clientSettingsService.saveClientSettings(clientSetting);

        return toClientSettingResponse(clientSetting);
    }

    @GetMapping
    public ClientSettingsResponse getClientSettings(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client) {

        final List<ClientSettingResponse> results = clientSettingsService.getClientSettings(client).stream()
                .map(this::toClientSettingResponse)
                .collect(Collectors.toList());

        return new ClientSettingsResponse(results);
    }

    @GetMapping("/{settingName}")
    public ClientSettingResponse getClientSetting(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client, @PathVariable final String settingName) {

        try {
            final ClientSetting.SettingName name = ClientSetting.SettingName.valueOf(settingName);
            return toClientSettingResponse(clientSettingsService.getClientSettingByNameOrThrows(client, name));

        } catch (Exception e) {
            throw new ObjectNotFoundException(settingName, ClientSetting.class);
        }
    }

    @PostMapping("/{settingName}")
    public ClientSettingResponse updateClientSetting(@RequestAttribute(ClientResolver.REQ_ATTR_CLIENT) Client client,
                                                     @PathVariable String settingName,
                                                     @Valid @RequestBody UpdateClientSettingRequest request) {

        try {
            final ClientSetting.SettingName name = ClientSetting.SettingName.valueOf(settingName);
            final ClientSetting clientSetting = clientSettingsService.getClientSettingByNameOrThrows(client, name);

            updateClientSettingFromRequest(clientSetting, request);
            clientSettingsService.saveClientSettings(clientSetting);

            return toClientSettingResponse(clientSetting);

        } catch (Exception e) {
            throw new ObjectNotFoundException(settingName, ClientSetting.class);
        }
    }

    private void updateClientSettingFromRequest(final ClientSetting clientSetting, final UpdateClientSettingRequest request) {

        clientSetting.setStoredValue(request.getValue());
        clientSetting.setEnabled(request.isEnabled());
    }

    private ClientSetting fromClientSettingsRequest(final Client client, final ClientSettingRequest request) {

        final ClientSetting.SettingName settingName = ClientSetting.SettingName.valueOf(request.getSettingName());

        return new ClientSetting(client, settingName, request.getValue(), settingName.getValueType(), request.isEnabled());
    }

    private ClientSettingResponse toClientSettingResponse(final ClientSetting clientSettings) {

        return new ClientSettingResponse(clientSettings.getId(),
                clientSettings.getName(),
                clientSettings.getStoredValue(),
                clientSettings.getValueType(),
                clientSettings.isEnabled());
    }
}
