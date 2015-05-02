package test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class TestRandomAccess {
	public static void main(String[] args){
		int n = 1000;
		int r = 30*1000*1000;
		
		ArrayListTest(n,r);
		HashmapTest(n,r);

		
		
	}
	
	public static void ArrayListTest(int n, int rp){
		System.out.println("======================ArrayList==================");
		Random r = new Random();
		long ct = System.currentTimeMillis();
		ArrayList<Double> list = new ArrayList<Double>();
		
		for(int i = 0; i<n; i++){
			list.add(r.nextDouble());
		}
		System.out.printf("Load %d ms\n",System.currentTimeMillis()-ct);
		ct = System.currentTimeMillis();
		double sum = 0.0;
		for(int i = 0; i<rp; i++){
			sum+=list.get(r.nextInt(n));
		}
		System.out.printf("Read %d ms\tsum:%f\n",System.currentTimeMillis()-ct,sum);
		
		
	}
	public static void HashmapTest(int n, int rp){
		System.out.println("======================HashMap==================");
		Random r = new Random();
		long ct = System.currentTimeMillis();
		HashMap<Integer,Double> hm= new HashMap<Integer,Double>();
		
		for(int i = 0; i<n; i++){
			hm.put(i,r.nextDouble());
		}
		System.out.printf("Load %d ms\n",System.currentTimeMillis()-ct);
		ct = System.currentTimeMillis();
		double sum = 0.0;
		for(int i = 0; i<rp; i++){
			sum+=hm.get(r.nextInt(n));
		}
		System.out.printf("Read %d ms\tsum:%f\n",System.currentTimeMillis()-ct,sum);
	}
}
