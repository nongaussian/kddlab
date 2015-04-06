package cm;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

import common.SparseMatrix;

/*
 * manage model parameters
 * - variables
 * - functions to save to & load from files 
 */
public class cm_model {
	// constant
	public double mu							= 1.;
	public int radius							= -1;
	
	// data
	private cm_data dat 						= null;
	public HashSet<Integer>[][] eff_pairs		= null; 
	
	
	// model parameters
	public SparseMatrix[] w 								= null;
	public SparseMatrix[] w_next							= null;
	public double[] alpha						= null;
	public double[][][] delta					= null;
	public double[] c							= null;
	public double[] c_next						= null;
	
	public int maxrnodesize						= 0;
	public int maxlneighborsize					= 0;
	public int maxrneighborsize					= 0;
	
	public boolean is_cont						= false;
	public boolean firstrun						= true;

	public cm_model(int radius) {
		this.radius = radius;
	}
	
	public void init_learner (cm_data dat, double mu) {
		this.dat = dat;
		this.mu = mu;
		
		c = new double[dat.ntype];
		c_next = new double[dat.ntype];
		
		w = new SparseMatrix[dat.ntype];
		w_next = new SparseMatrix[dat.ntype];
		
		
		for (int t=0; t<dat.ntype; t++) {
			w[t] = new SparseMatrix(dat.lnodes[t].size);
			w_next[t] = new SparseMatrix(dat.lnodes[t].size);
		}
		
		
		temp_stat();
		
		alpha = new double[maxrnodesize];
		
		delta = new double[dat.ntype][maxlneighborsize][maxrneighborsize];
	}
	
	public void init_tester (cm_data dat, double mu) {
		this.dat = dat;
		this.mu = mu;
		
		c = new double[dat.ntype];
		
		w = new SparseMatrix[dat.ntype];
		for (int t=0; t<dat.ntype; t++) {
			w[t] = new SparseMatrix(dat.lnodes[t].size);
		}
		
		temp_stat();
		
		is_cont = true;
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
	
	public void init_with(String filename) {
		load_model(filename);
		firstrun = false;
		is_cont = true;
	}

	
	
	@SuppressWarnings("unchecked")
	private void initialize_effpairs(cm_data dat){
		eff_pairs = new HashSet[dat.ntype][];
		HashSet<Integer>[] lgroup = new HashSet[dat.ntype];
		HashSet<Integer>[] rgroup = new HashSet[dat.ntype];
		
		for(int t = 0; t<dat.ntype; t++){
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
				register_node(lgroup, dat.lnodes[q.t].arr[q.id], q.t, radius, true);
				register_node(rgroup, dat.rnodes[q.t].arr[q.matched_id], q.t, radius, false);
				
				w[q.t].put(q.id, dat.lnodes[q.t].arr[q.id].label, 1.0);
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
	
	/*
	 * setting variational variables
	 */
	public void random_initialize_var(cm_data dat) {
		if (!firstrun && is_cont) return;
		initialize_effpairs(dat);
		firstrun = false;
		
		// init w
		@SuppressWarnings("unused")
		Random rand = new Random(System.currentTimeMillis());

		
		for (int t=0; t<dat.ntype; t++) {
			for (int i=0; i<dat.lnodes[t].size; i++) {
				Integer[] cand_r = eff_pairs[t][i].toArray(new Integer[0]);
				if(cand_r.length==0){
					continue;
				}
				double tmp = 1. / cand_r.length;
				
				for (int j=0; j<cand_r.length; j++) {
					w_next[t].put(i, cand_r[j], tmp);
				}
			}
		}
		
		// init c
		for (int t=0; t<dat.ntype; t++) {
			c_next[t] = 1. / (double)dat.ntype;
		}
	}

	public void save_model(String prefix) {
		// print w
		save_w(prefix);

		// print c
		save_c(prefix);
	}

	private void save_w(String prefix) {
		try {
			PrintStream out = new PrintStream(new FileOutputStream(prefix + ".w"));
			for (int t=0; t<dat.ntype; t++) {
				for(int i = 0 ; i < dat.lnodes[t].size ; i++) {
					Iterator<Integer> iter = eff_pairs[t][i].iterator();
					int j;
					while(iter.hasNext()){
						j = iter.next();
						out.println ("" + t + "\t" + i + "\t" + j + "\t" + w[t].get(i, j));
					}
				}
			}
			out.close();
		}
		catch(IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void save_c(String prefix) {
		try {
			PrintStream out = new PrintStream(new FileOutputStream(prefix + ".c"));
			out.print("" + c[0]);
			for(int t = 1 ; t < dat.ntype ; t++) {
				out.print("\t" + c[t]);
			}
			out.println();
			out.close();
		}
		catch(IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public void save_label(String prefix) {
		try {
			for (int t=0; t<dat.ntype; t++) {
				PrintStream out = new PrintStream(
						new FileOutputStream(prefix + ".label." + dat.nodetypes[t]));
				
				for(int i = 1 ; i < dat.lnodes[t].size; i++) {
					if (dat.lnodes[t].arr[i].label >= 0) {
						out.println(i + "\t" + dat.lnodes[t].arr[i].label);
					}
				}
				out.close();
			}
		}
		catch(IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public void load_model(String prefix) {
		// read w
		load_w(prefix);

		// read c
		load_c(prefix);
	}

	private void load_w(String prefix) {
		System.out.println("load: " + prefix + ".w");
		try {
			BufferedReader reader = new BufferedReader(new FileReader(prefix + ".w"));
			String line = null;
			
			while((line = reader.readLine()) != null) {
				String[] arr = line.split("\t");
				
				int t = Integer.parseInt(arr[0]);
				int i = Integer.parseInt(arr[1]);
				int j = Integer.parseInt(arr[2]);
				double val = Double.parseDouble(arr[3]);
				
				w_next[t].put(i, j, val);
			}
			reader.close();
		}
		catch(IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void load_c(String prefix) {
		System.out.println("load: " + prefix + ".c");
		try {
			BufferedReader reader = new BufferedReader(new FileReader(prefix + ".c"));
			String line = null;
			
			while((line = reader.readLine()) != null) {
				String[] arr = line.split("\t");
				
				int t=0;
				for (String val : arr) {
					c_next[t++] = Double.parseDouble(val);
				}
			}
			reader.close();
		}
		catch(IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}
