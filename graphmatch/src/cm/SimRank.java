package cm;

import graph.node;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;

import net.sourceforge.argparse4j.inf.Namespace;

import common.SparseMatrix;

public class SimRank extends cm_learner{
	//SparseIntMatrix[] neighbor_pairs;
	public HashSet<Integer>[][] eff_pairs		= null; 
	SparseMatrix[] sim;
	SparseMatrix[] sim_next;
	public static double C=0.8;
	public boolean first = true;
	
	public int nIter;
	public double th_convergence;
	private int radius							= -1;
	

	public int stype;
	public static final int SIM_TYPE_SR 		= 0;
	//public static final int SIM_TYPE_SRKL		= 1;
	
	
	public int wtype;
	//public static final int WEIGHT_TYPE_NPAIRS 	= 0;
	public static final int WEIGHT_TYPE_UNIFORM = 1;
	
	
	
	public SimRank(Namespace nes){
		this.stype 			= SIM_TYPE_SR;
		this.wtype 			= WEIGHT_TYPE_UNIFORM;
		this.nIter 			= nes.getInt("niter");
		this.th_convergence = nes.getDouble("thres");
		this.setRadius(nes.getInt("radius"));
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
		for(int i = 0; i < nIter; i++){
			double convergence_ratio = updateSimRank(i);
			System.out.printf("Simrank iteration (%d/%d)\n", i, nIter);
			if((convergence_ratio<th_convergence&&i>3)){
				break;
			}
		}
		System.out.println();
	}
	
	
	public void initialize(cm_data dat){
		set_dat(dat);
		eff_pairs = new HashSet[dat.ntype][];
		HashSet<Integer>[] lgroup = new HashSet[dat.ntype];
		HashSet<Integer>[] rgroup = new HashSet[dat.ntype];
		sim = new SparseMatrix[dat.ntype];
		sim_next = new SparseMatrix[dat.ntype];
		for(int t = 0; t<dat.ntype; t++){
			sim[t] = new SparseMatrix(dat.lnodes[t].size);
			sim_next[t] = new SparseMatrix(dat.lnodes[t].size);
			lgroup[t] = new HashSet<Integer>();
			rgroup[t] = new HashSet<Integer>();
			eff_pairs[t] = new HashSet[dat.lnodes[t].size];
			for(int i = 0; i<eff_pairs[t].length; i++){
				eff_pairs[t][i] = new HashSet<Integer>();
			}
		}
		

		for(int t = 0; t<dat.ntype; t++){
			for(int i = 0; i<eff_pairs[t].length; i++){
				if(dat.lnodes[t].arr[i].label>=0){
					eff_pairs[t][i].add(dat.lnodes[t].arr[i].label);
					register_node(lgroup, dat.lnodes[t].arr[i], t, getRadius(), true);
					register_node(rgroup, dat.rnodes[t].arr[dat.lnodes[t].arr[i].label], t, getRadius(), false);
				}


				Integer[] l_list,r_list;
				
				for(int t_ = 0; t_<dat.ntype; t_++){
					l_list = lgroup[t_].toArray(new Integer[0]);
					r_list = rgroup[t_].toArray(new Integer[0]);
					for(Integer x:l_list){
						for(Integer y:r_list){
							eff_pairs[t_][x].add(y);
						}
					}
					lgroup[t_].clear();
					rgroup[t_].clear();
				}
				
			}
		}
		
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
	
	public void update_effpairs(QueryNode[] queries, cm_data dat){
		//TODO: implementation
		/*
		 * update effective pairs
		 * update w
		 *  
		 */
		HashSet<Integer>[] lgroup = new HashSet[dat.ntype];
		HashSet<Integer>[] rgroup = new HashSet[dat.ntype];
		for(int t = 0; t<dat.ntype; t++){
			lgroup[t] = new HashSet<Integer>();
			rgroup[t] = new HashSet<Integer>();
		}
		Integer[] l_list,r_list;
		for (QueryNode q : queries) {
			if(q.matched_id >= 0){
				eff_pairs[q.t][q.id].add(q.matched_id);
				register_node(lgroup, dat.lnodes[q.t].arr[q.id], q.t, getRadius(), true);
				register_node(rgroup, dat.rnodes[q.t].arr[q.matched_id], q.t, getRadius(), false);
				
				sim[q.t].put(q.id, dat.lnodes[q.t].arr[q.id].label, 1.0);
			}
			
			l_list = lgroup[q.t].toArray(new Integer[0]);
			r_list = rgroup[q.t].toArray(new Integer[0]);
			for(Integer x:l_list){
				for(Integer y:r_list){
					eff_pairs[q.t][x].add(y);
				}
			}
			
			lgroup[q.t].clear();
			rgroup[q.t].clear();
			
		}
		//smoothing();
		
	}	
	private void register_node(HashSet<Integer>[] nodeset, node n, int type, int radius, boolean left){
		if(radius==0){
			return;
		}
		for(int t = 0; t < dat.ntype; t++){
			if (!dat.rel[type][t]) continue;
			for(int x = 0; x < n.neighbors[t].size; x++){
				nodeset[t].add(n.neighbors[t].arr[x]);
				register_node(nodeset, left?dat.lnodes[t].arr[n.neighbors[t].arr[x]]:dat.rnodes[t].arr[n.neighbors[t].arr[x]], t, radius-1,left);
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
		System.out.printf("%d/%d iteration  sum:%f, diffsum:%f convergence ratio: :%f\n",(iter+1),nIter,sim_sum,diff_sum,diff_sum/sim_sum);
		return diff_sum/sim_sum;
	}

	
	
	public int getRadius() {
		return radius;
	}

	public void setRadius(int radius) {
		this.radius = radius;
	}
	
	
	
}
