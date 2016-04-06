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
 * @version $Id: ChannelBuffer.java 4054 2005-08-11 20:16:51Z mmatthews $
 * @author Mark Matthews
 */
class ChannelBuffer extends Buffer {

	private byte[] asBytes = null;

	// private int position = 0;

	private int bufLength = 0;

	private ByteBuffer directBuffer;

	private boolean dirty = true;

	ChannelBuffer(byte[] buf) {
		this.directBuffer = ByteBuffer.wrap(buf);
		setBufLength(buf.length);
	}

	ChannelBuffer(int size, boolean direct) {

		if (direct) {
			this.directBuffer = ByteBuffer.allocateDirect(size);
		} else {
			this.directBuffer = ByteBuffer.allocate(size);
		}

		// this.directBuffer.limit(size);
		setBufLength(size);
		// this.position = MysqlIO.HEADER_LENGTH;
		this.directBuffer.position(MysqlIO.HEADER_LENGTH);
	}

	private byte[] bufferToArray() {
		if (!this.dirty) {
			return this.asBytes;
		} else if (this.directBuffer.hasArray()) {
			this.asBytes = this.directBuffer.array();
			this.dirty = false;

			return this.asBytes;
		} else {
			int bufferLength = this.directBuffer.limit();

			this.asBytes = new byte[bufferLength];

			int oldPosition = getPosition();

			this.directBuffer.position(0);
			this.directBuffer.get(this.asBytes, 0, bufferLength);
			this.directBuffer.position(oldPosition);
			this.dirty = false;

			return this.asBytes;
		}
	}

	final void clear() {
		this.directBuffer.position(MysqlIO.HEADER_LENGTH);
	}

	final void ensureCapacity(int additionalData) throws SQLException {
		int bufferCapacity = this.directBuffer.capacity();

		int currentPosition = this.directBuffer.position();

		if ((currentPosition + additionalData) > getBufLength()) {
			if ((currentPosition + additionalData) < bufferCapacity) {
				// byteBuffer.length is != getBufLength() all of the time
				// due to re-using of packets (we don't shrink them)
				//
				// If we can, don't re-alloc, just set buffer length
				// to size of current buffer
				setBufLength(currentPosition + additionalData);
			} else {
				//
				// Otherwise, re-size, and pad so we can avoid
				// allocing again in the near future
				//

				int newLength = (int) (bufferCapacity * 1.25);

				if (newLength < 4096) {
					newLength = 4096;
				}

				if (newLength < (bufferCapacity + additionalData)) {
					newLength = bufferCapacity + (int) (additionalData * 1.25);
				}

				if (newLength < bufferCapacity) {
					newLength = bufferCapacity + additionalData;
				}

				ByteBuffer largerBuffer = ByteBuffer.allocateDirect(newLength);

				this.directBuffer.position(0);
				largerBuffer.put(this.directBuffer);
				this.directBuffer = largerBuffer;
				this.directBuffer.position(currentPosition);

				bufferCapacity = this.directBuffer.capacity(); // re-alloc'd
				setBufLength(bufferCapacity);

			}
		}
	}

	/**
	 * Skip over a length-encoded string
	 * 
	 * @return The position past the end of the string
	 */
	public int fastSkipLenString() {
		long len = this.readFieldLength();

		// position += len;

		this.directBuffer.position((int) (this.directBuffer.position() + len));

		return (int) len; // this is safe, as this is only
	}

	int getBufLength() {
		return this.directBuffer.limit();
	}

	/**
	 * Returns the array of bytes this Buffer is using to read from.
	 * 
	 * @return byte array being read from
	 */
	public byte[] getByteBuffer() {
		return bufferToArray();
	}

