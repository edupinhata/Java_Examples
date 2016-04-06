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
package testsuite.regression;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;

import testsuite.BaseTestCase;

import com.mysql.jdbc.DatabaseMetaData;
import com.mysql.jdbc.NonRegisteringDriver;
import com.mysql.jdbc.SQLError;

/**
 * Tests fixes for bugs in CallableStatement code.
 * 
 * @version $Id: CallableStatementRegressionTest.java,v 1.1.2.6 2004/12/09
 *          15:57:26 mmatthew Exp $
 */
public class CallableStatementRegressionTest extends BaseTestCase {
	/**
	 * DOCUMENT ME!
	 * 
	 * @param name
	 */
	public CallableStatementRegressionTest(String name) {
		super(name);

		// TODO Auto-generated constructor stub
	}

	/**
	 * Runs all test cases in this test suite
	 * 
	 * @param args
	 *            ignored
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(CallableStatementRegressionTest.class);
	}

	/**
	 * Tests fix for BUG#3539 getProcedures() does not return any procedures in
	 * result set
	 * 
	 * @throws Exception
	 *             if an error occurs.
	 */
	public void testBug3539() throws Exception {
		if (versionMeetsMinimum(5, 0)) {
			try {
				this.stmt.executeUpdate("DROP PROCEDURE IF EXISTS testBug3539");
				this.stmt.executeUpdate("CREATE PROCEDURE testBug3539()\n"
						+ "BEGIN\n" + "SELECT 1;" + "end\n");

				this.rs = this.conn.getMetaData().getProcedures(null, null,
						"testBug3539");

				assertTrue(this.rs.next());
				assertTrue("testBug3539".equals(this.rs.getString(3)));
			} finally {
				this.stmt.executeUpdate("DROP PROCEDURE IF EXISTS testBug3539");
			}
		}
	}

	/**
	 * Tests fix for BUG#3540 getProcedureColumns doesn't work with wildcards
	 * for procedure name
	 * 
	 * @throws Exception
	 *             if an error occurs.
	 */
	public void testBug3540() throws Exception {
		if (versionMeetsMinimum(5, 0)) {
			try {
				this.stmt.executeUpdate("DROP PROCEDURE IF EXISTS testBug3540");
				this.stmt
						.executeUpdate("CREATE PROCEDURE testBug3540(x int, out y int)\n"
								+ "BEGIN\n" + "SELECT 1;" + "end\n");

				this.rs = this.conn.getMetaData().getProcedureColumns(null,
						null, "testBug3540%", "%");

				assertTrue(this.rs.next());
				assertTrue("testBug3540".equals(this.rs.getString(3)));
				assertTrue("x".equals(this.rs.getString(4)));

				assertTrue(this.rs.next());
				assertTrue("testBug3540".equals(this.rs.getString(3)));
				assertTrue("y".equals(this.rs.getString(4)));

				assertTrue(!this.rs.next());
			} finally {
				this.stmt.executeUpdate("DROP PROCEDURE IF EXISTS testBug3540");
			}
		}
	}

	/**
	 * Tests fix for BUG#7026 - DBMD.getProcedures() doesn't respect catalog
	 * parameter
	 * 
	 * @throws Exception
	 *             if the test fails.
	 */
	public void testBug7026() throws Exception {
		if (versionMeetsMinimum(5, 0)) {
			try {
				this.stmt.executeUpdate("DROP PROCEDURE IF EXISTS testBug7026");
				this.stmt
						.executeUpdate("CREATE PROCEDURE testBug7026(x int, out y int)\n"
								+ "BEGIN\n" + "SELECT 1;" + "end\n");

				//
				// Should be found this time.
				//
				this.rs = this.conn.getMetaData().getProcedures(
						this.conn.getCatalog(), null, "testBug7026");

				assertTrue(this.rs.next());
				assertTrue("testBug7026".equals(this.rs.getString(3)));

				assertTrue(!this.rs.next());

				//
				// This time, shouldn't be found, because not associated with
				// this (bogus) catalog
				//
				this.rs = this.conn.getMetaData().getProcedures("abfgerfg",
						null, "testBug7026");
				assertTrue(!this.rs.next());

				//
				// Should be found this time as well, as we haven't
				// specified a catalog.
				//
				this.rs = this.conn.getMetaData().getProcedures(null, null,
						"testBug7026");

				assertTrue(this.rs.next());
				assertTrue("testBug7026".equals(this.rs.getString(3)));

				assertTrue(!this.rs.next());
			} finally {
				this.stmt.executeUpdate("DROP PROCEDURE IF EXISTS testBug7026");
			}
		}
	}

