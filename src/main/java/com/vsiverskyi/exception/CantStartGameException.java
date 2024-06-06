package com.vsiverskyi.exception;

public class CantStartGameException extends RuntimeException{
    public CantStartGameException(String message) {
        super(message);
    }
}
