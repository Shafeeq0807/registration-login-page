package net.shafeeq.accounts;

public record AccountView(Long id, String firstName, String lastName, String email, String role) {
    public static AccountView of(Account account) {
        return new AccountView(
            account.getId(),
            account.getFirstName(),
            account.getLastName(),
            account.getEmail(),
            account.getRole()
        );
    }
}
