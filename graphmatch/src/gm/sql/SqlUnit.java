package gm.sql;

import gm.main.GraphMatching;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class SqlUnit {
	public static Connection con = null;
	private static Statement st = null;
	
	public SqlUnit(String dbname){
		try {
			connect(dbname);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			handlingSQLException(e);
			System.exit(0);
		}
	}
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
	
	
	private static void connect(String dbname) throws SQLException{
		con = DriverManager.getConnection("jdbc:mysql://147.46.143.245/?useUnicode=true&characterEncoding=utf8","root", "adminjwh");
		st = con.createStatement();
		st.execute("use "+dbname);
		
		//pst_timeline = con.prepareStatement("insert into Status(id, user_id, text, created, rtcount, isRetweet) values (?,?,?,?,?,?);");
		//pst_timeline_loc = con.prepareStatement("insert into Status(id, user_id, text, created, rtcount, isRetweet, loc_latitude, loc_longitude) values (?,?,?,?,?,?,?,?);");
	}
	
	public Statement getStatement(){
		return st;
	}
	public ResultSet executeQuery(String query){
		try {
			return st.executeQuery(query);
		} catch (SQLException e) {
			handlingSQLException(e);
		}
		return null;
	}
	public int executeUpdate(String query){
		try {
			if(GraphMatching.LOGGING){
				return st.executeUpdate(query);
			}
		} catch (SQLException e) {
			System.out.println(query);
			handlingSQLException(e);
		}
		return 0;
	}
	public int executeUpdateGetAutoKey(String query){
		try {
			if(GraphMatching.LOGGING){
				st.executeUpdate(query,Statement.RETURN_GENERATED_KEYS);
				ResultSet rs = st.getGeneratedKeys();
				
				if(rs.next()){
					return rs.getInt(1);
				}
			}
		} catch (SQLException e) {
			handlingSQLException(e);
			System.exit(0);
		}
		return 0;
	} 
	private static void disconnect(){
		try {
			con.close();
		} catch (SQLException e) {
			handlingSQLException(e);
		}
	}
	public static void exit(){
		disconnect();
	}
	
}
