package gm.graph;

import java.util.ArrayList;

public class Movie extends Vertex{
	private String title;
	private int year;
	
	private ArrayList<Vertex> actors = new ArrayList<Vertex>();
	private ArrayList<Vertex> directors = new ArrayList<Vertex>();
	
	
	public Movie (int original_id, String title, int year){
		super(original_id);
		this.title = title;
		this.year = year;
	}
	public Movie(Movie m){
		super(m.getOriginalId());
		this.title = m.getTitle();
		this.year = m.year;
	}

	
	public String toString(){
		return String.format("<%d,%d> %s (%d)",getId(), getOriginalId(), title, year); 
	}
	
	public void addActor(Actor a){
		actors.add(a);
	}
	public void addDirector(Director d){
		directors.add(d);
	}
	@Override
	public int compareTo(Vertex arg0) {
		return getId()-arg0.getId();
	}
	public String getTitle(){return title;}
	public ArrayList<Vertex> getActors(){return actors;}
	public ArrayList<Vertex> getDirectors(){return directors;}
}
