/*
 * Copyright (c) 2004-2005 by OpenSymphony
 * All rights reserved.
 * 
 * Previously Copyright (c) 2001-2004 James House
 */
package org.quartz.utils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <p>
 * A <code>ConnectionProvider</code> that provides connections from a <code>DataSource</code>
 * that is managed by an application server, and made available via JNDI.
 * </p>
 * 
 * @see DBConnectionManager
 * @see ConnectionProvider
 * @see PoolingConnectionProvider
 * 
 * @author James House
 * @author Sharada Jambula
 * @author Mohammad Rezaei
 * @author Patrick Lightbody
 * @author Srinivas Venkatarangaiah
 */
public class JNDIConnectionProvider implements ConnectionProvider {

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Data members.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    private String url;

    private Properties props;

    private Object datasource;

    private boolean alwaysLookup = false;

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Constructors.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    /**
     * Constructor
     * 
     * @param jndiUrl
     *          The url for the datasource
     */
    public JNDIConnectionProvider(String jndiUrl, boolean alwaysLookup) {
        this.url = jndiUrl;
        this.alwaysLookup = alwaysLookup;
        init();
    }

    /**
     * Constructor
     * 
     * @param jndiUrl
     *          The URL for the DataSource
     * @param jndiProps
     *          The JNDI properties to use when establishing the InitialContext
     *          for the lookup of the given URL.
     */
    public JNDIConnectionProvider(String jndiUrl, Properties jndiProps,
            boolean alwaysLookup) {
        this.url = jndiUrl;
        this.props = jndiProps;
        this.alwaysLookup = alwaysLookup;
        init();
    }

    /*
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     * 
     * Interface.
     * 
     * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
     */

    Log getLog() {
        return LogFactory.getLog(getClass());
    }

    private void init() {

        if (!isAlwaysLookup()) {
            try {
                Context ctx = null;
                if (props != null) ctx = new InitialContext(props);
                else
                    ctx = new InitialContext();

                datasource = (DataSource) ctx.lookup(url);
            } catch (Exception e) {
                getLog().error(
                        "Error looking up datasource: " + e.getMessage(), e);
            }
        }
    }

    public Connection getConnection() throws SQLException {
        try {
            Object ds = this.datasource;

            if (ds == null || isAlwaysLookup()) {
                Context ctx = null;
                if (props != null) ctx = new InitialContext(props);
                else
                    ctx = new InitialContext();

                ds = ctx.lookup(url);
                if (!isAlwaysLookup()) this.datasource = ds;
            }

            if (ds == null)
                    throw new SQLException(
                            "There is no object at the JNDI URL '" + url + "'");

            if (ds instanceof XADataSource) return (((XADataSource) ds)
                    .getXAConnection().getConnection());
            else if (ds instanceof DataSource) return ((DataSource) ds)
                    .getConnection();
            else
                throw new SQLException("Object at JNDI URL '" + url
                        + "' is not a DataSource.");
        } catch (Exception e) {
            this.datasource = null;
            throw new SQLException(
                    "Could not retrieve datasource via JNDI url '" + url + "' "
                            + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    public boolean isAlwaysLookup() {
        return alwaysLookup;
    }

    public void setAlwaysLookup(boolean b) {
        alwaysLookup = b;
    }

    /* 
     * @see org.quartz.utils.ConnectionProvider#shutdown()
     */
    public void shutdown() throws SQLException {
        // do nothing
    }

}
