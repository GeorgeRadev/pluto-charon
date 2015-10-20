package pluto.handlers;

import java.sql.Connection;
import java.util.List;
import java.util.Map;

import cx.Context;
import cx.ast.Node;
import cx.ast.Visitor;
import cx.runtime.Handler;
import pluto.charon.Utils;
import pluto.managers.DBManager;

public class PlutoImportHandler implements Handler {
	private static final String INCLUDE = "include";
	private static final String IMPORT = "import";

	Context cx;
	DBManager dbManager;

	public PlutoImportHandler(Context cx, DBManager dbManager) {
		this.cx = cx;
		this.dbManager = dbManager;
	}

	public void init(Visitor cx) {
	}

	@Override
	public Object[] supportedClasses() {
		return null;
	}

	public String[] supportedStaticCalls() {
		return new String[] { INCLUDE, IMPORT };
	}

	@Override
	public void set(Object object, String variable, Object value) {
	}

	@Override
	public Object get(Object object, String variable) {
		return null;
	}

	@Override
	public Object call(Object object, Object[] args) {
		return null;
	}

	@Override
	public Object staticCall(String method, Object[] args) {
		if (!INCLUDE.equals(method) && !IMPORT.equals(method)) {
			return null;
		}
		Connection conn = dbManager.getConnection();
		if (conn == null) {
			return null;
		}
		Object result = null;
		for (Object arg : args) {
			if (arg == null) {
				continue;
			}
			String value = dbManager.plutoGet(conn, arg.toString());
			List<Node> code = Utils.asCX(value);
			if (code != null) {
				result = cx.evaluate(code);
				continue;
			}
			Map<Object, Object> json = Utils.asJSON(value);
			if (json != null) {
				result = json;
				continue;
			}
		}
		dbManager.close(conn);		
		return result;
	}
}
