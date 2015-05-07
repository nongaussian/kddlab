package similarity;

import graph.node;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;

import cm.cm_data;

import net.sourceforge.argparse4j.inf.Namespace;

import common.SparseMatrix;

public class SimRankD extends Similarity{
	public static double C=0.8;
	
	public SimRankD(Namespace nes){
		super(nes);
	}
	
	public SparseMatrix[] getSimRank(){
		return sim;
	}
	public SparseMatrix[] getNewSimRank(){
		return sim_next;
	}
	
	
	@Override
	public void run() throws IOException {
		computeSimRank();
	}

	
	
	public void computeSimRank(){
		System.out.println("Compute simrank: ");
		for(int i = 0; i < max_iter; i++){
			double convergence_ratio = updateSimRank(i);
			System.out.printf("Simrank iteration (%d/%d)\n", i, max_iter);
			if((convergence_ratio<th_convergence && i >= min_iter)){
				break;
			}
		}
		System.out.println();
	}
	
	
	public void initialize(cm_data dat){
		if (!firstrun ) return;
		
		initialize_effpairs(dat);
		firstrun = false;
		
		for(int t = 0; t<dat.ntype; t++){
			for(int i = 0; i<dat.lnodes[t].size; i++){
				if(dat.lnodes[t].arr[i].label>=0){
					sim[t].put(i, dat.lnodes[t].arr[i].label, 1.0);
				}else{
					int psize = eff_pairs[t][i].size();
					if(psize>0){
						Iterator<Integer> iter_j = eff_pairs[t][i].iterator();
						int j;
						while(iter_j.hasNext()){
							j = iter_j.next();
							sim_next[t].put(i, j, 1.0/psize);
						}
					}
				}
			}
		}
	}
	

	
	public double updateSimRank(int iter){
		double diff_sum = 0.0;
		double sim_sum = 0.0;
		//init simrank
		for (int t=0; t<dat.ntype; t++) {
			for (int i=0; i<dat.lnodes[t].size; i++) {
				Iterator<Integer> iter_j  = eff_pairs[t][i].iterator();
				int j;
				while(iter_j.hasNext()){
					j = iter_j.next();
					sim[t].put(i, j, sim_next[t].get(i, j));
				}
			}
		}
		int[] ntsi_s = new int[dat.ntype];
		double cs = 0.5;
		for (int t=0; t<dat.ntype; t++) {
			for (int i=0; i<dat.lnodes[t].size; i++) {
				Iterator<Integer> iter_j  = eff_pairs[t][i].iterator();
				for (int s=0; s<dat.ntype; s++) {
					if(!dat.rel[t][s])
						continue;
					ntsi_s[s] = dat.lnodes[t].arr[i].neighbors[s].size;
				}
				int j;
				while(iter_j.hasNext()){
					double sr_tmp;
					double sum = 0.0;
					j = iter_j.next();
					if(dat.lnodes[t].arr[i].label==j){
						sum = 1.0;
					}else{
						for (int s=0; s<dat.ntype; s++) {
							if(!dat.rel[t][s])
								continue;
							sr_tmp = 0.0;
							int ntsj = dat.rnodes[t].arr[j].neighbors[s].size;
							if(ntsi_s[s]>0&& ntsj>0){
								for(int x: dat.lnodes[t].arr[i].neighbors[s].arr){
									for(int y: dat.rnodes[t].arr[j].neighbors[s].arr){
										sr_tmp += sim[s].get(x, y);
									}
								}
								sum+=(cs*sr_tmp/ntsi_s[s]/ntsj);
							}
						}	
						sum *= C;
					}
					sim_sum += sum;
					diff_sum += Math.abs(sum-sim[t].get(i, j));
					sim_next[t].put(i, j, sum);
				}
			}
		}
		System.out.printf("%d/%d iteration  sum:%f, diffsum:%f convergence ratio: :%f\n",(iter+1),max_iter,sim_sum,diff_sum,diff_sum/sim_sum);
		return diff_sum/sim_sum;
	}

	
	
	public int getRadius() {
		return radius;
	}

	public void setRadius(int radius) {
		this.radius = radius;
	}


	
	
	
}
