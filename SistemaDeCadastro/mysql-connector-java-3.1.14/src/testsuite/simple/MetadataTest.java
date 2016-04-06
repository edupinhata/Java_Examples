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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;

import testsuite.BaseTestCase;

/**
 * Tests DatabaseMetaData methods.
 * 
 * @author Mark Matthews
 * @version $Id: MetadataTest.java 5313 2006-05-30 17:59:29Z mmatthews $
 */
public class MetadataTest extends BaseTestCase {
	// ~ Constructors
	// -----------------------------------------------------------

	/**
	 * Creates a new MetadataTest object.
	 * 
	 * @param name
	 *            DOCUMENT ME!
	 */
	public MetadataTest(String name) {
		super(name);
	}

	// ~ Methods
	// ----------------------------------------------------------------

	/**
	 * Runs all test cases in this test suite
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(MetadataTest.class);
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * @throws Exception
	 *             DOCUMENT ME!
	 */
	public void setUp() throws Exception {
		super.setUp();
		createTestTable();
	}

	/**
	 * DOCUMENT ME!
	 * 
	 * @throws SQLException
	 *             DOCUMENT ME!
	 */
	public void testForeignKeys() throws SQLException {
		try {
			DatabaseMetaData dbmd = this.conn.getMetaData();
			this.rs = dbmd.getImportedKeys(null, null, "child");

			while (this.rs.next()) {
				String pkColumnName = this.rs.getString("PKCOLUMN_NAME");
				String fkColumnName = this.rs.getString("FKCOLUMN_NAME");
				assertTrue("Primary Key not returned correctly ('"
						+ pkColumnName + "' != 'parent_id')", pkColumnName
						.equalsIgnoreCase("parent_id"));
				assertTrue("Foreign Key not returned correctly ('"
						+ fkColumnName + "' != 'parent_id_fk')", fkColumnName
						.equalsIgnoreCase("parent_id_fk"));
			}

			this.rs.close();
			this.rs = dbmd.getExportedKeys(null, null, "parent");

			while (this.rs.next()) {
				String pkColumnName = this.rs.getString("PKCOLUMN_NAME");
				String fkColumnName = this.rs.getString("FKCOLUMN_NAME");
				String fkTableName = this.rs.getString("FKTABLE_NAME");
				assertTrue("Primary Key not returned correctly ('"
						+ pkColumnName + "' != 'parent_id')", pkColumnName
						.equalsIgnoreCase("parent_id"));
				assertTrue(
						"Foreign Key table not returned correctly for getExportedKeys ('"
								+ fkTableName + "' != 'child')", fkTableName
								.equalsIgnoreCase("child"));
				assertTrue(
						"Foreign Key not returned correctly for getExportedKeys ('"
								+ fkColumnName + "' != 'parent_id_fk')",
						fkColumnName.equalsIgnoreCase("parent_id_fk"));
			}

			this.rs.close();

			this.rs = dbmd.getCrossReference(null, null, "cpd_foreign_3", null,
					null, "cpd_foreign_4");

			assertTrue(this.rs.next());

			String pkColumnName = this.rs.getString("PKCOLUMN_NAME");
			String pkTableName = this.rs.getString("PKTABLE_NAME");
			String fkColumnName = this.rs.getString("FKCOLUMN_NAME");
			String fkTableName = this.rs.getString("FKTABLE_NAME");
			String deleteAction = cascadeOptionToString(this.rs
					.getInt("DELETE_RULE"));
			String updateAction = cascadeOptionToString(this.rs
					.getInt("UPDATE_RULE"));

			assertEquals(pkColumnName, "cpd_foreign_1_id");
			assertEquals(pkTableName, "cpd_foreign_3");
			assertEquals(fkColumnName, "cpd_foreign_1_id");
			assertEquals(fkTableName, "cpd_foreign_4");
			assertEquals(deleteAction, "NO ACTION");
			assertEquals(updateAction, "CASCADE");

			this.rs.close();
			this.rs = null;
		} finally {
			if (this.rs != null) {
				this.rs.close();
				this.rs = null;
			}
		}

	}

