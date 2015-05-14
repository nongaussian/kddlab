package common;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

public class SparseBinaryMatrix {
	private IntOpenHashSet[] val;
	
	public SparseBinaryMatrix(int size1){
		val = new IntOpenHashSet[size1];
		for(int i = 0; i< size1; i++){
			val[i] = new IntOpenHashSet();
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
	

	public static void main(String[] args){
		SparseBinaryMatrix sm = new SparseBinaryMatrix(100);
		System.out.println(sm.get(1, 1));
		sm.setTrue(1, 1);
		System.out.println(sm.get(1, 1));
		sm.setFalse(1, 1);
		System.out.println(sm.get(1, 1));
		sm.setTrue(2, 2);
		System.out.println(sm.get(2, 2));
	}
}