	/**
	 * Tests fix for BUG#9319 -- Stored procedures with same name in different
	 * databases confuse the driver when it tries to determine parameter
	 * counts/types.
	 * 
	 * @throws Exception
	 *             if the test fails
	 */
	public void testBug9319() throws Exception {
		boolean doASelect = true; // SELECT currently causes the server to
									// hang on the
		// last execution of this testcase, filed as BUG#9405

		if (versionMeetsMinimum(5, 0, 2)) {
			if (isAdminConnectionConfigured()) {
				Connection db2Connection = null;
				Connection db1Connection = null;

				try {
					db2Connection = getAdminConnection();
					db1Connection = getAdminConnection();

					db2Connection.createStatement().executeUpdate(
							"CREATE DATABASE IF NOT EXISTS db_9319_2");
					db2Connection.setCatalog("db_9319_2");

					db2Connection.createStatement().executeUpdate(
							"DROP PROCEDURE IF EXISTS COMPROVAR_USUARI");

					db2Connection
							.createStatement()
							.executeUpdate(
									"CREATE PROCEDURE COMPROVAR_USUARI(IN p_CodiUsuari VARCHAR(10),"
											+ "\nIN p_contrasenya VARCHAR(10),"
											+ "\nOUT p_userId INTEGER,"
											+ "\nOUT p_userName VARCHAR(30),"
											+ "\nOUT p_administrador VARCHAR(1),"
											+ "\nOUT p_idioma VARCHAR(2))"
											+ "\nBEGIN"

											+ (doASelect ? "\nselect 2;"
													: "\nSELECT 2 INTO p_administrador;")
											+ "\nEND");

					db1Connection.createStatement().executeUpdate(
							"CREATE DATABASE IF NOT EXISTS db_9319_1");
					db1Connection.setCatalog("db_9319_1");

					db1Connection.createStatement().executeUpdate(
							"DROP PROCEDURE IF EXISTS COMPROVAR_USUARI");
					db1Connection
							.createStatement()
							.executeUpdate(
									"CREATE PROCEDURE COMPROVAR_USUARI(IN p_CodiUsuari VARCHAR(10),"
											+ "\nIN p_contrasenya VARCHAR(10),"
											+ "\nOUT p_userId INTEGER,"
											+ "\nOUT p_userName VARCHAR(30),"
											+ "\nOUT p_administrador VARCHAR(1))"
											+ "\nBEGIN"
											+ (doASelect ? "\nselect 1;"
													: "\nSELECT 1 INTO p_administrador;")
											+ "\nEND");

					CallableStatement cstmt = db2Connection
							.prepareCall("{ call COMPROVAR_USUARI(?, ?, ?, ?, ?, ?) }");
					cstmt.setString(1, "abc");
					cstmt.setString(2, "def");
					cstmt.registerOutParameter(3, java.sql.Types.INTEGER);
					cstmt.registerOutParameter(4, java.sql.Types.VARCHAR);
					cstmt.registerOutParameter(5, java.sql.Types.VARCHAR);

					cstmt.registerOutParameter(6, java.sql.Types.VARCHAR);

					cstmt.execute();

					if (doASelect) {
						this.rs = cstmt.getResultSet();
						assertTrue(this.rs.next());
						assertEquals(2, this.rs.getInt(1));
					} else {
						assertEquals(2, cstmt.getInt(5));
					}

					cstmt = db1Connection
							.prepareCall("{ call COMPROVAR_USUARI(?, ?, ?, ?, ?, ?) }");
					cstmt.setString(1, "abc");
					cstmt.setString(2, "def");
					cstmt.registerOutParameter(3, java.sql.Types.INTEGER);
					cstmt.registerOutParameter(4, java.sql.Types.VARCHAR);
					cstmt.registerOutParameter(5, java.sql.Types.VARCHAR);

					try {
						cstmt.registerOutParameter(6, java.sql.Types.VARCHAR);
						fail("Should've thrown an exception");
					} catch (SQLException sqlEx) {
						assertEquals(SQLError.SQL_STATE_ILLEGAL_ARGUMENT, sqlEx
								.getSQLState());
					}

					cstmt = db1Connection
							.prepareCall("{ call COMPROVAR_USUARI(?, ?, ?, ?, ?) }");
					cstmt.setString(1, "abc");
					cstmt.setString(2, "def");
					cstmt.registerOutParameter(3, java.sql.Types.INTEGER);
					cstmt.registerOutParameter(4, java.sql.Types.VARCHAR);
					cstmt.registerOutParameter(5, java.sql.Types.VARCHAR);

					cstmt.execute();

					if (doASelect) {
						this.rs = cstmt.getResultSet();
						assertTrue(this.rs.next());
						assertEquals(1, this.rs.getInt(1));
					} else {
						assertEquals(1, cstmt.getInt(5));
					}

					String quoteChar = db2Connection.getMetaData()
							.getIdentifierQuoteString();

					cstmt = db2Connection.prepareCall("{ call " + quoteChar
							+ db1Connection.getCatalog() + quoteChar + "."
							+ quoteChar + "COMPROVAR_USUARI" + quoteChar
							+ "(?, ?, ?, ?, ?) }");
					cstmt.setString(1, "abc");
					cstmt.setString(2, "def");
					cstmt.registerOutParameter(3, java.sql.Types.INTEGER);
					cstmt.registerOutParameter(4, java.sql.Types.VARCHAR);
					cstmt.registerOutParameter(5, java.sql.Types.VARCHAR);

					cstmt.execute();

					if (doASelect) {
						this.rs = cstmt.getResultSet();
						assertTrue(this.rs.next());
						assertEquals(1, this.rs.getInt(1));
					} else {
						assertEquals(1, cstmt.getInt(5));
					}
				} finally {
					if (db2Connection != null) {
						db2Connection.createStatement().executeUpdate(
								"DROP PROCEDURE IF EXISTS COMPROVAR_USUARI");
						db2Connection.createStatement().executeUpdate(
								"DROP DATABASE IF EXISTS db_9319_2");
					}

					if (db1Connection != null) {
						db1Connection.createStatement().executeUpdate(
								"DROP PROCEDURE IF EXISTS COMPROVAR_USUARI");
						db1Connection.createStatement().executeUpdate(
								"DROP DATABASE IF EXISTS db_9319_1");
					}
				}
			}
		}
	}

