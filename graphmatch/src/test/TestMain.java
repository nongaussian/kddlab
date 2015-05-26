package test;

import gm.data.DataPlot;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap.Entry;
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.PriorityQueue;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import sim.GraphMatching;
import sim.Param;
import cm.CandNode;
import cm.QueryNode;

import common.SparseMatrix;

public class TestMain {
	
	public static void main(String[] args) throws IOException{
		movingAvgTest();
		//switch_test();
		/*
		randomTiebreak();
		randomTiebreak();
		randomTiebreak();
		randomTiebreak();
		randomTiebreak();
		randomTiebreak();
		GraphMatching gm = new GraphMatching(args);
		gm.init();
		gm.initDB();
		gm.run();
		gm.close();*/
		
	}
	public static void movingAvgTest(){
		double[][] a = {
				{1.0,1.0},
				{2.0,1.0},
				{3.0,1.0},
				{4.0,2.0},
				{5.0,2.0},
				{6.0,2.0},
				{7.0,2.0},
				{8.0,2.0},
				{9.0,2.0},
				{10.0,2.0}
		};
		double [][]b = DataPlot.movingAvg(a,4);
		for(int i =0; i<a.length; i++){
			System.out.printf("%3d", (int)a[i][0]);
		}
		System.out.println();
		for(int i =0; i<a.length; i++){
			System.out.printf("%3f", a[i][1]);
		}
		System.out.println();
		for(int i =0; i<b.length; i++){
			System.out.printf("%3f", b[i][1]);
		}
		System.out.println();
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
	
	public static void switch_test(){
		sw(1);
		sw(2);
		sw(3);
		sw(4);
		sw(5);
		sw(6);
		sw(7);
		sw(8);
	}
	public static void sw(int val){
		boolean b_cont = false;
		switch (val){

		case Param.QUERY_MINVAR:
		case Param.QUERY_NEIGHBORPAIR:
			b_cont = true;
			break;
		
		}
		System.out.println(b_cont);
	}
	public static void randomTiebreak(){
		ArrayList<IntDouble> ar = new ArrayList<IntDouble>();
		ar.add(new IntDouble(0, 2.0));
		ar.add(new IntDouble(1, 2.0));
		ar.add(new IntDouble(2, 2.0));
		ar.add(new IntDouble(3, 3.0));
		ar.add(new IntDouble(4, 3.0));
		ar.add(new IntDouble(5, 3.0));
		ar.add(new IntDouble(6, 4.0));
		ar.add(new IntDouble(7, 2.0));
		ar.add(new IntDouble(8, 3.0));
		
		int n = 2;
		Comparator<IntDouble> comp = new Comparator<IntDouble>(){
			@Override
			public int compare(IntDouble o1, IntDouble o2) {
				return (o1.v< o2.v) ? -1 : ((o1.v > o2.v) ? 1 : 0);
			}
			
		};
		PriorityQueue<IntDouble> topk = new PriorityQueue<IntDouble>(n,comp);
		int tiecount = 0;
		for(IntDouble a:ar){
			if(topk.size()<n){
				if(topk.size()>0&&topk.peek().v==a.v){
					tiecount++;
				}
				topk.add(a);
			}else if(topk.peek().v<a.v){
				topk.poll();
				topk.add(a);
				if(topk.size()>=n+tiecount){
					while(tiecount>0){
						topk.poll();
						tiecount--;
					}
				}
			}else if(topk.peek().v==a.v){
				topk.add(a);
				tiecount++;
			}
		}
		
		ArrayList<IntDouble>ar2 = new ArrayList<IntDouble>();
		if(tiecount>0){
			double tv = topk.peek().v;
			while (topk.peek().v==tv){
				ar2.add(topk.poll());
			}
			Collections.shuffle(ar2);
			while(topk.size()+ar2.size()>n){
				ar2.remove(ar2.size()-1);
			}
		}
		while(topk.size()>0){
			ar2.add(topk.poll());
		}
		
		for(IntDouble a1:ar2){
			System.out.println(a1);
		}
		System.out.println("-------------");
	}
}

class IntDouble{
	int i;
	double v;
	public IntDouble(int i, double v){
		this.i = i;
		this.v = v;
	}
	public String toString(){
		return i+" "+v;
	}
}