	final byte[] getBytes(int len) {
		byte[] b = new byte[len];
		byte[] nioByteBuffer = bufferToArray();

		try {

			System.arraycopy(nioByteBuffer, this.directBuffer.position(), b, 0,
					len);
			// this.position += len; // update cursor
			this.directBuffer.position((this.directBuffer.position() + len));
		} catch (ArrayIndexOutOfBoundsException aiobex) {
			throw aiobex;
		}

		return b;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.mysql.jdbc.Buffer#getBytes(int, int)
	 */
	byte[] getBytes(int offset, int len) {
		byte[] b = new byte[len];
		byte[] nioByteBuffer = bufferToArray();

		try {

			System.arraycopy(nioByteBuffer, offset, b, 0, len);
			// this.position += len; // update cursor
			this.directBuffer.position((offset + len));
		} catch (ArrayIndexOutOfBoundsException aiobex) {
			throw aiobex;
		}

		return b;
	}

	int getCapacity() {
		return this.directBuffer.capacity();
	}

	public ByteBuffer getNioBuffer() {
		return this.directBuffer;
	}

	/**
	 * Returns the current position to write to/ read from
	 * 
	 * @return the current position to write to/ read from
	 */
	public int getPosition() {
		// if (directBuffer.position() != this.position) {
		// System.err.println("WARN: directBuffer position != this.position");
		// }

		return this.directBuffer.position();
	}

	// 2000-06-05 Changed
	final boolean isLastDataPacket() {
		boolean hasMarker = ((this.directBuffer.get(0) & 0xff) == 254);

		return (hasMarker && this.bufLength < 9);
	}

	final long newReadLength() {
		int sw = this.directBuffer.get(this.directBuffer.position()) & 0xff;
		this.directBuffer.position(this.directBuffer.position() + 1);

		switch (sw) {
		case 251:
			return 0;

		case 252:
			return readInt();

		case 253:
			return readLongInt();

		case 254: // changed for 64 bit lengths
			return readLongLong();

		default:
			return sw;
		}
	}

	final byte readByte() {
		byte b = this.directBuffer.get();

		return b;
	}

	final byte readByte(int readAt) {
		return this.directBuffer.get(readAt);
	}

	final long readFieldLength() {
		int sw = this.directBuffer.get() & 0xff;

		switch (sw) {
		case 251:
			return NULL_LENGTH;

		case 252:
			return readInt();

		case 253:
			return readLongInt();

		case 254:
			return readLongLong();

		default:
			return sw;
		}
	}

	final int readInt() {
		return (this.directBuffer.get() & 0xff)
				| ((this.directBuffer.get() & 0xff) << 8);
	}

	final int readIntAsLong() {
		int i = (this.directBuffer.get() & 0xff)
				| ((this.directBuffer.get() & 0xff) << 8)
				| ((this.directBuffer.get() & 0xff) << 16)
				| ((this.directBuffer.get() & 0xff) << 24);

		// this.directBuffer.position(this.position);

		return i;
	}

	final byte[] readLenByteArray(int offset) {
		long len = this.readFieldLength();

		if (len == NULL_LENGTH) {
			return null;
		}

		if (len == 0) {
			return Constants.EMPTY_BYTE_ARRAY;
		}

		this.directBuffer.position(this.directBuffer.position() + offset);
		// this.directBuffer.position(this.position);

		return getBytes((int) len);
	}

	final long readLength() {
		int sw = this.directBuffer.get() & 0xff;
		// this.directBuffer.position(this.position);

		switch (sw) {
		case 251:
			return 0;

		case 252:
			return readInt();

		case 253:
			return readLongInt();

		case 254:
			return readLong();

		default:
			return sw;
		}
	}

	final long readLong() {
		long l = ((long)this.directBuffer.get() & 0xff)
				| (((long)this.directBuffer.get() & 0xff) << 8)
				| (((long)this.directBuffer.get() & 0xff) << 16)
				| (((long)this.directBuffer.get() & 0xff) << 24);

		// this.directBuffer.position(this.position);

		return l;
	}

	final int readLongInt() {

		int i = (this.directBuffer.get() & 0xff)
				| ((this.directBuffer.get() & 0xff) << 8)
				| ((this.directBuffer.get() & 0xff) << 16);

		// this.directBuffer.position(this.position);

		return i;
	}

	// 2000-06-05 Fixed
	final long readLongLong() {

		long l = (this.directBuffer.get() & 0xff)
				| ((long) (this.directBuffer.get() & 0xff) << 8)
				| ((long) (this.directBuffer.get() & 0xff) << 16)
				| ((long) (this.directBuffer.get() & 0xff) << 24)
				| ((long) (this.directBuffer.get() & 0xff) << 32)
				| ((long) (this.directBuffer.get() & 0xff) << 40)
				| ((long) (this.directBuffer.get() & 0xff) << 48)
				| ((long) (this.directBuffer.get() & 0xff) << 56);

		// this.directBuffer.position(this.position);

		return l;
	}

	final int readnBytes() {
		int sw = this.directBuffer.get() & 0xff;
		// this.directBuffer.position(this.position);

		switch (sw) {
		case 1:
			return this.directBuffer.get() & 0xff;

		case 2:
			return this.readInt();

		case 3:
			return this.readLongInt();

		case 4:
			return (int) this.readLong();

		default:
			return 255;
		}
	}

	//
	// Read a null-terminated string
	//
	// To avoid alloc'ing a new byte array, we
	// do this by hand, rather than calling getNullTerminatedBytes()
	//
	final String readString() {

		int len = 0;
		int maxLen = getBufLength();
		int oldPosition = getPosition();

		while ((getPosition() < maxLen) && (this.directBuffer.get() != 0)) {
			len++;
		}

		setPosition(oldPosition);

		String s = new String(bufferToArray(), getPosition(), len);

		this.directBuffer.position(getPosition() + len + 1); // update cursor

		return s;
	}

	final String readString(String encoding) throws SQLException {

		int len = 0;

		int maxLen = getBufLength();

		while ((getPosition() < maxLen) && (this.directBuffer.get() != 0)) {
			len++;
		}

		try {
			return new String(bufferToArray(), getPosition(), len, encoding);
		} catch (UnsupportedEncodingException uEE) {
			throw new SQLException(
					Messages.getString("ChannelBuffer.0") //$NON-NLS-1$
							+ encoding + Messages.getString("ChannelBuffer.1"), SQLError.SQL_STATE_ILLEGAL_ARGUMENT); //$NON-NLS-1$
		} finally {
			this.directBuffer.position(getPosition() + len + 1); // update
																	// cursor
		}
	}

	void setBufLength(int bufLengthToSet) {
		this.bufLength = bufLengthToSet;
		this.directBuffer.limit(this.bufLength);
		this.dirty = true;
	}

	/**
	 * Sets the array of bytes to use as a buffer to read from.
	 * 
	 * @param byteBuffer
	 *            the array of bytes to use as a buffer
	 */
	public void setByteBuffer(byte[] byteBuffer) {
		this.directBuffer = ByteBuffer.wrap(byteBuffer);
	}

	/**
	 * Set the current position to write to/ read from
	 * 
	 * @param position
	 *            the position (0-based index)
	 */
	public void setPosition(int position) {
		// this.position = position;
		this.directBuffer.position(position);
	}

	final void writeByte(byte b) throws SQLException {
		ensureCapacity(1);

		this.directBuffer.put(b);
		this.dirty = true;
	}

	// Write a byte array
	final void writeBytesNoNull(byte[] bytes) throws SQLException {
		int len = bytes.length;
		ensureCapacity(len);

		this.directBuffer.put(bytes, 0, len);
		this.dirty = true;
	}

	// Write a byte array with the given offset and length
	final void writeBytesNoNull(byte[] bytes, int offset, int length)
			throws SQLException {
		ensureCapacity(length);

		this.directBuffer.put(bytes, offset, length);

		this.dirty = true;
	}

	final void writeDouble(double d) throws SQLException {
		long l = Double.doubleToLongBits(d);
		writeLongLong(l);
		this.dirty = true;
	}

	final void writeFieldLength(long length) throws SQLException {
		if (length < 251) {
			writeByte((byte) length);
		} else if (length < 65536L) {
			ensureCapacity(3);
			writeByte((byte) 252);
			writeInt((int) length);
		} else if (length < 16777216L) {
			ensureCapacity(4);
			writeByte((byte) 253);
			writeLongInt((int) length);
		} else {
			ensureCapacity(9);
			writeByte((byte) 254);
			writeLongLong(length);
		}

	}

	final void writeFloat(float f) throws SQLException {
		ensureCapacity(4);

		int i = Float.floatToIntBits(f);

		this.directBuffer.put((byte) (i & 0xff));
		this.directBuffer.put((byte) (i >>> 8));
		this.directBuffer.put((byte) (i >>> 16));
		this.directBuffer.put((byte) (i >>> 24));

		this.dirty = true;
	}

	// 2000-06-05 Changed
	final void writeInt(int i) throws SQLException {
		ensureCapacity(2);
		this.directBuffer.put((byte) (i & 0xff));
		this.directBuffer.put((byte) (i >>> 8));

		this.dirty = true;
	}

	// Write a String using the specified character
	// encoding
	final void writeLenBytes(byte[] b) throws SQLException {
		int len = b.length;
		ensureCapacity(len + 9);
		writeFieldLength(len);
		this.directBuffer.put(b, 0, len);

		this.dirty = true;
	}

	// Write a String using the specified character
	// encoding
	final void writeLenString(String s, String encoding, String serverEncoding,
			SingleByteCharsetConverter converter, boolean parserKnowsUnicode)
			throws UnsupportedEncodingException, SQLException {
		byte[] b = null;

		if (converter != null) {
			b = converter.toBytes(s);
		} else {
			b = StringUtils.getBytes(s, encoding, serverEncoding,
					parserKnowsUnicode);
		}

		int len = b.length;
		ensureCapacity(len + 9);
		writeFieldLength(len);
		this.directBuffer.put(b, 0, len);

		this.dirty = true;
	}

	// 2000-06-05 Changed
	final void writeLong(long i) throws SQLException {
		ensureCapacity(4);

		this.directBuffer.put((byte) (i & 0xff));
		this.directBuffer.put((byte) (i >>> 8));
		this.directBuffer.put((byte) (i >>> 16));
		this.directBuffer.put((byte) (i >>> 24));

		this.dirty = true;
	}

	// 2000-06-05 Changed
	final void writeLongInt(int i) throws SQLException {
		ensureCapacity(3);

		this.directBuffer.put((byte) (i & 0xff));
		this.directBuffer.put((byte) (i >>> 8));
		this.directBuffer.put((byte) (i >>> 16));

		this.dirty = true;
	}

	final void writeLongLong(long i) throws SQLException {
		ensureCapacity(8);

		this.directBuffer.put((byte) (i & 0xff));
		this.directBuffer.put((byte) (i >>> 8));
		this.directBuffer.put((byte) (i >>> 16));
		this.directBuffer.put((byte) (i >>> 24));
		this.directBuffer.put((byte) (i >>> 32));
		this.directBuffer.put((byte) (i >>> 40));
		this.directBuffer.put((byte) (i >>> 48));
		this.directBuffer.put((byte) (i >>> 56));

		this.dirty = true;
	}

	// Write null-terminated string
	final void writeString(String s) throws SQLException {
		ensureCapacity((s.length() * 2) + 1);

		writeStringNoNull(s);
		this.directBuffer.put((byte) 0);

		this.dirty = true;
	}

	// Write string, with no termination
	final void writeStringNoNull(String s) throws SQLException {
		int len = s.length();
		ensureCapacity(len * 2);

		this.directBuffer.put(s.getBytes(), 0, len);

		this.dirty = true;
	}

	// Write a String using the specified character
	// encoding
	final void writeStringNoNull(String s, String encoding,
			String serverEncoding, boolean parserKnowsUnicode)
			throws UnsupportedEncodingException, SQLException {
		byte[] b = StringUtils.getBytes(s, encoding, serverEncoding,
				parserKnowsUnicode);

		int len = b.length;
		ensureCapacity(len);

		this.directBuffer.put(b, 0, len);

		this.dirty = true;
	}
}
