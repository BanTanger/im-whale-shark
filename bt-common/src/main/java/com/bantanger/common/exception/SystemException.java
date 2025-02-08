package com.bantanger.common.exception;

/**
 * @author gim
 **/
public class SystemException extends RuntimeException {

  private String msg;

  public SystemException(String msg) {
    super(msg);
    this.msg = msg;
  }
}
