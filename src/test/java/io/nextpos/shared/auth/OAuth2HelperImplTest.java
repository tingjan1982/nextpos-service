package io.nextpos.shared.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = "nomock=true")
class OAuth2HelperImplTest {

    @Autowired
    private OAuth2Helper oAuth2Helper;

    @Test
    @WithMockUser("dummyUser")
    void getCurrentPrincipal() {
        assertThat(oAuth2Helper.getCurrentPrincipal()).isEqualTo("dummyUser");
    }
}