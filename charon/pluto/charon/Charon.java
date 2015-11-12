package pluto.charon;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UTFDataFormatException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import cx.Context;
import cx.Parser;
import cx.ast.Node;
import cx.handlers.DatabaseHandler;
import cx.handlers.DateHandler;
import cx.handlers.MathHandler;
import cx.handlers.ObjectHandler;
import cx.handlers.StringHandler;
import cx.handlers.SystemHandler;
import cx.runtime.Function;
import json.JSONBuilder;
import json.JSONParser;

public class Charon {
	private final Socket socket;
	private final DataOutputStream outStream;
	private final DataInputStream inStream;
	private final JSONBuilder jsonBuilder = new JSONBuilder();
	private final Context cx;
	private final CharonCore core;

	private static final JSONParser jsonParser = new JSONParser();
	private static final Parser cxParser = new Parser();

	/**
	 * Create CharonClient and connect to PlutoServer
	 * 
	 * @param host
	 * @param port
	 * @param timeout
	 * @throws UnknownHostException
	 * @throws IOException
	 * @throws PlutoCharonException
	 */
	public Charon(String host, int port, int timeout) throws UnknownHostException, IOException, PlutoCharonException {
		socket = new Socket(host, port);
		if (timeout > 0) {
			socket.setSoTimeout(timeout);
		}
		outStream = new DataOutputStream(socket.getOutputStream());
		inStream = new DataInputStream(socket.getInputStream());

		cx = new Context();
		core = new CharonCore(this);
		initContext(cx);
	}

	public static Socket createSSLSocket(String host, int port, int timeout, String sslKeystoreType,
			String sslContextType, String sslCertificateFile, String sslCertificatePassword)
					throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException,
					KeyManagementException, UnrecoverableKeyException {
		KeyStore keyStore = KeyStore.getInstance(sslKeystoreType);
		FileInputStream fileStream = new FileInputStream(sslCertificateFile);
		keyStore.load(fileStream, sslCertificatePassword.toCharArray());

		TrustManager[] trustManagers = new TrustManager[1];
		trustManagers[0] = Utils.trustAllCert;// Utils.getX509TrustManager(keyStore);
		SSLContext ctx = SSLContext.getInstance(sslContextType);

		KeyManager[] keyManagers = new KeyManager[1];
		keyManagers[0] = Utils.getX509KeyManager(keyStore, sslCertificatePassword);

		ctx.init(keyManagers, trustManagers, null);
		SSLSocketFactory SocketFactory = (SSLSocketFactory) ctx.getSocketFactory();

		Socket socket = (SSLSocket) SocketFactory.createSocket(host, port);
		if (timeout > 0) {
			socket.setSoTimeout(timeout);
		}
		return socket;
	}

	/**
	 * create client by custom socket implementation
	 */
	public Charon(Socket socket) throws UnknownHostException, IOException, PlutoCharonException {
		this.socket = socket;
		outStream = new DataOutputStream(socket.getOutputStream());
		inStream = new DataInputStream(socket.getInputStream());

		cx = new Context();
		core = new CharonCore(this);
		initContext(cx);
	}

	private void initContext(Context cx) {
		cx.addHandler(new StringHandler());
		cx.addHandler(new DateHandler());
		cx.addHandler(new MathHandler());
		cx.addHandler(new DatabaseHandler());
		cx.addHandler(new SystemHandler("system"));

		cx.addHandler(new CharonImportHandler(this));
		cx.addHandler(new ObjectHandler(core, "charon"));
	}

	/**
	 * Close connection with server
	 */
	public void close() {
		try {
			inStream.close();
		} catch (Throwable e) {
		}
		try {
			outStream.close();
		} catch (Throwable e) {
		}
		try {
			socket.shutdownOutput();
		} catch (Throwable e) {
		}
		try {
			socket.shutdownInput();
		} catch (Throwable e) {
		}
	}

	/**
	 * Close connection with server
	 */
	public void logout() {
		close();
	}

