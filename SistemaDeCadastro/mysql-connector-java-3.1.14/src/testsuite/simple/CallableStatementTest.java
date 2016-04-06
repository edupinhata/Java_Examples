/*
 Copyright (C) 2002-2004 MySQL AB

 This program is free software; you can redistribute it and/or modify
 it under the terms of version 2 of the GNU General Public License as 
 published by the Free Software Foundation.

 There are special exceptions to the terms and conditions of the GPL 
 as it is applied to this software. View the full text of the 
 exception in file EXCEPTIONS-CONNECTOR-J in the directory of this 
 software distribution.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA



 */
package testsuite.simple;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;

import testsuite.BaseTestCase;

import com.mysql.jdbc.SQLError;

/**
 * Tests callable statement functionality.
 * 
 * @author Mark Matthews
 * @version $Id: CallableStatementTest.java,v 1.1.2.1 2005/05/13 18:58:37
 *          mmatthews Exp $
 */
public class CallableStatementTest extends BaseTestCase {
	/**
	 * DOCUMENT ME!
	 * 
	 * @param name
	 */
	public CallableStatementTest(String name) {
		super(name);

		// TODO Auto-generated constructor stub
	}

	/**
	 * Tests functioning of inout parameters
	 * 
	 * @throws Exception
	 *             if the test fails
	 */

	public void testInOutParams() throws Exception {
		if (versionMeetsMinimum(5, 0)) {
			CallableStatement storedProc = null;

			try {
				this.stmt
						.executeUpdate("DROP PROCEDURE IF EXISTS testInOutParam");
				this.stmt
						.executeUpdate("create procedure testInOutParam(IN p1 VARCHAR(255), INOUT p2 INT)\n"
								+ "begin\n"
								+ " DECLARE z INT;\n"
								+ "SET z = p2 + 1;\n"
								+ "SET p2 = z;\n"
								+ "SELECT p1;\n"
								+ "SELECT CONCAT('zyxw', p1);\n"
								+ "end\n");

				storedProc = this.conn.prepareCall("{call testInOutParam(?, ?)}");

				storedProc.setString(1, "abcd");
				storedProc.setInt(2, 4);
				storedProc.registerOutParameter(2, Types.INTEGER);

				storedProc.execute();
		
				assertEquals(5, storedProc.getInt(2));
			} finally {
				this.stmt.executeUpdate("DROP PROCEDURE IF EXISTS testInOutParam");
			}
		}
	}
	
	public void testBatch() throws Exception {
		if (versionMeetsMinimum(5, 0)) {
			CallableStatement storedProc = null;

			try {
				this.stmt
						.executeUpdate("DROP PROCEDURE IF EXISTS testBatch");
				createTable("testBatchTable", "(field1 INT)");
				
				this.stmt
						.executeUpdate("create procedure testBatch(IN foo VARCHAR(15))\n"
								+ "begin\n"
								+ "INSERT INTO testBatchTable VALUES (foo);\n"
								+ "end\n");

				storedProc = this.conn.prepareCall("{call testBatch(?)}");

				storedProc.setInt(1, 1);
				storedProc.addBatch();
				storedProc.setInt(1, 2);
				storedProc.addBatch();
				int[] counts = storedProc.executeBatch();
				
				assertEquals(2, counts.length);
				assertEquals(1, counts[0]);
				assertEquals(1, counts[1]);
				
				this.rs = this.stmt.executeQuery("SELECT field1 FROM testBatchTable ORDER BY field1 ASC");
				assertTrue(this.rs.next());
				assertEquals(1, this.rs.getInt(1));
				assertTrue(this.rs.next());
				assertEquals(2, this.rs.getInt(1));
			} finally {
				if (this.rs != null) {
					this.rs.close();
					this.rs = null;
				}
				
				this.stmt.executeUpdate("DROP PROCEDURE IF EXISTS testBatch");
			}
		}
	}

