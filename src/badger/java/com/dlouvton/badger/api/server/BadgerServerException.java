package com.dlouvton.badger.api.server;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class BadgerServerException extends WebApplicationException {
   
	private static final long serialVersionUID = 1L;
	public BadgerServerException(String message) {
        super(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity(message+"\n").type(MediaType.TEXT_PLAIN).build());
    }
}
