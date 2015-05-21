package test;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import java.io.IOException;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import common.SparseMatrix;

import cm.CandNode;
import cm.QueryNode;

public class TestMain {
	
	public static void main(String[] args) throws IOException{
		//testArrayAddAll();
		//testEntropy();
		testEditSparseEntry();
	}
	public static void testArrayAddAll(){
		String[] a   = {"a"};
		String[] b = {"b","b"};
		String[] c = {"c_","c","c"};
		
		String[] out = (String[]) ArrayUtils.addAll(a, b, c);
		System.out.println(StringUtils.join(out," "));
	}
	
	public static void testEntropy(){
		//Compute Entropy
		QueryNode q = new QueryNode(0, 0);
		int n_cand = 5;
		CandNode[] cn_list = new CandNode[5];
		cn_list[0] = new CandNode();
		cn_list[0].w = 1.0;
		cn_list[1] = new CandNode();
		cn_list[1].w = 0.1;
		cn_list[2] = new CandNode();
		cn_list[2].w = 0.0;
		q.max_sim = 1.0;
		q.entropy = 0;
		if(q.max_sim>0.0){
			int j=0;
			double sum = 0.0;
			double p = 0.0;
			for(; j<n_cand; j++){
				if(cn_list[j].w==0.0){
					break;
				}
				sum += cn_list[j].w;
			}
			for(j=0; j<n_cand; j++){
				p =cn_list[j].w;
				if(p==0.0){
					break;
				}
				p /= sum; 
				q.entropy -= (p*Math.log(p));
			}
		}
		System.out.println(q.entropy);
	}
	public static void testEditSparseEntry(){
		
		int[] j_list = {0,2,3,5};
		SparseMatrix sm = new SparseMatrix(10);
		for(int j: j_list){
			System.out.printf("(%d,%f)", j,sm.get(5, j));
		}
		System.out.println("\n===============================");
		
		sm.put(5, 2, 0.2);
		sm.put(5, 3, 0.3);
		sm.put(5, 5, 0.5);
		
		for(int j: j_list){
			System.out.printf("(%d,%f)", j,sm.get(5, j));
		}

		System.out.println("\n===============================");
		ObjectIterator<Entry> simiter = sm.getEntryIterator(5);
		while (simiter.hasNext()){
			Entry entry = simiter.next();
			entry.setValue(0.33);
		}
		
		
		for(int j: j_list){
			System.out.printf("(%d,%f)", j,sm.get(5, j));
		}
		System.out.println("\n===============================");
		
		
		
	}
}
