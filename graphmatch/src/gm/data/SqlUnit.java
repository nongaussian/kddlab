package gm.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SqlUnit {
	public static String DEFUALT_ID = "graphmatching";
	public static String DEFUALT_PWD = "graphmatching";
	public static String DEFUALT_IP = "147.46.143.245";
	
	
	private boolean usedb = false;
	public Connection con = null;
	private Statement st = null;
	
	public String dbname;
	
	public SqlUnit(String dbname){
		try {
			this.dbname = dbname;
			connect(dbname, DEFUALT_ID, DEFUALT_PWD);
		} catch (SQLException e) {

			//handlingSQLException(e);
			//System.exit(0);
			usedb = false;
		}
		
	}
	public SqlUnit(String dbname, String username, String pwd) {
		try {
			this.dbname = dbname;
			connect(dbname,username,pwd);
		} catch (SQLException e) {
			usedb = false;
		}
	}
	public SqlUnit() {}
	private static String reform(String a){
		//a = a.replaceAll("\\", "\\\\");
		a = a.replaceAll("'", "\\\\'");	
		return a;
	}
	
	
	public static void handlingSQLException(SQLException sqex){
		System.out.println("SQLException: " + sqex.getMessage());
		System.out.println("SQLState: " + sqex.getSQLState());
		sqex.printStackTrace();
	}
	
	
	
	private void connect(String dbname, String username, String pwd) throws SQLException{
		con = DriverManager.getConnection("jdbc:mysql://"+DEFUALT_IP+"/?useUnicode=true&characterEncoding=utf8",username, pwd);
		st = con.createStatement();
		st.execute("use "+dbname);
		usedb = true;
		//pst_timeline = con.prepareStatement("insert into Status(id, user_id, text, created, rtcount, isRetweet) values (?,?,?,?,?,?);");
		//pst_timeline_loc = con.prepareStatement("insert into Status(id, user_id, text, created, rtcount, isRetweet, loc_latitude, loc_longitude) values (?,?,?,?,?,?,?,?);");
	}
	
	public Statement getStatement(){
		return st;
	}
	public ResultSet executeQuery(String query){
		if(!usedb) return null;
		try {
			return st.executeQuery(query);
		} catch (SQLException e) {
			System.out.println(query);
			handlingSQLException(e);
		}
		return null;
	}
	public int executeUpdate(String query){
		if(!usedb) return 0;
		try {
			
			return st.executeUpdate(query);
			
		} catch (SQLException e) {
			System.out.println(query);
			handlingSQLException(e);
		}
		return 0;
	}
	public int executeUpdateGetAutoKey(String query){
		if(!usedb) return 0;
		try {
			
			st.executeUpdate(query,Statement.RETURN_GENERATED_KEYS);
			ResultSet rs = st.getGeneratedKeys();
			
			if(rs.next()){
				return rs.getInt(1);
			}
		
		} catch (SQLException e) {
			System.out.println(query);
			handlingSQLException(e);
			System.exit(0);
		}
		return 0;
	} 
	private void disconnect(){
		if(con==null)
			return;
		try {
			
			con.close();
		} catch (SQLException e) {
			handlingSQLException(e);
		}
	}
	public void exit(){
		if(!usedb) return;
		disconnect();
	}
	
}
