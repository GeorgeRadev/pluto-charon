package pluto;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class PlutoServer implements Runnable {
	private List<PlutoSession> sessions = new ArrayList<PlutoSession>(64);
	private Map<PlutoSession, Thread> sessionthreads = new HashMap<PlutoSession, Thread>(64);
	private ServerSocket socket;
	private boolean running = false;
	public final boolean needAuthentication;
	public final String plutoConnectionString;
	public final String plutoTable;
	public final int plutoLineLength;

	public static void main(String[] args) throws Exception {

		final PlutoServer server = new PlutoServer(-1, true);
		final Thread serverThread = new Thread(server);
		serverThread.start();
		while (!server.isRunning()) {
			Thread.yield();
		}
		System.out.println("to close this application, press Ctrl+C or kill the process: "
				+ ManagementFactory.getRuntimeMXBean().getName());
		try {
			serverThread.join();
		} catch (InterruptedException e) {
			// ok
		}
	}

	/**
	 * @param port
	 *            -1 to use the one from property file
	 * @param needAuthentication
	 *            true to require authentication
	 * @throws IOException
	 */
	public PlutoServer(int port, boolean needAuthentication) throws IOException {
		this.needAuthentication = needAuthentication;

		final Properties properties = new Properties();
		final String filename = "pluto.properties";

		// load properties
		try {
			InputStream is = new FileInputStream(filename);
			if (is != null) {
				properties.load(is);
				is.close();
			}
		} catch (Exception e) {
			Log.error("pluto.properties reading problem:", e);
		}

		if (port < 0) {
			String plutoPort = properties.getProperty("PLUTO.Port", "-1");
			try {
				port = Integer.parseInt(plutoPort, 10);
			} catch (Exception e) {
				throw new IllegalStateException("PLUTO.Port property should be defined in " + filename + "!");
			}
		}

		String dbClass = properties.getProperty("PLUTO.JDBC.Driver", "");
		String JDBSConnectionString = properties.getProperty("PLUTO.JDBC.ConnectionString", "");
		int plutoLineLength;
		{
			String lineLength = properties.getProperty("PLUTO.JDBC.Table.Line.Length", "-1");
			try {
				plutoLineLength = Integer.parseInt(lineLength, 10);
			} catch (Exception e) {
				throw new IllegalStateException("PLUTO.JDBC.Table.Line.Length property should be defined in "
						+ filename + "!");
			}

		}
		String plutoTable = properties.getProperty("PLUTO.JDBC.Table", "");

		// validate properties
		if (dbClass.length() <= 0) {
			throw new IllegalStateException("PLUTO.JDBC.Driver not defined in " + filename + "!");
		}
		Driver dbDriver;
		try {
			dbDriver = (Driver) Class.forName(dbClass).newInstance();
			DriverManager.registerDriver(dbDriver);
		} catch (Exception e) {
			throw new IllegalStateException("JDBC driver [" + dbClass + "] cannot be loaded!", e);
		}
		if (JDBSConnectionString.length() <= 0) {
			throw new IllegalStateException("PLUTO.JDBC.ConnectionString not defined in " + filename + "!");
		}

		Connection conn;
		try {
			conn = DriverManager.getConnection(JDBSConnectionString);
		} catch (Exception e) {
			throw new IllegalStateException("JDBC connection string [" + JDBSConnectionString + "] is invalid!", e);
		}

		PreparedStatement pstmt = null;
		try {
			String sql = "SELECT pluto_key, pluto_value from " + plutoTable + " where pluto_key = ?";
			pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, "pluto.core.init");

			ResultSet rs = pstmt.executeQuery();
			if (rs.next()) {
				Clob clob = rs.getClob(2);
				if (clob != null) {
					clob.getSubString(1, (int) clob.length());
				}
			}

		} catch (Exception e) {
			throw new IllegalStateException("cannot load initial script [pluto.core.init.script]!", e);

		} finally {
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (SQLException e) {
				}
				try {
					conn.close();
				} catch (SQLException e) {
				}
			}
		}

		this.socket = new ServerSocket(port);
		this.plutoConnectionString = JDBSConnectionString;
		this.plutoLineLength = plutoLineLength;
		this.plutoTable = plutoTable;
	}

	public void removeSession(PlutoSession plutoSession) {
		sessions.remove(plutoSession);
		sessionthreads.remove(plutoSession);
	}

	public void run() {
		Socket client = null;
		running = true;
		Log.log("server started. socket: " + socket);
		while (!Thread.interrupted()) {
			try {
				client = socket.accept();
				PlutoSession session = new PlutoSession(this, client);
				sessions.add(session);
				Thread thread = new Thread(session);
				thread.setName("pluto thread: " + session.id + " connection: " + client);
				sessionthreads.put(session, thread);
				thread.start();

			} catch (Throwable e) {
				Log.error("server thread error: ", e);
				e.printStackTrace();
			}
		}
		running = false;
	}

	public boolean isRunning() {
		return running;
	}
}
