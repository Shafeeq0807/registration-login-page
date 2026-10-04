package net.shafeeq.accounts;

import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    UserDetailsService userDetailsService(AccountRepository accounts) {
        return email ->
            accounts
                .findByEmail(AccountService.normalizeEmail(email))
                .map(account ->
                    User.withUsername(account.getEmail())
                        .password(account.getPasswordHash())
                        .roles(account.getRole())
                        .build()
                )
                .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password."));
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth ->
            auth
                .requestMatchers(
                    "/",
                    "/index",
                    "/login",
                    "/register",
                    "/register/save",
                    "/styles.css",
                    "/error",
                    "/api/csrf",
                    "/api/register"
                )
                .permitAll()
                .requestMatchers("/users", "/api/users")
                .hasRole("ADMIN")
                .anyRequest()
                .authenticated()
        )
            .formLogin(form ->
                form
                    .loginPage("/login")
                    .usernameParameter("email")
                    .defaultSuccessUrl("/dashboard", true)
                    .permitAll()
            )
            .logout(logout -> logout.logoutSuccessUrl("/login?logout"))
            .headers(headers ->
                headers.contentSecurityPolicy(csp ->
                    csp.policyDirectives(
                        "default-src 'self'; style-src 'self'; img-src 'self' data:; form-action 'self'; base-uri 'self'; frame-ancestors 'none'"
                    )
                )
            )
            .exceptionHandling(ex ->
                ex.defaultAuthenticationEntryPointFor(
                    (request, response, error) -> response.sendError(401),
                    request -> request.getServletPath().startsWith("/api/")
                )
            );
        // CSRF protection and POST-only logout remain enabled.
        return http.build();
    }
}
