package com.vsiverskyi.exception;

public class NoRoleWithSuchTitleException extends RuntimeException{
    public NoRoleWithSuchTitleException(String message) {
        super(message);
    }
}
