package net.shafeeq.accounts;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final AccountService service;
    private final AccountRepository accounts;

    public ApiController(AccountService service, AccountRepository accounts) {
        this.service = service;
        this.accounts = accounts;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
    }

    @PostMapping("/register")
    public ResponseEntity<AccountView> register(@Valid @RequestBody Registration form) {
        return ResponseEntity.status(201).body(service.register(form));
    }

    @GetMapping("/me")
    public AccountView me(Principal principal) {
        return service.profile(principal.getName());
    }

    @GetMapping("/users")
    public Map<String, Object> users(@RequestParam(defaultValue = "0") int page) {
        var result = accounts
            .findAll(PageRequest.of(Math.max(0, Math.min(page, 1000000)), 20, Sort.by("id")))
            .map(AccountView::of);
        return Map.of(
            "content",
            result.getContent(),
            "page",
            result.getNumber(),
            "totalPages",
            result.getTotalPages()
        );
    }

    @ExceptionHandler({ DuplicateAccountException.class, DataIntegrityViolationException.class })
    ResponseEntity<Map<String, String>> duplicate() {
        return ResponseEntity.status(409).body(
            Map.of("error", "An account already exists with this email.")
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, String>> invalid() {
        return ResponseEntity.badRequest().body(
            Map.of(
                "error",
                "Provide valid names, email, and a password of 12–72 characters (at most 72 UTF-8 bytes)."
            )
        );
    }
}
