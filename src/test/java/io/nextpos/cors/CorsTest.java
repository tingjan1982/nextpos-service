package io.nextpos.cors;

import io.nextpos.shared.config.CorsConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * https://stackoverflow.com/questions/42588692/testing-cors-in-springboottest
 *
 * Various ways to use SpringBootTest:
 *
 * https://reflectoring.io/spring-boot-test/
 */
@SpringBootTest(classes = {CorsTest.TestApplication.class, CorsConfig.class, CorsTest.TestSecurityConfig.class})
class CorsTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .dispatchOptions(true).build();
    }

    @Test
    void testCorsRequest() throws Exception {


        this.mockMvc.perform(options("/test-cors")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .header(HttpHeaders.ORIGIN, "http://some.origin.host:8080"))
                .andDo(print())
                .andExpect(status().isOk());

        this.mockMvc
                .perform(get("/test-cors").header("Origin", "http://some.origin.host:8080"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("whatever"));
    }

    @SpringBootApplication(scanBasePackages = "io.nextpos.cors")
    @RestController
    static class TestApplication {

        public static void main(String[] args) throws Exception {
            SpringApplication.run(TestApplication.class, args);
        }

        @RequestMapping(value = {"test-cors"}, method = RequestMethod.GET)
        public String testCors() {
            return "whatever";
        }
    }

    @Configuration
    static class TestSecurityConfig extends WebSecurityConfigurerAdapter {

        @Override
        protected void configure(final HttpSecurity http) throws Exception {
            http.csrf().disable()
                    .cors().and()
                    .authorizeRequests().anyRequest().permitAll();
        }
    }
}