	/**
	 * DOCUMENT ME!
	 * 
	 * @throws SQLException
	 *             DOCUMENT ME!
	 */
	public void testGetPrimaryKeys() throws SQLException {
		try {
			DatabaseMetaData dbmd = this.conn.getMetaData();
			this.rs = dbmd.getPrimaryKeys(this.conn.getCatalog(), "",
					"multikey");

			short[] keySeqs = new short[4];
			String[] columnNames = new String[4];
			int i = 0;

			while (this.rs.next()) {
				this.rs.getString("TABLE_NAME");
				columnNames[i] = this.rs.getString("COLUMN_NAME");

				this.rs.getString("PK_NAME");
				keySeqs[i] = this.rs.getShort("KEY_SEQ");
				i++;
			}

			if ((keySeqs[0] != 3) && (keySeqs[1] != 2) && (keySeqs[2] != 4)
					&& (keySeqs[4] != 1)) {
				fail("Keys returned in wrong order");
			}
		} finally {
			if (this.rs != null) {
				try {
					this.rs.close();
				} catch (SQLException sqlEx) {
					/* ignore */
				}
			}
		}
	}

	private static String cascadeOptionToString(int option) {
		switch (option) {
		case DatabaseMetaData.importedKeyCascade:
			return "CASCADE";

		case DatabaseMetaData.importedKeySetNull:
			return "SET NULL";

		case DatabaseMetaData.importedKeyRestrict:
			return "RESTRICT";

		case DatabaseMetaData.importedKeyNoAction:
			return "NO ACTION";
		}

		return "SET DEFAULT";
	}

	private void createTestTable() throws SQLException {
		this.stmt.executeUpdate("DROP TABLE IF EXISTS child");
		this.stmt.executeUpdate("DROP TABLE IF EXISTS parent");
		this.stmt.executeUpdate("DROP TABLE IF EXISTS multikey");
		this.stmt.executeUpdate("DROP TABLE IF EXISTS cpd_foreign_4");
		this.stmt.executeUpdate("DROP TABLE IF EXISTS cpd_foreign_3");
		this.stmt.executeUpdate("DROP TABLE IF EXISTS cpd_foreign_2");
		this.stmt.executeUpdate("DROP TABLE IF EXISTS cpd_foreign_1");
		this.stmt.executeUpdate("DROP TABLE IF EXISTS fktable2");
		this.stmt.executeUpdate("DROP TABLE IF EXISTS fktable1");

		this.stmt
				.executeUpdate("CREATE TABLE parent(parent_id INT NOT NULL, PRIMARY KEY (parent_id)) TYPE=INNODB");
		this.stmt
				.executeUpdate("CREATE TABLE child(child_id INT, parent_id_fk INT, INDEX par_ind (parent_id_fk), "
						+ "FOREIGN KEY (parent_id_fk) REFERENCES parent(parent_id)) TYPE=INNODB");
		this.stmt
				.executeUpdate("CREATE TABLE multikey(d INT NOT NULL, b INT NOT NULL, a INT NOT NULL, c INT NOT NULL, PRIMARY KEY (d, b, a, c))");

		// Test compound foreign keys
		this.stmt.executeUpdate("create table cpd_foreign_1("
				+ "id int(8) not null auto_increment primary key,"
				+ "name varchar(255) not null unique," + "key (id)"
				+ ") type=InnoDB");
		this.stmt.executeUpdate("create table cpd_foreign_2("
				+ "id int(8) not null auto_increment primary key,"
				+ "key (id)," + "name varchar(255)" + ") type=InnoDB");
		this.stmt
				.executeUpdate("create table cpd_foreign_3("
						+ "cpd_foreign_1_id int(8) not null,"
						+ "cpd_foreign_2_id int(8) not null,"
						+ "key(cpd_foreign_1_id),"
						+ "key(cpd_foreign_2_id),"
						+ "primary key (cpd_foreign_1_id, cpd_foreign_2_id),"
						+ "foreign key (cpd_foreign_1_id) references cpd_foreign_1(id),"
						+ "foreign key (cpd_foreign_2_id) references cpd_foreign_2(id)"
						+ ") type=InnoDB");
		this.stmt
				.executeUpdate("create table cpd_foreign_4("
						+ "cpd_foreign_1_id int(8) not null,"
						+ "cpd_foreign_2_id int(8) not null,"
						+ "key(cpd_foreign_1_id),"
						+ "key(cpd_foreign_2_id),"
						+ "primary key (cpd_foreign_1_id, cpd_foreign_2_id),"
						+ "foreign key (cpd_foreign_1_id, cpd_foreign_2_id) "
						+ "references cpd_foreign_3(cpd_foreign_1_id, cpd_foreign_2_id) "
						+ "ON DELETE RESTRICT ON UPDATE CASCADE"
						+ ") type=InnoDB");

		this.stmt
				.executeUpdate("create table fktable1 (TYPE_ID int not null, TYPE_DESC varchar(32), primary key(TYPE_ID)) TYPE=InnoDB");
		this.stmt
				.executeUpdate("create table fktable2 (KEY_ID int not null, COF_NAME varchar(32), PRICE float, TYPE_ID int, primary key(KEY_ID), "
						+ "index(TYPE_ID), foreign key(TYPE_ID) references fktable1(TYPE_ID)) TYPE=InnoDB");
	}

