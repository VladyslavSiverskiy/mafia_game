package com.vsiverskyi.exception;

public class NoRoleWithSuchIdException extends RuntimeException {
    public NoRoleWithSuchIdException(String message) {
        super(message);
    }
}
