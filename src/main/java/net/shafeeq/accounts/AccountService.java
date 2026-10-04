package net.shafeeq.accounts;

import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final AccountRepository accounts;
    private final PasswordEncoder encoder;

    public AccountService(AccountRepository accounts, PasswordEncoder encoder) {
        this.accounts = accounts;
        this.encoder = encoder;
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    @Transactional
    public AccountView register(Registration form) {
        String email = normalizeEmail(form.getEmail());
        if (accounts.existsByEmail(email)) throw new DuplicateAccountException();
        Account account = new Account(
            form.getFirstName().trim(),
            form.getLastName().trim(),
            email,
            encoder.encode(form.getPassword())
        );
        return AccountView.of(accounts.saveAndFlush(account));
    }

    @Transactional(readOnly = true)
    public AccountView profile(String email) {
        return accounts.findByEmail(normalizeEmail(email)).map(AccountView::of).orElseThrow();
    }
}
