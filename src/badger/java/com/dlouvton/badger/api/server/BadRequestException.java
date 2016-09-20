package com.dlouvton.badger.api.server;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class BadRequestException extends WebApplicationException {
   
	private static final long serialVersionUID = 1L;

	//TODO: have a constructor with a throwable, so we can print the cause
	public BadRequestException(String message) {
        super(Response.status(Response.Status.BAD_REQUEST)
            .entity(message+"\n").type(MediaType.TEXT_PLAIN).build());
    }
	
}
