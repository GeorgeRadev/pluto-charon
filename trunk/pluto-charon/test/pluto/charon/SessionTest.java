package pluto.charon;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.sqlite.JDBC;

import cx.Parser;
import cx.ast.Node;
import junit.framework.TestCase;
import pluto.core.Pluto;
import pluto.managers.DBManager;
import pluto.managers.SessionManager;
import sql.helper.SQLHelperManager;
import utils.LocalAuthentication;

public class SessionTest extends TestCase {

	public static class LocalDB extends SQLHelperManager {
		private final JDBC driver = new JDBC();
		private final Properties driverParameters = new Properties();
		private final String connectionString;

		LocalDB(Properties properties) {
			connectionString = properties.getProperty(DBManager.DB_JDBC);
		}

		public Connection createConnection() throws Exception {
			Connection conn = null;
			conn = driver.connect(connectionString, driverParameters);
			return conn;
		}
	}

	static DBManager dbManager = null;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		if (dbManager == null) {
			// initialize DB if required
			final Properties properties = new Properties();
			properties.load(new FileInputStream(new File(Pluto.PROPERTIES_FILE)));

			SQLHelperManager db = new LocalDB(properties);
			Connection conn = db.getConnection();
			db.ddlQuery(conn, "CREATE TABLE IF NOT EXISTS PLUTO_CORE (pluto_key, pluto_line, pluto_value)");
			db.close(conn);

			dbManager = new DBManager(properties);
		}
	}

	public void testClientSessionSSL() throws Exception {
		System.gc();
		final Properties properties = new Properties();
		properties.load(new FileInputStream(new File(Pluto.PROPERTIES_FILE)));
		properties.setProperty(SessionManager.SERVER_CONTEXT_TYPE, "SSL");

		// bring up the server session with no authentication
		final int port = 12121;
		properties.put(SessionManager.SERVER_PORT, String.valueOf(port));

		SessionManager sessionManager = new SessionManager(properties, new LocalAuthentication(), dbManager);
		sessionManager.start();

		Socket sslClientSocket = Charon.createSSLCocket("localhost", port, 0, "JKS", "SSL", "charon.jks",
				"pluto-charon");

		Charon client = new Charon(sslClientSocket);

		comunicationScenario(client);

		client.logout();
		client.close();
		sessionManager.stop();
	}

	public void testClientSession() throws Exception {
		System.gc();
		final Properties properties = new Properties();
		properties.load(new FileInputStream(new File(Pluto.PROPERTIES_FILE)));
		properties.remove(SessionManager.SERVER_CONTEXT_TYPE);

		// bring up the server session with no authentication
		final int port = 12121;
		properties.put(SessionManager.SERVER_PORT, String.valueOf(port));

		SessionManager sessionManager = new SessionManager(properties, new LocalAuthentication(), dbManager);
		sessionManager.start();

		Charon client = new Charon("localhost", port, 0);

		comunicationScenario(client);

		client.logout();
		client.close();
		sessionManager.stop();
	}

	void comunicationScenario(Charon client) throws Exception {
		{ // connection
			client.ping();
			client.login("test", "test");
			client.ping();
			client.ping();
			client.login("newUser", "test");
			client.ping();

			String expect;
			client.plutoSet("id", expect = "value");
			String value = client.plutoGet("id");
			assertEquals(expect, value);
			client.ping();
		}

		{// local
			assertEquals(2L, client.charonExecute("1+1;"));

			// execute
			Object result = client.plutoExecute("1+1;");
			assertEquals("2", result);

			// source
			String source = "function func(a,b){return (a) + (b);}";
			result = client.plutoExecute(source);
			assertTrue(result instanceof List);
			assertEquals(source, ((List<?>) result).get(0).toString());

			// call
			result = client.plutoCall("'test substring'.substring", 4, 8);
			assertEquals(" sub", result);

			// frame
			result = client.plutoExecute("new {a:2,b:'str'};");
			assertTrue(result instanceof Map);
			assertEquals("2", ((Map<?, ?>) result).get("a").toString());
			assertEquals("str", ((Map<?, ?>) result).get("b").toString());

			// search
			client.plutoSet("id1", "value1");
			client.plutoSet("id2", "value2");
			List<String> search = client.plutoSearch("id", 2);
			assertEquals(2, search.size());
			for(String line: search){
				assertTrue(line.startsWith("id"));
			}
			
			search = client.plutoSearch("", -1);
			assertTrue(search.size() > 2);
		}

		{// import
			client.plutoSet("object1", "{a:1,b:'string'}");
			Object obj = client.charonExecute("obj = import('object1');");
			assertTrue(obj instanceof Map);
			String str = String.valueOf(client.charonExecute("obj.b;"));
			assertEquals("string", str);
		}

		{// include
			client.plutoSet("object1", "{a:1,b:'string'}");
			Object obj = client.charonExecute("obj = include('object1');");
			assertTrue(obj instanceof Map);
			String str = String.valueOf(client.charonExecute("obj.b;"));
			assertEquals("string", str);
		}

		{// import
			client.plutoSet("object1", "{a:1,b:'string'}");
			Object obj = client.plutoExecute("obj = import('object1');");
			assertTrue(obj instanceof Map);
			String str = String.valueOf(client.plutoExecute("obj.b;"));
			assertEquals("string", str);
		}

		{// include
			client.plutoSet("object1", "{a:1,b:'string'}");
			Object obj = client.plutoExecute("obj = include('object1');");
			assertTrue(obj instanceof Map);
			String str = String.valueOf(client.plutoExecute("obj.b;"));
			assertEquals("string", str);
		}

		{// put a default program to server
			String programName = "demo";
			String program = fileToString("./test/demo.cx");
			client.plutoSet(programName, program);
			String str = String.valueOf(client.plutoGet(programName));
			assertEquals(program, str);
			String programCode = client.plutoGet(programName);
			List<Node> programAST = Utils.asCX(programCode);
			client.charonExecute(programAST);
		}
		{// put a default program to server
			String programName = "editor";
			String program = fileToString("./test/editor.cx");
			client.plutoSet(programName, program);
			String str = String.valueOf(client.plutoGet(programName));
			assertEquals(program, str);
			String programCode = client.plutoGet(programName);
			Parser cxParser = new Parser();
			cxParser.parse(programCode);
		}
	}

	String fileToString(String fileName) throws Exception {
		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "UTF8"));
		String line = null;
		StringBuilder stringBuilder = new StringBuilder();

		while ((line = reader.readLine()) != null) {
			stringBuilder.append(line);
			stringBuilder.append('\n');
		}
		reader.close();
		return stringBuilder.toString();

	}
}
