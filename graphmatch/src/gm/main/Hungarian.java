package gm.main;

public class Hungarian {
	int d0,d1;
	HungarianAlgorithm ha;
	
	public Hungarian(double[][] sim){
		set(sim);
	}
	
	public static int[]  match(double[][] sim){
		Hungarian h = new Hungarian(sim);
		return h.execute();
	}
	
	
	public void set(double[][] sim){
		d0 = sim.length;
		d1 = sim[1].length;
		//d = Math.max(d0, d1);
		
		double[][] cost = new double[d0][d1];
		for(int i = 0; i<d0; i++){
			for(int j = 0; j<d1; j++){
				cost[i][j] = 1.0 - sim[i][j];
			}
		}
		ha = new HungarianAlgorithm(cost);
	}
	
	public int[] execute(){
		System.out.print("hungarian ");
		int[] result =  ha.execute();
		System.out.println("End");
		return result;
	}


}