	/**
	 * Tests the implementation of metadata for views.
	 * 
	 * This test automatically detects whether or not the server it is running
	 * against supports the creation of views.
	 * 
	 * @throws SQLException
	 *             if the test fails.
	 */
	public void testViewMetaData() throws SQLException {
		try {
			this.rs = this.conn.getMetaData().getTableTypes();

			while (this.rs.next()) {
				if ("VIEW".equalsIgnoreCase(this.rs.getString(1))) {

					this.stmt
							.executeUpdate("DROP VIEW IF EXISTS vTestViewMetaData");
					this.stmt
							.executeUpdate("DROP TABLE IF EXISTS testViewMetaData");
					this.stmt
							.executeUpdate("CREATE TABLE testViewMetaData (field1 INT)");
					this.stmt
							.executeUpdate("CREATE VIEW vTestViewMetaData AS SELECT field1 FROM testViewMetaData");

					ResultSet tablesRs = null;

					try {
						tablesRs = this.conn.getMetaData().getTables(
								this.conn.getCatalog(), null, "%ViewMetaData",
								new String[] { "TABLE", "VIEW" });
						assertTrue(tablesRs.next());
						assertTrue("testViewMetaData".equalsIgnoreCase(tablesRs
								.getString(3)));
						assertTrue(tablesRs.next());
						assertTrue("vTestViewMetaData"
								.equalsIgnoreCase(tablesRs.getString(3)));

					} finally {
						if (tablesRs != null) {
							tablesRs.close();
						}
					}

					try {
						tablesRs = this.conn.getMetaData().getTables(
								this.conn.getCatalog(), null, "%ViewMetaData",
								new String[] { "TABLE" });
						assertTrue(tablesRs.next());
						assertTrue("testViewMetaData".equalsIgnoreCase(tablesRs
								.getString(3)));
						assertTrue(!tablesRs.next());
					} finally {
						if (tablesRs != null) {
							tablesRs.close();
						}
					}
					break;
				}
			}

		} finally {
			if (this.rs != null) {
				this.rs.close();
			}
		}
	}

	/**
	 * Tests detection of read-only fields when the server is 4.1.0 or newer.
	 * 
	 * @throws Exception
	 *             if the test fails.
	 */
	public void testRSMDIsReadOnly() throws Exception {
		try {
			this.rs = this.stmt.executeQuery("SELECT 1");

			ResultSetMetaData rsmd = this.rs.getMetaData();

			if (versionMeetsMinimum(4, 1)) {
				assertTrue(rsmd.isReadOnly(1));

				try {
					this.stmt
							.executeUpdate("DROP TABLE IF EXISTS testRSMDIsReadOnly");
					this.stmt
							.executeUpdate("CREATE TABLE testRSMDIsReadOnly (field1 INT)");
					this.stmt
							.executeUpdate("INSERT INTO testRSMDIsReadOnly VALUES (1)");

					this.rs = this.stmt
							.executeQuery("SELECT 1, field1 + 1, field1 FROM testRSMDIsReadOnly");
					rsmd = this.rs.getMetaData();

					assertTrue(rsmd.isReadOnly(1));
					assertTrue(rsmd.isReadOnly(2));
					assertTrue(!rsmd.isReadOnly(3));
				} finally {
					this.stmt
							.executeUpdate("DROP TABLE IF EXISTS testRSMDIsReadOnly");
				}
			} else {
				assertTrue(rsmd.isReadOnly(1) == false);
			}
		} finally {
			if (this.rs != null) {
				this.rs.close();
			}
		}
	}

