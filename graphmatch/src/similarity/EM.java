package similarity;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Iterator;

import net.sourceforge.argparse4j.inf.Namespace;
import cm.cm_data;

public class EM extends Similarity{
	double mu						= 1;
	public double[] alpha			= null;
	public double[][][] delta		= null;
	public double[] c				= null;
	public double[] c_next			= null;
	double[] tmp_w					= null;
	
	
	public EM(Namespace nes) {
		super(nes);
	}
	
	public void init_learner (cm_data dat, double mu) {
		this.dat = dat;
		this.mu = mu;
		
		c = new double[dat.ntype];
		c_next = new double[dat.ntype];
		
		
		temp_stat();
		
		alpha = new double[maxrnodesize];
		
		delta = new double[dat.ntype][maxlneighborsize][maxrneighborsize];
	}
	
	
	
	private void temp_stat() {
		maxrnodesize = 0;
		for (int t=0; t<dat.ntype; t++) {
			if (maxrnodesize < dat.rnodes[t].size) {
				maxrnodesize = dat.rnodes[t].size;
			}
		}
		
		maxlneighborsize = 0;
		for (int t=0; t<dat.ntype; t++) {
			for (int i=0; i<dat.lnodes[t].size; i++) {
				for (int s=0; s<dat.ntype; s++) {
					if (!dat.rel[t][s]) continue;
					if (maxlneighborsize < dat.lnodes[t].arr[i].neighbors[s].size) {
						maxlneighborsize = dat.lnodes[t].arr[i].neighbors[s].size;
					}
				}
			}
		}
		
		maxrneighborsize = 0;
		for (int t=0; t<dat.ntype; t++) {
			for (int i=0; i<dat.rnodes[t].size; i++) {
				for (int s=0; s<dat.ntype; s++) {
					if (!dat.rel[t][s]) continue;
					if (maxrneighborsize < dat.rnodes[t].arr[i].neighbors[s].size) {
						maxrneighborsize = dat.rnodes[t].arr[i].neighbors[s].size;
					}
				}
			}
		}
	}

	@Override
	public void initialize(cm_data dat) {
		if (!firstrun ) return;
		initSimMatrix();
		initialize_effpairs(dat);
		firstrun = false;
		
		// init w
		for (int t=0; t<dat.ntype; t++) {
			for (int i=0; i<dat.lnodes[t].size; i++) {
				Integer[] cand_r = eff_pairs[t][i].toArray(new Integer[0]);
				if(cand_r.length==0){
					continue;
				}
				double tmp = 1. / cand_r.length;
				
				for (int j=0; j<cand_r.length; j++) {
					sim_next[t].put(i, cand_r[j], tmp);
				}
			}
		}
		
		// init c
		for (int t=0; t<dat.ntype; t++) {
			c_next[t] = 1. / (double)dat.ntype;
		}
	}

	@Override
	public void run() throws IOException{
		// open an output file stream for logging scores
		PrintStream distance_file = new PrintStream(
				new FileOutputStream("res/distance.dat"));
		
		// initialize variational variables
		initialize(dat);
		
		long t_start = System.currentTimeMillis();
		long t_finish, t_start_iter, t_finish_iter, gap;

		// run expectation maximization
		double distance = 0, distance_old = 0, converged_ratio = 1;

		int iter = 0;
		while (((converged_ratio > th_convergence) || (iter <= min_iter)) && (iter < max_iter)) {
			
			/* 1) start an iteration */
			iter++;
			
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

			
		}

		t_finish = System.currentTimeMillis();
		gap = t_finish - t_start;
		distance_file.println("execution time: " + gap);
		distance_file.close();

		// output the final model
		//save_model("res/final");
	}
	
