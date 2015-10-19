package pluto.managers;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UTFDataFormatException;
import java.net.Socket;
import java.net.SocketException;
import java.sql.Connection;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import json.JSONBuilder;
import json.JSONParser;
import pluto.charon.PlutoCharonConstants;
import pluto.charon.PlutoCharonException;
import pluto.charon.UTF8Modified;
import pluto.core.Log;
import pluto.handlers.PlutoHandler;
import cx.Context;
import cx.Parser;
import cx.ast.Node;
import cx.handlers.DatabaseHandler;
import cx.handlers.DateHandler;
import cx.handlers.MathHandler;
import cx.handlers.StringHandler;
import cx.handlers.SystemHandler;

public class ClientSession implements Runnable {
	private final Socket socket;
	private final DataOutputStream outStream;
	private final DataInputStream inStream;
	private final JSONParser jsonParser;
	private final JSONBuilder jsonBuilder;
	private final Context cx;
	private final Parser cxParser;
	private final PlutoHandler core;
	private final SessionManager sessionManager;
	private final DBManager dbManager;
	private final Connection connection;

	// id generator for the threads
	private static int idSequence = 0;
	private boolean authorized = false;
	private String currentUser = null;
	final int id = ++idSequence;

	public ClientSession(Socket clientsocket, SessionManager sessionManager, DBManager dbManager) throws IOException {
		socket = clientsocket;
		this.sessionManager = sessionManager;
		this.dbManager = dbManager;
		this.connection = dbManager.beginTransaction();
		outStream = new DataOutputStream(socket.getOutputStream());
		inStream = new DataInputStream(socket.getInputStream());
		cxParser = new Parser();
		cxParser.supportSQLEscaping = true;
		cxParser.supportTryCatchThrow = true;
		jsonParser = new JSONParser();
		jsonBuilder = new JSONBuilder();

		cx = new Context();
		core = new PlutoHandler(this, dbManager);
		cx.addHandler(core);
		cx.addHandler(new StringHandler());
		cx.addHandler(new DateHandler());
		cx.addHandler(new MathHandler());
		cx.addHandler(new DatabaseHandler());
		cx.addHandler(new SystemHandler("system"));
		Log.log("new session started: " + clientsocket);
	}

	public void run() {
		clientSessionHandler();
	}

