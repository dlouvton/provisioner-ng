package com.dlouvton.badger.api.server;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;

import org.codehaus.jackson.jaxrs.JacksonJsonProvider;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;
import com.dlouvton.badger.api.v1.EnvironmentResource;
import com.dlouvton.badger.api.v1.FileResource;
import com.dlouvton.badger.api.v1.ModelResource;
import com.dlouvton.badger.api.v1.MainResource;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;

/**
 * Badger Guice Server configuration. Creates an Injector, and binds it to
 * an anonymous Module. 
 */
public class BadgerServerConfig extends GuiceServletContextListener {
    @Override
    protected Injector getInjector() {
        return Guice.createInjector(new ServletModule() {
            @Override
            protected void configureServlets() {
                /* bind the REST resources */
                bind(MainResource.class).in(Scopes.SINGLETON);
                bind(ModelResource.class).in(Scopes.SINGLETON);
                bind(EnvironmentResource.class).in(Scopes.SINGLETON);
                bind(FileResource.class);

                /* bind jackson converters for JAXB/JSON serialization */
                bind(MessageBodyReader.class).to(JacksonJsonProvider.class);
                bind(MessageBodyWriter.class).to(JacksonJsonProvider.class);
                Map<String, String> initParams = new HashMap<String, String>();
                initParams.put("com.sun.jersey.config.feature.Trace",
                        "true");
                serve("*").with(
                        GuiceContainer.class,
                        initParams);
            }
        });
    }
}