	// compute the (approximate) expected model change when we ask to annotate for node i of type t
	public double compute_expected_model_change(int t, int i) {
		double sum = 0;
		
		Iterator<Integer> iter_j = eff_pairs[t][i].iterator();
		int j;
		while(iter_j.hasNext()){
			j = iter_j.next();
			double diff = 0;

			// change of the node i//TODO: Log?????
			//diff += -Math.log(sim[t].get(i, j)) / 2.;
			
			// change of the neighbors of i
			for (int s=0; s<dat.ntype; s++) {
				if (!dat.rel[t][s]) continue;
				
				for (int n_x=0; n_x<dat.lnodes[t].arr[i].neighbors[s].size; n_x++) {
					int x = dat.lnodes[t].arr[i].neighbors[s].arr[n_x];
					double tmp = 0;
					
					estimate_w(tmp_w, t, i, j, s, x);
					
					for (int n_y=0; n_y<dat.rnodes[t].arr[j].neighbors[s].size; n_y++) {
						int y = dat.rnodes[t].arr[j].neighbors[s].arr[n_y];
						tmp += Math.sqrt(sim_next[s].get(x, y) * tmp_w[n_y]);
					}
					if(tmp>0.0){
						diff += -Math.log(tmp);
					}
					if(Double.isNaN(diff)||Double.isInfinite(diff)){
						System.out.printf("diff: %f, tmp:%f\n",diff,tmp);
					}
				}
			}
			sum += diff * sim_next[t].get(i, j);
		}
		
		return sum;
	}

	// estimate w[s].arr[u]
	
