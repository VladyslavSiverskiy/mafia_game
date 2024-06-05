package com.vsiverskyi.exception;

public class NoGameWithSuchIdException extends RuntimeException{
    public NoGameWithSuchIdException(String message) {
        super(message);
    }
}
