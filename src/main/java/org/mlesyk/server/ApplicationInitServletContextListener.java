package org.mlesyk.server;

import org.mlesyk.Loggers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Created by Maks on 20.08.2017.
 */
public class ApplicationInitServletContextListener implements ServletContextListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(Loggers.SERVER);

    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        LOGGER.debug("ServletContextListener destroyed");
    }

    //Run this before web application is started
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        LOGGER.debug("ServletContextListener started");
    }
}
