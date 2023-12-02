package com.example.demo.exceptions;

public class DuplicateItemNameException extends RuntimeException {

	public DuplicateItemNameException(String message) {
        super(message);
    }
}
