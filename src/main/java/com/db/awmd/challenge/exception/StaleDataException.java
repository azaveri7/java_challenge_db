package com.db.awmd.challenge.exception;

public class StaleDataException extends RuntimeException {

  public StaleDataException(String message) {
    super(message);
  }
}