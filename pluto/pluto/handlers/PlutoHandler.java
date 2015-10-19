package pluto.handlers;

import pluto.managers.ClientSession;
import pluto.managers.DBManager;
import cx.ast.Visitor;
import cx.runtime.Handler;

/**
 * All server functions should be defined here
 */
public class PlutoHandler implements Handler {
	public static final String VERSION = "pluto.0.0.1";
	static final String PLUTO_CORE_INIT = "pluto_core_init";
	public static final String PLUTO_CORE_AUTHENTICATION = "pluto_core_authentication";

	private static final String IMPORT = "import";

	final ClientSession plutoSession;
	final DBManager dbManager;

	// private final Connection plutoConnection;

	public PlutoHandler(ClientSession plutoSession, DBManager dbManager) {
		this.plutoSession = plutoSession;
		this.dbManager = dbManager;
		// plutoConnection = dbManager.getConnection();
		// if (dbManager.SQLError != null) {
		// throw new IllegalStateException(dbManager.SQLError,
		// dbManager.SQLException);
		// }
	}

	public String version() {
		return VERSION;
	}

	public void init(Visitor cx) {}

	public void set(Object object, String variable, Object value) {}

	public Object get(Object object, String variable) {
		return null;
	}

	public Object call(Object object, Object[] args) {
		return null;
	}

	public boolean acceptStaticCall(String method, Object[] args) {
		return IMPORT.equals(method);
	}

	public Object[] supportedClasses() {
		return null;
	}

	public String[] supportedStaticCalls() {
		return new String[] { IMPORT };
	}

	public Object staticCall(String method, Object[] args) {
		if (IMPORT.equals(method)) {
			Object result = null;
			for (Object arg : args) {
				if (arg != null) {
					try {
						// Object importObj = core.getValue(IMPORT + "." +
						// arg.toString());
						// if (importObj instanceof String) {
						// result = core.plutoSession.execute((String)
						// importObj);
						// }
					} catch (Exception e) {}
				}
			}
			return result;
		}
		return null;
	}
}
