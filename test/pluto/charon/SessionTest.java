package pluto.charon;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.util.List;
import java.util.Properties;
import junit.framework.TestCase;
import org.sqlite.JDBC;
import pluto.core.Pluto;
import pluto.managers.DBManager;
import pluto.managers.SessionManager;
import sql.helper.SQLHelperManager;
import utils.LocalAuthentication;

public class SessionTest extends TestCase {

	public void testClientSession() throws Exception {

		final Properties properties = new Properties();
		properties.load(new FileInputStream(new File(Pluto.PROPERTIES_FILE)));
		// bring up the server session with no authentication
		final int port = 12121;
		properties.put(SessionManager.SERVER_PORT, String.valueOf(port));

		{// initialize DB if required
			SQLHelperManager db = new SQLHelperManager() {
				private final JDBC driver = new JDBC();
				private final Properties driverParameters = new Properties();
				private final String connectionString;
				{
					connectionString = properties.getProperty(DBManager.DB_JDBC);
				}

				public Connection createConnection() throws Exception {
					Connection conn = null;
					conn = driver.connect(connectionString, driverParameters);
					return conn;
				}
			};
			Connection conn = db.getConnection();
			db.ddlQuery(conn, "CREATE TABLE IF NOT EXISTS PLUTO_CORE ( pluto_key, pluto_line, pluto_value)");
			db.close(conn);
		}
		DBManager dbManager = new DBManager(properties);
		SessionManager sessionManager = new SessionManager(properties, new LocalAuthentication(), dbManager);
		sessionManager.start();

		Charon client = new Charon("localhost", port, 0);
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

		// local
		assertEquals(2L, client.localExecute("1+1;"));

		// execute
		String result = client.plutoExecute("1+1;");
		assertEquals("2", result);

		// source
		result = client.plutoExecute("function func(a,b){return a+b;}");
		assertEquals("function func(a,b){return (a) + (b);}", result);

		// call
		result = client.plutoCall("'test substring'.substring", 4, 8);
		assertEquals(" sub", result);

		// frame
		result = client.plutoExecute("new {a:2,b:'str'};");
		assertEquals("{\na : 2,\nb : \"str\"\n}", result);

		// search
		client.plutoSet("id1", "value1");
		client.plutoSet("id2", "value2");
		List<String> search = client.plutoSearch("id", 2);
		assertEquals(2, search.size());
		search = client.plutoSearch("", -1);
		assertTrue(search.size() > 2);

		client.logout();
		client.close();
		sessionManager.stop();
	}
}
