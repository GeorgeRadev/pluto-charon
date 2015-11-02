package pluto.managers;

/**
 * All server functions should be defined here
 */
public class PlutoCore {
	public static final String VERSION = "pluto.0.0.1";

	final ClientSession plutoSession;

	// private final Connection plutoConnection;

	public PlutoCore(ClientSession plutoSession) {
		this.plutoSession = plutoSession;
	}

	public String version() {
		return VERSION;
	}

	public Object get(String key) {
		return plutoSession.get(key);
	}

	public void set(String key, String value) {
		plutoSession.set(key, value);
	}

	public Object search(String prefix) throws Exception {
		return plutoSession.search(prefix, 100);
	}

	public Object search(String prefix, int limit) throws Exception {
		return plutoSession.search(prefix, limit);
	}
}
