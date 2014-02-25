package pluto;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * All server functions should be defined here
 */
public class PlutoCore {
	public static final String VERSION = "pluto.0.0.1";
	static final String PLUTO_CORE_INIT = "pluto_core_init";
	static final String PLUTO_CORE_AUTHENTICATION = "pluto_core_authentication";

	final PlutoSession plutoSession;
	private final Connection plutoConnection;
	private final Map<String, String> keyValueCache;

	PlutoCore(PlutoSession plutoSession) {
		this.plutoSession = plutoSession;
		Connection conn;
		try {
			conn = DriverManager.getConnection(plutoSession.pluto.plutoConnectionString);
			conn.setAutoCommit(false);
		} catch (SQLException e) {
			conn = null;
			throw new IllegalStateException("Cannot connect to database!", e);
		}
		plutoConnection = conn;
		keyValueCache = new WeakHashMap<String, String>(512);
	}

	public String version() {
		return VERSION;
	}

	public String getGUID() {
		return UUID.randomUUID().toString();
	}

	public String getValue(String key) throws Exception {
		if (plutoConnection == null) {
			return "";
		}
		PreparedStatement pstmt = null;
		String value = keyValueCache.get(key);
		if (value != null) {
			return value;
		}
		value = "";

		try {
			String sql = "SELECT pluto_key, pluto_value from " + plutoSession.pluto.plutoTable
					+ " where pluto_key = ? order by pluto_line";
			pstmt = plutoConnection.prepareStatement(sql);
			pstmt.setString(1, key);

			ResultSet rs = pstmt.executeQuery();
			StringBuilder lines = new StringBuilder(4096);
			if (rs.next()) {
				String line = rs.getString(2);
				if (line != null) {
					lines.append(line);
				}
			}
			pstmt.close();
			keyValueCache.put(key, value = lines.toString());

		} catch (Exception e) {
			Log.error("getValue issue:", e);
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (SQLException x) {
				}
			}
		}
		return value;
	}

	public boolean setValue(String key, String value) throws Exception {
		if (plutoConnection == null) {
			return false;
		}

		PreparedStatement pstmt = null;
		try {
			plutoConnection.rollback();
			String sql = "DELETE FROM " + plutoSession.pluto.plutoTable + " WHERE pluto_key = ?";
			pstmt = plutoConnection.prepareStatement(sql);
			pstmt.setString(1, key);
			pstmt.executeUpdate();
			pstmt.close();

			final int length = value.length();
			final int lines = (length / plutoSession.pluto.plutoLineLength) + 1;
			int start = 0;
			for (int line = 0; line < lines; ++line) {
				int end = start + plutoSession.pluto.plutoLineLength;
				if (end > length) {
					end = length;
				}
				sql = "INSERT INTO " + plutoSession.pluto.plutoTable
						+ "(pluto_key, pluto_line, pluto_value) VALUES(?,?,?)";
				pstmt = plutoConnection.prepareStatement(sql);
				pstmt.setString(1, key);
				pstmt.setInt(2, line);
				pstmt.setString(3, value.substring(start, end));
				pstmt.executeUpdate();
				pstmt.close();
				start = end;
			}
			pstmt = null;
			plutoConnection.commit();
			keyValueCache.put(key, value);
			return true;

		} catch (Throwable e) {
			Log.error("setValue issue:", e);
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (SQLException x) {
				}
			}
			try {
				plutoConnection.rollback();
			} catch (SQLException x) {
			}
			throw new Exception(e);
		}
	}

	public Object findKeys(String prefix) throws Exception {
		List<String> result = new ArrayList<String>();
		if (plutoConnection == null) {
			return result;
		}
		if (prefix == null) {
			prefix = "";
		}
		PreparedStatement pstmt = null;		 
		try {
			String sql = "SELECT distinct pluto_key from " + plutoSession.pluto.plutoTable + " where pluto_key like ?";
			pstmt = plutoConnection.prepareStatement(sql);
			pstmt.setString(1, prefix + "%");

			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				String line = rs.getString(1);
				if (line != null) {
					result.add(line);
				}
			}
			pstmt.close();
		} catch (Exception e) {
			Log.error("findKeys issue:", e);
			if (pstmt != null) {
				try {
					pstmt.close();
				} catch (SQLException x) {
				}
			}
		}
		return result;
	}
}
