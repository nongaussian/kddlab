package gm.graph;

import java.util.ArrayList;
import java.util.List;

public class Director extends Vertex{


	private String first_name;
	private String last_name;

	private ArrayList<Vertex> movies = new ArrayList<Vertex>();
	
	
	public Director (int original_id, String first_name, String last_name){
		super(original_id);
		this.first_name = first_name;
		this.last_name = last_name;

	}


	
	
	public Director(Director director) {
		super(director.getOriginalId());
		this.first_name = director.first_name;
		this.last_name = director.last_name;
	}




	public void addDirectedMovie(Movie m){
		movies.add(m);
	}
	
	public String toString(){
		return String.format("<%d,%d> %s %s",getId(), getOriginalId(), first_name, last_name); 
	}

	@Override
	public int compareTo(Vertex arg0) {
		return getId()-arg0.getId();
	}
	public ArrayList<Vertex> getMovies() {
		return movies;
	}


}
