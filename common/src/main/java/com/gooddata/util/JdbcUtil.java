/*
 * Copyright (c) 2009, GoodData Corporation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided
 * that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright notice, this list of conditions and
 *        the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright notice, this list of conditions
 *        and the following disclaimer in the documentation and/or other materials provided with the distribution.
 *     * Neither the name of the GoodData Corporation nor the names of its contributors may be used to endorse
 *        or promote products derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.gooddata.util;

import org.apache.log4j.Logger;

import java.io.IOException;
import java.sql.*;


/**
 * GoodData JDBC utilities
 *
 * @author zd <zd@gooddata.com>
 * @version 1.0
 */
public class JdbcUtil {

    private static Logger l = Logger.getLogger(JdbcUtil.class);

    /**
     * Execute update
     *
     * @param con connection
     * @param sql sql statement
     * @return number of affected rows
     * @throws SQLException in case of a db issue
     */
    public static int executeUpdate(Connection con, String sql) throws SQLException {
        Statement s = null;
        int rc = 0;
        try {
            s = con.createStatement();
            l.debug("Executing SQL: statement='" + sql + "'");
            rc = s.executeUpdate(sql);
            l.debug("Executed SQL: statement='" + sql + "' rows=" + rc);
            return rc;
        } catch (SQLException e) {
            l.debug("Error executing SQL: statement='" + sql + "', result='" + rc + "'", e);
            throw e;
        } finally {
            if (s != null)
                s.close();
        }
    }

    /**
     * Executes an update using a prepared statement.
     * <p/>
     * Example:
     * <pre> final int myid = 42;
     * JdbcUtil.executeUpdate(con, "select * from table where id = ?", new StatementHandler() {
     *     public void prepare(PreparedStatement stmt) throws SQLException {
     *         stmt.setInt(1, myid);
     *     }
     * });
     * </pre>
     *
     * @param con connection
     * @param sql sql prepared statement (i.e. may contain the "?" placeholders to be populated by the <tt>sh</tt> handler
     * @param sh  {@link StatementHandler} instance to setup the prepared statement
     * @return number of affected rows
     * @throws SQLException in case of a db issue
     */
    public static int executeUpdate(Connection con, String sql, StatementHandler sh) throws SQLException {
        PreparedStatement s = null;
        int rc = 0;
        try {
            s = con.prepareStatement(sql);
            sh.prepare(s);
            l.debug("Executing SQL: statement='" + sh.toString() + "'");
            rc = s.executeUpdate();
            return rc;
        } catch (SQLException e) {
            l.debug("Error executing SQL: statement='" + sql + "', result='" + rc + "'", e);
            throw e;
        } finally {
            if (s != null)
                s.close();
        }
    }

    /**
     * Execute query
     *
     * @param s         JDBC statement
     * @param sql       sql statement
     * @param fetchSize max fetch size
     * @return Jdbc ResultSet
     * @throws SQLException in case of a db issue
     */
    private static ResultSet executeQuery(Statement s, String sql, int fetchSize) throws SQLException {
        ResultSet rs = null;
        try {
            s.setFetchSize(fetchSize);
            l.debug("Executing SQL: statement='" + sql + "'");
            rs = s.executeQuery(sql);
            l.debug("Executed SQL: statement='" + sql + "'");
            return rs;
        } catch (SQLException e) {
            l.debug("Error executing SQL: statement='" + sql + "'", e);
            throw e;
        }
    }

    /**
     * Execute query an passes the ResultSet to the given handler on each record
     *
     * @param c         JDBC connection
     * @param sql       sql statement
     * @param handler   Jdbc ResultSet handler
     * @param limit     maximum number of rows to process
     * @param fetchSize max fetch size
     * @throws SQLException in case of a db issue
     */
    public static void executeQuery(Connection c, String sql, ResultSetHandler handler, int limit, int fetchSize) throws SQLException, IOException {
        Statement st = null;
        ResultSet rs = null;
        try {
            st = c.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY,
                    java.sql.ResultSet.CONCUR_READ_ONLY);
            l.debug("Executing SQL: statement='" + st.toString() + "'");
            rs = executeQuery(st, sql, fetchSize);
            while (rs.next() && limit-- > 0) {
                handler.handle(rs);
            }
        } finally {
            if (rs != null)
                rs.close();
            if (st != null)
                st.close();
        }
    }

    /**
     * Execute query an passes the ResultSet to the given handler on each record
     *
     * @param c         JDBC connection
     * @param sql       sql statement
     * @param handler   Jdbc ResultSet handler
     * @param fetchSize max fetch size
     * @throws SQLException in case of a db issue
     */
    public static void executeQuery(Connection c, String sql, ResultSetHandler handler, int fetchSize) throws SQLException, IOException {
        executeQuery(c, sql, handler, Integer.MAX_VALUE, fetchSize);
    }

    /**
     * Result set handler callback interface for {@link JdbcUtil#executeQuery(Connection, String, ResultSetHandler, int)}
     */
    public static interface ResultSetHandler {
        public void handle(ResultSet rs) throws SQLException, IOException;
    }

    /**
     * Dummy resultset handler callback; literally does nothing
     */
    public static class DummyResultSetHandler implements ResultSetHandler {
        public void handle(ResultSet rs) throws SQLException {
            ; // intentionally does nothing
        }
    }

    /**
     * Statement handler callback interface for {@link JdbcUtil#executeUpdate(Connection, String, StatementHandler)}
     */
    public static interface StatementHandler {
        public void prepare(PreparedStatement stmt) throws SQLException;
    }

}
