package pluto.utils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CSVTableBufferedWriter {
	/**
	 * 1997,Ford,E350,"Go get one"" now"" <br/>
	 * they are going fast"
	 */
	final BufferedWriter writer;
	public static final String PAGE_ENCODING = "text/html; charset=utf-8";

	public CSVTableBufferedWriter(Writer writerOut, int bufferSize) throws IOException {
		writer = new BufferedWriter(writerOut, bufferSize);
		newRow = true;
		writer.write("sep=,\n");// define the CSV separator
	}

	private boolean newRow = true;

	public CSVTableBufferedWriter newRow() throws IOException {
		newRow = true;
		writer.write('\n');
		return this;
	}

	public CSVTableBufferedWriter cell(String value) throws IOException {
		if (!newRow) {
			writer.write(',');
		}
		newRow = false;
		if (value.length() > 0) {
			if (value.indexOf('\n') < 0) {
				writer.write('=');
			}
			writer.write('"');
			StringBuilder escaped = new StringBuilder(value.length() * 2);
			escape(escaped, value);
			writer.write(escaped.toString());
			writer.write('"');
		}
		return this;
	}

	public CSVTableBufferedWriter cell(int value) throws IOException {
		if (!newRow) {
			writer.write(',');
		}
		newRow = false;
		writer.write(String.valueOf(value));
		return this;
	}

	public CSVTableBufferedWriter cell(long value) throws IOException {
		if (!newRow) {
			writer.write(',');
		}
		newRow = false;
		writer.write(String.valueOf(value));
		return this;
	}

	SimpleDateFormat YYYY_MM_DD_HH_MM_SS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public CSVTableBufferedWriter cell(Date value) throws IOException {
		if (!newRow) {
			writer.write(',');
		}
		newRow = false;
		writer.write(YYYY_MM_DD_HH_MM_SS.format(value));
		return this;
	}

	public void done() throws IOException {
		writer.flush();
		writer.close();
	}

	public static final StringBuilder escape(StringBuilder buffer, String str) {
		if (str == null) {
			return buffer;
		}
		for (int i = 0, j = str.length(); i < j; i++) {
			final char c = str.charAt(i);
			String s;
			if (c == '"') {
				s = "\"\"";
				buffer.append(s);
			} else {
				buffer.append(c);
			}
		}
		return buffer;
	}
}
