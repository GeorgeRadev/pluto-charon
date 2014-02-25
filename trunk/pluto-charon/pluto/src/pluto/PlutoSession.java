package pluto;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.Map;

import json.JSONBuilder;
import json.JSONDOMParser;
import cx.Context;
import cx.Parser;
import cx.ast.Node;
import cx.runtime.ContextFrame;
import cx.runtime.DateHandler;
import cx.runtime.MathHandler;
import cx.runtime.ObjectHandler;

public class PlutoSession implements Runnable {
	final PlutoServer pluto;
	private final Socket socket;
	private final DataOutputStream outStream;
	private final DataInputStream inStream;
	private final JSONDOMParser JSONParser;
	private final JSONBuilder jsonBuilder;
	private final Context cx;
	private final PlutoCore core;

	// id generator for the threads
	private static int idSequence = 0;
	final int id = ++idSequence;

	public PlutoSession(PlutoServer pluto, Socket clientsocket) throws IOException {
		this.pluto = pluto;
		socket = clientsocket;
		outStream = new DataOutputStream(socket.getOutputStream());
		inStream = new DataInputStream(socket.getInputStream());
		JSONParser = new JSONDOMParser();
		jsonBuilder = new JSONBuilder();

		cx = new Context();
		this.core = new PlutoCore(this);
		cx.addHandler(new DateHandler());
		cx.addHandler(new MathHandler());
		cx.addHandler(new ObjectHandler(core, "core"));
		cx.addHandler(new ImportHandler(core));
	}

	@Override
	public int hashCode() {
		return id;
	}

	@SuppressWarnings("unchecked")
	public void run() {
		try {
			if (pluto.needAuthentication) {
				// login expected
				outStream.writeUTF("{type: authentication, message:'send user and password'}");
				Object result = JSONParser.parse(inStream.readUTF());
				if (result instanceof Map) {
					Map<Object, Object> map = (Map<Object, Object>) result;
					String user = map.get("user").toString();
					String password = map.get("password").toString();
					if (user != null && password != null) {
						if (!validAuthentication(user, password)) {
							throw new Exception("provided 'user' and 'password' are invalid!");
						}
						outStream.writeUTF("{type: ok}");
					} else {
						throw new Exception("JSON Object does not have 'user' and 'password' attributes!");
					}
				} else {
					throw new Exception("provided string is not a JSON Object!");
				}
			} else {
				outStream.writeUTF("{type: ok}");
			}

			while (true) {
				String inString = inStream.readUTF();
				try {
					Object result;
					{
						final Parser parser = new Parser(inString);
						List<Node> block = parser.parse();
						result = cx.evaluate(block);
					}
					final String resultStr;
					if (result == null) {
						resultStr = "null";
					} else if (result instanceof ContextFrame) {
						jsonBuilder.reset();
						JSONBuilder.objectToJSON(jsonBuilder, ((ContextFrame) result).frame);
						resultStr = jsonBuilder.toString();
					} else {
						jsonBuilder.reset();
						JSONBuilder.objectToJSON(jsonBuilder, result);
						resultStr = jsonBuilder.toString();
					}

					outStream.writeUTF(resultStr);
				} catch (Exception e) {
					jsonBuilder.reset().startObject();
					jsonBuilder.addKey("error").addValue(e.getMessage());
					jsonBuilder.addKey("type").addValue(e.getClass().getName());
					jsonBuilder.endObject();
					outStream.writeUTF(jsonBuilder.toString());
				}
			}

		} catch (SocketException e) {
			Log.error("socket:", e);
			return;

		} catch (IOException e) {
			Log.error("io:", e);
			return;

		} catch (Throwable e) {
			try {
				jsonBuilder.reset().startObject();
				jsonBuilder.addKey("error").addValue(e.getMessage());
				jsonBuilder.addKey("type").addValue(e.getClass().getName());
				jsonBuilder.endObject();
				outStream.writeUTF(jsonBuilder.toString());
			} catch (IOException x) {
			}
		} finally {
			try {
				socket.close();
			} catch (IOException e) {
			}
			pluto.removeSession(this);
		}
	}

	Object execute(String str) {
		Object result = cx.evaluate((new Parser(str)).parse());
		return result;
	}

	private Object callfunction(String functionName, Object... parameters) {
		StringBuilder call = new StringBuilder(2048);
		call.append(functionName).append('(');
		if (parameters.length > 0) {
			for (Object obj : parameters) {
				call.append(JSONBuilder.objectToJSON(obj)).append(',');
			}
			call.setLength(call.length() - 1);
		}
		call.append(");");
		List<Node> theCall = new Parser(call.toString()).parse();
		return cx.evaluate(theCall);
	}

	private boolean validAuthentication(String user, String password) {
		try {
			String authFunction = core.getValue(PlutoCore.PLUTO_CORE_AUTHENTICATION);
			List<Node> auth = new Parser(authFunction).parse();
			cx.evaluate(auth);
			Object result = callfunction(PlutoCore.PLUTO_CORE_AUTHENTICATION, user, password);
			return result == null || Context.isTrue(result);
		} catch (Exception e) {
			return true;
		}
	}
}
