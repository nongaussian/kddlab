package similarity;

import graph.node;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;

import java.io.IOException;
import java.util.Iterator;

import net.sourceforge.argparse4j.inf.Namespace;
import cm.QueryNode;
import cm.cm_data;

import common.SparseMatrix;

public abstract class Similarity{
	boolean firstrun = true;
	
	protected cm_data dat					= null;
	protected double th_convergence	= 1.0e-5;
	protected int 	max_iter			= 100;
	protected int 	min_iter			= 5;
	protected int 	radius				= -1;
	
	public IntOpenHashSet[][] eff_pairs	= null; 

	
	public int maxrnodesize						= 0;
	public int maxlneighborsize					= 0;
	public int maxrneighborsize					= 0;
	
	public Similarity(Namespace nes) {
		radius = nes.getInt("radius");
		max_iter					= nes.getInt("niter");
		th_convergence				= nes.getDouble("thres");
	}
	
	
	public SparseMatrix[] sim;
	public SparseMatrix[] sim_next;
	
	public void set_dat (cm_data dat) {
		this.dat = dat;
		
	}
	protected void initSimMatrix(){
		sim = new SparseMatrix[dat.ntype];
		sim_next = new SparseMatrix[dat.ntype];	
		
		for (int t=0; t<dat.ntype; t++) {
			sim[t] = new SparseMatrix(dat.lnodes[t].size);
			sim_next[t] = new SparseMatrix(dat.lnodes[t].size);
		}
	}
	
	
	
	
	
	
	public SparseMatrix[] getSim(){
		return sim;
	}
	public SparseMatrix[] getNewSim(){
		return sim_next;
	}
	
	public int getRadius() {
		return radius;
	}
	public void setRadius(int radius) {
		this.radius = radius;
	}
	
	
	
	
	
	
	
	
	public abstract void initialize(cm_data dat);
	
	@SuppressWarnings("unchecked")
	public void initialize_effpairs(cm_data dat){
		eff_pairs = new IntOpenHashSet[dat.ntype][];
		IntOpenHashSet[] lgroup = new IntOpenHashSet[dat.ntype];
		IntOpenHashSet[] rgroup = new IntOpenHashSet[dat.ntype];
		
		for(int t = 0; t<dat.ntype; t++){
			lgroup[t] = new IntOpenHashSet();
			rgroup[t] = new IntOpenHashSet();
			eff_pairs[t] = new IntOpenHashSet[dat.lnodes[t].size];
			for(int i = 0; i<eff_pairs[t].length; i++){
				eff_pairs[t][i] = new IntOpenHashSet();
			}
		}
		

		for(int t = 0; t<dat.ntype; t++){
			for(int i = 0; i<eff_pairs[t].length; i++){
				if(dat.lnodes[t].arr[i].label>=0){
					eff_pairs[t][i].add(dat.lnodes[t].arr[i].label);
					register_node(lgroup, dat.lnodes[t].arr[i], t, radius, true);
					register_node(rgroup, dat.rnodes[t].arr[dat.lnodes[t].arr[i].label], t, radius, false);
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
	}
	
	public void update_effpairs(QueryNode[] queries, cm_data dat){
		
		IntOpenHashSet[] lgroup = new IntOpenHashSet[dat.ntype];
		IntOpenHashSet[] rgroup = new IntOpenHashSet[dat.ntype];
		for(int t = 0; t<dat.ntype; t++){
			lgroup[t] = new IntOpenHashSet();
			rgroup[t] = new IntOpenHashSet();
		}
		int[] l_list,r_list;
		for (QueryNode q : queries) {
			if(q.matched_id >= 0){
				eff_pairs[q.t][q.id].add(q.matched_id);
				register_node(lgroup, dat.lnodes[q.t].arr[q.id], q.t, radius, true);
				register_node(rgroup, dat.rnodes[q.t].arr[q.matched_id], q.t, radius, false);
				
				sim_next[q.t].put(q.id, dat.lnodes[q.t].arr[q.id].label, 1.0);
			}
			
			l_list = lgroup[q.t].toArray(new int[0]);
			r_list = rgroup[q.t].toArray(new int[0]);
			for(Integer x:l_list){
				for(Integer y:r_list){
					eff_pairs[q.t][x].add(y);
				}
			}
			
			lgroup[q.t].clear();
			rgroup[q.t].clear();
			
		}
		
	}
	
	public void smoothing(){
		for(int t = 0; t < dat.ntype; t++){
			for (int i=0; i<dat.lnodes[t].size; i++) {
				
				IntIterator iter_j = eff_pairs[t][i].iterator();
				int j;
				double sm_f = .2/eff_pairs[t][i].size();
				double sum = 0.0;
				while(iter_j.hasNext()){
					j = iter_j.nextInt();
					sim_next[t].add(i, j, sm_f);
					sum += sim_next[t].get(i, j);
				}
				
				iter_j = eff_pairs[t][i].iterator();
								
				while(iter_j.hasNext()){
					j = iter_j.nextInt();
					sim_next[t].put(i, j, sim_next[t].get(i, j)/sum);
				}
				
			}
		}
		
	}
	private void register_node(IntOpenHashSet[] nodeset, node n, int type, int radius, boolean left){
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
	
	public abstract void run() throws IOException;
}
