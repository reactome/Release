package org.reactome.core.controller;

import java.rmi.RemoteException;


public class ReactomeRemoteException extends RemoteException {
    // A hacker to make message visible to the client. Message
    // in super class Throwable cannot be displayed
    private String message;

    public ReactomeRemoteException() {
    }

    public ReactomeRemoteException(String message) {
        this();
        setMessage(message);
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String msg) {
        this.message = msg;
    }

}