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
package com.mysql.jdbc;

import java.io.UnsupportedEncodingException;

import java.nio.ByteBuffer;

import java.sql.SQLException;

/**
 * Buffer contains code to read and write packets from/to the MySQL server.
 * 
 * @version $Id: Buffer.java 3726 2005-05-19 15:52:24Z mmatthews $
 * @author Mark Matthews
 */
abstract class Buffer {
	static final int MAX_BYTES_TO_DUMP = 512;

	static final int NO_LENGTH_LIMIT = -1;

	static final long NULL_LENGTH = -1;

	public static Buffer allocateDirect(int size, boolean useNewIo) {
		if (!useNewIo) {
			return allocateNew(size, useNewIo);
		}

		return new ChannelBuffer(size, true);
	}

	public static Buffer allocateNew(byte[] buf, boolean useNewIo) {
		if (!useNewIo) {
			return new ByteArrayBuffer(buf);
		}

		return new ChannelBuffer(buf);
	}

	public static Buffer allocateNew(int size, boolean useNewIo) {
		if (!useNewIo) {
			return new ByteArrayBuffer(size);
		}

		return new ChannelBuffer(size, true);
	}

	protected boolean wasMultiPacket = false;

	abstract void clear();

	final void dump() {
		dump(getBufLength());
	}

	final String dump(int numBytes) {
		return StringUtils.dumpAsHex(getBytes(0,
				numBytes > getBufLength() ? getBufLength() : numBytes),
				numBytes > getBufLength() ? getBufLength() : numBytes);
	}

	final String dumpClampedBytes(int numBytes) {
		int numBytesToDump = numBytes < MAX_BYTES_TO_DUMP ? numBytes
				: MAX_BYTES_TO_DUMP;

		String dumped = StringUtils.dumpAsHex(getBytes(0,
				numBytesToDump > getBufLength() ? getBufLength()
						: numBytesToDump),
				numBytesToDump > getBufLength() ? getBufLength()
						: numBytesToDump);

		if (numBytesToDump < numBytes) {
			return dumped + " ....(packet exceeds max. dump length)";
		}

		return dumped;
	}

	final void dumpHeader() {
		for (int i = 0; i < MysqlIO.HEADER_LENGTH; i++) {
			String hexVal = Integer.toHexString(readByte(i) & 0xff);

			if (hexVal.length() == 1) {
				hexVal = "0" + hexVal; //$NON-NLS-1$
			}

			System.out.print(hexVal + " "); //$NON-NLS-1$
		}
	}

	final void dumpNBytes(int start, int nBytes) {
		StringBuffer asciiBuf = new StringBuffer();

		for (int i = start; (i < (start + nBytes)) && (i < getBufLength()); i++) {
			String hexVal = Integer.toHexString(readByte(i) & 0xff);

			if (hexVal.length() == 1) {
				hexVal = "0" + hexVal; //$NON-NLS-1$
			}

			System.out.print(hexVal + " "); //$NON-NLS-1$

			if ((readByte(i) > 32) && (readByte(i) < 127)) {
				asciiBuf.append((char) readByte(i));
			} else {
				asciiBuf.append("."); //$NON-NLS-1$
			}

			asciiBuf.append(" "); //$NON-NLS-1$
		}

		System.out.println("    " + asciiBuf.toString()); //$NON-NLS-1$
	}

	abstract void ensureCapacity(int additionalData) throws SQLException;

	/**
	 * Skip over a length-encoded string
	 * 
	 * @return The position past the end of the string
	 */
	public abstract int fastSkipLenString();

	abstract int getBufLength();

	/**
	 * Returns the array of bytes this Buffer is using to read from.
	 * 
	 * @return byte array being read from
	 */
	public abstract byte[] getByteBuffer();

	abstract byte[] getBytes(int len);

	abstract byte[] getBytes(int offset, int len);

	abstract int getCapacity();

	/**
	 * Returns the Java NIO Buffer (if any)
	 */
	public abstract ByteBuffer getNioBuffer();

	/**
	 * Returns the current position to write to/ read from
	 * 
	 * @return the current position to write to/ read from
	 */
	public abstract int getPosition();

	// 2000-06-05 Changed
	abstract boolean isLastDataPacket();

	abstract long newReadLength();

	abstract byte readByte();

	abstract byte readByte(int readAt);

	abstract long readFieldLength();

	abstract int readInt();

	abstract int readIntAsLong();

	abstract byte[] readLenByteArray(int offset);

	abstract long readLength();

	// 2000-06-05 Fixed
	abstract long readLong();

	// 2000-06-05 Changed
	abstract int readLongInt();

	// 2000-06-05 Fixed
	abstract long readLongLong();

	abstract int readnBytes();

	//
	// Read a null-terminated string
	//
	// To avoid alloc'ing a new byte array, we
	// do this by hand, rather than calling getNullTerminatedBytes()
	//
	abstract String readString();

	abstract String readString(String encoding) throws SQLException;

	abstract void setBufLength(int bufLength);

	/**
	 * Sets the array of bytes to use as a buffer to read from.
	 * 
	 * @param byteBuffer
	 *            the array of bytes to use as a buffer
	 */
	public abstract void setByteBuffer(byte[] byteBuffer);

	/**
	 * Set the current position to write to/ read from
	 * 
	 * @param position
	 *            the position (0-based index)
	 */
	public abstract void setPosition(int position);

	/**
	 * Sets whether this packet was part of a multipacket
	 * 
	 * @param flag
	 *            was this packet part of a multipacket?
	 */
	public void setWasMultiPacket(boolean flag) {
		this.wasMultiPacket = flag;
	}

	public String toString() {
		return dumpClampedBytes(getPosition());
	}

	public String toSuperString() {
		return super.toString();
	}

	/**
	 * Was this packet part of a multipacket?
	 * 
	 * @return was this packet part of a multipacket?
	 */
	public boolean wasMultiPacket() {
		return this.wasMultiPacket;
	}

	abstract void writeByte(byte b) throws SQLException;

	// Write a byte array
	abstract void writeBytesNoNull(byte[] bytes) throws SQLException;

	// Write a byte array with the given offset and length
	abstract void writeBytesNoNull(byte[] bytes, int offset, int length)
			throws SQLException;

	abstract void writeDouble(double d) throws SQLException;

	abstract void writeFieldLength(long length) throws SQLException;

	abstract void writeFloat(float f) throws SQLException;

	// 2000-06-05 Changed
	abstract void writeInt(int i) throws SQLException;

	// Write a String using the specified character
	// encoding
	abstract void writeLenBytes(byte[] b) throws SQLException;

	// Write a String using the specified character
	// encoding
	abstract void writeLenString(String s, String encoding,
			String serverEncoding, SingleByteCharsetConverter converter,
			boolean parserKnowsUnicode) throws UnsupportedEncodingException,
			SQLException;

	// 2000-06-05 Changed
	abstract void writeLong(long i) throws SQLException;

	// 2000-06-05 Changed
	abstract void writeLongInt(int i) throws SQLException;

	abstract void writeLongLong(long i) throws SQLException;

	// Write null-terminated string
	abstract void writeString(String s) throws SQLException;

	// Write string, with no termination
	abstract void writeStringNoNull(String s) throws SQLException;

	// Write a String using the specified character
	// encoding
	abstract void writeStringNoNull(String s, String encoding,
			String serverEncoding, boolean parserKnowsUnicode)
			throws UnsupportedEncodingException, SQLException;
}
