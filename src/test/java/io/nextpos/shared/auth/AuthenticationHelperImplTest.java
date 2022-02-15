package io.nextpos.shared.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = "nomock=true")
class AuthenticationHelperImplTest {

    private final AuthenticationHelper authenticationHelper;

    @Autowired
    AuthenticationHelperImplTest(AuthenticationHelper authenticationHelper) {
        this.authenticationHelper = authenticationHelper;
    }

    @Test
    @WithMockUser("dummyUser")
    void resolveCurrentUsername() {

        String username = authenticationHelper.resolveCurrentUsername();

        assertThat(username).isEqualTo("dummyUser");

        String clientId = authenticationHelper.resolveCurrentClientId();

        assertThat(clientId).isEqualTo("dummyUser");
    }
}