	/*
	 * public void testBug9319() throws Exception { boolean doASelect = false; //
	 * SELECT currently causes the server to hang on the // last execution of
	 * this testcase, filed as BUG#9405
	 * 
	 * if (versionMeetsMinimum(5, 0, 2)) { if (isAdminConnectionConfigured()) {
	 * Connection db2Connection = null; Connection db1Connection = null;
	 * 
	 * try { db2Connection = getAdminConnection();
	 * 
	 * db2Connection.createStatement().executeUpdate( "CREATE DATABASE IF NOT
	 * EXISTS db_9319"); db2Connection.setCatalog("db_9319");
	 * 
	 * db2Connection.createStatement().executeUpdate( "DROP PROCEDURE IF EXISTS
	 * COMPROVAR_USUARI");
	 * 
	 * db2Connection.createStatement().executeUpdate( "CREATE PROCEDURE
	 * COMPROVAR_USUARI(IN p_CodiUsuari VARCHAR(10)," + "\nIN p_contrasenya
	 * VARCHAR(10)," + "\nOUT p_userId INTEGER," + "\nOUT p_userName
	 * VARCHAR(30)," + "\nOUT p_administrador VARCHAR(1)," + "\nOUT p_idioma
	 * VARCHAR(2))" + "\nBEGIN"
	 *  + (doASelect ? "\nselect 2;" : "\nSELECT 2 INTO p_administrador;" ) +
	 * "\nEND");
	 * 
	 * this.stmt .executeUpdate("DROP PROCEDURE IF EXISTS COMPROVAR_USUARI");
	 * this.stmt .executeUpdate("CREATE PROCEDURE COMPROVAR_USUARI(IN
	 * p_CodiUsuari VARCHAR(10)," + "\nIN p_contrasenya VARCHAR(10)," + "\nOUT
	 * p_userId INTEGER," + "\nOUT p_userName VARCHAR(30)," + "\nOUT
	 * p_administrador VARCHAR(1))" + "\nBEGIN" + (doASelect ? "\nselect 1;" :
	 * "\nSELECT 1 INTO p_administrador;" ) + "\nEND");
	 * 
	 * CallableStatement cstmt = db2Connection .prepareCall("{ call
	 * COMPROVAR_USUARI(?, ?, ?, ?, ?, ?) }"); cstmt.setString(1, "abc");
	 * cstmt.setString(2, "def"); cstmt.registerOutParameter(3,
	 * java.sql.Types.INTEGER); cstmt.registerOutParameter(4,
	 * java.sql.Types.VARCHAR); cstmt.registerOutParameter(5,
	 * java.sql.Types.VARCHAR);
	 * 
	 * cstmt.registerOutParameter(6, java.sql.Types.VARCHAR);
	 * 
	 * cstmt.execute();
	 * 
	 * if (doASelect) { this.rs = cstmt.getResultSet();
	 * assertTrue(this.rs.next()); assertEquals(2, this.rs.getInt(1)); } else {
	 * assertEquals(2, cstmt.getInt(5)); }
	 * 
	 * cstmt = this.conn .prepareCall("{ call COMPROVAR_USUARI(?, ?, ?, ?, ?, ?)
	 * }"); cstmt.setString(1, "abc"); cstmt.setString(2, "def");
	 * cstmt.registerOutParameter(3, java.sql.Types.INTEGER);
	 * cstmt.registerOutParameter(4, java.sql.Types.VARCHAR);
	 * cstmt.registerOutParameter(5, java.sql.Types.VARCHAR);
	 * 
	 * try { cstmt.registerOutParameter(6, java.sql.Types.VARCHAR);
	 * fail("Should've thrown an exception"); } catch (SQLException sqlEx) {
	 * assertEquals(SQLError.SQL_STATE_ILLEGAL_ARGUMENT, sqlEx .getSQLState()); }
	 * 
	 * cstmt = this.conn .prepareCall("{ call COMPROVAR_USUARI(?, ?, ?, ?, ?)
	 * }"); cstmt.setString(1, "abc"); cstmt.setString(2, "def");
	 * cstmt.registerOutParameter(3, java.sql.Types.INTEGER);
	 * cstmt.registerOutParameter(4, java.sql.Types.VARCHAR);
	 * cstmt.registerOutParameter(5, java.sql.Types.VARCHAR);
	 * 
	 * cstmt.execute();
	 * 
	 * if (doASelect) { this.rs = cstmt.getResultSet();
	 * assertTrue(this.rs.next()); assertEquals(1, this.rs.getInt(1)); } else {
	 * assertEquals(1, cstmt.getInt(5)); }
	 * 
	 * String quoteChar =
	 * db2Connection.getMetaData().getIdentifierQuoteString();
	 * 
	 * cstmt = db2Connection .prepareCall("{ call " + quoteChar +
	 * this.conn.getCatalog() + quoteChar + "." + quoteChar + "COMPROVAR_USUARI" +
	 * quoteChar + "(?, ?, ?, ?, ?) }"); cstmt.setString(1, "abc");
	 * cstmt.setString(2, "def"); cstmt.registerOutParameter(3,
	 * java.sql.Types.INTEGER); cstmt.registerOutParameter(4,
	 * java.sql.Types.VARCHAR); cstmt.registerOutParameter(5,
	 * java.sql.Types.VARCHAR);
	 * 
	 * cstmt.execute();
	 * 
	 * if (doASelect) { this.rs = cstmt.getResultSet();
	 * assertTrue(this.rs.next()); assertEquals(1, this.rs.getInt(1)); } else {
	 * assertEquals(1, cstmt.getInt(5)); } } finally { if (db2Connection !=
	 * null) { db2Connection.createStatement().executeUpdate( "DROP PROCEDURE IF
	 * EXISTS COMPROVAR_USUARI"); //
	 * db2Connection.createStatement().executeUpdate( // "DROP DATABASE IF
	 * EXISTS db_9319"); }
	 * 
	 * this.stmt .executeUpdate("DROP PROCEDURE IF EXISTS COMPROVAR_USUARI"); } } } }
	 */

