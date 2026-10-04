package net.shafeeq.accounts;

import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    private final AccountService service;
    private final AccountRepository accounts;

    public AuthController(AccountService service, AccountRepository accounts) {
        this.service = service;
        this.accounts = accounts;
    }

    @GetMapping({ "/", "/index" })
    public String home() {
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("user", new Registration());
        return "register";
    }

    @PostMapping("/register/save")
    public String save(@Valid @ModelAttribute("user") Registration form, BindingResult result) {
        if (result.hasErrors()) {
            form.setPassword(null);
            return "register";
        }
        try {
            service.register(form);
        } catch (DuplicateAccountException | DataIntegrityViolationException error) {
            result.rejectValue("email", "duplicate", "An account already exists with this email.");
            form.setPassword(null);
            return "register";
        }
        return "redirect:/login?registered";
    }

    @GetMapping("/dashboard")
    public String dashboard(Principal principal, Model model) {
        model.addAttribute("profile", service.profile(principal.getName()));
        return "dashboard";
    }

    @GetMapping("/users")
    public String users(@RequestParam(defaultValue = "0") int page, Model model) {
        var users = accounts
            .findAll(PageRequest.of(Math.max(0, Math.min(page, 1000000)), 20, Sort.by("id")))
            .map(AccountView::of);
        model.addAttribute("users", users);
        return "users";
    }
}
