package common;


import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;

public class SparseIntMatrix {
	public static final int DEFAULT_VALUE = 0;
	
	//private HashMap<Integer,Integer>[] val;
	private Int2IntOpenHashMap[] val;
	
	public SparseIntMatrix(int size1){
		val = new Int2IntOpenHashMap[size1];
		
		for(int i = 0; i< size1; i++){
			val[i] = new Int2IntOpenHashMap();
			val[i].defaultReturnValue(DEFAULT_VALUE);
		}
	}
	
	public int get(int i, int j){
		return val[i].get(j);
	}
	public int size(int i){
		return val[i].size();
	}
	public void put(int i, int j, int v){
		val[i].put(j, v);
	}
	
	public IntIterator keyiterator(int i){
		return val[i].keySet().iterator();
	}
	
	
	public void add(int i, int j, int v_add){
		int v = val[i].get(j);
		
		v += v_add;
		
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

	

	
}







