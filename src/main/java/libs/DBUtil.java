package libs;

import org.apache.log4j.Logger;

import java.sql.*;
import java.util.Collection;

public class DBUtil {
	static Logger log = Logger.getLogger(DBUtil.class.getName());

	/*
	 * all methods are static,  
	 *   so prevent instantiation.
	 */
	private DBUtil() {};

	/**
	 * Attempt to update <stmt>.
	 * Retry up to <attempts> times before throwing SQLException
	 * On error Log <message>
	 * 
	 * @param stmt PreparedStatement
	 * @param message Message to log on failure
	 * @param attempts how many times to retry before giving up
	 * @return number of records updated.
	 * @throws java.sql.SQLException
	 */
	public static int update(PreparedStatement stmt, String message, int attempts) throws SQLException {
		boolean retry = false;
		int retryCount = attempts;
		SQLException lastException = null;;
		do {
			try {
				return stmt.executeUpdate();
			} catch(SQLException e) {
				lastException = e;
				if(e.getErrorCode() == 1205 /* ER_LOCK_WAIT_TIMEOUT */) {
					retry = true;
					retryCount--;
					log.error(e.getMessage() + ": " + message + " - retrying.");
				}
			}
		} while(retry && (retryCount > 0));
		log.error("Unable to update successfully in " + attempts + " attempts.", lastException);
		throw lastException;
	}

	/**
	 * Attempt to execute <sql> update using <stmt>.
	 * Retry up to <attempts> times before throwing SQLException
	 * On error Log <message>
	 *
	 * @param stmt Statement object to use for update
	 * @param sql SQL update query
	 * @param message Message to log on failure
	 * @param attempts how many times to retry before giving up
	 * @return number of records updated.
	 * @throws java.sql.SQLException
	 */
	public static int update(Statement stmt, String sql, String message, int attempts) throws SQLException {
		boolean retry = false;
		int retryCount = attempts;
		SQLException lastException = null;;
		do {
			try {
				return stmt.executeUpdate(sql);
			} catch(SQLException e) {
				lastException = e;
				if(e.getErrorCode() == 1205 /* ER_LOCK_WAIT_TIMEOUT */) {
					retry = true;
					retryCount--;
					log.error(e.getMessage() + ": " + message + " - retrying.");
				}
			}
		} while(retry && (retryCount > 0));
		log.error("Unable to update successfully in " + attempts + " attempts.", lastException);
		throw lastException;
	}

	/*
	 * Returns a single non-Pooled connection to the specified MySQL database
	 * Typically used to connect to external databases 
	 * 
	 * @param url      - url to db(ex. <serverName>/<schema>)
	 * @param user 	   - username if db is protected
	 * @param password - password if db is protected
	 * @return Connection
	 */
	public static Connection getMySQLConnection(String url, String user, String password) throws Exception {
		// load MySQL Driver
		Class.forName("com.mysql.jdbc.Driver").newInstance();
		String database = "jdbc:mysql://";
		database+= url.trim() + "?useOldAliasMetadataBehavior=true&rewriteBatchedStatements=true";
		System.out.println(database);
		return DriverManager.getConnection( database ,(user==null?"":user),(password==null?"":password));
	}
	
	public static Statement getStatement(Connection conn) {
		try {
			return conn.createStatement();
		} catch(SQLException e) { }
		return null;
	}
	
	public static void close(Object... objects) {
		for(Object o : objects) {
			if(o instanceof Connection)
				close((Connection)o);
			else if(o instanceof Statement)
				close((Statement)o);
			else if(o instanceof PreparedStatement)
				close((PreparedStatement)o);
			else if(o instanceof ResultSet)
				close((ResultSet)o);
			else if(o instanceof Collection) {
				@SuppressWarnings("rawtypes")
				Object[] olist = ((Collection)o).toArray();
				for(Object oitem : olist) {
					close(oitem);
				}
			} else 
				; // do nothing
		}
	}	
	
	public static void close(Connection o) {
		if(o != null)
			try {
				o.close();
				o = null;
			} catch(SQLException e) {}
	}
	public static void close(Statement o) {
		if(o != null)
			try {
				o.close();
				o = null;
			} catch(SQLException e) {}
	}
	public static void close(PreparedStatement o) {
		if(o != null)
			try {
				o.close();
				o = null;
			} catch(SQLException e) {}
	}
	public static void close(ResultSet o) {
		if(o != null)
			try {
				o.close();
				o = null;
			} catch(SQLException e) {}
	}
}

