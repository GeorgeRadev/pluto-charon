package utils;

import java.io.IOException;
import java.io.StringWriter;
import pluto.utils.CSVTableBufferedWriter;
import junit.framework.TestCase;

public class CSVTableBufferedWriterTest extends TestCase {

	public void testEscaping() {
		StringBuilder buff = new StringBuilder();

		buff.setLength(0);
		CSVTableBufferedWriter.escape(buff, null);
		assertEquals("", buff.toString());

		buff.setLength(0);
		CSVTableBufferedWriter.escape(buff, "");
		assertEquals("", buff.toString());

		buff.setLength(0);
		CSVTableBufferedWriter.escape(buff, "test\"test");
		assertEquals("test\"\"test", buff.toString());

		buff.setLength(0);
		CSVTableBufferedWriter.escape(buff, "\"\"");
		assertEquals("\"\"\"\"", buff.toString());
	}

	public void testFormat() throws IOException {
		StringWriter stringWriter = new StringWriter(1024);
		CSVTableBufferedWriter writer = new CSVTableBufferedWriter(stringWriter, 100);

		writer.cell(42);
		writer.cell(36028797018963967L);
		writer.newRow();
		writer.cell("test\ntest");
		writer.cell("test\"test");
		writer.done();

		String result = stringWriter.getBuffer().toString();
		assertEquals("sep=,\n42,36028797018963967\n\"test\ntest\",=\"test\"\"test\"", result);
	}
}
