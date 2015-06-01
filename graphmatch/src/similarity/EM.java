package similarity;

import graph.node;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap.Entry;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap.FastEntrySet;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import java.io.IOException;
import java.util.Arrays;

import net.sourceforge.argparse4j.inf.Namespace;
import cm.cm_data;

public class EM extends Similarity{
	double mu						= 1;
	public double[] alpha			= null;
	public double[] beta			= null;
	public double[][][] delta		= null;
	public double[] c				= null;
	public double[] c_next			= null;
	
	double[] tmp_w					= null;
	double[] tmp_wbar				= null;
	
	private double[] delta_w_par				= null;
	
	public EM(Namespace nes) {
		super(nes);
		
		mu = nes.getDouble("mu");
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
		this.dat = dat;
		
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
		c = new double[dat.ntype];
		c_next = new double[dat.ntype];
		for (int t=0; t<dat.ntype; t++) {
			c_next[t] = 1. / (double)dat.ntype;
		}
		
		
		temp_stat();
		//init variables
		alpha = new double[maxrnodesize];
		beta = new double[maxrnodesize];
		delta = new double[dat.ntype][maxlneighborsize][maxrneighborsize];
		tmp_w = new double[maxrnodesize];
		tmp_wbar = new double[maxrnodesize];
		delta_w_par = new double[maxrnodesize];
	}

	@Override
	public void run() throws IOException{
		// open an output file stream for logging scores
		//PrintStream distance_file = new PrintStream(new FileOutputStream("res/distance.dat"));
		
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
			//distance_file.println(distance + "\t" + converged_ratio + "\t" + gap + "\t" + (t_finish_iter - t_start));
			System.out.println("iteration (" + iter + ") distance: " + distance + "\t" + converged_ratio + " (" + gap + " ms)");

			
		}

		t_finish = System.currentTimeMillis();
		gap = t_finish - t_start;
		//distance_file.println("execution time: " + gap);
		//distance_file.close();

