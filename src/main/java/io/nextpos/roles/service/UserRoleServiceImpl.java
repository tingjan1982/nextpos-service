package io.nextpos.roles.service;

import io.nextpos.client.data.Client;
import io.nextpos.roles.data.UserRole;
import io.nextpos.roles.data.UserRoleRepository;
import io.nextpos.shared.exception.ObjectNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;

@Service
@Transactional
public class UserRoleServiceImpl implements UserRoleService {

    private final UserRoleRepository userRoleRepository;

    @Autowired
    public UserRoleServiceImpl(final UserRoleRepository userRoleRepository) {
        this.userRoleRepository = userRoleRepository;
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
    public void deleteUserRole(final UserRole userRole) {
        userRoleRepository.delete(userRole);
    }
}
