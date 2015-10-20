package pluto.managers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import pluto.charon.PlutoCharonException;
import pluto.charon.Utils;
import pluto.core.Log;
import sql.helper.SQLHelperManager;

public class DBManager extends SQLHelperManager {

	public static final String DB_DRIVER = "PLUTO.JDBC.Driver";
	public static final String DB_JDBC = "PLUTO.JDBC.ConnectionString";
	public static final String DB_POOL = "PLUTO.JDBC.pool";

	public static final String DB_PLUTO_TABLE = "PLUTO.JDBC.Table";
	public static final String DB_PLUTO_TABLE_LINE_SIZE = "PLUTO.JDBC.Table.Line.Length";

	private final String connectionString;
	private final String PLUTO_TABLE;
	private final int PLUTO_LINE_SIZE;

	public DBManager(Properties properties) {
		String key;
		connectionString = properties.getProperty(key = DB_JDBC, null);
		if (connectionString == null) {
			String error = "Define " + key + " in property file";
			Log.error(error);
			throw new IllegalStateException(error);
		}

		try {
			String dbDriver = properties.getProperty(key = DB_DRIVER, null);
			if (dbDriver != null && dbDriver.length() > 0) {
				Class.forName(dbDriver);
			}
		} catch (ClassNotFoundException e) {
			Log.illegalState("Defined " + key + " jdbc driver in property file cannot be loaded!", e);
		}

		{// validate threshold
			String _poolSize = properties.getProperty(key = DB_POOL, null);
			if (_poolSize != null) {
				Integer poolSize = Utils.toInteger(_poolSize);
				if (poolSize == null) {
					Log.illegalState(key + " = " + _poolSize + " should be a number");
				} else if (poolSize > 0) {
					setPoolSize(poolSize);
				}
			}
		}

		{// load table names
			String _plutoTable = properties.getProperty(key = DB_PLUTO_TABLE, null);
			if (_plutoTable == null) {
				Log.illegalState("Define " + key + " in property file");
			}
			PLUTO_TABLE = _plutoTable;
		}

		{// validate line size
			String _lineSize = properties.getProperty(key = DB_PLUTO_TABLE_LINE_SIZE, null);
			if (_lineSize == null) {
				Log.illegalState("Define " + key + " in property file");
			}
			Integer lineSize = Utils.toInteger(_lineSize);
			if (lineSize == null) {
				Log.illegalState(key + " = " + _lineSize + " should be a number");
				lineSize = 0;// not reachable
			} else if (lineSize > 0) {
				setPoolSize(lineSize);
			}
			PLUTO_LINE_SIZE = lineSize;
		}

		{// test connection and table existence
			Connection conn = getConnection();
			if (conn == null) {
				Log.illegalState("Cannot connect to Database: " + SQLError, SQLException);
			}
			try {
				ResultSet rs = query(conn, "SELECT pluto_key, pluto_line, pluto_value FROM " + PLUTO_TABLE);
				if (rs == null) {
					Log.illegalState("cannot select from table " + PLUTO_TABLE + ": " + SQLError, SQLException);
				}
			} finally {
				rollbackAndClose(conn);
			}
		}
	}

	@Override
	public Connection createConnection() throws Exception {
		return DriverManager.getConnection(connectionString);
	}

	public String plutoGet(Connection plutoConnection, String key) {
		if (plutoConnection == null) {
			Log.error(SQLError, SQLException);
			throw new IllegalStateException(SQLError, SQLException);
		}
		PreparedStatement pstmt = null;
		String value = "";

		try {
			String sql = "SELECT pluto_value from " + PLUTO_TABLE + " where pluto_key = ? order by pluto_line";
			pstmt = plutoConnection.prepareStatement(sql);
			pstmt.setString(1, key);

			ResultSet rs = pstmt.executeQuery();
			StringBuilder lines = new StringBuilder(4096);
			while (rs.next()) {
				String line = rs.getString(1);
				if (line != null) {
					lines.append(line);
				}
			}
			pstmt.close();
			value = lines.toString();

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

	public boolean plutoSet(Connection plutoConnection, String key, String value) throws PlutoCharonException {
		if (plutoConnection == null) {
			Log.error(SQLError, SQLException);
			throw new IllegalStateException(SQLError, SQLException);
		}

		PreparedStatement pstmt = null;
		try {
			plutoConnection.rollback();
			String sql = "DELETE FROM " + PLUTO_TABLE + " WHERE pluto_key = ?";
			pstmt = plutoConnection.prepareStatement(sql);
			pstmt.setString(1, key);
			pstmt.executeUpdate();
			pstmt.close();

			if (value != null && value.length() > 0) {
				final int length = value.length();
				final int lines = (length / PLUTO_LINE_SIZE) + 1;
				int start = 0;
				for (int line = 0; line < lines; ++line) {
					int end = start + PLUTO_LINE_SIZE;
					if (end > length) {
						end = length;
					}
					sql = "INSERT INTO " + PLUTO_TABLE + "(pluto_key, pluto_line, pluto_value) VALUES(?,?,?)";
					pstmt = plutoConnection.prepareStatement(sql);
					pstmt.setString(1, key);
					pstmt.setInt(2, line);
					pstmt.setString(3, value.substring(start, end));
					pstmt.executeUpdate();
					pstmt.close();
					start = end;
				}
				pstmt = null;
			}
			plutoConnection.commit();
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
			throw new PlutoCharonException(e);
		}
	}

	public List<String> plutoSearch(Connection connection, String prefix, int limit) {
		if (connection == null) {
			Log.error(SQLError, SQLException);
			throw new IllegalStateException(SQLError, SQLException);
		}
		List<String> result = new ArrayList<String>();
		if (prefix == null) {
			prefix = "";
		}
		PreparedStatement pstmt = null;
		try {
			String sql = "SELECT distinct pluto_key from " + PLUTO_TABLE + " where pluto_key like ?";
			pstmt = connection.prepareStatement(sql);
			pstmt.setString(1, prefix + "%");

			ResultSet rs = pstmt.executeQuery();
			while (rs.next() && limit != 0) {
				String line = rs.getString(1);
				if (line != null) {
					result.add(line);
					limit--;
				}
			}
			pstmt.close();
		} catch (Exception e) {
			Log.error("plutoSearch issue:", e);
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
