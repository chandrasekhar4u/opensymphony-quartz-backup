/*
 * Copyright (c) 2004-2005 by OpenSymphony
 * All rights reserved.
 * 
 * Previously Copyright (c) 2001-2004 James House
 */
package org.quartz.impl.jdbcjobstore.oracle;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import oracle.sql.BLOB;

import org.apache.commons.logging.Log;
import org.quartz.Calendar;
import org.quartz.JobDetail;
import org.quartz.impl.jdbcjobstore.StdJDBCDelegate;

/**
 * <p>
 * This is a driver delegate for the Oracle JDBC driver. To use this delegate,
 * <code>jdbcDriverVendor</code> should be configured as 'Oracle' with any
 * <code>jdbcDriverVersion</code>.
 * </p>
 * 
 * @author James House
 * @author Patrick Lightbody
 */
public class OracleDelegate extends StdJDBCDelegate {
    /**
     * <p>
     * Create new OrcaleDelegate instance.
     * </p>
     * 
     * @param logger
     *          the logger to use during execution
     * @param tablePrefix
     *          the prefix of all table names
     */
    public OracleDelegate(Log logger, String tablePrefix, String instanceId) {
        super(logger, tablePrefix, instanceId);
    }

    /**
     * <p>
     * Create new OrcaleDelegate instance.
     * </p>
     * 
     * @param logger
     *          the logger to use during execution
     * @param tablePrefix
     *          the prefix of all table names
     * @param useProperties
     *          use java.util.Properties for storage
     */
    public OracleDelegate(Log logger, String tablePrefix, String instanceId,
            Boolean useProperties) {
        super(logger, tablePrefix, instanceId, useProperties);
    }

    public static final String INSERT_ORACLE_JOB_DETAIL = "INSERT INTO "
            + TABLE_PREFIX_SUBST + TABLE_JOB_DETAILS + " (" + COL_JOB_NAME
            + ", " + COL_JOB_GROUP + ", " + COL_DESCRIPTION + ", "
            + COL_JOB_CLASS + ", " + COL_IS_DURABLE + ", " + COL_IS_VOLATILE
            + ", " + COL_IS_STATEFUL + ", " + COL_REQUESTS_RECOVERY + ") "
            + " VALUES(?, ?, ?, ?, ?, ?, ?, ?)";

    public static final String UPDATE_ORACLE_JOB_DETAIL = "UPDATE "
            + TABLE_PREFIX_SUBST + TABLE_JOB_DETAILS + " SET "
            + COL_DESCRIPTION + " = ?, " + COL_JOB_CLASS + " = ?, "
            + COL_IS_DURABLE + " = ?, " + COL_IS_VOLATILE + " = ?, "
            + COL_IS_STATEFUL + " = ?, " + COL_REQUESTS_RECOVERY + " = ? "
            + " WHERE " + COL_JOB_NAME + " = ? AND " + COL_JOB_GROUP + " = ?";

    public static final String UPDATE_ORACLE_JOB_DETAIL_BLOB = "UPDATE "
            + TABLE_PREFIX_SUBST + TABLE_JOB_DETAILS + " SET "
            + COL_JOB_DATAMAP + " = ? " + " WHERE " + COL_JOB_NAME
            + " = ? AND " + COL_JOB_GROUP + " = ?";

    public static final String UPDATE_ORACLE_JOB_DETAIL_EMPTY_BLOB = "UPDATE "
            + TABLE_PREFIX_SUBST + TABLE_JOB_DETAILS + " SET "
            + COL_JOB_DATAMAP + " = EMPTY_BLOB() " + " WHERE " + COL_JOB_NAME
            + " = ? AND " + COL_JOB_GROUP + " = ?";

    public static final String SELECT_ORACLE_JOB_DETAIL_BLOB = "SELECT "
            + COL_JOB_DATAMAP + " FROM " + TABLE_PREFIX_SUBST
            + TABLE_JOB_DETAILS + " WHERE " + COL_JOB_NAME + " = ? AND "
            + COL_JOB_GROUP + " = ? FOR UPDATE";

    public static final String INSERT_ORACLE_CALENDAR = "INSERT INTO "
            + TABLE_PREFIX_SUBST + TABLE_CALENDARS + " (" + COL_CALENDAR_NAME
            + ", " + COL_CALENDAR + ") " + " VALUES(?, EMPTY_BLOB())";

    public static final String SELECT_ORACLE_CALENDAR_BLOB = "SELECT "
            + COL_CALENDAR + " FROM " + TABLE_PREFIX_SUBST + TABLE_CALENDARS
            + " WHERE " + COL_CALENDAR_NAME + " = ? FOR UPDATE";

    public static final String UPDATE_ORACLE_CALENDAR_BLOB = "UPDATE "
            + TABLE_PREFIX_SUBST + TABLE_CALENDARS + " SET " + COL_CALENDAR
            + " = ? " + " WHERE " + COL_CALENDAR_NAME + " = ?";

