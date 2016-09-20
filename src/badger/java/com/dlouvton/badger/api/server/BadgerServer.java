package com.dlouvton.badger.api.server;


import java.util.EnumSet;

import javax.servlet.DispatcherType;

import com.dlouvton.badger.util.PropertyLoader;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;

import com.google.inject.servlet.GuiceFilter;

/**
 * Starts an embedded Jetty server, serving resources injected by Guice (configured in BadgerServerConfig.java).
 */
public class BadgerServer {
	static Server server; 
	public static final String DEFAULT_SERVER_PORT="8080";
	
    public static void main(String[] args) throws Exception {
        start();
    }
    
    public static void start() throws Exception {
        start(Integer.valueOf(PropertyLoader.getTopLevelProperties().getProperty("badger-server-port",DEFAULT_SERVER_PORT)));
    }
    
    public static void start(int port) throws Exception {
        server = new Server(port);
        ServletContextHandler root = new ServletContextHandler(server, "/", ServletContextHandler.SESSIONS);

        root.addEventListener(new BadgerServerConfig());
        root.addFilter(GuiceFilter.class, "/*", EnumSet.of(DispatcherType.REQUEST));
        root.addServlet(EmptyServlet.class, "/*"); //must add an empty servlet first, otherwise it will not work

        server.start();
    }
    public static void stop() throws Exception {
        if (server.isRunning()) {
        	server.stop();
        }
    }
}
