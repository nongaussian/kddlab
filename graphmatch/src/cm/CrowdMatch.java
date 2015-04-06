package cm;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Iterator;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;


public class CrowdMatch extends CM_Learner{
	// constants
	private double em_converged				= 1.0e-5;
	private int em_max_iter					= 100;
	private double mu						= 1.;
	private int radius						= -1;

	protected String lprefix					= null;
	protected String rprefix					= null;

	private cm_model model					= null;
	

	
	// debug
	public boolean verbose					= false;
	//public boolean verbose					= true;
	

	
	public CrowdMatch(Namespace nes){
		// data parameters
		lprefix						= nes.getString("lprefix");
		rprefix						= nes.getString("rprefix");
		em_max_iter					= nes.getInt("niter");
		em_converged				= nes.getDouble("thres");
		mu							= nes.getDouble("mu");
		radius						= nes.getInt("radius");
		
	}

	
	public void set_model (cm_model model) {
		this.model = model;
	}

	public void run() throws IOException{
		// open an output file stream for logging scores
		PrintStream distance_file = new PrintStream(
				new FileOutputStream("res/distance.dat"));
		
		// initialize variational variables
		model.random_initialize_var(dat);
		
		long t_start = System.currentTimeMillis();
		long t_finish, t_start_iter, t_finish_iter, gap;

		// run expectation maximization
		double distance = 0, distance_old = 0, converged_ratio = 1;

		int iter = 0;
		while (((converged_ratio > em_converged) || (iter <= 10)) && (iter < em_max_iter)) {
			
			/* 1) start an iteration */
			iter++;
			if (verbose) System.out.println("**** em iteration " + iter + " ****");

			// start time
			t_start_iter = System.currentTimeMillis();

			/* 3) m-step : update model parameters (w) */
			em_mle(iter);

			distance = compute_distance(); 

			/* 4) now, finalize this iteration */
			t_finish_iter = System.currentTimeMillis();
			gap = t_finish_iter - t_start_iter;

			if(distance_old==0.0){
				converged_ratio = 0.0;
			}else{
				converged_ratio = (distance_old - distance) / (distance_old);
			}
			distance_old = distance;

			// output model and distance
			distance_file.println(distance + "\t" + converged_ratio + "\t" + gap + "\t" + (t_finish_iter - t_start));
			System.out.println("iteration (" + iter + ") distance: " + distance + "\t" + converged_ratio + " (" + gap + " ms)");

			if (verbose && (iter % 20) == 0) {
				model.save_model(String.format("res/%03d", iter));
			}
		}

		t_finish = System.currentTimeMillis();
		gap = t_finish - t_start;
		distance_file.println("execution time: " + gap);
		distance_file.close();

		// output the final model
		model.save_model("res/final");
	}

	private void em_mle(int iter) {
		double z = 0;
		
		// init c
		for (int t=0; t<dat.ntype; t++) {
			model.c[t] = model.c_next[t];
			model.c_next[t] = 0;
		}

		// init w
		for (int t=0; t<dat.ntype; t++) {
			for (int i=0; i<dat.lnodes[t].size; i++) {
				Iterator<Integer> iter_j = model.eff_pairs[t][i].iterator();
				int j;
				while(iter_j.hasNext()){
					j = iter_j.next();
					model.w[t].put(i, j, model.w_next[t].get(i, j));
					model.w_next[t].put(i, j, 0.0);
				}
				
			}
		}
		
		// em step
		for (int t=0; t<dat.ntype; t++) {
			for (int i=0; i<dat.lnodes[t].size; i++) {
				// compute variables use in this iteration only
				compute_alpha(t, i, model.alpha);
				
				// compute a w distribution
				//*/
				Iterator<Integer> iter_j = model.eff_pairs[t][i].iterator();
				int j;
				while(iter_j.hasNext()){
					j = iter_j.next();
				/*/
				for (int j=0; j<dat.lnodes[t].size; j++) {
					
				//*/
					boolean tij_eff = model.eff_pairs[t][i].contains(j);
					
					compute_delta(t, i, j, model.delta);
					
					if(tij_eff){
						// add alpha
						model.w_next[t].add(i, j, model.alpha[j]);
					}
					
					// distribute delta
					for (int s=0; s<dat.ntype; s++) {
						if (!dat.rel[t][s]) continue;
						int n_i = dat.lnodes[t].arr[i].neighbors[s].size;
						int n_j = dat.rnodes[t].arr[j].neighbors[s].size;
						
						for (int x=0; x<n_i; x++) {
							int u = dat.lnodes[t].arr[i].neighbors[s].arr[x];
							for (int y=0; y<n_j; y++) {
								int v = dat.rnodes[t].arr[j].neighbors[s].arr[y];
								boolean suv_eff = model.eff_pairs[s][u].contains(v);
								if(suv_eff){
									double tmp = model.alpha[j] * model.delta[s][x][y];
									if(Double.isNaN(tmp)){
										System.out.printf("NaN w_%d(%d,%d)   alpha[%d]=%f delta[%d][%d][%d]=%f:\n",t,i,j,j,model.alpha[j],s,x,y,model.delta[s][x][y]);
									}
									model.w_next[s].add(u, v, tmp);
									model.c_next[s] += tmp;
								}
							}
						}
					}
				}
				
				// XXX: simple w_bar
				if (dat.lnodes[t].arr[i].label >= 0) {
					model.w_next[t].add(i, dat.lnodes[t].arr[i].label, model.mu);
				}
			}
		}
		
		// normalize w
		for (int t=0; t<dat.ntype; t++) {
			for (int i=0; i<dat.lnodes[t].size; i++) {
				Iterator<Integer> iter_j = model.eff_pairs[t][i].iterator();
				
				z = 0;
				int j;
				while(iter_j.hasNext()){
					j = iter_j.next();
					z += model.w_next[t].get(i, j);
				}
				
				if(z>0.0){
					iter_j = model.eff_pairs[t][i].iterator();
					while(iter_j.hasNext()){
						j = iter_j.next();
						model.w_next[t].put(i, j, model.w_next[t].get(i, j)/z);
					}
				}
			}
		}
		
		// normalize c
		z = 0;
		for (int t=0; t<dat.ntype; t++) {
			z += model.c_next[t];
		}
		if(z==0.0){
			Arrays.fill(model.c_next, 1.0/dat.ntype);
		}else{
			for (int t=0; t<dat.ntype; t++) {
				model.c_next[t] /= z;
			}
		}
	}
	
