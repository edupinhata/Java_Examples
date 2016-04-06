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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

import testsuite.BaseTestCase;

/**
 * Tests fixes for BLOB handling.
 * 
 * @author Mark Matthews
 * @version $Id: BlobRegressionTest.java,v 1.1.2.19 2005/03/09 18:16:16
 *          mmatthews Exp $
 */
public class BlobRegressionTest extends BaseTestCase {
	/**
	 * Creates a new BlobRegressionTest.
	 * 
	 * @param name
	 *            name of the test to run
	 */
	public BlobRegressionTest(String name) {
		super(name);
	}

	/**
	 * Runs all test cases in this test suite
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		junit.textui.TestRunner.run(BlobRegressionTest.class);
	}

	/**
	 * 
	 * 
	 * @throws Exception
	 *             ...
	 */
	public void testBug2670() throws Exception {
		
		if (isRunningOnJdk131()) {
			return; // test not valid on JDK-1.3.1
		}
		
		try {
			byte[] blobData = new byte[32];

			for (int i = 0; i < blobData.length; i++) {
				blobData[i] = 1;
			}

			this.stmt.executeUpdate("DROP TABLE IF EXISTS testBug2670");
			this.stmt
					.executeUpdate("CREATE TABLE testBug2670(blobField LONGBLOB)");

			PreparedStatement pStmt = this.conn
					.prepareStatement("INSERT INTO testBug2670 (blobField) VALUES (?)");
			pStmt.setBytes(1, blobData);
			pStmt.executeUpdate();

			this.rs = this.stmt
					.executeQuery("SELECT blobField FROM testBug2670");
			this.rs.next();

			Blob blob = this.rs.getBlob(1);

			//
			// Test mid-point insertion
			//
			blob.setBytes(4, new byte[] { 2, 2, 2, 2 });

			byte[] newBlobData = blob.getBytes(1L, (int) blob.length());

			assertTrue("Blob changed length", blob.length() == blobData.length);

			assertTrue("New data inserted wrongly",
					((newBlobData[3] == 2) && (newBlobData[4] == 2)
							&& (newBlobData[5] == 2) && (newBlobData[6] == 2)));

			//
			// Test end-point insertion
			//
			blob.setBytes(32, new byte[] { 2, 2, 2, 2 });

			assertTrue("Blob length should be 3 larger",
					blob.length() == (blobData.length + 3));
		} finally {
			this.stmt.executeUpdate("DROP TABLE IF EXISTS testUpdateLongBlob");
		}
	}

	/**
	 * 
	 * 
	 * @throws Exception
	 *             ...
	 */
	public void testUpdateLongBlobGT16M() throws Exception {
		if (versionMeetsMinimum(4, 0)) {
			try {
				byte[] blobData = new byte[18 * 1024 * 1024]; // 18M blob

				this.stmt
						.executeUpdate("DROP TABLE IF EXISTS testUpdateLongBlob");
				this.stmt
						.executeUpdate("CREATE TABLE testUpdateLongBlob(blobField LONGBLOB)");
				this.stmt
						.executeUpdate("INSERT INTO testUpdateLongBlob (blobField) VALUES (NULL)");

				PreparedStatement pStmt = this.conn
						.prepareStatement("UPDATE testUpdateLongBlob SET blobField=?");
				pStmt.setBytes(1, blobData);
				pStmt.executeUpdate();
			} finally {
				this.stmt
						.executeUpdate("DROP TABLE IF EXISTS testUpdateLongBlob");
			}
		}
	}

