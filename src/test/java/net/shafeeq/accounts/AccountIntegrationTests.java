package net.shafeeq.accounts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(
    properties = { "spring.datasource.url=jdbc:h2:mem:accounts-test;MODE=MySQL;DB_CLOSE_DELAY=-1" }
)
@AutoConfigureMockMvc
class AccountIntegrationTests {

    @Autowired
    MockMvc mvc;

    @Autowired
    AccountRepository accounts;

    @Autowired
    AccountService service;

    @Autowired
    PasswordEncoder encoder;

    @BeforeEach
    void reset() {
        accounts.deleteAll();
    }

    private Registration form() {
        var form = new Registration();
        form.setFirstName("Ada");
        form.setLastName("Lovelace");
        form.setEmail("ADA@example.com");
        form.setPassword("A lengthy passphrase 123!");
        return form;
    }

    @Test
    void publicPagesRenderAndCsrfTokenIsIncluded() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk());
        mvc.perform(get("/register"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("_csrf")));
        mvc.perform(get("/login")).andExpect(status().isOk());
    }

    @Test
    void registrationHashesPasswordsAndNeverGrantsAdmin() {
        service.register(form());
        var account = accounts.findByEmail("ada@example.com").orElseThrow();
        assertThat(account.getRole()).isEqualTo("USER");
        assertThat(account.getPasswordHash()).isNotEqualTo(form().getPassword());
        assertThat(encoder.matches(form().getPassword(), account.getPasswordHash())).isTrue();
    }

    @Test
    void rejectsCaseInsensitiveDuplicates() {
        service.register(form());
        var second = form();
        second.setEmail("ada@example.com");
        org.junit.jupiter.api.Assertions.assertThrows(DuplicateAccountException.class, () ->
            service.register(second)
        );
        assertThat(accounts.count()).isEqualTo(1);
    }

    @Test
    void csrfIsRequiredForRegistration() throws Exception {
        mvc.perform(post("/register/save").param("email", "ada@example.com")).andExpect(
            status().isForbidden()
        );
        assertThat(accounts.count()).isZero();
    }

    @Test
    void validatesRegistrationAndDoesNotEchoPassword() throws Exception {
        mvc.perform(
            post("/register/save")
                .with(csrf())
                .param("firstName", "Ada")
                .param("lastName", "Lovelace")
                .param("email", "invalid")
                .param("password", "super-secret-123")
        )
            .andExpect(status().isOk())
            .andExpect(model().attributeHasFieldErrors("user", "email"))
            .andExpect(
                content().string(
                    org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("super-secret-123")
                    )
                )
            );
        assertThat(accounts.count()).isZero();
    }

    @Test
    void loginProvidesPersonalDashboardAndPostLogout() throws Exception {
        service.register(form());
        var result = mvc
            .perform(
                formLogin("/login").user("email", "ada@example.com").password(form().getPassword())
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/dashboard"))
            .andReturn();
        var session = (MockHttpSession) result.getRequest().getSession(false);
        mvc.perform(get("/dashboard").session(session))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Ada")));
        mvc.perform(post("/logout").session(session).with(csrf())).andExpect(
            status().is3xxRedirection()
        );
    }

    @Test
    void directoryRequiresAdmin() throws Exception {
        mvc.perform(get("/users").with(user("member").roles("USER"))).andExpect(
            status().isForbidden()
        );
        mvc.perform(get("/users").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
    }

    @Test
    void apiEnforcesAuthenticationAndValidation() throws Exception {
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/csrf"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isNotEmpty());
        mvc.perform(
            post("/api/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"firstName\":\"Ada\",\"lastName\":\"Lovelace\",\"email\":\"ada@example.com\",\"password\":\"A lengthy passphrase 123!\"}"
                )
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.role").value("USER"))
            .andExpect(jsonPath("$.passwordHash").doesNotExist());
        mvc.perform(get("/api/me").with(user("ada@example.com").roles("USER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("ada@example.com"));
        mvc.perform(
            post("/api/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"bad\",\"password\":\"short\"}")
        ).andExpect(status().isBadRequest());
    }
}
