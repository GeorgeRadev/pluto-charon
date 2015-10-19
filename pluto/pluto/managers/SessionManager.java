package pluto.managers;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import pluto.charon.Utils;
import pluto.core.Log;

public class SessionManager implements Runnable {
	public static final String SERVER_PORT = "PLUTO.Port";

	private boolean running = false;
	private final int port;

	private ServerSocket socket;
	private final Thread serverThread;
	private final List<ClientSession> sessions = new ArrayList<ClientSession>(64);
	private final Map<ClientSession, Thread> sessionthreads = new ConcurrentHashMap<ClientSession, Thread>(64);
	private final DBManager dbManager;
	private final IAuthenticationManager authenticationManager;

	public SessionManager(Properties properties, IAuthenticationManager authenticationManager, DBManager dbManager) {
		String key;
		String portStr = properties.getProperty(key = SERVER_PORT, null);
		if (portStr == null) {
			Log.illegalState("Define " + key + " in property file");
		}
		{
			int portInt = 0;
			Integer portObj = Utils.toInteger(portStr);
			if (portObj == null) {
				Log.illegalState(SERVER_PORT + " = " + portStr + " should be a number");
			} else {
				portInt = portObj.intValue();
			}

			this.port = portInt;
		}
		this.dbManager = dbManager;
		this.authenticationManager = authenticationManager;
		this.serverThread = new Thread(this);
	}

	public void start() throws IOException {
		socket = new ServerSocket(port);
		// run listening thread for handling client connections
		serverThread.start();
	}

	public void stop() throws IOException {
		socket.close();
		serverThread.interrupt();
		for (Thread sessionThread : sessionthreads.values()) {
			sessionThread.interrupt();
		}
		sessionthreads.clear();
	}

	public boolean isRunning() {
		return running;
	}

	@Override
	public void run() {
		Socket client = null;
		running = true;
		Log.log("server started at socket: " + socket);
		while (!Thread.interrupted()) {
			try {
				// attach new session
				client = socket.accept();
				ClientSession session = new ClientSession(client, this, dbManager);
				sessions.add(session);
				Thread thread = new Thread(session);
				thread.setName("sesion thread: " + session.id + " connection: " + client);
				sessionthreads.put(session, thread);
				thread.start();

			} catch (SocketException e) {
				if (socket.isClosed()) {
					break;
				} else {
					Log.error("server thread error: ", e);
				}
			} catch (Throwable e) {
				Log.error("server thread error: ", e);
			}
			Thread.yield();
		}
		running = false;
	}

	public void detachSession(ClientSession plutoSession) {
		sessions.remove(plutoSession);
		sessionthreads.remove(plutoSession);
	}
}
