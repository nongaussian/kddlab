package common;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

public class SparseMatrix {
	private HashMap<Integer,Double>[] val;
	
	public SparseMatrix(int size1){
		val = new HashMap[size1];
		for(int i = 0; i< size1; i++){
			val[i] = new HashMap<Integer,Double>();
		}
	}
	
	public double get(int i, int j){
		Double tmp = val[i].get(j);
		return tmp!=null?tmp:0.0;

	}
	public Iterator<Entry<Integer,Double>> getEntryIterator(int i){
		return val[i].entrySet().iterator();
	}
	public void put(int i, int j, double v){
		if(Double.isNaN(v)){
			System.out.printf("Nan (%d,%d)\n",i,j);
		}
		if(v==0){
			val[i].remove(j);
			return;
		}
		val[i].put(j, v);
	}
	public void add(int i, int j, double v_add){
		if(Double.isNaN(v_add)){
			System.out.printf("Nan (%d,%d)\n",i,j);
		}
		Double v = val[i].get(j);
		if(v==null){
			v = new Double(v_add);
		}else{
			v += v_add;
		}
		val[i].put(j, v);
	}
	
	
	public static void main(String[] args){
		SparseMatrix sm = new SparseMatrix(100);
		System.out.println(sm.get(1, 1));
		sm.put(1, 1, 2.0);
		System.out.println(sm.get(1, 1));
		sm.add(1, 1, 2.0);
		System.out.println(sm.get(1, 1));
		sm.add(2, 2, 2.0);
		System.out.println(sm.get(2, 2));
	}
}







