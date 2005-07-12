
/* 
 * Copyright 2004-2005 OpenSymphony 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not 
 * use this file except in compliance with the License. You may obtain a copy 
 * of the License at 
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0 
 *   
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT 
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the 
 * License for the specific language governing permissions and limitations 
 * under the License.
 * 
 */

/*
 * Previously Copyright (c) 2001-2004 James House
 */
package org.quartz.ee.servlet;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.quartz.Scheduler;
import org.quartz.impl.StdSchedulerFactory;

/**
 * <p>
 * A Servlet that can be used to initialize Quartz, if configured as a
 * load-on-startup servlet in a web application.
 * </p>
 * 
 * <p>
 * You'll want to add something like this to your WEB-INF/web.xml file:
 * 
 * <pre>
 *  &lt;servlet&gt; 
 *      &lt;servlet-name&gt;
 *          QuartzInitializer
 *      &lt;/servlet-name&gt; 
 *      &lt;display-name&gt;
 *          Quartz Initializer Servlet
 *      &lt;/display-name&gt; 
 *      &lt;servlet-class&gt;
 *          org.quartz.ee.servlet.QuartzInitializerServlet
 *      &lt;/servlet-class&gt; 
 *      &lt;load-on-startup&gt;
 *          1
 *      &lt;/load-on-startup&gt;
 *      &lt;init-param&gt;
 *          &lt;param-name&gt;config-file&lt;/param-name&gt;
 *          &lt;param-value&gt;/some/path/my_quartz.properties&lt;/param-value&gt;
 *      &lt;/init-param&gt;
 *      &lt;init-param&gt;
 *          &lt;param-name&gt;shutdown-on-unload&lt;/param-name&gt;
 *          &lt;param-value&gt;true&lt;/param-value&gt;
 *      &lt;/init-param&gt;
 *  &lt;/servlet&gt;
 * </pre>
 * </p>
 * <p>
 * The init parameter 'config-file' can be used to specify the path (and 
 * filename) of your Quartz properties file.  If you leave out this parameter, 
 * the default ("quartz.properties") will be used.
 * </p>
 * 
 * <p>
 * The init parameter 'shutdown-on-unload' can be used to specify whether you
 * want scheduler.shutdown() called when the servlet is unloaded (usually when
 * the application server is being shutdown).  Possible values are "true" or
 * "false".  The default is "true".
 * </p>
 * 
 * @author James House
 */
public class QuartzInitializerServlet extends HttpServlet {

    private boolean performShutdown = true;
    private Scheduler scheduler = null;
    
    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Interface.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    public void init(ServletConfig cfg) throws javax.servlet.ServletException {
        super.init(cfg);

        log("Quartz Initializer Servlet loaded, initializing Scheduler...");

        StdSchedulerFactory factory;
        try {

            String configFile = cfg.getInitParameter("config-file");
            String shutdownPref = cfg.getInitParameter("shutdown-on-unload");
            
            if(shutdownPref != null)
                performShutdown = Boolean.valueOf(shutdownPref).booleanValue();
                
            // get Properties
            if (configFile != null)
            {
                factory = new StdSchedulerFactory(configFile);
            }
            else
            {
                factory = new StdSchedulerFactory();
            }
    
            scheduler = factory.getScheduler();
            scheduler.start();
    
          } catch (Exception e) {
              log("Quartz Scheduler failed to initialize: " + e.toString());
              throw new ServletException(e);
          }
    }

    public void destroy() {
        
        if(!performShutdown)
            return;
        
        try {
            if (scheduler != null) scheduler.shutdown();
        } catch (Exception e) {
            log("Quartz Scheduler failed to shutdown cleanly: " + e.toString());
            e.printStackTrace();
        }

        log("Quartz Scheduler successful shutdown.");
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
    }

}