	/**
	 * Tests fix for BUG#9682 - Stored procedures with DECIMAL parameters with
	 * storage specifications that contained "," in them would fail.
	 * 
	 * @throws Exception
	 *             if the test fails.
	 */
	public void testBug9682() throws Exception {
		if (versionMeetsMinimum(5, 0)) {
			CallableStatement cStmt = null;

			try {
				this.stmt.executeUpdate("DROP PROCEDURE IF EXISTS testBug9682");
				this.stmt
						.executeUpdate("CREATE PROCEDURE testBug9682(decimalParam DECIMAL(18,0))"
								+ "\nBEGIN" + "\n   SELECT 1;" + "\nEND");
				cStmt = this.conn.prepareCall("Call testBug9682(?)");
				cStmt.setDouble(1, 18.0);
				cStmt.execute();
			} finally {
				if (cStmt != null) {
					cStmt.close();
				}

				this.stmt.executeUpdate("DROP PROCEDURE IF EXISTS testBug9682");
			}
		}
	}

	/**
	 * Tests fix forBUG#10310 - Driver doesn't support {?=CALL(...)} for calling
	 * stored functions. This involved adding support for function retrieval to
	 * DatabaseMetaData.getProcedures() and getProcedureColumns() as well.
	 * 
	 * @throws Exception
	 *             if the test fails.
	 */
	public void testBug10310() throws Exception {
		
		if (versionMeetsMinimum(5, 0)) {
			CallableStatement cStmt = null;

			try {
				this.stmt.executeUpdate("DROP FUNCTION IF EXISTS testBug10310");
				this.stmt
						.executeUpdate("CREATE FUNCTION testBug10310(a float, b int, c int) RETURNS INT"
								+ "\nBEGIN" + "\nRETURN a;" + "\nEND");
				cStmt = this.conn.prepareCall("{? = CALL testBug10310(?,?,?)}");
				cStmt.registerOutParameter(1, Types.INTEGER);
				cStmt.setFloat(2, 2);
				cStmt.setInt(3, 1);
				cStmt.setInt(4, 1);
				
				assertFalse(cStmt.execute());
				assertEquals(2f, cStmt.getInt(1), .001);
				assertEquals("java.lang.Integer", cStmt.getObject(1).getClass()
						.getName());

				assertEquals(-1, cStmt.executeUpdate());
				assertEquals(2f, cStmt.getInt(1), .001);
				assertEquals("java.lang.Integer", cStmt.getObject(1).getClass()
						.getName());

				if (!isRunningOnJdk131()) {
					cStmt.setFloat("a", 4);
					cStmt.setInt("b", 1);
					cStmt.setInt("c", 1);
					
					assertFalse(cStmt.execute());
					assertEquals(4f, cStmt.getInt(1), .001);
					assertEquals("java.lang.Integer", cStmt.getObject(1).getClass()
							.getName());
					
					assertEquals(-1, cStmt.executeUpdate());
					assertEquals(4f, cStmt.getInt(1), .001);
					assertEquals("java.lang.Integer", cStmt.getObject(1).getClass()
							.getName());
				}
				
				// Check metadata while we're at it

				java.sql.DatabaseMetaData dbmd = this.conn.getMetaData();

				this.rs = dbmd.getProcedures(this.conn.getCatalog(), null,
						"testBug10310");
				this.rs.next();
				assertEquals("testBug10310", this.rs
						.getString("PROCEDURE_NAME"));
				assertEquals(DatabaseMetaData.procedureReturnsResult, this.rs
						.getShort("PROCEDURE_TYPE"));
				cStmt.setNull(2, Types.FLOAT);
				cStmt.setInt(3, 1);
				cStmt.setInt(4, 1);
				
				assertFalse(cStmt.execute());
				assertEquals(0f, cStmt.getInt(1), .001);
				assertEquals(true, cStmt.wasNull());
				assertEquals(null, cStmt.getObject(1));
				assertEquals(true, cStmt.wasNull());

				assertEquals(-1, cStmt.executeUpdate());
				assertEquals(0f, cStmt.getInt(1), .001);
				assertEquals(true, cStmt.wasNull());
				assertEquals(null, cStmt.getObject(1));
				assertEquals(true, cStmt.wasNull());

				cStmt = this.conn.prepareCall("{? = CALL testBug10310(4,5,?)}");
				cStmt.registerOutParameter(1, Types.INTEGER);
				cStmt.setInt(2, 1);
				
				assertFalse(cStmt.execute());
				assertEquals(4f, cStmt.getInt(1), .001);
				assertEquals("java.lang.Integer", cStmt.getObject(1).getClass()
						.getName());
				
				assertEquals(-1, cStmt.executeUpdate());
				assertEquals(4f, cStmt.getInt(1), .001);
				assertEquals("java.lang.Integer", cStmt.getObject(1).getClass()
						.getName());
			} finally {
				if (this.rs != null) {
					this.rs.close();
					this.rs = null;
				}

				if (cStmt != null) {
					cStmt.close();
				}

				this.stmt.executeUpdate("DROP FUNCTION IF EXISTS testBug10310");
			}
		}
	}
	
