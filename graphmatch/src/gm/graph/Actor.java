package gm.graph;

import java.util.ArrayList;

public class Actor extends Vertex{
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
		return String.format("<%d,%d> %s %s (%c)",getId(), getOriginalId(), first_name, last_name, gender); 
	}

	
	@Override
	public int compareTo(Vertex arg0) {
		return getId()-arg0.getId();
	}
	
	public ArrayList<Vertex> getMovies(){
		return movies;
	}

}
