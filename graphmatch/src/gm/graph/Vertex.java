package gm.graph;

import java.io.Serializable;
import java.util.ArrayList;

public abstract class Vertex implements Comparable<Vertex>, Serializable{
	private int original_id;
	private int id;
	
	private Vertex knownLink;
	
	protected Vertex(){}
	public Vertex(int original_id){
		setOriginalId(original_id);
	}
	public int getId() {
		return id;
	}
	protected void setOriginalId(int original_id){
		this.original_id = original_id;
	}
	public void setId(int id) {
		this.id = id;
	}
	
	public int getOriginalId() {
		return original_id;
	}
	public boolean same(Vertex v) {
		if(knownLink==null)
			return false;
		return knownLink.equals(v);
	}
	public void setKnownLink(Vertex knownLink) {
		this.knownLink = knownLink;
	}
	public Vertex getKnownLink(){
		return knownLink;
	}
	
	
	public static String getVeticesIdString(ArrayList<Vertex> vertices){
		String out = "";
		for(Vertex v : vertices){
			out += String.format("%d/%d\t",v.getId(),v.getOriginalId());
		}
		return out;
	}
	
	
}
