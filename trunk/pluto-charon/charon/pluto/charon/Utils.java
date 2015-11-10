package pluto.charon;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;

import cx.Parser;
import cx.ast.Node;
import cx.exception.ParserException;
import json.JSONBuilder;
import json.JSONParser;

public class Utils {

	public final static SimpleDateFormat dateFormater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	final static String[] emptyStringArray = new String[0];

	/** convert String to long or null */
	public static Long toLong(String str) {
		Long res = null;
		if (str != null) {
			try {
				res = Long.parseLong(str, 10);
			} catch (Exception e) {
			}
		}
		return res;
	}

	/** convert String to long or null */
	public static Integer toInteger(String str) {
		Integer res = null;
		if (str != null) {
			try {
				res = Integer.parseInt(str, 10);
			} catch (Exception e) {
			}
		}
		return res;
	}

	/** return occurrences of separator in string */
	private static int count(final String string, final char separator) {
		int count = 0;
		for (int i = 0, len = string.length(); i < len; i++) {
			final char c = string.charAt(i);
			if (c == separator)
				count++;
		}
		return count;
	}

	/** tokenize string by separator without empties */
	public static String[] split(final String string, final char separator) {
		if (string == null || string.length() <= 0) {
			return emptyStringArray;
		}
		int count = count(string, separator);
		if (count <= 0) {
			return new String[] { string };
		}

		Vector<String> tokens = new Vector<String>(count + 2);
		int i = 0, len = string.length(), s = 0;
		for (; i < len; i++) {
			final char c = string.charAt(i);
			if (c == separator) {
				if (i - s > 0) {
					final String addstr = string.substring(s, i).trim();
					if (addstr.length() > 0)
						tokens.addElement(addstr);
				}
				s = i + 1;
			}
		}
		// if there is remaining string - add it
		if (len - s > 0) {
			final String addstr = string.substring(s, i).trim();
			if (addstr.length() > 0)
				tokens.addElement(addstr);
		}

		final String[] result = new String[tokens.size()];
		tokens.copyInto(result);
		return result;
	}

	/** tokenize string by separator with empties */
	public static String[] tokenize(final String string, final char separator) {
		if (string == null || string.length() <= 0) {
			return emptyStringArray;
		}
		int count = count(string, separator);
		if (count <= 0) {
			return new String[] { string };
		}

		Vector<String> tokens = new Vector<String>(count + 2);
		int i = 0, len = string.length(), s = 0;
		for (; i < len; i++) {
			final char c = string.charAt(i);
			if (c == separator) {
				final String addstr = string.substring(s, i).trim();
				tokens.addElement(addstr);
				s = i + 1;
			}
		}
		final String addstr = string.substring(s, i).trim();
		tokens.addElement(addstr);

		final String[] result = new String[tokens.size()];
		tokens.copyInto(result);
		return result;
	}

	/**
	 * converts "YYYY-MM-DD" to Date. On error it returns null
	 */
	public static Date stringToDate(String date) {
		if (date == null || date.length() < 10 || date.charAt(4) != '-' || date.charAt(7) != '-') {
			return null;
		}
		try {
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(0);
			if (date.length() >= 8) {
				calendar.set(Calendar.YEAR, Integer.parseInt(date.substring(0, 4)));
				calendar.set(Calendar.MONTH, Integer.parseInt(date.substring(5, 7)) - 1);
				calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(date.substring(8, 10)));
			}
			{
				calendar.set(Calendar.HOUR_OF_DAY, 0);
				calendar.set(Calendar.MINUTE, 0);
				calendar.set(Calendar.SECOND, 0);
			}
			return calendar.getTime();

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * converts Date to "YYYY-MM-DD".
	 */
	public static String dateToString(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		StringBuilder result = new StringBuilder();

		result.append(calendar.get(Calendar.YEAR));
		result.append('-');

		int month = calendar.get(Calendar.MONTH) + 1;
		if (month <= 9)
			result.append('0');
		result.append(month);
		result.append('-');

		int day = calendar.get(Calendar.DAY_OF_MONTH);
		if (day <= 9)
			result.append('0');
		result.append(day);

		return result.toString();
	}

	public static String timeSinceInSeconds(long time) {
		int sec = (int) ((System.currentTimeMillis() - time) / 1000);
		int min = sec / 60;
		sec %= 60;
		return min + "'" + sec + "''";
	}

	/* accepts only (String,Number,List,Map) types for arguments */
	public static String buildCall(String functionName, Object... parameters) {
		StringBuilder call = new StringBuilder(2048);
		call.append(functionName).append('(');
		if (parameters.length > 0) {
			for (Object obj : parameters) {
				call.append(JSONBuilder.objectToJSON(obj)).append(',');
			}
			call.setLength(call.length() - 1);
		}
		call.append(");");
		return call.toString();
	}

	private static final Parser cxParser;

	static {
		cxParser = new Parser();
		cxParser.supportTryCatchThrow = true;
		cxParser.supportSQLEscaping = true;
	}

	/**
	 * @param str
	 * @return parsed CX script or null if invalid.
	 */
	public static List<Node> asCX(String str) {
		if (str == null) {
			return null;
		}
		try {
			return cxParser.parse(str);
		} catch (ParserException e) {
			return null;
		}
	}

	private static final JSONParser jsonParser = new JSONParser();

	/**
	 * @param str
	 * @return parsed JSON or null if invalid.
	 */
	public static Map<Object, Object> asJSON(String str) {
		if (str == null) {
			return null;
		}
		try {
			return jsonParser.parseJSONString(str);
		} catch (Exception e) {
			return null;
		}
	}

	public static X509TrustManager getX509TrustManager(KeyStore keystore)
			throws NoSuchAlgorithmException, KeyStoreException {
		TrustManagerFactory trustMgrFactory = TrustManagerFactory
				.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		trustMgrFactory.init(keystore);
		TrustManager trustManagers[] = trustMgrFactory.getTrustManagers();
		for (int i = 0; i < trustManagers.length; i++) {
			if (trustManagers[i] instanceof X509TrustManager) {
				return (X509TrustManager) trustManagers[i];
			}
		}
		return null;
	};

	public static X509KeyManager getX509KeyManager(KeyStore keystore, String password)
			throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException {
		KeyManagerFactory keyMgrFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		keyMgrFactory.init(keystore, password.toCharArray());
		KeyManager keyManagers[] = keyMgrFactory.getKeyManagers();
		for (int i = 0; i < keyManagers.length; i++) {
			if (keyManagers[i] instanceof X509KeyManager) {
				return (X509KeyManager) keyManagers[i];
			}
		}
		return null;
	};

	public static X509TrustManager trustAllCert = new X509TrustManager() {
		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}

		public void checkClientTrusted(X509Certificate[] certs, String authType) {
		}

		public void checkServerTrusted(X509Certificate[] certs, String authType) {
		}
	};
}
