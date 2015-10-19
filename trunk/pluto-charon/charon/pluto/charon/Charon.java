package pluto.charon;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UTFDataFormatException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import json.JSONBuilder;
import json.JSONParser;
import cx.Context;
import cx.Parser;
import cx.ast.Node;
import cx.handlers.DatabaseHandler;
import cx.handlers.DateHandler;
import cx.handlers.MathHandler;
import cx.handlers.StringHandler;
import cx.handlers.SystemHandler;

public class Charon {
	private final Socket socket;
	private final DataOutputStream outStream;
	private final DataInputStream inStream;
	private final JSONParser jsonParser;
	private final JSONBuilder jsonBuilder;
	private final Context cx;
	private final CharonCore core;

	public Charon(String host, int port, int timeout) throws UnknownHostException, IOException, PlutoCharonException {
		socket = new Socket(host, port);
		if (timeout > 0) {
			socket.setSoTimeout(timeout);
		}
		outStream = new DataOutputStream(socket.getOutputStream());
		inStream = new DataInputStream(socket.getInputStream());
		jsonParser = new JSONParser();
		jsonBuilder = new JSONBuilder();

		cx = new Context();
		cx.addHandler(new StringHandler());
		cx.addHandler(new DateHandler());
		cx.addHandler(new MathHandler());
		cx.addHandler(new DatabaseHandler());
		cx.addHandler(new SystemHandler("system"));

		core = new CharonCore(this);
		cx.addHandler(new CharonHandler(core));
	}

	public void close() {
		try {
			inStream.close();
		} catch (IOException e) {}
		try {
			outStream.close();
		} catch (IOException e) {}
		try {
			socket.shutdownOutput();
		} catch (IOException e) {}
		try {
			socket.shutdownInput();
		} catch (IOException e) {}
	}

	public void logout() {
		close();
	}

	private Map<Object, Object> readMessage() throws PlutoCharonException, IOException {
		Map<Object, Object> actionMessage = null;
		{
			String actionMessageStr = null;
			try {
				// read \0 terminated JSON from the socket
				actionMessageStr = UTF8Modified.readUTFModifiedNull(inStream);
			} catch (UTFDataFormatException e) {
				// read till \0 on error
				while (inStream.read() != 0) {}
				throw new PlutoCharonException("problem with UTF format message: ", e);
			} catch (SocketTimeoutException e) {
				throw new PlutoCharonException("socket reading timeout - server not responding", e);
			} catch (IOException e) {
				throw new PlutoCharonException("problem with reading JSON message: ", e);
			}
			if (actionMessageStr == null || actionMessageStr.length() <= 0) {
				// empty message - just acknowledge it
				throw new PlutoCharonException("empty message");
			}

			Map<Object, Object> actionObject = null;
			try {
				// parse the message
				actionObject = jsonParser.parseJSONString(actionMessageStr);
			} catch (Exception e) {
				throw new PlutoCharonException("problem with JSON message: ", e);
			}
			if (!(actionObject instanceof Map)) {
				throw new PlutoCharonException("problem with JSON message: message should be a single object!");
			} else {
				actionMessage = actionObject;
				return actionMessage;
			}
		}
	}

	private void validateMessage(Map<Object, Object> message) throws PlutoCharonException {
		if (!Boolean.TRUE.equals(message.get(PlutoCharonConstants.OK))) {
			throw new PlutoCharonException(String.valueOf(message.get(PlutoCharonConstants.MESSAGE)));
		}
	}

	public void ping() throws IOException, PlutoCharonException {
		jsonBuilder.reset();
		jsonBuilder.reset().startObject();
		jsonBuilder.addKey(PlutoCharonConstants.ACTION).addValue(PlutoCharonConstants.ACTION_PING);
		jsonBuilder.endObject();
		UTF8Modified.writeUTFModifiedNull(jsonBuilder.toString(), outStream);
		outStream.flush();

		// wait for the result
		Map<Object, Object> message = readMessage();
		validateMessage(message);
	}

	/**
	 * @param username
	 * @param password
	 * @throws IOException
	 * @throws PlutoCharonException
	 *             when credentials are invalid
	 */
	public void login(String username, String password) throws IOException, PlutoCharonException {
		jsonBuilder.reset();
		jsonBuilder.reset().startObject();
		jsonBuilder.addKey(PlutoCharonConstants.ACTION).addValue(PlutoCharonConstants.ACTION_LOGIN);
		jsonBuilder.addKey(PlutoCharonConstants.USER).addValue(username == null ? "" : username);
		jsonBuilder.addKey(PlutoCharonConstants.PASSWORD).addValue(password == null ? "" : password);
		jsonBuilder.endObject();
		UTF8Modified.writeUTFModifiedNull(jsonBuilder.toString(), outStream);
		outStream.flush();

		// wait for the result
		Map<Object, Object> message = readMessage();
		validateMessage(message);
	}