	/**
	 * Tests functioning of output parameters.
	 * 
	 * @throws Exception
	 *             if the test fails.
	 */
	public void testOutParams() throws Exception {
		if (versionMeetsMinimum(5, 0)) {
			CallableStatement storedProc = null;

			try {
				this.stmt
						.executeUpdate("DROP PROCEDURE IF EXISTS testOutParam");
				this.stmt
						.executeUpdate("CREATE PROCEDURE testOutParam(x int, out y int)\n"
								+ "begin\n"
								+ "declare z int;\n"
								+ "set z = x+1, y = z;\n" + "end\n");

				storedProc = this.conn.prepareCall("{call testOutParam(?, ?)}");

				storedProc.setInt(1, 5);
				storedProc.registerOutParameter(2, Types.INTEGER);

				storedProc.execute();

				System.out.println(storedProc);

				int indexedOutParamToTest = storedProc.getInt(2);
				int namedOutParamToTest = storedProc.getInt("y");

				assertTrue("Named and indexed parameter are not the same",
						indexedOutParamToTest == namedOutParamToTest);
				assertTrue("Output value not returned correctly",
						indexedOutParamToTest == 6);

				// Start over, using named parameters, this time
				storedProc.clearParameters();
				storedProc.setInt("x", 32);
				storedProc.registerOutParameter("y", Types.INTEGER);

				storedProc.execute();

				indexedOutParamToTest = storedProc.getInt(2);
				namedOutParamToTest = storedProc.getInt("y");

				assertTrue("Named and indexed parameter are not the same",
						indexedOutParamToTest == namedOutParamToTest);
				assertTrue("Output value not returned correctly",
						indexedOutParamToTest == 33);

				try {
					storedProc.registerOutParameter("x", Types.INTEGER);
					assertTrue(
							"Should not be able to register an out parameter on a non-out parameter",
							true);
				} catch (SQLException sqlEx) {
					if (!SQLError.SQL_STATE_ILLEGAL_ARGUMENT.equals(sqlEx
							.getSQLState())) {
						throw sqlEx;
					}
				}

				try {
					storedProc.registerOutParameter(1, Types.INTEGER);
					assertTrue(
							"Should not be able to register an out parameter on a non-out parameter",
							true);
				} catch (SQLException sqlEx) {
					if (!SQLError.SQL_STATE_ILLEGAL_ARGUMENT.equals(sqlEx
							.getSQLState())) {
						throw sqlEx;
					}
				}

				try {
					storedProc.getInt("x");
					assertTrue(
							"Should not be able to retreive an out parameter on a non-out parameter",
							true);
				} catch (SQLException sqlEx) {
					if (!SQLError.SQL_STATE_COLUMN_NOT_FOUND.equals(sqlEx
							.getSQLState())) {
						throw sqlEx;
					}
				}
			} finally {
				this.stmt.executeUpdate("DROP PROCEDURE testOutParam");
			}
		}
	}

	/**
	 * Tests functioning of output parameters.
	 * 
	 * @throws Exception
	 *             if the test fails.
	 */
	public void testResultSet() throws Exception {
		if (versionMeetsMinimum(5, 0)) {
			CallableStatement storedProc = null;

			try {
				this.stmt
						.executeUpdate("DROP TABLE IF EXISTS testSpResultTbl1");
				this.stmt
						.executeUpdate("DROP TABLE IF EXISTS testSpResultTbl2");
				this.stmt
						.executeUpdate("CREATE TABLE testSpResultTbl1 (field1 INT)");
				this.stmt
						.executeUpdate("INSERT INTO testSpResultTbl1 VALUES (1), (2)");
				this.stmt
						.executeUpdate("CREATE TABLE testSpResultTbl2 (field2 varchar(255))");
				this.stmt
						.executeUpdate("INSERT INTO testSpResultTbl2 VALUES ('abc'), ('def')");

				this.stmt
						.executeUpdate("DROP PROCEDURE IF EXISTS testSpResult");
				this.stmt
						.executeUpdate("CREATE PROCEDURE testSpResult()\n"
								+ "BEGIN\n"
								+ "SELECT field2 FROM testSpResultTbl2 WHERE field2='abc';\n"
								+ "UPDATE testSpResultTbl1 SET field1=2;\n"
								+ "SELECT field2 FROM testSpResultTbl2 WHERE field2='def';\n"
								+ "end\n");

				storedProc = this.conn.prepareCall("{call testSpResult()}");

				storedProc.execute();

				this.rs = storedProc.getResultSet();

				ResultSetMetaData rsmd = this.rs.getMetaData();

				assertTrue(rsmd.getColumnCount() == 1);
				assertTrue("field2".equals(rsmd.getColumnName(1)));
				assertTrue(rsmd.getColumnType(1) == Types.VARCHAR);

				assertTrue(this.rs.next());

				assertTrue("abc".equals(this.rs.getString(1)));

				// TODO: This does not yet work in MySQL 5.0
				// assertTrue(!storedProc.getMoreResults());
				// assertTrue(storedProc.getUpdateCount() == 2);
				assertTrue(storedProc.getMoreResults());

				ResultSet nextResultSet = storedProc.getResultSet();

				rsmd = nextResultSet.getMetaData();

				assertTrue(rsmd.getColumnCount() == 1);
				assertTrue("field2".equals(rsmd.getColumnName(1)));
				assertTrue(rsmd.getColumnType(1) == Types.VARCHAR);

				assertTrue(nextResultSet.next());

				assertTrue("def".equals(nextResultSet.getString(1)));

				nextResultSet.close();

				this.rs.close();

				storedProc.execute();

			} finally {
				this.stmt
						.executeUpdate("DROP PROCEDURE IF EXISTS testSpResult");
				this.stmt
						.executeUpdate("DROP TABLE IF EXISTS testSpResultTbl1");
				this.stmt
						.executeUpdate("DROP TABLE IF EXISTS testSpResultTbl2");
			}
		}
	}

