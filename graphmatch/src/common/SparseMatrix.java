package common;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap.Entry;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;



public class SparseMatrix {
	private Int2DoubleOpenHashMap[] val;
	//private int[] max_idx;
	//private double[] max_val;
	
	public SparseMatrix(int size1){
		val = new Int2DoubleOpenHashMap[size1];
		//max_idx = new int[size1];
		for(int i = 0; i< size1; i++){
			val[i] = new Int2DoubleOpenHashMap();
			val[i].defaultReturnValue(0.0);
		}
	}
	
	public double get(int i, int j){
		return val[i].get(j);
	}
	public ObjectIterator<Entry> getEntryIterator(int i){
		return val[i].int2DoubleEntrySet().fastIterator();
	}
	public void put(int i, int j, double v){
		/*if(Double.isNaN(v)){
			System.out.printf("Nan (%d,%d)\n",i,j);
		}*/
		/*if(v==0.0){
			val[i].remove(j);
			return;
		}*/
		
		val[i].put(j, v);
	}
	public void add(int i, int j, double v_add){
		/*if(Double.isNaN(v_add)){
			System.out.printf("Nan (%d,%d)\n",i,j);
		}*/
		val[i].addTo(j, v_add);
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







