package pluto.managers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;

/**
 * Lightweight HTML Page Creator similar to Apache Wicket
 */
public class UIHTMLManager {

	private static final Logger log = Logger.getLogger(UIHTMLManager.class.getName());

	static final Map<Object, Page> pages = new HashMap<Object, UIHTMLManager.Page>(16);

	private static final HashMap<Character, String> charToCode;
	static {
		charToCode = new HashMap<Character, String>(32);
		charToCode.put(' ', "%20");
		charToCode.put('<', "%3C");
		charToCode.put('>', "%3E");
		charToCode.put('#', "%23");
		charToCode.put('%', "%25");
		charToCode.put('{', "%7B");
		charToCode.put('}', "%7D");
		charToCode.put('|', "%7C");
		charToCode.put('^', "%5E");
		charToCode.put('~', "%7E");
		charToCode.put('[', "%5B");
		charToCode.put(']', "%5D");
		charToCode.put('`', "%60");
		charToCode.put(';', "%3B");
		charToCode.put('\\', "%5C");
		charToCode.put('/', "%2F");
		charToCode.put('?', "%3F");
		charToCode.put(':', "%3A");
		charToCode.put('@', "%40");
		charToCode.put('=', "%3D");
		charToCode.put('&', "%26");
		charToCode.put('$', "%24");
		charToCode.put('\"', "%22");
		charToCode.put('+', "%2B");
		charToCode.put(',', "%2C");
		charToCode.put('\'', "%27");
	}

	public static Page getPage(Object key) {
		Page page = (Page) pages.get(key);
		if (page != null) {
			return new Page(page);
		} else {
			return null;
		}
	}

