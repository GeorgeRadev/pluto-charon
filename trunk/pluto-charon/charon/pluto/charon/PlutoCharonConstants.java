package pluto.charon;

import java.util.Map;

public class PlutoCharonConstants {
	protected PlutoCharonConstants() {}

	public static final String ACTION = "action";
	public static final String ACTION_PING = "ping";
	public static final String ACTION_LOGIN = "login";
	public static final String ACTION_GET = "get";
	public static final String ACTION_SET = "set";
	public static final String ACTION_EXECUTE = "execute";
	public static final String ACTION_SEARCH = "search";

	public static final String OK = "ok";
	public static final String MESSAGE = "message";
	public static final String USER = "user";
	public static final String PASSWORD = "password";
	public static final String ID = "id";
	public static final String LENGTH = "length";
	public static final String LIMIT = "limit";
	public static final String STACKTRACE = "stacktrace";
	public static final String PREFIX = "prefix";
	public static final String RESULT = "result";

	public static String getMessageString(Map<Object, Object> actionMessage, String key) throws PlutoCharonException {
		Object str = actionMessage.get(key);
		if (str == null) {
			throw new PlutoCharonException("message requires '" + key + "' element !");
		}
		String result = String.valueOf(str);
		if (result.length() <= 0) {
			throw new PlutoCharonException("message requires non-null non-empty '" + key + "' element !");
		}
		return result;
	}

	public static int getMessageInt(Map<Object, Object> actionMessage, String key) throws PlutoCharonException {
		Object _int = actionMessage.get(key);
		if (_int == null) {
			throw new PlutoCharonException("message requires '" + key + "' element !");
		}
		if (!(_int instanceof Number)) {
			throw new PlutoCharonException("message requires '" + key + "' number element !");
		}
		return ((Number) _int).intValue();
	}
}
