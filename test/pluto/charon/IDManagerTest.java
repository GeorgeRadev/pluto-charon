package pluto.charon;


import junit.framework.TestCase;
import pluto.managers.IDManager;

public class IDManagerTest extends TestCase {
	public void testgetID() {
		// requires that the ID is a strongly upper HEX string
		String id = IDManager.getID();
		assertTrue(id.matches("[0-9A-F]+"));
	}
}