	/**
	 * Tests fix for Bug#12417 - stored procedure catalog name is
	 * case-sensitive on Windows (this is actually a server bug,
	 * but we have a workaround in place for it now).
	 * 
	 * @throws Exception if the test fails.
	 */
	public void testBug12417() throws Exception {
		if (versionMeetsMinimum(5, 0) && isServerRunningOnWindows()) {
			Connection ucCatalogConn = null;
			
			try {
				this.stmt.executeUpdate("DROP PROCEDURE IF EXISTS testBug12417");
				this.stmt.executeUpdate("CREATE PROCEDURE testBug12417()\n"
						+ "BEGIN\n" + "SELECT 1;" + "end\n");
				ucCatalogConn = getConnectionWithProps(null);
				ucCatalogConn.setCatalog(this.conn.getCatalog().toUpperCase());
				ucCatalogConn.prepareCall("{call testBug12417()}");
			} finally {
				this.stmt.executeUpdate("DROP PROCEDURE IF EXISTS testBug3539");
				
				if (ucCatalogConn != null) {
					ucCatalogConn.close();
				}
			}
		}
	}
	
	public void testBug15121() throws Exception {
		if (false /* needs to be fixed on server */) {
			if (versionMeetsMinimum(5, 0)) {
				this.stmt.executeUpdate("DROP PROCEDURE IF EXISTS p_testBug15121");
				
				this.stmt.executeUpdate("CREATE PROCEDURE p_testBug15121()\n"
						+ "BEGIN\n"
						+ "SELECT * from idonotexist;\n"
						+ "END");
				
				Properties props = new Properties();
				props.setProperty(NonRegisteringDriver.DBNAME_PROPERTY_KEY, "");
				
				Connection noDbConn = null;
				
				try {
					noDbConn = getConnectionWithProps(props);
					
					StringBuffer queryBuf = new StringBuffer("{call ");
					String quotedId = this.conn.getMetaData().getIdentifierQuoteString();
					queryBuf.append(quotedId);
					queryBuf.append(this.conn.getCatalog());
					queryBuf.append(quotedId);
					queryBuf.append(".p_testBug15121()}");
					
					noDbConn.prepareCall(queryBuf.toString()).execute();
				} finally {
					if (noDbConn != null) {
						noDbConn.close();
					}
				}
			}
		}
	}

