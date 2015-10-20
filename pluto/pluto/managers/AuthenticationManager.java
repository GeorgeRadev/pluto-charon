package pluto.managers;

import java.sql.Connection;
import java.util.List;

import cx.Context;
import cx.ast.Node;
import pluto.charon.Utils;

public class AuthenticationManager implements IAuthenticationManager {
	private final DBManager dbManager;
	private static final String AUTHENTICATION_ROUTINE = "pluto.core.authenticate";
	private static final String AUTHORIZATION_ROUTINE = "pluto.core.authorize";

	private List<Node> authorization = null;
	private List<Node> authentication = null;

	public AuthenticationManager(DBManager dbManager) {
		this.dbManager = dbManager;
	}

	public void init() {
		Connection conn = dbManager.getConnection();
		if (conn != null) {
			String _authorization = dbManager.plutoGet(conn, AUTHORIZATION_ROUTINE);
			authorization = Utils.asCX(_authorization);

			String _authentication = dbManager.plutoGet(conn, AUTHENTICATION_ROUTINE);
			authentication = Utils.asCX(_authentication);
			dbManager.close(conn);
		} else {
			throw new IllegalStateException(dbManager.SQLError, dbManager.SQLException);
		}
	}

	@Override
	public boolean checkAuthentication(String username, String password) { 
		return true;
	}

	@Override
	public boolean checkAutorization(String username) {
		if (authorization == null) {
			return true;
		}
		Context cx = new Context();
		return true;
	}

}