	// XXX: for now, we assume each query has only one label 
	@SuppressWarnings("unused")
	private void compute_beta(int t, int i, double[] beta) {
	}

	private void compute_delta(int t, int i, int j, double[][][] delta) {
		double sum = 0;

		for (int s=0; s<dat.ntype; s++) {
			if (!dat.rel[t][s]) continue;
			
			int n_i = dat.lnodes[t].arr[i].neighbors[s].size;
			int n_j = dat.rnodes[t].arr[j].neighbors[s].size;
			
			if (n_i == 0) continue;
			
			
			
			for (int x=0; x<n_i; x++) {
				Arrays.fill(delta[s][x], 0.0);
				
				for (int y=0; y<n_j; y++) {
					int u = dat.lnodes[t].arr[i].neighbors[s].arr[x];
					int v = dat.rnodes[t].arr[j].neighbors[s].arr[y];
					
					if (dat.rnodes[s].arr[v].neighbors[t].size == 0) continue;
					
					delta[s][x][y] = 
							model.w[s].get(u, v)
							* model.c[s]
							/ (double) n_i
							/ (double) dat.rnodes[s].arr[v].neighbors[t].size;
					sum += delta[s][x][y]; 
					
				}
			}
		}
			
		if(sum>0.0){
			for (int s=0; s<dat.ntype; s++) {
				if (!dat.rel[t][s]) continue;
				
				int n_i = dat.lnodes[t].arr[i].neighbors[s].size;
				int n_j = dat.rnodes[t].arr[j].neighbors[s].size;
				
				for (int x=0; x<n_i; x++) {
					for (int y=0; y<n_j; y++) {
						delta[s][x][y] /= sum; 
						
					}
				}
			}
		}
	}

	private void compute_alpha(int t, int i, double[] alpha) {
		Arrays.fill(alpha, 0.0);
		double z = 0;
		Iterator<Integer> iter_j = model.eff_pairs[t][i].iterator();
		int j;
		while(iter_j.hasNext()){
			j = iter_j.next();
			double tmp = 0;
			for (int s=0; s<dat.ntype; s++) {
				if (!dat.rel[t][s]) continue;
				
				int n_i = dat.lnodes[t].arr[i].neighbors[s].size;
				int n_j = dat.rnodes[t].arr[j].neighbors[s].size;
				
				if (n_i == 0 ) continue;
				
				double sum = 0;
				for (int x=0; x<n_i; x++) {
					int u = dat.lnodes[t].arr[i].neighbors[s].arr[x];
					for (int y=0; y<n_j; y++) {
						int v = dat.rnodes[t].arr[j].neighbors[s].arr[y];
						
						if (dat.rnodes[s].arr[v].neighbors[t].size == 0) continue;
						
						sum += model.w[s].get(u, v)
								/ (double) dat.rnodes[s].arr[v].neighbors[t].size;
						
					}
				}
				
				tmp += model.c[s] * sum / (double) n_i;
			}
			
			alpha[j] = Math.sqrt(model.w[t].get(i, j) * tmp);
			if(Double.isNaN(alpha[j])){
				System.out.printf("alpha NaN  t:%d, i:%d, j:%d, tmp:%f, wij:%f\n",t,i,j,tmp, model.w[t].get(i, j));
			}
			z += alpha[j];
		}
		if(z==0.0)
			return;
		
		iter_j = model.eff_pairs[t][i].iterator();
		while(iter_j.hasNext()){
			j = iter_j.next();
			alpha[j] /= z;
			if(Double.isNaN(alpha[j])){
				System.out.printf("alpha NaN  t:%d, i:%d, j:%d, z:%f, wij:%f\n",t,i,j, z, model.w[t].get(i, j));
			}
		}
	}
	
	
	public double compute_distance () {
		double ret = 0;
		for (int t=0; t<dat.ntype; t++)  {
			for (int i=0; i<dat.lnodes[t].size; i++) {
				if(model.eff_pairs[t][i].size()>0){
					ret += compute_bdistance(t, i);
				}
			}
		}
		return ret;
	}