	private void clientSessionHandler() {
		boolean ever = true;
		try {
			for (; ever;) {
				try {
					Map<Object, Object> actionMessage = null;
					{
						String actionMessageStr = null;
						try {
							// read \0 terminated JSON from the socket
							actionMessageStr = UTF8Modified.readUTFModifiedNull(inStream);
						} catch (UTFDataFormatException e) {
							String msg = "problem with UTF format message: ";
							Log.error(msg, e);
							// read till \0 on error
							while (inStream.read() != 0) {}
							returnError(msg + e.getMessage(), PlutoCharonConstants.STACKTRACE,
									Arrays.asList(e.getStackTrace()).toString());
							continue;
						} catch (SocketException e) {
							// when there is no connection or transfer issue:
							// just close the session
							// Log.error("socket error:", e);
							ever = false;
							return;

						} catch (IOException e) {
							String msg = "problem with reading JSON message: ";
							Log.error(msg, e);
							returnError(msg + e.getMessage(), PlutoCharonConstants.STACKTRACE,
									Arrays.asList(e.getStackTrace()).toString());
							continue;
						}
						if (actionMessageStr == null || actionMessageStr.length() <= 0) {
							// empty message
							// usually when the connection is closed
							break;
						}

						Map<Object, Object> actionObject = null;
						try {
							// parse the message
							actionObject = jsonParser.parseJSONString(actionMessageStr);

						} catch (Exception e) {
							String msg = "problem with JSON message: ";
							Log.error(msg, e);
							returnError(msg + e.getMessage(), PlutoCharonConstants.STACKTRACE,
									Arrays.asList(e.getStackTrace()).toString());
						}
						if (!(actionObject instanceof Map)) {
							String msg = "problem with JSON message: message should be a single object!";
							Log.error(msg);
							returnError(msg);
						} else {
							actionMessage = actionObject;
						}
					}
					// interpret the message
					if (actionMessage != null) {
						try {
							performAction(actionMessage);
						} catch (PlutoCharonException e) {
							try {
								returnError("issue with the session: " + e.getMessage());
							} catch (IOException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
							continue;

						}
					}
				} catch (SocketException e) {
					// when there is no connection or transfer issue:
					// just close the session
					// Log.error("socket error:", e);
					ever = false;
					return;

				} catch (IOException e) {
					Log.error("io error:", e);
					ever = false;
					return;

				} catch (Throwable e) {
					Log.error("issue with the session: " + e.getMessage(), e);
					try {
						returnError("issue with the session: " + e.getMessage());
					} catch (Exception x) {
						Log.error("issue with reporting session error: ", e);
						ever = false;
					}
				}
			}// for ever
		} finally {
			try {
				socket.shutdownInput();
			} catch (IOException e) {}
			try {
				socket.shutdownOutput();
			} catch (IOException e) {}
			try {
				socket.close();
			} catch (IOException e) {}

			sessionManager.detachSession(this);
		}
	}

	private void returnOk(String string, Object... parameters) throws IOException {
		jsonBuilder.reset().startObject();
		jsonBuilder.addKey(PlutoCharonConstants.OK).addValue(true);
		jsonBuilder.addKey(PlutoCharonConstants.MESSAGE).addValue(string);
		if (parameters != null && parameters.length >= 2 && parameters.length % 2 == 0) {
			// parameters should be positive even number
			for (int i = 0; i < parameters.length; i += 2) {
				jsonBuilder.addKey(String.valueOf(parameters[i])).addValue(parameters[i + 1]);
			}
		}
		jsonBuilder.endObject();
		UTF8Modified.writeUTFModifiedNull(jsonBuilder.toString(), outStream);
		outStream.flush();
	}

	private void returnError(String string, Object... parameters) throws IOException {
		jsonBuilder.reset().startObject();
		jsonBuilder.addKey(PlutoCharonConstants.OK).addValue(false);
		jsonBuilder.addKey(PlutoCharonConstants.MESSAGE).addValue(string);
		if (parameters != null && parameters.length >= 2 && parameters.length % 2 == 0) {
			// parameters should be positive even number
			for (int i = 0; i < parameters.length; i += 2) {
				jsonBuilder.addKey(String.valueOf(parameters[i])).addValue(parameters[i + 1]);
			}
		}
		jsonBuilder.endObject();
		UTF8Modified.writeUTFModifiedNull(jsonBuilder.toString(), outStream);
		outStream.flush();
	}

	public String getCurrentUser() {
		return currentUser;
	}

	private void performAction(Map<Object, Object> actionMessage) throws IOException, PlutoCharonException {
		if (actionMessage == null) {
			// leave the method
			// this case should never happened
			return;

		}
		String action = String.valueOf(actionMessage.get(PlutoCharonConstants.ACTION));

		if (PlutoCharonConstants.ACTION_PING.equals(action)) {
			returnOk("pong");

		} else if (PlutoCharonConstants.ACTION_LOGIN.equals(action)) {
			doLogin(actionMessage);

		} else if (!authorized) {
			returnError("no correct authenticating message was recieved!");

			// now we can process any other messages that are send
		} else if (PlutoCharonConstants.ACTION_GET.equals(action)) {
			doGet(actionMessage);

		} else if (PlutoCharonConstants.ACTION_SET.equals(action)) {
			doSet(actionMessage);

		} else if (PlutoCharonConstants.ACTION_EXECUTE.equals(action)) {
			doExecute(actionMessage);

		} else if (PlutoCharonConstants.ACTION_SEARCH.equals(action)) {
			doSearch(actionMessage);

		}
	}

	private void doLogin(Map<Object, Object> actionMessage) throws IOException, PlutoCharonException {
		String user = PlutoCharonConstants.getMessageString(actionMessage, PlutoCharonConstants.USER);
		String pass = PlutoCharonConstants.getMessageString(actionMessage, PlutoCharonConstants.PASSWORD);

		authorized = true;
		// checkAuthentication(currentUser = user.toString(), pass.toString());
		if (authorized) {
			returnOk("login successful!");
		} else {
			returnError("no correct authenticating message was recieved!");
		}
	}

	private void doGet(Map<Object, Object> actionMessage) throws IOException, PlutoCharonException {
		String id = PlutoCharonConstants.getMessageString(actionMessage, PlutoCharonConstants.ID);

		String value;
		try {
			value = dbManager.plutoGet(connection, id);
		} catch (Exception e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
		returnOk("pluto get was successful!", PlutoCharonConstants.LENGTH, value.length());
		UTF8Modified.writeUTFModifiedNull(value, outStream);
		outStream.flush();
	}

	private void doSet(Map<Object, Object> actionMessage) throws IOException, PlutoCharonException {
		String id = PlutoCharonConstants.getMessageString(actionMessage, PlutoCharonConstants.ID);
		int length = PlutoCharonConstants.getMessageInt(actionMessage, PlutoCharonConstants.LENGTH);

		String content = null;
		if (length > 0) {
			// read data after the message if everything looks fine
			content = UTF8Modified.readUTFModifiedNull(inStream);
			if (content == null) {
				throw new IllegalStateException("pluto set content is null!");
			} else if (content.length() != length) {
				throw new IllegalStateException("server received size " + content.length()
						+ " differ from the declared length " + length + "!");
			}
		}

		try {
			dbManager.plutoSet(connection, id, content);
		} catch (Exception e) {
			throw new IllegalStateException(e.getMessage(), e);
		}

		returnOk("pluto set was successful!");
	}

	private void doExecute(Map<Object, Object> actionMessage) throws IOException, PlutoCharonException {
		int length = PlutoCharonConstants.getMessageInt(actionMessage, PlutoCharonConstants.LENGTH);
		if (length < 0) {
			throw new PlutoCharonException("Invalid '" + PlutoCharonConstants.LENGTH + "' = [" + length
					+ "]! Should be positive integer number!");
		}
		// read data after the message if everything looks fine
		String content = UTF8Modified.readUTFModifiedNull(inStream);
		if (content == null) {
			throw new IllegalStateException("pluto set content is null!");
		} else if (content.length() != length) {
			throw new IllegalStateException("server received size " + content.length()
					+ " differ from the declared length " + length + "!");
		}
		try {
			List<Node> nodes = cxParser.parse(content);
			Object result = cx.evaluate(nodes);
			final String resultString;
			if (result == null) {
				resultString = "null";
			} else if (result instanceof String) {
				resultString = (String) result;
			} else if (result instanceof Number) {
				resultString = ((Number) result).toString();
			} else if (result instanceof Map || result instanceof List) {
				jsonBuilder.reset();
				jsonBuilder.addValue(result);
				resultString = jsonBuilder.toString();
			} else {
				resultString = result.toString();
			}
			returnOk("execution was successful!", PlutoCharonConstants.LENGTH, resultString.length());
			UTF8Modified.writeUTFModifiedNull(resultString, outStream);

		} catch (Throwable e) {
			throw new PlutoCharonException(e.getMessage(), e);
		}
	}

	private void doSearch(Map<Object, Object> actionMessage) throws IOException, PlutoCharonException {
		String prefix = "";
		{
			Object _prefix = actionMessage.get(PlutoCharonConstants.PREFIX);
			if (_prefix != null) {
				prefix = prefix.toString();
			}
		}
		int limit = PlutoCharonConstants.getMessageInt(actionMessage, PlutoCharonConstants.LIMIT);

		List<String> result;
		try {
			result = dbManager.plutoSearch(connection, prefix, limit);
		} catch (Exception e) {
			throw new IllegalStateException(e.getMessage(), e);
		}

		returnOk("search was successful!", PlutoCharonConstants.LENGTH, result.size(), PlutoCharonConstants.RESULT,
				result);
	}
}