	public void testBitType() throws Exception {
		if (versionMeetsMinimum(5, 0, 3)) {
			try {
				this.stmt.executeUpdate("DROP TABLE IF EXISTS testBitType");
				this.stmt
						.executeUpdate("CREATE TABLE testBitType (field1 BIT, field2 BIT, field3 BIT)");
				this.stmt
						.executeUpdate("INSERT INTO testBitType VALUES (1, 0, NULL)");
				this.rs = this.stmt
						.executeQuery("SELECT field1, field2, field3 FROM testBitType");
				this.rs.next();

				assertTrue(((Boolean) this.rs.getObject(1)).booleanValue());
				assertTrue(!((Boolean) this.rs.getObject(2)).booleanValue());
				assertEquals(this.rs.getObject(3), null);

				System.out.println(this.rs.getObject(1) + ", "
						+ this.rs.getObject(2) + ", " + this.rs.getObject(3));

				this.rs = this.conn.prepareStatement(
						"SELECT field1, field2, field3 FROM testBitType")
						.executeQuery();
				this.rs.next();

				assertTrue(((Boolean) this.rs.getObject(1)).booleanValue());
				assertTrue(!((Boolean) this.rs.getObject(2)).booleanValue());

				assertEquals(this.rs.getObject(3), null);
				byte[] asBytesTrue = this.rs.getBytes(1);
				byte[] asBytesFalse = this.rs.getBytes(2);
				byte[] asBytesNull = this.rs.getBytes(3);

				assertEquals(asBytesTrue[0], 1);
				assertEquals(asBytesFalse[0], 0);
				assertEquals(asBytesNull, null);

				this.stmt.executeUpdate("DROP TABLE IF EXISTS testBitField");
				this.stmt
						.executeUpdate("CREATE TABLE testBitField(field1 BIT(9))");
				this.rs = this.stmt
						.executeQuery("SELECT field1 FROM testBitField");
				System.out.println(this.rs.getMetaData().getColumnClassName(1));
			} finally {
				this.stmt.executeUpdate("DROP TABLE IF EXISTS testBitType");
			}
		}
	}

	public void testSupportsSelectForUpdate() throws Exception {
		boolean supportsForUpdate = this.conn.getMetaData()
				.supportsSelectForUpdate();

		if (this.versionMeetsMinimum(4, 0)) {
			assertTrue(supportsForUpdate);
		} else {
			assertTrue(!supportsForUpdate);
		}
	}

	public void testTinyint1IsBit() throws Exception {
		String tableName = "testTinyint1IsBit";
		// Can't use 'BIT' or boolean
		createTable(tableName, "(field1 TINYINT(1))");
		this.stmt.executeUpdate("INSERT INTO " + tableName + " VALUES (1)");

		Properties props = new Properties();
		props.setProperty("tinyint1IsBit", "true");
		props.setProperty("transformedBitIsBoolean", "true");
		Connection boolConn = getConnectionWithProps(props);

		this.rs = boolConn.createStatement().executeQuery(
				"SELECT field1 FROM " + tableName);
		checkBitOrBooleanType(false);

		this.rs = boolConn.prepareStatement("SELECT field1 FROM " + tableName)
				.executeQuery();
		checkBitOrBooleanType(false);

		this.rs = boolConn.getMetaData().getColumns(boolConn.getCatalog(),
				null, tableName, "field1");
		assertTrue(this.rs.next());

		if (versionMeetsMinimum(4, 1)) {
			assertEquals(Types.BOOLEAN, this.rs.getInt("DATA_TYPE"));
		} else {
			assertEquals(Types.BIT, this.rs.getInt("DATA_TYPE"));
		}

		if (versionMeetsMinimum(4, 1)) {
			assertEquals("BOOLEAN", this.rs.getString("TYPE_NAME"));
		} else {
			assertEquals("BIT", this.rs.getString("TYPE_NAME"));
		}

		props.clear();
		props.setProperty("transformedBitIsBoolean", "false");
		props.setProperty("tinyint1IsBit", "true");

		Connection bitConn = getConnectionWithProps(props);

		this.rs = bitConn.createStatement().executeQuery(
				"SELECT field1 FROM " + tableName);
		checkBitOrBooleanType(true);

		this.rs = bitConn.prepareStatement("SELECT field1 FROM " + tableName)
				.executeQuery();
		checkBitOrBooleanType(true);

		this.rs = bitConn.getMetaData().getColumns(boolConn.getCatalog(), null,
				tableName, "field1");
		assertTrue(this.rs.next());

		assertEquals(Types.BIT, this.rs.getInt("DATA_TYPE"));

		assertEquals("BIT", this.rs.getString("TYPE_NAME"));
	}

	private void checkBitOrBooleanType(boolean usingBit) throws SQLException {

		assertTrue(this.rs.next());
		assertEquals("java.lang.Boolean", this.rs.getObject(1).getClass()
				.getName());
		if (!usingBit) {
			if (versionMeetsMinimum(4, 1)) {
				assertEquals(Types.BOOLEAN, this.rs.getMetaData()
						.getColumnType(1));
			} else {
				assertEquals(Types.BIT, this.rs.getMetaData().getColumnType(1));
			}
		} else {
			assertEquals(Types.BIT, this.rs.getMetaData().getColumnType(1));
		}

		assertEquals("java.lang.Boolean", this.rs.getMetaData()
				.getColumnClassName(1));
	}
}
