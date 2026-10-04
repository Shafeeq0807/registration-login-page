package net.shafeeq.accounts;

public class DuplicateAccountException extends RuntimeException {

    public DuplicateAccountException() {
        super("An account already exists with this email.");
    }
}
