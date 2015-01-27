package gm.graph;

import java.io.Serializable;
import java.util.ArrayList;

public class Actor extends Vertex implements Serializable{
	private String first_name;
	private String last_name;
	private char gender;
	
	public static char GENDER_FEMALE = 'F';
	public static char GENDER_MALE = 'M';
	
	private ArrayList<Vertex> movies = new ArrayList<Vertex>();
	
	public Actor (int original_id, String first_name, String last_name, char gender){
		super(original_id);
		this.first_name = first_name;
		this.last_name = last_name;
		this.gender = gender;
	}
	public Actor(String str){
		String[] args = str.split("\t");
		String[] ids = args[0].split("/");
		setId(Integer.parseInt(ids[0]));
		setOriginalId(Integer.parseInt(ids[1]));
		this.first_name = args[1];
		this.last_name = args[2];
		this.gender = args[3].charAt(0);
	}
	
	
	public Actor(Actor actor) {
		super(actor.getOriginalId());
		this.first_name = actor.first_name;
		this.last_name = actor.last_name;
		this.gender = actor.gender;
	}


	public void addActing(Movie m){
		movies.add(m);
	}
	
	public String toString(){
		return String.format("%d/%d\t%s\t%s\t%c",getId(), getOriginalId(), first_name, last_name, gender); 
	}

	
	@Override
	public int compareTo(Vertex arg0) {
		return getId()-arg0.getId();
	}
	
	public ArrayList<Vertex> getMovies(){
		return movies;
	}
	
	public String getMoviesIdString(){
		return getVeticesIdString(getMovies());
	}
}