	private double compute_bdistance(int t, int i) {
		double sum1 = 0, sum2 = 0;
		Iterator<Integer> iter_j = model.eff_pairs[t][i].iterator();
		int j;
		while(iter_j.hasNext()){
			j = iter_j.next();
			double tmp = 0;
			
			for (int s=0; s<dat.ntype; s++) {
				if (!dat.rel[t][s]) continue;
				
				if (dat.lnodes[t].arr[i].neighbors[s].size == 0) continue;
				
				double sub = 0;
				for (int x=0; x<dat.lnodes[t].arr[i].neighbors[s].size; x++) {
					for (int y=0; y<dat.rnodes[t].arr[j].neighbors[s].size; y++) {
						int u = dat.lnodes[t].arr[i].neighbors[s].arr[x];
						int v = dat.rnodes[t].arr[j].neighbors[s].arr[y];
						
						sub += model.w_next[s].get(u, v) / 
								(double)dat.rnodes[s].arr[v].neighbors[t].size;
					}
				}
				
				sub /= (double)dat.lnodes[t].arr[i].neighbors[s].size;
				
				tmp += model.c_next[s] * sub;
			}
			sum1 += Math.sqrt(model.w_next[t].get(i, j) * tmp);
			
			if(Double.isNaN(sum1)){
				System.out.printf("sum1 NaN t:%d, i:%d, j:%d, tmp:%f, wij: %f\n", t, i, j, tmp, model.w_next[t].get(i, j));
			}
			
		}
		
		if(sum1>0)sum1 = Math.log(sum1);
		
		
		// for labeled data, 
		if(dat.lnodes[t].arr[i].label>=0){
			sum2 = Math.sqrt(model.w_next[t].get(i, dat.lnodes[t].arr[i].label));
			/*
			iter_j = model.eff_pairs[t][i].iterator();
			while(iter_j.hasNext()){
				j = iter_j.next();
				
				sum2 = Math.sqrt(model.w_next[t].get(i, dat.lnodes[t].arr[i].label));
				if(Double.isNaN(sum2)){
					System.out.printf("sum2 NaN t:%d, i:%d, j:%d, wij: %f\n", t, i, j, model.w_next[t].get(i, j));
				}
			}
			if(sum2==0.0){
				System.out.printf("sum2 NaN t:%d, i:%d\n", t, i);
			}*/
			
			if (sum2 > 0) sum2 = Math.log(sum2);
		}
		
		
		
		return -(sum1 + model.mu * sum2);
	}
	
	
	
	
	
	
	
	
	
	
	
	@Deprecated
	public static void main(String[] args) throws IOException {
		Namespace nes = parseArguments(args);
		CrowdMatch obj = new CrowdMatch(nes);
		
		// run em
		obj.init();
		obj.run();
	}
	@Deprecated
	public static Namespace parseArguments(String[] args){
		ArgumentParser parser = ArgumentParsers.newArgumentParser("CrowdMatch")
	                .description("Graph matching");
			
		try {
			parser.addArgument("-mu")
						.type(Double.class)
						.setDefault(1.)
						.help("mu");
			parser.addArgument("-niter")
						.type(Integer.class)
						.setDefault(100)
						.help("Maximum number of iterations");
			parser.addArgument("-thres")
						.type(Double.class)
						.setDefault(1.0e-5)
						.help("Threshold of log-distance ratio for convergence test");
			parser.addArgument("-lprefix")
						.type(String.class)
						.required(true)
						.help("Prefix name of left graph");
			parser.addArgument("-rprefix")
						.type(String.class)
						.required(true)
						.help("Prefix name of right graph");

			Namespace res = parser.parseArgs(args);
			System.out.println(res);
			return res;
		} catch (ArgumentParserException e) {
			parser.handleError(e);
		}
		return null;
	}
	
	@Deprecated
	public void init() {
		// data loading
		dat 						= new cm_data(lprefix, rprefix);
		
		// init model parameter
		model						= new cm_model(radius);
		model.init_learner(dat, mu);
	}
	
}
