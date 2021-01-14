package io.nextpos.script;

import io.nextpos.client.data.ClientRepository;
import io.nextpos.client.service.ClientBootstrapService;
import io.nextpos.client.service.ClientService;
import io.nextpos.roles.data.Permission;
import io.nextpos.roles.data.UserRole;
import io.nextpos.roles.service.UserRoleService;
import io.nextpos.shared.config.SecurityConfig;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;
import org.springframework.security.oauth2.provider.client.JdbcClientDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.Map;

@Disabled
@SpringBootTest
@ActiveProfiles("gcp")
@TestPropertySource(properties = {"script=true", "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration"})
public class CreatePredefinedUserRoles {

    static {
        System.setProperty("jdk.tls.client.protocols", "TLSv1,TLSv1.1,TLSv1.2");
    }
    
    private static final Logger LOGGER = LoggerFactory.getLogger(CreatePredefinedUserRoles.class);

    private final ClientService clientService;

    private final JdbcClientDetailsService jdbcClientDetailsService;

    private final UserRoleService userRoleService;

    private final ClientBootstrapService clientBootstrapService;

    private final ClientRepository clientRepository;

    @Autowired
    public CreatePredefinedUserRoles(ClientService clientService, JdbcClientDetailsService jdbcClientDetailsService, UserRoleService userRoleService, ClientBootstrapService clientBootstrapService, ClientRepository clientRepository) {
        this.clientService = clientService;
        this.jdbcClientDetailsService = jdbcClientDetailsService;
        this.userRoleService = userRoleService;
        this.clientBootstrapService = clientBootstrapService;
        this.clientRepository = clientRepository;
    }

    @Test
    void updateClientUserAndUserRoles() {

        clientRepository.findAll().stream()
                .filter(c -> c.getId().equals("cli-3M01K3u00RxGN3BJcVbEUeyrybxi"))
                .forEach(c -> {
                    LOGGER.info("Begin updating client {}", c.getId());
                    
                    final BaseClientDetails clientDetails = ((BaseClientDetails) jdbcClientDetailsService.loadClientByClientId(c.getUsername()));
                    clientDetails.setScope(Permission.allPermissions());
                    final ArrayList<String> authorities = new ArrayList<>(Permission.allPermissions());
                    authorities.addAll(SecurityConfig.Role.getRoles());
                    clientDetails.setAuthorities(AuthorityUtils.createAuthorityList(authorities.toArray(String[]::new)));
                    jdbcClientDetailsService.updateClientDetails(clientDetails);
                    LOGGER.info("Updated oauth client details: {}", clientDetails.getClientId());

                    clientService.getClientUsers(c).forEach(userRoleService::removeClientUserRole);

                    userRoleService.getUserRoles(c).stream()
                            .peek(ur -> LOGGER.info("Found role: {}, about to be deleted.", ur.getName()))
                            .forEach(ur -> {
                                final UserRole userRole = userRoleService.loadUserRole(ur.getId());
                                userRoleService.deleteUserRole(userRole);
                            });

                    final Map<String, UserRole> definedUserRoles = clientBootstrapService.bootstrapUserRoles(c);
                    LOGGER.info("Created predefined user roles: {}", definedUserRoles.keySet());

                    clientService.getClientUsers(c).forEach(cu -> {
                        if (cu.getRoles().contains("ADMIN")) {
                            cu.setPermissions(String.join(",", Permission.allPermissions()));

                        } else if (cu.getRoles().contains("OWNER")) {
                            cu.setUserRole(definedUserRoles.get("主管"));

                        } else if (cu.getRoles().contains("MANAGER")) {
                            cu.setUserRole(definedUserRoles.get("店長"));

                        } else if (cu.getRoles().contains("USER")) {
                            cu.setUserRole(definedUserRoles.get("員工"));
                        }

                        clientService.saveClientUser(cu);
                        LOGGER.info("Updated user {}'s permissions to {}", cu.getName(), cu.getPermissions());
                    });
                });
    }
}
