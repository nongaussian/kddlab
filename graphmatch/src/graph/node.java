package graph;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

import java.util.Arrays;

public class node {
	public int id;
	
	private int last_label = -1;
	
	private Int2IntOpenHashMap annotations = null;
	private int nAnnotations = 0;
	
	public neighbors[] neighbors = null;
	
	public node (int id, int ntype, boolean[] rel) {
		this.id = id;
		this.neighbors = new neighbors[ntype];
		for (int i=0; i<ntype; i++) {
			if (rel[i])
				neighbors[i] = new neighbors(i);
			else
				neighbors[i] = null;
		}
		annotations = new Int2IntOpenHashMap();
		annotations.defaultReturnValue(0);
	}
	public void increase_neighbor_count(int type) {
		neighbors[type].increase_count();
	}
	public void alloc(int type) {
		neighbors[type].alloc();
	}
	public void add_neighbor(int type, int id) {
		neighbors[type].add_neighbor(id);
	}
	
	public void trim(int ntype){
		if(neighbors[ntype].arr.length>neighbors[ntype].size){
			int [] arr = Arrays.copyOf(neighbors[ntype].arr, neighbors[ntype].size);
			neighbors[ntype].arr = arr;
		}
		
	}
	public int getLastLabel() {
		return last_label;
	}
	/*
	public void setLabel(int label) {
		this.label = label;
	}*/
	
	public void annotate(int id){
		this.last_label = id;
		annotations.addTo(id, 1);
		nAnnotations++;
	}
	public double getWbar(int id){
		if(nAnnotations==0){
			return 0;
		}
		return ((double)annotations.get(id))/nAnnotations;
	}
	public int getNAnnotatios(){
		return nAnnotations;
	}
	
	public Int2IntOpenHashMap getAnnotations(){
		return annotations;
	}
}