	private void estimate_w(double[] w, int t, int i, int j, int s, int x) {
		int[] x_neighbors = dat.lnodes[s].arr[x].neighbors[t].arr;
		int[] j_neighbors = dat.rnodes[t].arr[j].neighbors[s].arr;
		for (int idx_y=0; idx_y< j_neighbors.length; idx_y++){
			int y = dat.rnodes[t].arr[j].neighbors[s].arr[idx_y];
			w[idx_y] = sim_next[s].get(x, y);
		}			
		
		if(x_neighbors.length>0&& j_neighbors.length>0){
			double nst_x = (double) x_neighbors.length;
			double nts_j = (double) j_neighbors.length;
			double tmp = (1.0-sim_next[t].get(i, j))/nst_x;
			
			for (int idx_y=0; idx_y< j_neighbors.length; idx_y++){
				w[idx_y] += tmp/nts_j;
			}
		}
				
		
	}
	private void em_mle(int iter) {
		double z = 0;
		
		// init c
		for (int t=0; t<dat.ntype; t++) {
			c[t] = c_next[t];
			c_next[t] = 0;
		}
	
		// init w
		for (int t=0; t<dat.ntype; t++) {
			for (int i=0; i<dat.lnodes[t].size; i++) {
				Iterator<Integer> iter_j = eff_pairs[t][i].iterator();
				int j;
				while(iter_j.hasNext()){
					j = iter_j.next();
					sim[t].put(i, j, sim_next[t].get(i, j));
					sim_next[t].put(i, j, 0.0);
				}
				
			}
		}
		
		// em step
		for (int t=0; t<dat.ntype; t++) {
			for (int i=0; i<dat.lnodes[t].size; i++) {
				// compute variables use in this iteration only
				compute_alpha(t, i, alpha);//alpha^t_i*
				
				// compute a w distribution
				//*/
				Iterator<Integer> iter_j = eff_pairs[t][i].iterator();
				int j;
				while(iter_j.hasNext()){
					j = iter_j.next();
				/*/
				for (int j=0; j<dat.lnodes[t].size; j++) {
					
				//*/
					boolean tij_eff = eff_pairs[t][i].contains(j);
					
					compute_delta(t, i, j, delta);//delta^ts_ij**
					
					if(tij_eff){
						// add alpha
						double a = alpha[j];
						if(a>0){
							a = 1.0*a;
						}
						sim_next[t].add(i, j, alpha[j]);
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
								boolean suv_eff = eff_pairs[s][u].contains(v);
								if(suv_eff){
									double tmp = alpha[j] * delta[s][x][y];
									if(Double.isNaN(tmp)){
										System.out.printf("NaN w_%d(%d,%d)   alpha[%d]=%f delta[%d][%d][%d]=%f:\n",t,i,j,j,alpha[j],s,x,y,delta[s][x][y]);
									}
									sim_next[s].add(u, v, tmp);
									c_next[s] += tmp;
								}
							}
						}
					}
				}
				
				// XXX: simple w_bar
				if (dat.lnodes[t].arr[i].label >= 0) {
					sim_next[t].add(i, dat.lnodes[t].arr[i].label, mu);
				}
			}
		}
		
		// normalize w
		for (int t=0; t<dat.ntype; t++) {
			for (int i=0; i<dat.lnodes[t].size; i++) {
				Iterator<Integer> iter_j = eff_pairs[t][i].iterator();
				
				z = 0;
				int j;
				while(iter_j.hasNext()){
					j = iter_j.next();
					z += sim_next[t].get(i, j);
				}
				
				if(z>0.0){
					iter_j = eff_pairs[t][i].iterator();
					while(iter_j.hasNext()){
						j = iter_j.next();
						sim_next[t].put(i, j, sim_next[t].get(i, j)/z);
					}
				}
			}
		}
		
		// normalize c
		z = 0;
		for (int t=0; t<dat.ntype; t++) {
			z += c_next[t];
		}
		if(z==0.0){
			Arrays.fill(c_next, 1.0/dat.ntype);
		}else{
			for (int t=0; t<dat.ntype; t++) {
				c_next[t] /= z;
			}
		}
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
				int u = dat.lnodes[t].arr[i].neighbors[s].arr[x];
				if(eff_pairs[s][u].size()==0){
					continue;
				}
				for (int y=0; y<n_j; y++) {
					int v = dat.rnodes[t].arr[j].neighbors[s].arr[y];
					//if (dat.rnodes[s].arr[v].neighbors[t].size == 0) continue; //it cannot be zero
					//if(!eff_pairs[t][u].contains(v)) continue;
						
					
					delta[s][x][y] = 
							sim[s].get(u, v)
							* c[s]
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
		Iterator<Integer> iter_j = eff_pairs[t][i].iterator();
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
						
						sum += sim[s].get(u, v)
								/ (double) dat.rnodes[s].arr[v].neighbors[t].size;
						
					}
				}
				
				tmp += c[s] * sum / (double) n_i;
			}
			
			alpha[j] = Math.sqrt(sim[t].get(i, j) * tmp);
			if(Double.isNaN(alpha[j])){
				System.out.printf("alpha NaN  t:%d, i:%d, j:%d, tmp:%f, wij:%f\n",t,i,j,tmp, sim[t].get(i, j));
			}
			z += alpha[j];
		}
		if(z==0.0)
			return;
		
		iter_j = eff_pairs[t][i].iterator();
		while(iter_j.hasNext()){
			j = iter_j.next();
			alpha[j] /= z;
			if(Double.isNaN(alpha[j])){
				System.out.printf("alpha NaN  t:%d, i:%d, j:%d, z:%f, wij:%f\n",t,i,j, z, sim[t].get(i, j));
			}
		}
	}
	
	
	public double compute_distance () {
		double ret = 0;
		for (int t=0; t<dat.ntype; t++)  {
			for (int i=0; i<dat.lnodes[t].size; i++) {
				if(eff_pairs[t][i].size()>0){
					ret += compute_bdistance(t, i);
				}
			}
		}
		return ret;
	}

	private double compute_bdistance(int t, int i) {
		double sum1 = 0, sum2 = 0;
		Iterator<Integer> iter_j = eff_pairs[t][i].iterator();
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
						
						sub += sim_next[s].get(u, v) / 
								(double)dat.rnodes[s].arr[v].neighbors[t].size;
					}
				}
				
				sub /= (double)dat.lnodes[t].arr[i].neighbors[s].size;
				
				tmp += c_next[s] * sub;
			}
			sum1 += Math.sqrt(sim_next[t].get(i, j) * tmp);
			
			if(Double.isNaN(sum1)){
				System.out.printf("sum1 NaN t:%d, i:%d, j:%d, tmp:%f, wij: %f\n", t, i, j, tmp, sim_next[t].get(i, j));
			}
			
		}
		
		if(sum1>0)sum1 = Math.log(sum1);
		
		
		// for labeled data, 
		if(dat.lnodes[t].arr[i].label>=0){
			sum2 = Math.sqrt(sim_next[t].get(i, dat.lnodes[t].arr[i].label));
			/*
			iter_j = eff_pairs[t][i].iterator();
			while(iter_j.hasNext()){
				j = iter_j.next();
				
				sum2 = Math.sqrt(sim_next[t].get(i, dat.lnodes[t].arr[i].label));
				if(Double.isNaN(sum2)){
					System.out.printf("sum2 NaN t:%d, i:%d, j:%d, wij: %f\n", t, i, j, sim_next[t].get(i, j));
				}
			}
			if(sum2==0.0){
				System.out.printf("sum2 NaN t:%d, i:%d\n", t, i);
			}*/
			
			if (sum2 > 0) sum2 = Math.log(sum2);
		}
		
		
		
		return -(sum1 + mu * sum2);
	}
}