	/**
	 * 
	 * @throws Exception
	 */
	public void testUpdatableBlobsWithCharsets() throws Exception {
		byte[] smallBlob = new byte[32];

		for (byte i = 0; i < smallBlob.length; i++) {
			smallBlob[i] = i;
		}

		try {
			this.stmt
					.executeUpdate("DROP TABLE IF EXISTS testUpdatableBlobsWithCharsets");
			this.stmt
					.executeUpdate("CREATE TABLE testUpdatableBlobsWithCharsets(pk INT NOT NULL PRIMARY KEY, field1 BLOB)");

			PreparedStatement pStmt = this.conn
					.prepareStatement("INSERT INTO testUpdatableBlobsWithCharsets (pk, field1) VALUES (1, ?)");
			pStmt.setBinaryStream(1, new ByteArrayInputStream(smallBlob),
					smallBlob.length);
			pStmt.executeUpdate();

			Statement updStmt = this.conn
					.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,
							ResultSet.CONCUR_UPDATABLE);

			this.rs = updStmt
					.executeQuery("SELECT pk, field1 FROM testUpdatableBlobsWithCharsets");
			System.out.println(this.rs);
			this.rs.next();

			for (byte i = 0; i < smallBlob.length; i++) {
				smallBlob[i] = (byte) (i + 32);
			}

			this.rs.updateBinaryStream(2, new ByteArrayInputStream(smallBlob),
					smallBlob.length);
			this.rs.updateRow();

			ResultSet newRs = this.stmt
					.executeQuery("SELECT field1 FROM testUpdatableBlobsWithCharsets");

			newRs.next();

			byte[] updatedBlob = newRs.getBytes(1);

			for (byte i = 0; i < smallBlob.length; i++) {
				byte origValue = smallBlob[i];
				byte newValue = updatedBlob[i];

				assertTrue("Original byte at position " + i + ", " + origValue
						+ " != new value, " + newValue, origValue == newValue);
			}

		} finally {
			this.stmt
					.executeUpdate("DROP TABLE IF EXISTS testUpdatableBlobsWithCharsets");
		}
	}

	public void testBug5490() throws Exception {
		try {
			this.stmt.executeUpdate("DROP TABLE IF EXISTS testBug5490");
			this.stmt.executeUpdate("CREATE TABLE testBug5490"
					+ "(pk INT NOT NULL PRIMARY KEY, blobField BLOB)");
			String sql = "insert into testBug5490 values(?,?)";

			int blobFileSize = 871;
			File blobFile = newTempBinaryFile("Bug5490", blobFileSize);

			PreparedStatement pStmt = this.conn.prepareStatement(sql,
					ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_READ_ONLY);
			pStmt.setInt(1, 2);
			FileInputStream fis = new FileInputStream(blobFile);
			pStmt.setBinaryStream(2, fis, blobFileSize);
			pStmt.execute();
			fis.close();
			pStmt.close();

			this.rs = this.stmt
					.executeQuery("SELECT blobField FROM testBug5490");

			this.rs.next();

			byte[] returned = this.rs.getBytes(1);

			assertEquals(blobFileSize, returned.length);
		} finally {
			this.stmt.executeUpdate("DROP TABLE IF EXISTS testBug5490");
		}
	}

	/**
	 * Tests BUG#8096 where emulated locators corrupt binary data when using
	 * server-side prepared statements.
	 * 
	 * @throws Exception
	 *             if the test fails.
	 */
	public void testBug8096() throws Exception {
		
		if (isRunningOnJdk131()) {
			return; // test not valid on JDK-1.3.1
		}
		
		int dataSize = 256;

		Properties props = new Properties();
		props.setProperty("emulateLocators", "true");
		Connection locatorConn = getConnectionWithProps(props);

		String createTable = "CREATE TABLE testBug8096 (ID VARCHAR(10) "
				+ "PRIMARY KEY, DATA LONGBLOB)";
		String select = "SELECT ID, 'DATA' AS BLOB_DATA FROM testBug8096 "
				+ "WHERE ID = ?";
		String insert = "INSERT INTO testBug8096 (ID, DATA) VALUES (?, '')";

		String id = "1";
		byte[] testData = new byte[dataSize];

		for (int i = 0; i < testData.length; i++) {
			testData[i] = (byte) i;
		}

		try {
			this.stmt.executeUpdate("DROP TABLE IF EXISTS testBug8096");

			this.stmt.executeUpdate(createTable);

			PreparedStatement ps = locatorConn.prepareStatement(insert);
			ps.setString(1, id);
			ps.execute();

			ps = locatorConn.prepareStatement(select);
			ps.setString(1, id);

			this.rs = ps.executeQuery();

			if (this.rs.next()) {
				Blob b = this.rs.getBlob("BLOB_DATA");
				b.setBytes(1, testData);
			}

			this.rs.close();
			ps.close();

			ps = locatorConn.prepareStatement(select);
			ps.setString(1, id);

			this.rs = ps.executeQuery();

			byte[] result = null;
			if (this.rs.next()) {
				Blob b = this.rs.getBlob("BLOB_DATA");

				result = b.getBytes(1, dataSize - 1);
			}

			this.rs.close();
			ps.close();

			assertNotNull(result);

			for (int i = 0; i < result.length && i < testData.length; i++) {
				// Will print out all of the values that don't match.
				// All negative values will instead be replaced with 63.
				if (result[i] != testData[i]) {
					assertEquals("At position " + i, testData[i], result[i]);
				}
			}

		} finally {
			this.stmt.executeUpdate("DROP TABLE IF EXISTS testBug8096");
		}
	}

	/**
	 * Tests fix for BUG#9040 - PreparedStatement.addBatch() doesn't work with
	 * server-side prepared statements and streaming BINARY data.
	 * 
	 * @throws Exception
	 *             if the test fails.
	 */
	public void testBug9040() throws Exception {
		try {
			this.stmt.executeUpdate("DROP TABLE IF EXISTS testBug9040");

			this.stmt.executeUpdate("create table if not exists testBug9040 "
					+ "(primary_key int not null primary key, "
					+ "data mediumblob)");

			this.pstmt = this.conn
					.prepareStatement("replace into testBug9040 (primary_key, data) values(?,?)");

			int primaryKey = 1;
			byte[] data = "First Row".getBytes();
			this.pstmt.setInt(1, primaryKey);
			this.pstmt.setBinaryStream(2, new ByteArrayInputStream(data),
					data.length);
			this.pstmt.addBatch();

			primaryKey = 2;
			data = "Second Row".getBytes();
			this.pstmt.setInt(1, primaryKey);
			this.pstmt.setBinaryStream(2, new ByteArrayInputStream(data),
					data.length);
			this.pstmt.addBatch();

			this.pstmt.executeBatch();
		} finally {
			this.stmt.executeUpdate("DROP TABLE IF EXISTS testBug9040");

			if (this.pstmt != null) {
				this.pstmt.close();
			}
		}
	}

	public void testBug10850() throws Exception {
		String tableName = "testBug10850";
		
		createTable(tableName, "(field1 TEXT)");
		
		PreparedStatement pStmt = null;
		
		try {
			pStmt = this.conn.prepareStatement("INSERT INTO " + 
		
				tableName + " VALUES (?)");
			pStmt.setCharacterStream(1, new StringReader(""), 0);
			pStmt.executeUpdate();	
		
			assertEquals("0", getSingleIndexedValueWithQuery(1, "SELECT LENGTH(field1) FROM " + tableName).toString());
			this.stmt.executeUpdate("TRUNCATE TABLE " + tableName);
		
			pStmt.clearParameters();
			pStmt.setBinaryStream(1, new ByteArrayInputStream(new byte[0]), 0);
			pStmt.executeUpdate();
			
			assertEquals("0", getSingleIndexedValueWithQuery(1, "SELECT LENGTH(field1) FROM " + tableName).toString());
			this.stmt.executeUpdate("TRUNCATE TABLE " + tableName);
		} finally {
			if (pStmt != null) {
				pStmt.close();
			}
		}
	}
	
	private static File testBlobFile;

    
    /*
     *Test for Bugs: #21515: not returning BLOB over 16M
     *
     * @throws Exception
     * 
     * Remarks, fails only if packetdebug is turned ON, 
     *   AFAIS server settings and remarked commands do not make difference 
    */

    public void testbug21515() throws Exception {

    	if (versionMeetsMinimum(4, 0)) {
   			int requiredSize = 17 * 1024 * 1024;

		if (testBlobFile == null || testBlobFile.length() != requiredSize) {
 				createBlobFile(requiredSize);
  			}

    		} else {
    			int requiredSize = 17 * 1024 * 1024;

    			if (testBlobFile == null || testBlobFile.length() != requiredSize) {
    				createBlobFile(requiredSize);
    			}
    		}

    	
    	createTable("bug21515", "(`id` bigint not null, `raw_data` longblob"+
    			" not null, primary key (`id`)) type=InnoDB;");   
    	try {
    		BufferedInputStream bIn = new BufferedInputStream(new FileInputStream(
    				testBlobFile));
    		
    		//this.stmt.execute("SET NAMES utf8");
    		
    		this.pstmt = this.conn
    				.prepareStatement("INSERT INTO bug21515 VALUES (1,?)");
    		this.pstmt.setBinaryStream(1, bIn, (int) testBlobFile.length());
    		this.pstmt.execute();

    		this.pstmt.clearParameters();
    		
    		//this.stmt.execute("FLUSH HOSTS");
    		
    		doRetrieval();
    		
    	} finally {
			closeMemberJDBCResources();
		}        
    }

	private void createBlobFile(int size) throws Exception {
		if (testBlobFile != null && testBlobFile.length() != size) {
			testBlobFile.delete();
		}

		testBlobFile = File.createTempFile("testblob", ".dat");
		testBlobFile.deleteOnExit();

		BufferedOutputStream bOut = new BufferedOutputStream(
				new FileOutputStream(testBlobFile));

		int dataRange = Byte.MAX_VALUE - Byte.MIN_VALUE;

		for (int i = 0; i < size; i++) {
			bOut.write((byte) ((Math.random() * dataRange) + Byte.MIN_VALUE));
		}

		bOut.flush();
		bOut.close();
	}    
    
	private void doRetrieval() throws Exception {
		boolean passed = false;
		this.rs = this.stmt
				.executeQuery("SELECT raw_data from bug21515 LIMIT 1");
		this.rs.next();

		byte[] retrBytes = this.rs.getBytes(1);
		passed = checkBlob(retrBytes);
		assertTrue(
				"Inserted BLOB data did not match retrieved BLOB data for getBytes().",
				passed);
		retrBytes = this.rs.getBlob(1).getBytes(1L,
				(int) this.rs.getBlob(1).length());
		passed = checkBlob(retrBytes);
		assertTrue(
				"Inserted BLOB data did not match retrieved BLOB data for getBlob().",
				passed);

		InputStream inStr = this.rs.getBinaryStream(1);
		ByteArrayOutputStream bOut = new ByteArrayOutputStream();
		int b;

		while ((b = inStr.read()) != -1) {
			bOut.write((byte) b);
		}

		retrBytes = bOut.toByteArray();
		passed = checkBlob(retrBytes);
		assertTrue(
				"Inserted BLOB data did not match retrieved BLOB data for getBinaryStream().",
				passed);
		inStr = this.rs.getAsciiStream(1);
		bOut = new ByteArrayOutputStream();

		while ((b = inStr.read()) != -1) {
			bOut.write((byte) b);
		}

		retrBytes = bOut.toByteArray();
		passed = checkBlob(retrBytes);
		assertTrue(
				"Inserted BLOB data did not match retrieved BLOB data for getAsciiStream().",
				passed);
	}	
	
	private boolean checkBlob(byte[] retrBytes) throws Exception {
		boolean passed = false;
		BufferedInputStream bIn = new BufferedInputStream(new FileInputStream(
				testBlobFile));

		try {
			int fileLength = (int) testBlobFile.length();
			if (retrBytes.length == fileLength) {
				for (int i = 0; i < fileLength; i++) {
					byte fromFile = (byte) (bIn.read() & 0xff);

					if (retrBytes[i] != fromFile) {
						passed = false;
						System.out.println("Byte pattern differed at position "
								+ i + " , " + retrBytes[i] + " != " + fromFile);

						for (int j = 0; (j < (i + 10)) /* && (j < i) */; j++) {
							System.out.print(Integer
									.toHexString(retrBytes[j] & 0xff)
									+ " ");
						}

						break;
					}

					passed = true;
				}
			} else {
				passed = false;
				System.out.println("retrBytes.length(" + retrBytes.length
						+ ") != testBlob.length(" + fileLength + ")");
			}

			return passed;
		} finally {
			if (bIn != null) {
				bIn.close();
			}
		}
	}

}