	/**
	 * Tests fix for BUG#15464 - INOUT parameter does not store IN value.
	 * 
	 * @throws Exception
	 *             if the test fails
	 */
	
	public void testBug15464() throws Exception {
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
	
	/**
	 * Tests fix for BUG#17898 - registerOutParameter not working when some 
	 * parameters pre-populated. Still waiting for feedback from JDBC experts
	 * group to determine what correct parameter count from getMetaData() 
	 * should be, however.
	 * 
	 * @throws Exception if the test fails
	 */
	public void testBug17898() throws Exception {
		if (versionMeetsMinimum(5, 0)) {
			this.stmt.executeUpdate("DROP PROCEDURE IF EXISTS testBug17898");
			this.stmt.executeUpdate("CREATE PROCEDURE testBug17898(param1 VARCHAR(50), OUT param2 INT)\nBEGIN\nDECLARE rtn INT;\nSELECT 1 INTO rtn;\nSET param2=rtn;\nEND");
			
			CallableStatement cstmt = this.conn.prepareCall("{CALL testBug17898('foo', ?)}");
			cstmt.registerOutParameter(1, Types.INTEGER);
			cstmt.execute();
			assertEquals(1, cstmt.getInt(1));
			
			if (!isRunningOnJdk131()) {
				cstmt.clearParameters();
				cstmt.registerOutParameter("param2", Types.INTEGER);
				cstmt.execute();
				assertEquals(1, cstmt.getInt(1));
			}
		}
	}
	
	public void testHugeNumberOfParameters() throws Exception {
		if (versionMeetsMinimum(5, 0)) {
			this.stmt.executeUpdate("DROP PROCEDURE IF EXISTS testHugeNumberOfParameters");
			
			StringBuffer procDef = new StringBuffer("CREATE PROCEDURE testHugeNumberOfParameters(");
			
			for (int i = 0; i < 274; i++) {
				if (i != 0) {
					procDef.append(",");
				}
				
				procDef.append(" OUT param_" + i + " VARCHAR(32)");
			}
			
			procDef.append(")\nBEGIN\nSELECT 1;\nEND");
			this.stmt.executeUpdate(procDef.toString());
			
			CallableStatement cStmt = null;
			
			try {
				cStmt = this.conn.prepareCall("{call testHugeNumberOfParameters(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?," +
			
					"?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?," +
					"?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?," +
					"?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?," +
					"?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?," +
					"?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?," +
					"?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}");
				cStmt.registerOutParameter(274, Types.VARCHAR);

				cStmt.execute();
			} finally {
				if (cStmt != null) {
					cStmt.close();
				}
			}
		}
	}
	
	public void testPrepareOfMultiRs() throws Exception {
		if (versionMeetsMinimum(5, 0)) {
			this.stmt.executeUpdate("Drop procedure if exists p");
			this.stmt.executeUpdate("create procedure p () begin select 1; select 2; end;");
			PreparedStatement ps = null;
			
			try {
				ps = this.conn.prepareStatement("call p()");

				ps.execute();
				this.rs = ps.getResultSet();
				assertTrue(this.rs.next());
				assertEquals(1, this.rs.getInt(1));
				assertTrue(ps.getMoreResults());
				this.rs = ps.getResultSet();
				assertTrue(this.rs.next());
				assertEquals(2, this.rs.getInt(1));
				assertTrue(!ps.getMoreResults());
			} finally {
				if (this.rs != null) {
					this.rs.close();
					this.rs = null;
				}
				
				if (ps != null) {
					ps.close();
				}
			}
		}
	}
}
