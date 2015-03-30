package sim;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Random;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import cm.CandNode;
import cm.CrowdMatch;
import cm.QueryNode;
import cm.cm_data;
import cm.cm_model;

/*
 * We simulate a crowdsourcing process with
 * two exactly the same heterogeneous graphs
 */
public class Simulation1 {
	static final int CONST_MAX_MATCH	= 2*1000;
	static Random r = new Random();
	
	int nmatch							= 0;
	
	int nquery							= 100;
	int ncand							= 100;
	int nmaxquery						= -1;
	
	double mu							= 1;
	public int radius					= -1;
	
	cm_data dat							= null;
	cm_model model						= null;
	CrowdMatch learner					= null;
	
	String lprefix						= null;
	String rprefix						= null;
	String wprefix						= null;
	String outputfile					= null;
	
	double[] tmp_w						= null;
	
	public Simulation1 (Namespace nes) {
		// data parameters
		lprefix						= nes.getString("lprefix");
		rprefix						= nes.getString("rprefix");
		wprefix						= nes.getString("wprefix");
		outputfile					= nes.getString("output");
		nmaxquery					= nes.getInt("nmaxq");
		nquery						= nes.getInt("nq");
		ncand						= nes.getInt("ncand");
		radius						= nes.getInt("radius");
		
		
		learner						= new CrowdMatch(nes);
	}

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Namespace nes 				= parseArguments(args);
		Simulation1 sim 			= new Simulation1 (nes);
		sim.init();
		sim.run();
	}
	
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
						.help("Threshold of log-likelihood ratio for convergence test");
			parser.addArgument("-lprefix")
						.type(String.class)
						.required(true)
						.help("Prefix name of left graph");
			parser.addArgument("-rprefix")
						.type(String.class)
						.required(true)
						.help("Prefix name of right graph");
			parser.addArgument("-wprefix")
						.type(String.class)
						.help("Prefix name of model parameters to use for initialization");
			parser.addArgument("-output")
						.type(String.class)
						.required(true)
						.help("Output file name");
			parser.addArgument("-nq")
		                .type(Integer.class)
		                .setDefault(10)
		                .help("Number of queries in each iteration");
			parser.addArgument("-nmaxq")
				        .type(Integer.class)
				        .setDefault(-1)
				        .help("Maximum number of queries (default: infinity)");
			parser.addArgument("-radius")
						.type(Integer.class)
						.setDefault(-1);
			parser.addArgument("-ncand")
						.type(Integer.class)
						.setDefault(-1);
			Namespace res = parser.parseArgs(args);
			System.out.println(res);
			return res;
		} catch (ArgumentParserException e) {
			parser.handleError(e);
		}
		return null;
	}
	
	public void init() {
		dat							= new cm_data(lprefix, rprefix);
		model						= new cm_model(radius);
		model.init_learner(dat, mu);
		if (wprefix != null) model.init_with(wprefix);
		model.is_cont = true;
		
		learner.set_dat(dat);
		learner.set_model(model);
	}

	public void run () throws IOException {
		int cnt = 0;
		int totalcost = 0;
		
		PrintStream out = new PrintStream(new FileOutputStream(outputfile));
		
		QueryNode[] queries = new QueryNode[nquery];
		CandNode[] cand = new CandNode[model.maxrnodesize];
		for (int i=0; i<model.maxrnodesize; i++) cand[i] = new CandNode();
		tmp_w = new double[model.maxrnodesize];
		
		System.out.println("now, we start to find queries..");
		
		while ((nmaxquery == -1 || cnt < nmaxquery) && (cnt < dat.totallnode)&&(nmatch<CONST_MAX_MATCH)) {
			
			// run model
			learner.run();
			
			// select a query & candidates
			int nq = select_query (nquery, queries);
			
			int tmpcnt = 0;
			for (int i=0; i<nq; i++) {
				QueryNode q = queries[i];
				int nc = select_candidates(q, cand, ncand);

				double cost = match(q, cand, nc);
				totalcost += cost;
				System.out.println("CNT=" + (cnt + tmpcnt) + ", COST: " + cost + ", ACC: " + totalcost);
				out.println("" + (cnt + tmpcnt) + "\t" + cost + "\t" + totalcost + "\t" + q.t + "\t" + q.id);
				
				tmpcnt++;
			}
			
			model.save_label("res/test");
			
			cnt += queries.length;

			// set labels in dat.lnodes
			for (QueryNode q : queries) {
				if (q.matched_id >= 0){
					dat.lnodes[q.t].arr[q.id].label = q.matched_id;
					dat.rnodes[q.t].arr[q.matched_id].label = q.id;
					System.out.printf("match node (t,i,j,w)=(%2d,%5d,%5d,%5f)\n",q.t,q.id,q.matched_id,model.w[q.t].get(q.id, q.matched_id));
					nmatch++;
				}
			}
			System.out.printf("#Matchs %d\n",nmatch);
			model.update_effpairs(queries, dat);
			stat();
		}
		
		out.close();
	}



	// select the best n queries & return the actual number of selected queries
	private int select_query(int n, QueryNode[] res) {
		PriorityQueue<QueryNode> topk = new PriorityQueue<QueryNode> (n, new Comparator<QueryNode> () {
			public int compare (QueryNode q1, QueryNode q2) {
				return (q1.diff < q2.diff) ? -1 : ((q1.diff > q2.diff) ? 1 : 0);
			}
		});
		
		for (int t=0; t<dat.ntype; t++) {
			for (int i=0; i<dat.lnodes[t].size; i++) {
				if (dat.lnodes[t].arr[i].label >= 0) continue;
				            
				double val = compute_expected_model_change(t, i);
				//System.out.println("\texpected model chanage of node " + i + " of type " + dat.nodetypes[t] + ": " + val);
				if (topk.size() < n) {
					topk.add(new QueryNode(t, i, val));
				}
				else if (topk.peek().diff < val) {
					QueryNode tmp = topk.remove();
					tmp.t = t;
					tmp.id = i;
					tmp.diff = val;
					topk.add(tmp);
				
				}
			}
		}
		
		if(topk.peek().diff==0.0){
			ArrayList<QueryNode> zero_queries = new ArrayList<QueryNode>();
			
			while(topk.size()>0&&topk.peek().diff==0.0){
				zero_queries.add(topk.poll());
			}
			
			HashSet<IntInt> selected = new HashSet<IntInt>();
			for(QueryNode nonzero_node: topk){
				selected.add(new IntInt(nonzero_node.t, nonzero_node.id));
			}
			
			IntInt key = new IntInt(-1,-1);
			while(topk.size( )< n){
				int t = r.nextInt(dat.ntype);
				int i = r.nextInt(dat.lnodes[t].size);
				key.v1 = t;
				key.v2 = i;
				if(!selected.contains(key)){
					selected.add(new IntInt(key));
					QueryNode qn = zero_queries.remove(zero_queries.size()-1);
					qn.t = t;
					qn.id = i;
					qn.diff = 0.0;
					topk.add(qn);
				}
			}

		}
		
		int idx=0;
		for (QueryNode q : topk) {
			System.out.println("=> selected: " + q.id + " of type " + dat.nodetypes[q.t]+"  expected model change"+q.diff);
			res[idx++] = q;
		}
		return idx;
	}
	
	// compute the (approximate) expected model change when we ask to annotate for node i of type t
	private double compute_expected_model_change(int t, int i) {
		double sum = 0;
		
		Iterator<Integer> iter_j = model.eff_pairs[t][i].iterator();
		int j;
		while(iter_j.hasNext()){
			j = iter_j.next();
			double diff = 0;

			// change of the node i
			diff += -Math.log(model.w[t].get(i, j)) / 2.;
			
			// change of the neighbors of i
			for (int s=0; s<dat.ntype; s++) {
				if (!dat.rel[t][s]) continue;
				
				for (int x=0; x<dat.lnodes[t].arr[i].neighbors[s].size; x++) {
					int u = dat.lnodes[t].arr[i].neighbors[s].arr[x];
					double tmp = 0;
					
					estimate_w(tmp_w, t, i, j, s, u);
					Iterator<Integer> iter_v = model.eff_pairs[t][i].iterator();
					int v;
					while(iter_v.hasNext()){
						v = iter_v.next();
						tmp += Math.sqrt(model.w[s].get(u, v) * tmp_w[v]);
					}
					diff += tmp;
					if(Double.isNaN(diff)){
						System.out.printf("tmp:%f\n",tmp);
						 iter_v = model.eff_pairs[t][i].iterator();
						while(iter_v.hasNext()){
							v = iter_v.next();
							System.out.printf("s:%d,u:%d,v:%d,w_suv:%f, tmp_w[v]=%f \n",s,u,v,model.w[s].get(u, v),tmp_w[v]);
						}
					}
				}
			}
			sum += diff * model.w[t].get(i, j);
		}
		
		return sum;
	}
	
	private void stat(){
		int count = 0;
		int eff_l = 0;
		
		
		for (int t=0; t<dat.ntype; t++) {
			for (int i=0; i<dat.lnodes[t].size; i++) {
				count++;
				if(model.eff_pairs[t][i].size()>0){
					eff_l++;
				}
			}
		}
		System.out.printf("count tot:%d, leff:%d\n", count,eff_l);
	}

	// estimate w[s].arr[u]
	private void estimate_w(double[] w, int t, int i, int j, int s, int x) {
		double tmp = 0;
		Arrays.fill(w, 0.0);
		
		
		//check whether i and x are friends or not
		int[] x_neighbors = dat.lnodes[s].arr[x].neighbors[t].arr;
		boolean ix_connected = false;
		for(int xn: x_neighbors){
			if(xn==i){
				ix_connected = true;
				break;
			}
		}
		
		if(!ix_connected){
			return;
		}
		
		int[] j_neighbors = dat.rnodes[t].arr[j].neighbors[s].arr;
		
		double nst_x = (double) x_neighbors.length;
		double nts_j = (double) j_neighbors.length;
		
		for(int y:j_neighbors){
			w[y] = model.w[s].get(x, y)+(1.0-model.w[t].get(i, j)/nst_x/nts_j);
		}
		
		return;			
		
	}



	// choose n candidates for the node i & return the actual number of candidates
	private int select_candidates(QueryNode q, CandNode[] cand, int maxcand) {
			
		Iterator<Integer> iter_j = model.eff_pairs[q.t][q.id].iterator();
		int j;
		int idx=0;
		while(iter_j.hasNext()){
			j = iter_j.next();
			if(dat.rnodes[q.t].arr[j].label<0){
				cand[idx].id = j;
				cand[idx].w = model.w[q.t].get(q.id, j);
				idx++;
			}
		}
		
		int n_cand = Math.min(maxcand, idx);
		
		Arrays.sort(cand, 0,idx, new Comparator<CandNode> () {
			@Override
			public int compare(CandNode c1, CandNode c2) {
				return (c1.w > c2.w) ? -1 : ((c1.w < c2.w) ? 1 : 0);
			}
		});
		
		
		if(maxcand<idx){
			int idx_begin = 0;
			while(idx_begin<maxcand){
				if(cand[idx_begin].w>0.0){
					idx_begin++;
					continue;
				}
				break;
			}
			
			CandNode cn;
			if(idx_begin<maxcand){
				int len = idx-idx_begin;
				int rep = 0;
				while(rep++ < len){
					int idx1 = idx_begin+r.nextInt(len);
					int idx2 = idx_begin+r.nextInt(len);
					cn = cand[idx1];
					cand[idx1] = cand[idx2];
					cand[idx2] = cn;
				}
			
			}
		}
		
		
		
		
		return n_cand;
	}
	
	// compute cost (how many entries checked in cands to find the node i by linear search)
	private int match(QueryNode q, CandNode[] cand, int nc) {
		q.setKnownLink(null);
		for (int i=0; i<nc; i++) {
			if (q.id == cand[i].id) {
				q.setKnownLink(cand[i]);
				return (i+1);
			}
		}
		return nc;
		
	}
}


class IntInt {
	public int v1,v2;
	
	public IntInt(IntInt obj){
		this.v1 = obj.v1;
		this.v2 = obj.v2;
	}
	public IntInt(int v1, int v2){
		this.v1 = v1;
		this.v2 = v2;
	}
	@Override
	public int hashCode(){
		return v1*99999+v2;
		
	}
	
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof IntInt)) return false;
		IntInt obj = (IntInt)o;
		if(v2!=obj.v2) return false;
		if(v1!=obj.v1) return false;
		return true;
	} 
}
