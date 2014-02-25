package charon;

import java.util.List;
import java.util.Map;

import junit.framework.TestCase;
import pluto.PlutoServer;
import cx.runtime.ContextFrame;

public class TestConnection extends TestCase {

	public void testConnection() throws Exception {
		int port = 4444;
		Thread serverThread;
		PlutoServer server = null;
		server = new PlutoServer(port, true);
		serverThread = new Thread(server);
		serverThread.start();
		while (!server.isRunning()) {
			Thread.yield();
		}
		final Charon client = new Charon("localhost", port);

		Object result = client.executeOnServer("a = 2+2;");
		assertTrue("4".equals(result.toString()));

		result = client.executeOnServer("a +=2;");
		assertTrue("6".equals(result.toString()));

		result = client.execute("a = 2+2;");
		assertTrue("4".equals(result.toString()));

		result = client.execute("a +=2;");
		assertTrue("6".equals(result.toString()));

		client.execute("function nullFunc(){return null;}");
		result = client.execute("nullFunc();");
		assertNull(result);

		client.executeOnServer("function nullFunc(){return null;}");
		result = client.executeOnServer("nullFunc();");
		assertNull(result);

		result = client.executeOnServer("core.version();");
		System.out.println(result);
		assertNotNull(result);

		String theKey = "test_key";
		String expected;
		client.setValue(theKey, expected = "test_value");
		Object value = client.getValue(theKey);
		assertEquals(expected, value);

		client.setValue(theKey, expected = "\\  \" \n \t \r \b test");
		value = client.getValue(theKey);
		assertEquals(expected, value);

		List<?> keys;
		keys = (List<?>) client.findKeys(null);
		assertNotNull(keys);
		assertTrue(keys.contains(theKey));

		keys = (List<?>) client.findKeys("");
		assertNotNull(keys);
		assertTrue(keys.contains(theKey));

		keys = (List<?>) client.findKeys(theKey.substring(0, 4));
		assertNotNull(keys);
		assertTrue(keys.contains(theKey));

		// test import
		try {
			client.setValue(theKey = "import.demo",
					expected = "demo={a:5, getA:function(){return a;}, setA:function(b){a=b;}};");
			value = client.getValue(theKey);
			assertEquals(expected, value);

			Object t = client.execute("import('demo');");
			assertTrue(t instanceof ContextFrame);
			assertEquals(5L, ((ContextFrame) t).frame.get("a"));
			t = client.execute("demo.setA(42);demo.getA();");
			assertEquals(42L, t);

			t = client.executeOnServer("import('demo');");
			assertTrue(t instanceof Map);
			assertEquals("5", ((Map) t).get("a"));
			t = client.executeOnServer("demo.setA(42);demo.getA();");
			assertEquals(42L, t);
			t = client.executeOnServer("demo.setA(3.14);demo.getA();");
			assertEquals(3.14d, t);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
	}
}
