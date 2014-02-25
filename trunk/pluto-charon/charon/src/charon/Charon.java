package charon;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import json.JSONBuilder;
import json.JSONDOMParser;
import cx.Context;
import cx.Parser;
import cx.ast.Node;
import cx.runtime.DateHandler;
import cx.runtime.MathHandler;
import cx.runtime.ObjectHandler;

public class Charon {
	private final Socket socket;
	private final DataOutputStream outStream;
	private final DataInputStream inStream;
	private final JSONDOMParser JSONParser = new JSONDOMParser();
	private final Context cx;
	private final CharonCore core;

	public Charon(String host, int port) throws Exception {
		this(host, port, "", "");
	}

	@SuppressWarnings("unchecked")
	public Charon(String host, int port, String user, String password) throws Exception {
		socket = new Socket(host, port);
		outStream = new DataOutputStream(socket.getOutputStream());
		inStream = new DataInputStream(socket.getInputStream());

		String helloMessage = inStream.readUTF();
		Object result = JSONParser.parse(helloMessage);
		if (result instanceof Map) {
			Map<Object, Object> map = (Map<Object, Object>) result;
			if ("authentication".equals(map.get("type"))) {
				Map<String, String> authentication = new HashMap<String, String>();
				authentication.put("user", user);
				authentication.put("password", password);
				map = (Map<Object, Object>) executeOnServer(authentication);
				if (!"ok".equals(map.get("type"))) {
					throw new Exception("Authentication error!");
				}
			}
		}

		cx = new Context();
		core = new CharonCore(this);
		cx.addHandler(new DateHandler());
		cx.addHandler(new MathHandler());
		cx.addHandler(new ObjectHandler(core, "core"));
		cx.addHandler(new ImportHandler(core));
	}

	Object executeOnServer(String str) throws Exception {
		outStream.writeUTF(str);
		String json = inStream.readUTF();
		Object result = JSONParser.parse(json);
		if (result instanceof Map) {
			@SuppressWarnings("rawtypes")
			Object error = ((Map) result).get("error");
			if (error != null && error instanceof String) {
				throw new Exception(error.toString());
			}
		} else if (result instanceof String) {
			try {
				result = Parser.parseNumber((String) result);
			} catch (Exception e) {
			}
		}
		return result;
	}

	Object executeOnServer(Object object) throws Exception {
		String str = JSONBuilder.objectToJSON(object);
		return executeOnServer(str);
	}

	private String buildCall(String functionName, Object... parameters) {
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

	Object callOnServer(String functionName, Object... arguments) throws Exception {
		String call = buildCall(functionName, arguments);
		return executeOnServer(call);
	}

	Object execute(String str) {
		final Parser parser = new Parser(str);
		List<Node> block = parser.parse();
		Object result = cx.evaluate(block);
		return result;
	}

	/**
	 * define value for a key. it is persisted in the DB
	 * 
	 * @param key
	 * @param value
	 * @throws Exception
	 */
	public void setValue(String key, String value) throws Exception {
		core.setValue(key, value);
	}

	/**
	 * retrieve value from DB by key
	 * 
	 * @param key
	 * @return retrieve value for this key
	 * @throws Exception
	 */
	public Object getValue(String key) throws Exception {
		return core.getValue(key);
	}

	/**
	 * @param prefix
	 * @return return all keys with that prefix
	 * @throws Exception
	 */
	public Object findKeys(String prefix) throws Exception {
		return core.findKeys(prefix);
	}
}
