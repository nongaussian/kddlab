package common;

import java.util.HashMap;
import java.util.Iterator;

public class SparseIntMatrix {
	private HashMap<Integer,Integer>[] val;
	
	public SparseIntMatrix(int size1){
		val = new HashMap[size1];
		for(int i = 0; i< size1; i++){
			val[i] = new HashMap<Integer,Integer>();
		}
	}
	
	public int get(int i, int j){
		Integer tmp = val[i].get(j);
		return tmp!=null?tmp:0;

	}
	public int size(int i){
		return val[i].size();
	}
	public void put(int i, int j, int v){
		val[i].put(j, v);
	}
	
	public Iterator<Integer> keyiterator(int i){
		return val[i].keySet().iterator();
	}
	
	
	public void add(int i, int j, int v_add){
		Integer v = val[i].get(j);
		if(v==null){
			v = new Integer(v_add);
		}else{
			v += v_add;
		}
		val[i].put(j, v);
	}
	
	
	public static void main(String[] args){
		SparseIntMatrix sm = new SparseIntMatrix(100);
		System.out.println(sm.get(1, 1));
		sm.put(1, 1, 2);
		System.out.println(sm.get(1, 1));
		sm.add(1, 1, 2);
		System.out.println(sm.get(1, 1));
		sm.add(2, 2, 2);
		System.out.println(sm.get(2, 2));
	}

	public boolean haspair(int i, int j) {
		return get(i, j)>0;
	}

	public void setTrue(int i, int j) {
		put(i,j,1);
		
	}
}







