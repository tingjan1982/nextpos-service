package io.nextpos.roles.service;

import io.nextpos.client.data.Client;
import io.nextpos.client.data.ClientUser;
import io.nextpos.client.data.ClientUserRepository;
import io.nextpos.roles.data.UserRole;
import io.nextpos.roles.data.UserRoleRepository;
import io.nextpos.shared.exception.ObjectNotFoundException;
import io.nextpos.shared.service.annotation.JpaTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@JpaTransaction
public class UserRoleServiceImpl implements UserRoleService {

    private final UserRoleRepository userRoleRepository;

    private final ClientUserRepository clientUserRepository;

    @Autowired
    public UserRoleServiceImpl(final UserRoleRepository userRoleRepository, ClientUserRepository clientUserRepository) {
        this.userRoleRepository = userRoleRepository;
        this.clientUserRepository = clientUserRepository;
    }

    @Override
    public UserRole saveUserRole(final UserRole userRole) {
        return userRoleRepository.save(userRole);
    }

    @Override
    public UserRole getUserRole(final String id) {
        return userRoleRepository.findById(id).orElseThrow(() -> {
            throw new ObjectNotFoundException(id, UserRole.class);
        });
    }

    @Override
    public UserRole loadUserRole(String id) {
        return userRoleRepository.loadById(id);
    }

    @Override
    public List<UserRole> getUserRoles(Client client) {
        return userRoleRepository.findAllByClientOrderByName(client);
    }

    @Override
    public UserRole updateUserRole(final UserRole userRole) {

        userRole.getClientUsers().forEach((id, user) -> user.setUserRole(userRole));

        return userRoleRepository.save(userRole);
    }

    @Override
    public void removeClientUserRole(ClientUser clientUser) {

        if (clientUser.getUserRole() != null) {
            clientUserRepository.loadById(clientUser.getId()).ifPresent(cu -> {
                final UserRole userRole = cu.getUserRole();
                cu.removeUserRole();

                saveUserRole(userRole);
                clientUserRepository.save(cu);
            });
        }
    }

    @Override
    public void deleteUserRole(final UserRole userRole) {
        userRoleRepository.delete(userRole);
    }
}
