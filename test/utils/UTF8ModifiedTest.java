package utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import junit.framework.TestCase;
import pluto.charon.UTF8Modified;

public class UTF8ModifiedTest extends TestCase {
	public void testUTF8() throws IOException {
		{
			ByteArrayOutputStream bos = new ByteArrayOutputStream(200);
			String theString = "\u0800a\0\u0080";
			UTF8Modified.writeUTFModifiedNull(theString, bos);
			byte[] bytes = bos.toByteArray();
			// decode
			ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
			String decoded = UTF8Modified.readUTFModifiedNull(bis);
			assertEquals(decoded, theString);
		}
		{
			ByteArrayOutputStream bos = new ByteArrayOutputStream(200);
			String theString = "\u000f\u00ff\u0fff\uffff";
			UTF8Modified.writeUTFModifiedNull(theString, bos);
			byte[] bytes = bos.toByteArray();
			// decode
			ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
			String decoded = UTF8Modified.readUTFModifiedNull(bis);
			assertEquals(decoded, theString);
		}
		{
			ByteArrayOutputStream bos = new ByteArrayOutputStream(200);
			String theString = "a\0\u0080\u0800";
			UTF8Modified.writeUTFModifiedNull(theString, bos);
			byte[] bytes = bos.toByteArray();
			assertEquals(bos.size(), 9);
			// a
			assertEquals(bytes[0], 'a');
			// \0
			assertEquals((int) bytes[1] & 0xFF, 0xC0);
			assertEquals((int) bytes[2] & 0xFF, 0x80);
			// \80
			assertEquals((int) bytes[3] & 0xFF, 0xC2);
			assertEquals((int) bytes[4] & 0xFF, 0x80);
			// \800
			assertEquals((int) bytes[5] & 0xFF, 0xE0);
			assertEquals((int) bytes[6] & 0xFF, 0xA0);
			assertEquals((int) bytes[7] & 0xFF, 0x80);

			// decode
			ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
			String decoded = UTF8Modified.readUTFModifiedNull(bis);
			assertEquals(decoded, theString);
		}
		{
			StringBuilder stringBuff = new StringBuilder(4096);
			for (int i = 0; i <= 0xFF; i++) {
				stringBuff.setLength(0);
				for (int j = 0; j <= 0xFF; j++) {
					stringBuff.append((char) (i << 16 + j));
				}
				// encode
				ByteArrayOutputStream bos = new ByteArrayOutputStream(200);
				String theString = stringBuff.toString();
				UTF8Modified.writeUTFModifiedNull(theString, bos);
				// decode
				byte[] bytes = bos.toByteArray();
				ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
				String decoded = UTF8Modified.readUTFModifiedNull(bis);
				assertEquals(decoded, theString);
			}
		}
	}

	public void testUTF8BortherCases() throws IOException {
		assertEquals(UTF8Modified.writeUTFModifiedNull(null, null), 0);
		assertEquals(UTF8Modified.writeUTFModifiedNull("", null), 0);
		assertNull(UTF8Modified.readUTFModifiedNull(null));

		try {
			byte[] bytes = new byte[] { (byte) 0xF7, (byte) 0x80, (byte) 0x80, (byte) 0x80 };
			ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
			UTF8Modified.readUTFModifiedNull(bis);
			fail();
		} catch (Exception e) {
			// ok
		}
		try {
			byte[] bytes = new byte[] { (byte) 0xFB, (byte) 0x80, (byte) 0x80, (byte) 0x80 };
			ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
			UTF8Modified.readUTFModifiedNull(bis);
			fail();
		} catch (Exception e) {
			// ok
		}

		try {
			byte[] bytes = new byte[] { (byte) 0xFD, (byte) 0x80, (byte) 0x80, (byte) 0x80 };
			ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
			UTF8Modified.readUTFModifiedNull(bis);
			fail();
		} catch (Exception e) {
			// ok
		}

		try {
			byte[] bytes = new byte[] { (byte) 0x80, (byte) 0x80, (byte) 0x80 };
			ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
			UTF8Modified.readUTFModifiedNull(bis);
			fail();
		} catch (Exception e) {
			// ok
		}

		try {
			byte[] bytes = new byte[] { (byte) 0x80 };
			ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
			UTF8Modified.readUTFModifiedNull(bis);
			fail();
		} catch (Exception e) {
			// ok
		}
		try {
			byte[] bytes = new byte[] { (byte) 0xC0, (byte) 0xC0, (byte) 0x80 };
			ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
			UTF8Modified.readUTFModifiedNull(bis);
			fail();
		} catch (Exception e) {
			// ok
		}
	}
}
