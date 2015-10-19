package pluto.charon;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UTFDataFormatException;

public class UTF8Modified {

	/**
	 * write a \0 terminated modifiedUTF8(\0 is two bytes) string to the
	 * specified DataOutput using encoding in a machine-independent manner.
	 * <p>
	 * Each character of the string is written to the output, in sequence, using
	 * the modified UTF-8 encoding for the character and a \0 termination at the
	 * end.
	 * 
	 * @param str
	 *            a string to be written.
	 * @param out
	 *            destination to write to
	 * @return The number of bytes written out.
	 * @exception IOException
	 *                if an I/O error occurs.
	 */
	public static int writeUTFModifiedNull(String str, OutputStream out) throws IOException {
		if (str == null || str.length() <= 0 || out == null) {
			return 0;
		}
		int c;
		final int strlen = str.length();
		int utflen = 0;

		/* use charAt instead of copying String to char array */
		for (int i = 0; i < strlen; i++) {
			c = str.charAt(i);
			if ((c >= 0x0001) && (c <= 0x007F)) {
				utflen++;
			} else if (c > 0x07FF) {
				utflen += 3;
			} else {// \0 is counted as escaped
				utflen += 2;
			}
		}

		byte[] bytearr = new byte[utflen + 1]; // add \0 termination
		bytearr[utflen] = 0; // and set it

		// create modifiedUTF8(\0 is two bytes)
		for (int i = 0, count = 0; i < strlen; i++) {
			c = str.charAt(i);
			if ((c >= 0x0001) && (c <= 0x007F)) {
				bytearr[count++] = (byte) c;
			} else if (c > 0x07FF) {
				bytearr[count++] = (byte) (0xE0 | ((c >> 12) & 0x0F));
				bytearr[count++] = (byte) (0x80 | ((c >> 6) & 0x3F));
				bytearr[count++] = (byte) (0x80 | ((c >> 0) & 0x3F));
			} else {// \0 is also escaped
				bytearr[count++] = (byte) (0xC0 | ((c >> 6) & 0x1F));
				bytearr[count++] = (byte) (0x80 | ((c >> 0) & 0x3F));
			}
		}
		out.write(bytearr);
		return utflen + 1;
	}

	/**
	 * Reads from the stream <code>in</code> a representation of a Unicode
	 * character string encoded in modified UTF-8(\0 is two bytes) format; this
	 * string of characters is then returned as a <code>String</code>.
	 * 
	 * @param in
	 *            a data input stream.
	 * @return a Unicode string.
	 * @exception EOFException
	 *                if the input stream reaches the end before all the bytes.
	 * @exception IOException
	 *                the stream has been closed and the contained input stream
	 *                does not support reading after close, or another I/O error
	 *                occurs.
	 * @exception UTFDataFormatException
	 *                if the bytes do not represent a valid modified UTF-8
	 *                encoding of a Unicode string.
	 */
	public final static String readUTFModifiedNull(InputStream in) throws IOException {
		if (in == null) {
			return null;
		}
		StringBuilder byteBuffer = new StringBuilder(8192);
		int p = 7;// initial pointer in the rolling buffer
		int[] buf = new int[] { 0, 0, 0, 0, 0, 0, 0, 0 };// rolling back buffer
		boolean waitingMode = false;
		while (true) {
			int c = in.read();
			if (c == -1 || c == 0) {
				break;
			}
			buf[p] = c;
			if (c > 0xEF) {
				throw new UTFDataFormatException("UTF8 prefixes more than 3 bytes not supported");
			} else if ((c & 0xE0) == 0xC0) {
				// probably 110x xxxx 10xx xxxx
				// wait for more chars
				if (waitingMode) {
					throw new UTFDataFormatException("UTF8 escape already started!");
				}
				waitingMode = true;
			} else if ((c & 0xF0) == 0xE0) {
				// probably 1110 xxxx 10xx xxxx 10xx xxxx
				// wait for more chars
				if (waitingMode) {
					throw new UTFDataFormatException("UTF8 escape already started!");
				}
				waitingMode = true;
			} else if ((c & 0xC0) == 0x80) {
				if (!waitingMode) {
					throw new UTFDataFormatException("UTF8 invalid escaping!");
				}
				// decode UTF8
				int c2 = buf[(p + 1) & 0x7];
				// check if previous is 110x xxxx 10xx xxxx
				if ((c2 & 0xF0) == 0xE0) {
					// probably 1110 xxxx 10xx xxxx 10xx xxxx
					// wait for more chars
				} else if ((c2 & 0xE0) == 0xC0) {
					// 110x xxxx 10xx xxxx
					byteBuffer.append((char) (((c2 & 0x1F) << 6) | (c & 0x3F)));
					waitingMode = false;
				} else if ((c2 & 0xC0) == 0x80) {
					int c3 = buf[(p + 2) & 0x7];
					// check if previous is 1110 xxxx 10xx xxxx 10xx xxxx
					if ((c3 & 0xF0) == 0xE0) {
						// 1110 xxxx 10xx xxxx 10xx xxxx
						byteBuffer.append((char) (((c3 & 0x0F) << 12) | ((c2 & 0x3F) << 6) | ((c & 0x3F) << 0)));
						waitingMode = false;
					} else {
						throw new UTFDataFormatException(String.format("malformed input: %2x %2x %2x", c3, c2, c));
					}
				} else {
					throw new UTFDataFormatException(String.format("malformed input: %2x %2x", c2, c));
				}
			} else if (c <= 0x7f) {
				// normal ASCII(<=0x7f) char
				byteBuffer.append((char) c);
			}
			p = (p - 1) & 0x7;// rolling back pointer
		}

		return byteBuffer.length() <= 0 ? null : byteBuffer.toString();
	}
}
