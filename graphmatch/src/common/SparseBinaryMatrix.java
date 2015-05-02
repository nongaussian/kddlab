package common;

import java.util.HashMap;
import java.util.HashSet;

public class SparseBinaryMatrix {
	private HashSet<Integer>[] val;
	
	public SparseBinaryMatrix(int size1){
		val = new HashSet[size1];
		for(int i = 0; i< size1; i++){
			val[i] = new HashSet<Integer>();
		}
	}
	
	public int size(int i){
		return val[i].size();
	}
	public boolean get(int i, int j){
		return val[i].contains(j);
	}
	
	public void setTrue(int i, int j){
		val[i].add(j);
	}
	public void setFalse(int i, int j){
		val[i].remove(j);
	}
	

	
}