	public void plutoSet(String id, String value) throws IOException, PlutoCharonException {
		if (id == null || id.length() <= 0) {
			throw new PlutoCharonException("id is required");
		}
		final int valueLen = (value != null) ? value.length() : 0;

		jsonBuilder.reset();
		jsonBuilder.reset().startObject();
		jsonBuilder.addKey(PlutoCharonConstants.ACTION).addValue(PlutoCharonConstants.ACTION_SET);
		jsonBuilder.addKey(PlutoCharonConstants.ID).addValue(id);
		jsonBuilder.addKey(PlutoCharonConstants.LENGTH).addValue(valueLen);
		jsonBuilder.endObject();
		UTF8Modified.writeUTFModifiedNull(jsonBuilder.toString(), outStream);
		if (valueLen > 0) {
			UTF8Modified.writeUTFModifiedNull(value, outStream);
		}
		outStream.flush();

		// wait for the result
		Map<Object, Object> message = readMessage();
		validateMessage(message);
	}

	public String plutoGet(String id) throws IOException, PlutoCharonException {
		if (id == null || id.length() <= 0) {
			throw new PlutoCharonException("ID is required");
		}
		jsonBuilder.reset().startObject();
		jsonBuilder.addKey(PlutoCharonConstants.ACTION).addValue(PlutoCharonConstants.ACTION_GET);
		jsonBuilder.addKey(PlutoCharonConstants.ID).addValue(id);
		jsonBuilder.endObject();
		UTF8Modified.writeUTFModifiedNull(jsonBuilder.toString(), outStream);
		outStream.flush();

		// wait for the result
		Map<Object, Object> message = readMessage();
		validateMessage(message);
		int length = PlutoCharonConstants.getMessageInt(message, PlutoCharonConstants.LENGTH);
		if (length <= 0) {
			throw new PlutoCharonException("server value for '" + PlutoCharonConstants.LENGTH
					+ "' was expected to be positive value!");
		}

		// read length content from the socket
		String content = UTF8Modified.readUTFModifiedNull(inStream);

		if (content.length() != length) {
			throw new PlutoCharonException("server document received size differ from the declared length!");
		}
		return content;
	}

	@SuppressWarnings("unchecked")
	List<String> plutoSearch(String prefix, int limit) throws Exception {
		jsonBuilder.reset();
		jsonBuilder.reset().startObject();
		jsonBuilder.addKey(PlutoCharonConstants.ACTION).addValue(PlutoCharonConstants.ACTION_SEARCH);
		jsonBuilder.addKey(PlutoCharonConstants.PREFIX).addValue(prefix);
		jsonBuilder.addKey(PlutoCharonConstants.LIMIT).addValue(limit);
		jsonBuilder.endObject();
		UTF8Modified.writeUTFModifiedNull(jsonBuilder.toString(), outStream);
		outStream.flush();

		// wait for the result
		Map<Object, Object> message = readMessage();
		validateMessage(message);
		int length = PlutoCharonConstants.getMessageInt(message, PlutoCharonConstants.LENGTH);

		Object res = message.get(PlutoCharonConstants.RESULT);

		final List<String> result;
		if (!(res instanceof List)) {
			throw new PlutoCharonException(PlutoCharonConstants.RESULT + " was expected to be of type List!");
		} else {
			result = (List<String>) res;
		}
		if (result.size() != length) {
			throw new PlutoCharonException(PlutoCharonConstants.LENGTH + "=" + length
					+ " do not match the returned length " + result.size() + "!");
		}
		return result;
	}

	String plutoExecute(String code) throws Exception {
		if (code == null || code.length() <= 0) {
			throw new PlutoCharonException("code is required");
		}
		jsonBuilder.reset();
		jsonBuilder.reset().startObject();
		jsonBuilder.addKey(PlutoCharonConstants.ACTION).addValue(PlutoCharonConstants.ACTION_EXECUTE);
		jsonBuilder.addKey(PlutoCharonConstants.LENGTH).addValue(code.length());
		jsonBuilder.endObject();
		UTF8Modified.writeUTFModifiedNull(jsonBuilder.toString(), outStream);
		UTF8Modified.writeUTFModifiedNull(code, outStream);
		outStream.flush();

		// wait for the result
		Map<Object, Object> message = readMessage();
		validateMessage(message);
		int length = PlutoCharonConstants.getMessageInt(message, PlutoCharonConstants.LENGTH);
		if (length <= 0) {
			throw new PlutoCharonException("server value for '" + PlutoCharonConstants.LENGTH
					+ "' was expected to be positive value!");
		}

		// read length content from the socket
		String content = UTF8Modified.readUTFModifiedNull(inStream);

		if (content.length() != length) {
			throw new PlutoCharonException("server document received size differ from the declared length!");
		}
		return content;
	}

	public String plutoCall(String functionName, Object... arguments) throws Exception {
		String call = buildCall(functionName, arguments);
		return plutoExecute(call);
	}

	/* accepts only (String,Number,List,Map) types for arguments */
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

	public Object localExecute(String str) {
		final Parser parser = new Parser(str);
		List<Node> block = parser.parse();
		Object result = cx.evaluate(block);
		return result;
	}
}