		// output the final model
		//save_model("res/final");
	}
	

	
	
	// compute the (approximate) expected model change when we ask to annotate for node i of type t
	
	public double compute_expected_model_change(int t, int i) {
		double sum = 0;
		node n_ti = dat.lnodes[t].arr[i];
		
		IntIterator iter_j = eff_pairs[t][i].iterator();
		int j;
		while(iter_j.hasNext()){
			j = iter_j.nextInt();
			if(sim_next[t].get(i, j)==0.0)
				continue;
			estimate_wnext(tmp_wbar, n_ti, t, i, j);
			
			double delta_tij = tmp_wbar[j]- sim_next[t].get(i, j);
			
			double diff = 0;
			// change of the neighbors of i
			for (int s=0; s<dat.ntype; s++) {
				if( t == s ){
					int x = i;
					double tmp = 0;
					IntIterator iter_y = dat.lnodes[s].arr[x].getAnnotations().keySet().iterator();
					//IntIterator iter_y = eff_pairs[s][x].iterator();
					int y;
					while(iter_y.hasNext()){
						y = iter_y.nextInt();
						tmp += Math.sqrt(sim_next[s].get(x, y)*tmp_wbar[y]);
					}
					if(tmp>0.0){
						diff += -Math.log(tmp);
					}
					
				}
				if(dat.rel[t][s]){
					//2
					computeDeltaWpar(t, i, j, s, delta_tij);
					for (int n_x=0; n_x<n_ti.neighbors[s].size; n_x++) {	
						int x = n_ti.neighbors[s].arr[n_x];
						IntIterator iter_y= null;
						int y;
						double tmp = 0;
						
						//2
						double x_const = c[t]/dat.lnodes[s].arr[x].neighbors[t].size;
						double sum_tmp_w = 0.0;
						iter_y = eff_pairs[s][x].iterator();
						while(iter_y.hasNext()){
							y = iter_y.nextInt();
							tmp_w[y] += (sim_next[s].get(x, y)+x_const*delta_w_par[y]);
							sum_tmp_w += tmp_w[y];
						}
						iter_y = eff_pairs[s][x].iterator();
						while(iter_y.hasNext()){
							y = iter_y.nextInt();
							
							//Multiply normalized conditional similarity
							tmp += Math.sqrt(sim_next[s].get(x, y)*tmp_w[y]/sum_tmp_w);
						}
						
						
						/*/1
						//estimate_w(tmp_w, t, i, j, s, x, delta_tij);
						
						iter_y = eff_pairs[s][x].iterator();
						while(iter_y.hasNext()){
							y = iter_y.nextInt();
							tmp += Math.sqrt(sim_next[s].get(x, y)*tmp_w[y]);
						}
						*/
						
						if(tmp>0.0){
							diff += -Math.log(tmp);
						}
						if(Double.isNaN(diff)||Double.isInfinite(diff)){
							System.out.printf("diff: %f, tmp:%f\n",diff,tmp);
						}
					}
				}
			}
			sum += diff * sim_next[t].get(i, j);
		}
		
		return sum;
	}
	

	
	
	private void estimate_wnext(double[] w_next, node n_ti, int t, int i, int j){
		Arrays.fill(w_next, 0,dat.lnodes[t].size, 0.0);
		if(n_ti.getNAnnotatios()==0){
			w_next[j] = 1.0; 
			return;
		}
		FastEntrySet es = n_ti.getAnnotations().int2IntEntrySet();
		
		boolean j_ann = false;
		double sum = 1.0;
		ObjectIterator<Int2IntMap.Entry> iter = es.iterator();
		Int2IntMap.Entry entry;
		while(iter.hasNext()){
			entry = iter.next();
			w_next[entry.getIntKey()] = entry.getIntValue();
			sum +=w_next[entry.getIntKey()];
			if(entry.getIntKey()==j){
				j_ann = true;
			}
		}
		w_next[j]++;
		
		
		iter = es.iterator();
		while(iter.hasNext()){
			entry = iter.next();
			w_next[entry.getIntKey()] /= sum;
		}
		
		if(!j_ann){
			w_next[j]/=sum;
		}
	}
	
	
	private void computeDeltaWpar(int t, int i, int j,int s, double delta_tij){
		Arrays.fill(delta_w_par, 0.0);
		int[] j_neighbors = dat.rnodes[t].arr[j].neighbors[s].arr;
		if(j_neighbors.length > 0 && delta_tij>0.0){
			double nts_j = (double) j_neighbors.length;
			for (int idx_y=0; idx_y< j_neighbors.length; idx_y++){
				delta_w_par[j_neighbors[idx_y]] = delta_tij/nts_j;
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
				ObjectIterator<Entry> sim_iter = sim_next[t].getEntryIterator(i);

				while(sim_iter.hasNext()){
					Entry entry = sim_iter.next();
					sim[t].put(i,entry.getIntKey(),entry.getDoubleValue());
					entry.setValue(0.0);

				}
				
			}
		}
		
		// em step
		for (int t=0; t<dat.ntype; t++) {
			for (int i=0; i<dat.lnodes[t].size; i++) {
				// compute variables use in this iteration only
				compute_alpha(t, i, alpha);	//alpha^t_i*
				
				if(dat.lnodes[t].arr[i].getNAnnotatios() > 0){
					compute_beta(t, i, beta);	//beta^t_i*
				}
				// compute a w distribution
				//*/
				
				int j;
				/*/
				Entry entry;
				
				ObjectIterator<Entry> sim_iter = sim_next[t].getEntryIterator(i);
				while(sim_iter.hasNext()){
					entry = sim_iter.next();
					j = entry.getIntKey();
				/*/
				IntIterator iter_j = eff_pairs[t][i].iterator();
				while(iter_j.hasNext()){
					j = iter_j.nextInt();
				//*/
					
					if(alpha[j]>0.0){
						compute_delta(t, i, j, delta);//delta^ts_ij**
						
						// add alpha
						//entry.setValue(alpha[j]);
						sim_next[t].add(i, j, alpha[j]);
						
						
						// distribute delta
					
						for (int s=0; s<dat.ntype; s++) {
							if (!dat.rel[t][s]) continue;
							int n_i = dat.lnodes[t].arr[i].neighbors[s].size;
							int n_j = dat.rnodes[t].arr[j].neighbors[s].size;
							
							for (int x=0; x<n_i; x++) {
								int u = dat.lnodes[t].arr[i].neighbors[s].arr[x];
								for (int y=0; y<n_j; y++) {
									int v = dat.rnodes[t].arr[j].neighbors[s].arr[y];
									double tmp = alpha[j] * delta[s][x][y];
									if(tmp > 0.0){
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
					//add beta
					if (dat.lnodes[t].arr[i].getNAnnotatios() > 0) {
						sim_next[t].add(i, j, beta[j]*mu);
					}
				}
				
				

			}
		}
		
		// normalize w
		for (int t=0; t<dat.ntype; t++) {
			for (int i=0; i<dat.lnodes[t].size; i++) {
				ObjectIterator<Entry> sim_iter = sim_next[t].getEntryIterator(i);
				
				z = 0;

				while(sim_iter.hasNext()){
					z += sim_iter.next().getDoubleValue();
				}
				
				if(z>0.0){
					sim_iter = sim_next[t].getEntryIterator(i);
					Entry entry = null;
					while(sim_iter.hasNext()){
						entry = sim_iter.next();
						entry.setValue(entry.getDoubleValue()/z);
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
					//if(!eff_pairs[s][u].contains(v)) continue;
						
					
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
		IntIterator iter_j = eff_pairs[t][i].iterator();
		int j;
		while(iter_j.hasNext()){
			j = iter_j.nextInt();
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
						
						//Impossible condition
						//if (dat.rnodes[s].arr[v].neighbors[t].size == 0) continue;
						
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
			j = iter_j.nextInt();
			alpha[j] /= z;
			if(Double.isNaN(alpha[j])){
				System.out.printf("alpha NaN  t:%d, i:%d, j:%d, z:%f, wij:%f\n",t,i,j, z, sim[t].get(i, j));
			}
		}
	}
	
	private void compute_beta(int t, int i, double[] beta) {
		Arrays.fill(beta, 0.0);
		
		ObjectIterator<Int2IntMap.Entry> iter = dat.lnodes[t].arr[i].getAnnotations().int2IntEntrySet().iterator();
		
		while(iter.hasNext()){
			Int2IntMap.Entry entry = iter.next();
			
			beta[entry.getIntKey()] = Math.sqrt(sim[t].get(i, entry.getIntKey()) * dat.lnodes[t].arr[i].getWbar(entry.getIntKey()));
			
		}
		
		/*
		ObjectIterator<Entry> sim_iter = sim[t].getEntryIterator(i);
		while(sim_iter.hasNext()){
			Entry entry = sim_iter.next();
			if(entry.getDoubleValue()>0.0){
				beta[entry.getIntKey()] = 
						Math.sqrt(entry.getDoubleValue() * dat.lnodes[t].arr[i].getWbar(entry.getIntKey())); 
			}
		}
		*/	
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
		node ti = dat.lnodes[t].arr[i];
		double sum1 = 0, sum2 = 0;
		int j;
		
		
		ObjectIterator<Entry> sim_iter = sim_next[t].getEntryIterator(i);
		while(sim_iter.hasNext()){
			Entry entry = sim_iter.next();
			
		/*
		IntIterator iter_j = eff_pairs[t][i].iterator();
		while(iter_j.hasNext()){
			j = iter_j.nextInt();*/
			j = entry.getIntKey();
			double tmp = 0;
			
			for (int s=0; s<dat.ntype; s++) {
				if (!dat.rel[t][s]) continue;
				
				if (ti.neighbors[s].size == 0) continue;
				
				double sub = 0;
				for (int u:ti.neighbors[s].arr) {
					for (int v:dat.rnodes[t].arr[j].neighbors[s].arr) {
						sub += sim_next[s].get(u, v) / 
								(double)dat.rnodes[s].arr[v].neighbors[t].size;
					}
				}
				
				tmp += c_next[s] * sub / ti.neighbors[s].size;
			}
			sum1 += Math.sqrt(entry.getDoubleValue() * tmp);
			
			if(Double.isNaN(sum1)){
				System.out.printf("sum1 NaN t:%d, i:%d, j:%d, tmp:%f, wij: %f\n", t, i, j, tmp, sim_next[t].get(i, j));
			}
			
		}
		
		if(sum1>0)sum1 = Math.log(sum1);
		
		
		// for labeled data, 
		if(ti.getNAnnotatios()>0){
			IntIterator iter = ti.getAnnotations().keySet().iterator();
			while(iter.hasNext()){
				j = iter.nextInt();
				sum2 += Math.sqrt(sim_next[t].get(i, j)*ti.getWbar(j));
				//sum2 = Math.sqrt(sim_next[t].get(i, dat.lnodes[t].arr[i].getLabel()));
			}
			
			/*
			iter_j = eff_pairs[t][i].iterator();
			while(iter_j.hasNext()){
				j = iter_j.nextInt();
				
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

	public double compute_entropy(int t, int i) {
		double entropy = 0.0;
		
		ObjectIterator<Entry> sim_iter = sim_next[t].getEntryIterator(i);
		while(sim_iter.hasNext()){
			Entry entry = sim_iter.next();
			if(entry.getDoubleValue()>0.0){
				entropy += (entry.getDoubleValue()*Math.log(entry.getDoubleValue()));
			}
		}
		
		
		return -entropy;
	}
	
	
}