	/**
	 * Tests parsing of stored procedures
	 * 
	 * @throws Exception
	 *             if an error occurs.
	 */
	public void testSPParse() throws Exception {

		if (versionMeetsMinimum(5, 0)) {

			CallableStatement storedProc = null;

			try {

				this.stmt.executeUpdate("DROP PROCEDURE IF EXISTS testSpParse");
				this.stmt
						.executeUpdate("CREATE PROCEDURE testSpParse(IN FOO VARCHAR(15))\n"
								+ "BEGIN\n" + "SELECT 1;\n" + "end\n");

				storedProc = this.conn.prepareCall("{call testSpParse()}");

			} finally {
				this.stmt.executeUpdate("DROP PROCEDURE IF EXISTS testSpParse");
			}
		}
	}

	/**
	 * Tests parsing/execution of stored procedures with no parameters...
	 * 
	 * @throws Exception
	 *             if an error occurs.
	 */
	public void testSPNoParams() throws Exception {

		if (versionMeetsMinimum(5, 0)) {

			CallableStatement storedProc = null;

			try {

				this.stmt
						.executeUpdate("DROP PROCEDURE IF EXISTS testSPNoParams");
				this.stmt.executeUpdate("CREATE PROCEDURE testSPNoParams()\n"
						+ "BEGIN\n" + "SELECT 1;\n" + "end\n");

				storedProc = this.conn.prepareCall("{call testSPNoParams()}");
				storedProc.execute();

			} finally {
				this.stmt
						.executeUpdate("DROP PROCEDURE IF EXISTS testSPNoParams");
			}
		}
	}

	/**
	 * Tests parsing of stored procedures
	 * 
	 * @throws Exception
	 *             if an error occurs.
	 */
	public void testSPCache() throws Exception {

		if (versionMeetsMinimum(5, 0)) {

			CallableStatement storedProc = null;

			try {

				this.stmt.executeUpdate("DROP PROCEDURE IF EXISTS testSpParse");
				this.stmt
						.executeUpdate("CREATE PROCEDURE testSpParse(IN FOO VARCHAR(15))\n"
								+ "BEGIN\n" + "SELECT 1;\n" + "end\n");

				int numIterations = 10000;

				long startTime = System.currentTimeMillis();

				for (int i = 0; i < numIterations; i++) {
					storedProc = this.conn.prepareCall("{call testSpParse(?)}");
					storedProc.close();
				}

				long elapsedTime = System.currentTimeMillis() - startTime;

				System.out.println("Standard parsing/execution: " + elapsedTime
						+ " ms");

				storedProc = this.conn.prepareCall("{call testSpParse(?)}");
				storedProc.setString(1, "abc");
				this.rs = storedProc.executeQuery();

				assertTrue(this.rs.next());
				assertTrue(this.rs.getInt(1) == 1);

				Properties props = new Properties();
				props.setProperty("cacheCallableStmts", "true");

				Connection cachedSpConn = getConnectionWithProps(props);

				startTime = System.currentTimeMillis();

				for (int i = 0; i < numIterations; i++) {
					storedProc = cachedSpConn
							.prepareCall("{call testSpParse(?)}");
					storedProc.close();
				}

				elapsedTime = System.currentTimeMillis() - startTime;

				System.out
						.println("Cached parse stage: " + elapsedTime + " ms");

				storedProc = cachedSpConn.prepareCall("{call testSpParse(?)}");
				storedProc.setString(1, "abc");
				this.rs = storedProc.executeQuery();

				assertTrue(this.rs.next());
				assertTrue(this.rs.getInt(1) == 1);

			} finally {
				this.stmt.executeUpdate("DROP PROCEDURE IF EXISTS testSpParse");
			}
		}
	}

	/**
	 * Runs all test cases in this test suite
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(CallableStatementTest.class);
	}
}