    //---------------------------------------------------------------------------
    // protected methods that can be overridden by subclasses
    //---------------------------------------------------------------------------

    protected Object getObjectFromBlob(ResultSet rs, String colName)
            throws ClassNotFoundException, IOException, SQLException {
        Object obj = null;
        InputStream binaryInput = rs.getBinaryStream(colName);
        if (binaryInput != null) {
            ObjectInputStream in = new ObjectInputStream(binaryInput);
            obj = in.readObject();
            in.close();
        }

        return obj;
    }

    public int insertJobDetail(Connection conn, JobDetail job)
            throws IOException, SQLException {

        ByteArrayOutputStream baos = serializeJobData(job.getJobDataMap());
        byte[] data = baos.toByteArray();
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            ps = conn.prepareStatement(rtp(INSERT_JOB_DETAIL));
            ps.setString(1, job.getName());
            ps.setString(2, job.getGroup());
            ps.setString(3, job.getDescription());
            ps.setString(4, job.getJobClass().getName());
            ps.setBoolean(5, job.isDurable());
            ps.setBoolean(6, job.isVolatile());
            ps.setBoolean(7, job.isStateful());
            ps.setBoolean(8, job.requestsRecovery());

            ps.setBinaryStream(9, null, 0);
            ps.executeUpdate();
            ps.close();

            ps = conn
                    .prepareStatement(rtp(UPDATE_ORACLE_JOB_DETAIL_EMPTY_BLOB));
            ps.setString(1, job.getName());
            ps.setString(2, job.getGroup());
            ps.executeUpdate();
            ps.close();

            ps = conn.prepareStatement(rtp(SELECT_ORACLE_JOB_DETAIL_BLOB));
            ps.setString(1, job.getName());
            ps.setString(2, job.getGroup());

            rs = ps.executeQuery();

            int res = 0;

            BLOB dbBlob = null;
            if (rs.next()) {
                dbBlob = (BLOB) rs.getBlob(1);
                dbBlob.putBytes(1, data);
            } else {
                return res;
            }

            rs.close();
            ps.close();

            ps = conn.prepareStatement(rtp(UPDATE_ORACLE_JOB_DETAIL_BLOB));
            ps.setBlob(1, dbBlob);
            ps.setString(2, job.getName());
            ps.setString(3, job.getGroup());

            res = ps.executeUpdate();

            if (res > 0) {
                String[] jobListeners = job.getJobListenerNames();
                for (int i = 0; jobListeners != null && i < jobListeners.length; i++)
                    insertJobListener(conn, job, jobListeners[i]);
            }

            return res;
        } finally {
            if (null != rs) {
                try {
                    rs.close();
                } catch (SQLException ignore) {
                }
            }
            if (null != ps) {
                try {
                    ps.close();
                } catch (SQLException ignore) {
                }
            }
        }

    }

    protected Object getJobDetailFromBlob(ResultSet rs, String colName)
            throws ClassNotFoundException, IOException, SQLException {
        if (canUseProperties()) {
            InputStream binaryInput = rs.getBinaryStream(colName);
            return binaryInput;
        }

        return getObjectFromBlob(rs, colName);
    }

    public int updateJobDetail(Connection conn, JobDetail job)
            throws IOException, SQLException {
        ByteArrayOutputStream baos = serializeJobData(job.getJobDataMap());
        byte[] data = baos.toByteArray();

        PreparedStatement ps = null;
        PreparedStatement ps2 = null;
        ResultSet rs = null;

        try {
            ps = conn.prepareStatement(rtp(UPDATE_ORACLE_JOB_DETAIL));
            ps.setString(1, job.getDescription());
            ps.setString(2, job.getJobClass().getName());
            ps.setBoolean(3, job.isDurable());
            ps.setBoolean(4, job.isVolatile());
            ps.setBoolean(5, job.isStateful());
            ps.setBoolean(6, job.requestsRecovery());
            ps.setString(7, job.getName());
            ps.setString(8, job.getGroup());

            ps.executeUpdate();
            ps.close();

            ps = conn
                    .prepareStatement(rtp(UPDATE_ORACLE_JOB_DETAIL_EMPTY_BLOB));
            ps.setString(1, job.getName());
            ps.setString(2, job.getGroup());
            ps.executeUpdate();
            ps.close();

            ps = conn.prepareStatement(rtp(SELECT_ORACLE_JOB_DETAIL_BLOB));
            ps.setString(1, job.getName());
            ps.setString(2, job.getGroup());

            rs = ps.executeQuery();

            int res = 0;

            if (rs.next()) {
                BLOB dbBlob = (BLOB) rs.getBlob(1);
                dbBlob.putBytes(1, data);
                ps2 = conn.prepareStatement(rtp(UPDATE_ORACLE_JOB_DETAIL_BLOB));

                ps2.setBlob(1, dbBlob);
                ps2.setString(2, job.getName());
                ps2.setString(3, job.getGroup());

                res = ps2.executeUpdate();
            }

            if (res > 0) {
                deleteJobListeners(conn, job.getName(), job.getGroup());
                String[] jobListeners = job.getJobListenerNames();
                for (int i = 0; jobListeners != null && i < jobListeners.length; i++)
                    insertJobListener(conn, job, jobListeners[i]);
            }

            return res;

        } finally {
            if (null != rs) {
                try {
                    rs.close();
                } catch (SQLException ignore) {
                }
            }
            if (null != ps) {
                try {
                    ps.close();
                } catch (SQLException ignore) {
                }
            }
            if (null != ps2) {
                try {
                    ps2.close();
                } catch (SQLException ignore) {
                }
            }
        }
    }

    public int insertCalendar(Connection conn, String calendarName,
            Calendar calendar) throws IOException, SQLException {
        ByteArrayOutputStream baos = serializeObject(calendar);

        PreparedStatement ps = null;
        PreparedStatement ps2 = null;
        ResultSet rs = null;

        try {
            ps = conn.prepareStatement(rtp(INSERT_ORACLE_CALENDAR));
            ps.setString(1, calendarName);

            ps.executeUpdate();
            ps.close();

            ps = conn.prepareStatement(rtp(SELECT_ORACLE_CALENDAR_BLOB));
            ps.setString(1, calendarName);

            rs = ps.executeQuery();

            if (rs.next()) {
                BLOB dbBlob = (BLOB) rs.getBlob(1);
                dbBlob.putBytes(1, baos.toByteArray());
                ps2 = conn.prepareStatement(rtp(UPDATE_ORACLE_CALENDAR_BLOB));

                ps2.setBlob(1, dbBlob);
                ps2.setString(2, calendarName);

                return ps2.executeUpdate();
            }

            return 0;

        } finally {
            if (null != rs) {
                try {
                    rs.close();
                } catch (SQLException ignore) {
                }
            }
            if (null != ps) {
                try {
                    ps.close();
                } catch (SQLException ignore) {
                }
            }
            if (null != ps2) {
                try {
                    ps2.close();
                } catch (SQLException ignore) {
                }
            }
        }
    }

    public int updateCalendar(Connection conn, String calendarName,
            Calendar calendar) throws IOException, SQLException {
        ByteArrayOutputStream baos = serializeObject(calendar);

        PreparedStatement ps = null;
        PreparedStatement ps2 = null;
        ResultSet rs = null;

        try {
            ps = conn.prepareStatement(rtp(SELECT_ORACLE_CALENDAR_BLOB));
            ps.setString(1, calendarName);

            rs = ps.executeQuery();

            if (rs.next()) {
                BLOB dbBlob = (BLOB) rs.getBlob(1);
                dbBlob.putBytes(1, baos.toByteArray());
                ps2 = conn.prepareStatement(rtp(UPDATE_ORACLE_CALENDAR_BLOB));

                ps2.setBlob(1, dbBlob);
                ps2.setString(2, calendarName);

                return ps2.executeUpdate();
            }

            return 0;

        } finally {
            if (null != rs) {
                try {
                    rs.close();
                } catch (SQLException ignore) {
                }
            }
            if (null != ps) {
                try {
                    ps.close();
                } catch (SQLException ignore) {
                }
            }
            if (null != ps2) {
                try {
                    ps2.close();
                } catch (SQLException ignore) {
                }
            }
        }
    }

    public int updateJobData(Connection conn, JobDetail job)
            throws IOException, SQLException {
        ByteArrayOutputStream baos = serializeJobData(job.getJobDataMap());
        byte[] data = baos.toByteArray();

        PreparedStatement ps = null;
        PreparedStatement ps2 = null;
        ResultSet rs = null;

        try {
            ps = conn.prepareStatement(rtp(SELECT_ORACLE_JOB_DETAIL_BLOB));
            ps.setString(1, job.getName());
            ps.setString(2, job.getGroup());

            rs = ps.executeQuery();

            int res = 0;

            if (rs.next()) {
                BLOB dbBlob = (BLOB) rs.getBlob(1);
                dbBlob.putBytes(1, data);
                ps2 = conn.prepareStatement(rtp(UPDATE_ORACLE_JOB_DETAIL_BLOB));

                ps2.setBlob(1, dbBlob);
                ps2.setString(2, job.getName());
                ps2.setString(3, job.getGroup());

                res = ps2.executeUpdate();
            }

            return res;
        } finally {
            if (null != rs) {
                try {
                    rs.close();
                } catch (SQLException ignore) {
                }
            }
            if (null != ps) {
                try {
                    ps.close();
                } catch (SQLException ignore) {
                }
            }
            if (null != ps2) {
                try {
                    ps2.close();
                } catch (SQLException ignore) {
                }
            }
        }
    }

}

// EOF