	public static void addPage(Class<?> clazz) {
		try {
			final String filename = clazz.getSimpleName() + ".html";
			InputStream is = clazz.getResourceAsStream(filename);
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);

			Page page = new Page(clazz, null);
			parse(br, page);
			pages.put(clazz, page);

		} catch (Exception e) {
			log.log(Level.SEVERE, "Cannot generate UI for class: " + clazz.getName(), e);
		}
	}

	public static void addPage(Class<?> clazz, String pageName) {
		InputStream is = null;
		InputStreamReader isr = null;
		BufferedReader br = null;
		try {
			is = clazz.getResourceAsStream(pageName);
			isr = new InputStreamReader(is);
			br = new BufferedReader(isr);

			Page page = new Page(clazz, pageName);
			parse(br, page);
			pages.put(pageName, page);

		} catch (Exception e) {
			log.log(Level.SEVERE, "Cannot generate UI for class: " + clazz.getName(), e);
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (isr != null) {
				try {
					isr.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * separate content by ${___} values.
	 */
	static void parse(Reader br, Page page) {
		page.lines.clear();
		page.clearContext();
		int c = 0;
		char pc = 0;
		StringBuilder expression = new StringBuilder();
		StringBuilder result = new StringBuilder();

		try {
			while (c != -1) {
				pc = (char) c;
				c = br.read();
				if (c == '$') {
					// skip saving and wait till recognized
				} else {
					if (pc == '$') {
						if (c == '{') {
							expression.setLength(0);
							expression.append(pc);
							while (c != -1) {
								expression.append((char) c);
								if (c == '}') break;
								c = br.read();
							}

							if (c != '}') {
								// not terminated ${
								result.append(expression);
								page.lines.add(result.toString());
								result.setLength(0);
							} else {
								pc = 0;
								if (result.length() > 0) {
									page.lines.add(result.toString());
									result.setLength(0);
								}
								page.lines.add(expression.toString());
							}
							continue;
						} else {
							// it was only a $ without { after it
							result.append(pc);
						}
					}
					// add current char to the result
					if (c != -1) result.append((char) c);
				}
			}

		} catch (IOException e) {
			// reach the end of the stream
		}
		if (result.length() > 0) {
			page.lines.add(result.toString());
		}
	}

	public static class Page {
		final List<String> lines;
		final Map<String, String> context = new HashMap<String, String>();// <String,
																			// String>
		public final Class<?> clazz;
		public final String filename;

		Page(Class<?> clazz, String file) {
			lines = new ArrayList<String>();
			this.clazz = clazz;
			this.filename = file;
		}

		public Page(Page page) {
			this.lines = page.lines;
			this.clazz = page.clazz;
			this.filename = page.filename;
		}

		public void clearContext() {
			context.clear();
		}

		public void clearVar(String var) {
			context.remove(var);
		}

		public void setVar(String var, String value) {
			if (value != null) {
				context.put(var, value);
			} else {
				context.remove(var);
			}
		}

		public void setVar(String var, long value) {
			context.put(var, String.valueOf(value));
		}

		public void setVar(String var, int value) {
			context.put(var, String.valueOf(value));
		}

		public void setVar(String var, String value, boolean attributeEscape) {
			if (value != null) {
				context.put(var, escape(value));
			} else {
				context.remove(var);
			}
		}

		public void setVar(String var, Object value) {
			if (value != null) {
				context.put(var, value.toString());
			} else {
				context.remove(var);
			}
		}

		public void getVar(String var) {
			context.get(var);
		}

		public void render(Writer writer) throws IOException {
			final Iterator<String> i = lines.iterator();
			while (i.hasNext()) {
				final String line = (String) i.next();
				if (line.charAt(0) == '$' && line.charAt(1) == '{' && line.length() > 3) {
					final String varname = line.substring(2, line.length() - 1);
					final String cx = (String) context.get(varname);
					writer.append(cx == null ? "" : cx);
				} else {
					writer.append(line);
				}
			}
		}

		public String render() throws IOException {
			final StringBuilder result = new StringBuilder();
			final Iterator<String> i = lines.iterator();
			while (i.hasNext()) {
				final String line = (String) i.next();
				if (line.charAt(0) == '$' && line.charAt(1) == '{' && line.length() > 3) {
					final String varname = line.substring(2, line.length() - 1);
					final String cx = (String) context.get(varname);
					result.append(cx == null ? "" : cx);
				} else {
					result.append(line);
				}
			}
			return result.toString();
		}
	}

	public static final String escape(String str) {
		if (str == null) {
			return "";
		}
		StringBuilder escaped = new StringBuilder(str.length() * 2);
		escape(escaped, str);
		return escaped.toString();
	}

	public static final StringBuilder escape(StringBuilder buffer, String str) {
		if (str == null) {
			return buffer;
		}
		for (int i = 0, j = str.length(); i < j; i++) {
			final char c = str.charAt(i);
			String s;
			switch (c) {
				case '<':
					s = "&lt;";
					break;
				case '>':
					s = "&gt;";
					break;
				case '&':
					s = "&amp;";
					break;
				case '"':
					s = "&quot;";
					break;
				case '\'':
					s = "&#39;";
					break;

				default:
					buffer.append(c);
					continue;
			}
			buffer.append(s);
		}
		return buffer;
	}

	public static final String escapeURL(String str) {
		if (str == null) {
			return "";
		}
		StringBuilder escaped = new StringBuilder(str.length() * 2);
		escapeURL(escaped, str);
		return escaped.toString();
	}

	public static final StringBuilder escapeURL(StringBuilder buffer, String str) {
		if (str == null) {
			return buffer;
		}
		for (int i = 0, j = str.length(); i < j; i++) {
			final char c = str.charAt(i);
			String s = charToCode.get(c);
			if (s == null) {
				buffer.append(c);
				continue;
			}
			buffer.append(s);
		}
		return buffer;
	}

	public static long toLong(String str) {
		try {
			return Long.parseLong(str, 10);
		} catch (Exception e) {
			return 0;
		}
	}

	public static void initFromRequest(HttpServletRequest request, Object context) {
		if (context == null) {
			return;
		}

		Field[] fields = context.getClass().getFields();
		for (int i = 0, j = fields.length; i < j; i++) {
			Field field = fields[i];

			String value = request.getParameter(field.getName());
			String trimmed;
			if (value == null || (trimmed = value.trim()).length() <= 0) {
				Class<?> type = field.getType();
				if (type.isAssignableFrom(String.class)) {
					try {
						field.set(context, null);
					} catch (Throwable e) {}
				} else if (type.isAssignableFrom(int.class)) {
					try {
						field.setInt(context, 0);
					} catch (Throwable e) {}
				} else if (type.isAssignableFrom(long.class)) {
					try {
						field.setLong(context, 0);
					} catch (Throwable e) {}
				}

			} else {
				Class<?> type = field.getType();
				if (type.isAssignableFrom(String.class)) {
					try {
						field.set(context, (trimmed.length() <= 0) ? null : trimmed);
					} catch (Throwable e) {}
				} else if (type.isAssignableFrom(int.class)) {
					try {
						field.setInt(context, (int) toLong(trimmed));
					} catch (Throwable e) {}
				} else if (type.isAssignableFrom(long.class)) {
					try {
						field.setLong(context, toLong(trimmed));
					} catch (Throwable e) {}
				}
			}
		}
	}

	public static Date toDate(String text, boolean endOfDate) {
		String trimmed;
		if (text == null || (trimmed = text.trim()).length() < 10) {
			return null;
		}

		int i = 0;
		char c;

		int len = trimmed.length();

		int n1 = 0;
		int n2 = 0;
		int n3 = 0;

		// skip non digit chars
		for (; i < len && !Character.isDigit(c = trimmed.charAt(i)); i++)
			;
		// take first number
		for (; i < len && Character.isDigit(c = trimmed.charAt(i)); i++) {
			n1 = n1 * 10 + ((int) c & 0x0F);
		}
		// skip non digit chars
		for (; i < len && !Character.isDigit(c = trimmed.charAt(i)); i++)
			;
		// take second number
		for (; i < len && Character.isDigit(c = trimmed.charAt(i)); i++) {
			n2 = n2 * 10 + ((int) c & 0x0F);
		}
		// skip non digit chars
		for (; i < len && !Character.isDigit(c = trimmed.charAt(i)); i++)
			;
		// take third number
		for (; i < len && Character.isDigit(c = trimmed.charAt(i)); i++) {
			n3 = n3 * 10 + ((int) c & 0x0F);
		}

		if (n1 > 1000) {
			// y,m,d
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.YEAR, n1);
			cal.set(Calendar.MONTH, n2 - 1);
			cal.set(Calendar.DAY_OF_MONTH, n3);
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);

			if (n1 == cal.get(Calendar.YEAR) && (n2 - 1) == cal.get(Calendar.MONTH)
					&& n3 == cal.get(Calendar.DAY_OF_MONTH)) {
				if (endOfDate) {
					cal.add(Calendar.DAY_OF_MONTH, 1);
					cal.add(Calendar.MILLISECOND, -1);
				}
				return cal.getTime();
			}
		} else if (n3 > 1000) {
			// d.m.y
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.YEAR, n3);
			cal.set(Calendar.MONTH, n2 - 1);
			cal.set(Calendar.DAY_OF_MONTH, n1);
			cal.set(Calendar.HOUR_OF_DAY, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);

			if (n1 == cal.get(Calendar.DAY_OF_MONTH) && (n2 - 1) == cal.get(Calendar.MONTH)
					&& n3 == cal.get(Calendar.YEAR)) {
				if (endOfDate) {
					cal.add(Calendar.DAY_OF_MONTH, 1);
					cal.add(Calendar.MILLISECOND, -1);
				}
				return cal.getTime();
			}
		}
		return null;
	}
}