	/**
	 * Reads JSON message. All messages are send as UTF8Modified non null
	 * containing through the socket stream.
	 * 
	 * @return
	 * @throws PlutoCharonException
	 * @throws IOException
	 */
	private Map<Object, Object> readMessage() throws PlutoCharonException, IOException {
		Map<Object, Object> actionMessage = null;
		{
			String actionMessageStr = null;
			try {
				// read \0 terminated JSON from the socket
				actionMessageStr = UTF8Modified.readUTFModifiedNull(inStream);
			} catch (UTFDataFormatException e) {
				// read till \0 on error
				while (inStream.read() != 0) {
				}
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

	/**
	 * Verifies that the message contains ok:true inside, otherwize throw
	 * exception with the error message
	 * 
	 * @param message
	 * @throws PlutoCharonException
	 */
	private void validateMessage(Map<Object, Object> message) throws PlutoCharonException {
		if (!Boolean.TRUE.equals(message.get(PlutoCharonConstants.OK))) {
			throw new PlutoCharonException(String.valueOf(message.get(PlutoCharonConstants.MESSAGE)));
		}
	}

	/**
	 * Check if the PlutoServer responds
	 * 
	 * @throws IOException
	 * @throws PlutoCharonException
	 */
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
	 * log to the PlutoServer with provided credentials
	 * 
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

	/**
	 * Defines variable in the PlutoServer core table. all sources and JSON
	 * objects are stored there.
	 * 
	 * @param id
	 * @param value
	 * @throws IOException
	 * @throws PlutoCharonException
	 */
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

	/**
	 * retrieves PlutoServer variable (JSON or source code)
	 * 
	 * @param id
	 * @return
	 * @throws IOException
	 * @throws PlutoCharonException
	 */
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
		if (length < 0) {
			throw new PlutoCharonException(
					"server value for '" + PlutoCharonConstants.LENGTH + "' was expected to be positive value!");
		}
		if (length > 0) {
			// read length content from the socket
			String content = UTF8Modified.readUTFModifiedNull(inStream);

			if (content.length() != length) {
				throw new PlutoCharonException("server document received size differ from the declared length!");
			}
			return content;
		} else {
			return null;
		}
	}

	/**
	 * Search all PlutoServer defined variables by prefix.
	 * 
	 * @param prefix
	 * @param limit
	 * @return
	 * @throws Exception
	 */
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

	/**
	 * Execute CX script on PlutoServer
	 * 
	 * @param code
	 * @return
	 * @throws Exception
	 */
	Object plutoExecute(String code) throws Exception {
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
			throw new PlutoCharonException(
					"server value for '" + PlutoCharonConstants.LENGTH + "' was expected to be positive value!");
		}

		// read length content from the socket
		String content = UTF8Modified.readUTFModifiedNull(inStream);

		if (content.length() != length) {
			throw new PlutoCharonException("server document received size differ from the declared length!");
		}

		List<Node> cxScript = Utils.asCX(content);
		if (cxScript != null) {
			return cxScript;
		}
		Map<Object, Object> json = Utils.asJSON(content);
		if (json != null) {
			return json;
		}
		return content;
	}

	/**
	 * creates a function call as CX script with all escaping and execute it on
	 * PlutoServer.
	 * 
	 * @param functionName
	 * @param arguments
	 * @return
	 * @throws Exception
	 */
	public Object plutoCall(String functionName, Object... arguments) throws Exception {
		String call = Utils.buildCall(functionName, arguments);
		return plutoExecute(call);
	}

	/**
	 * Execute CX script on the CharonClient.
	 * 
	 * @param str
	 * @return
	 */
	public Object charonExecute(String str) {
		List<Node> block = cxParser.parse(str);
		Object result = cx.evaluate(block);
		return result;
	}

	public Object charonExecute(List<Node> block) {
		Object result = cx.evaluate(block);
		return result;
	}

	/**
	 * creates a function call as CX script with all escaping and execute it on
	 * CharonClient.
	 * 
	 * @param functionName
	 * @param arguments
	 * @return
	 * @throws Exception
	 */
	public Object charonCall(String functionName, Object... arguments) throws Exception {
		String call = Utils.buildCall(functionName, arguments);
		return charonExecute(call);
	}

	/**
	 * creates a function call as CX script with all escaping and execute it on
	 * CharonClient.
	 * 
	 * @param functionName
	 * @param arguments
	 * @return
	 * @throws Exception
	 */
	public Object charonFunction(Function function, Object... arguments) throws Exception {
		return cx.callFunction(function, Arrays.asList(arguments));
	}

	public void addContextHandler(String name, Object handler) {
		cx.addHandler(new ObjectHandler(handler, name));
	}
}
