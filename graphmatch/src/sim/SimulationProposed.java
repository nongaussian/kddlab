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
import java.util.Map.Entry;

import common.SparseMatrix;

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
public abstract class SimulationProposed extends Simulation{	
	double mu							= 1;
	public int radius					= -1;

	cm_model model						= null;
	CrowdMatch learner					= null;

	
	double[] tmp_w						= null;
	
	public SimulationProposed (Namespace nes) {
		// data parameters
		super(nes);
		
		radius						= nes.getInt("radius");
		learner						= new CrowdMatch(nes);
	}
	
	public SimulationProposed (String[] args) {
		this(parseArguments(args));
	}


	
	public static Namespace parseArguments(String[] args){
		ArgumentParser parser = getArgParser();
			
		try {
			Namespace res = parser.parseArgs(args);
			System.out.println(res);
			return res;
		} catch (ArgumentParserException e) {
			parser.handleError(e);
		}
		return null;
	}
	
	public static ArgumentParser getArgParser(){
		ArgumentParser parser =Simulation.getArgParser();
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
		parser.addArgument("-radius")
				.type(Integer.class)
				.setDefault(-1);
		return parser;
	}
	
	public void init() {
		dat							= new cm_data(lprefix, rprefix,rm_ratio);
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
		tmp_w = new double[model.maxrneighborsize];
		
		System.out.println("now, we start to find queries..");
		initialize();
		int iteration = 0;
		double accuracy = 0.0;
		while ((accuracy<max_accuracy)&&(nmaxquery == -1 || cnt < nmaxquery) && (cnt < dat.totallnode)) {
			iteration++;
			// run model
			learner.run();
			
			// select a query & candidates
			int nq = select_query (nquery, queries);
			
			int tmpcnt = 0;
			for (int i=0; i<nq; i++) {
				QueryNode q = queries[i];
				ncand = dat.rnodes[q.t].size;
				int nc = select_candidates(q, cand, dat.rnodes[q.t].size);

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
					//System.out.printf("match node (t,i,j,w)=(%2d,%5d,%5d,%5f)\n",q.t,q.id,q.matched_id,model.w[q.t].get(q.id, q.matched_id));
					nmatch++;
				}
			}
			model.update_effpairs(queries, dat);
			update_after_matching(queries);
			stat();
			accuracy = computeAccuracy(iteration, totalcost, nmatch);
			System.out.printf("Accuracy:  %f\n", accuracy);
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
				int t = rand.nextInt(dat.ntype);
				int i = rand.nextInt(dat.lnodes[t].size);
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

			// change of the node i//TODO: Log?????
			//diff += -Math.log(model.w[t].get(i, j)) / 2.;
			
			// change of the neighbors of i
			for (int s=0; s<dat.ntype; s++) {
				if (!dat.rel[t][s]) continue;
				
				for (int n_x=0; n_x<dat.lnodes[t].arr[i].neighbors[s].size; n_x++) {
					int x = dat.lnodes[t].arr[i].neighbors[s].arr[n_x];
					double tmp = 0;
					
					estimate_w(tmp_w, t, i, j, s, x);
					
					for (int n_y=0; n_y<dat.rnodes[t].arr[j].neighbors[s].size; n_y++) {
						int y = dat.rnodes[t].arr[j].neighbors[s].arr[n_y];
						tmp += Math.sqrt(model.w_next[s].get(x, y) * tmp_w[n_y]);
					}
					if(tmp>0.0){
						diff += -Math.log(tmp);
					}
					if(Double.isNaN(diff)||Double.isInfinite(diff)){
						System.out.printf("diff: %f, tmp:%f\n",diff,tmp);
					}
				}
			}
			sum += diff * model.w_next[t].get(i, j);
		}
		
		return sum;
	}
	
	private void stat(){
		int count = 0;
		int eff_l = 0;
		int eff_r = 0;
		
		HashSet<Integer>[][] effpair_r = new HashSet[dat.ntype][];
		for (int t=0; t<dat.ntype; t++) {
			effpair_r[t] = new HashSet[dat.rnodes[t].size];
			for (int j=0; j<dat.rnodes[t].size; j++) {
				effpair_r[t][j] = new HashSet<Integer>();
			}
		}
		
		for (int t=0; t<dat.ntype; t++) {
			for (int i=0; i<dat.lnodes[t].size; i++) {
				count++;
				if(model.eff_pairs[t][i].size()>0){
					eff_l++;
				}
				Iterator<Integer> iterator  = model.eff_pairs[t][i].iterator();
				while(iterator.hasNext()){
					effpair_r[t][iterator.next()].add(i);
				}
			}
		}
		for (int t=0; t<dat.ntype; t++) {
			for (int j=0; j<dat.rnodes[t].size; j++) {
				if(effpair_r[t][j].size()>0){
					eff_r++;
				}
			}
		}
		System.out.printf("count tot:%d, leff:%d, reff:%d\n", count,eff_l, eff_r);
	}

	// estimate w[s].arr[u]
	
	private void estimate_w(double[] w, int t, int i, int j, int s, int x) {
		int[] x_neighbors = dat.lnodes[s].arr[x].neighbors[t].arr;
		int[] j_neighbors = dat.rnodes[t].arr[j].neighbors[s].arr;
		for (int idx_y=0; idx_y< j_neighbors.length; idx_y++){
			int y = dat.rnodes[t].arr[j].neighbors[s].arr[idx_y];
			w[idx_y] = model.w_next[s].get(x, y);
		}			
		
		if(x_neighbors.length>0&& j_neighbors.length>0){
			double nst_x = (double) x_neighbors.length;
			double nts_j = (double) j_neighbors.length;
			double tmp = (1.0-model.w_next[t].get(i, j))/nst_x;
			
			for (int idx_y=0; idx_y< j_neighbors.length; idx_y++){
				w[idx_y] += tmp/nts_j;
			}
		}
				
		
	}



	// choose n candidates for the node i & return the actual number of candidates
	protected abstract int select_candidates(QueryNode q, CandNode[] cand, int maxcand);
	
	
	protected final double computeAccuracy(int iteration,int cost, int nmatched){
		int count_correct 	= 0;
		int count_tot		= 0;

		for (int t=0; t<dat.ntype; t++) {
			count_tot += dat.lnodes[t].size;
			for (int i=0; i<dat.lnodes[t].size; i++) {
				if(dat.lnodes[t].arr[i].label>=0){
					count_correct++;
					continue;
				}
				
				int idx_max = 0;
				double val_max = 0;
				Iterator<Entry<Integer,Double>> iter = model.w_next[t].getEntryIterator(i);
				while(iter.hasNext()){
					Entry<Integer,Double> entry = iter.next();
					if(entry.getValue()>val_max){
						idx_max = entry.getKey();
						val_max = entry.getValue();
					}
				}
				
				if(i == idx_max){
					count_correct++;
				}
			}
		}
		regiResult(iteration, cost, nmatched, count_correct, count_tot-count_correct);
		return ((double)count_correct)/count_tot;
	}

	@Override
	public void regiExp() {
		expid = sqlunit.executeUpdateGetAutoKey(String.format("insert into experiment (algorithm,nq,radius,rmr) values ('%s',%d,%d,%f);",algorithm,nquery,radius,rm_ratio));		
	}
	abstract void initialize();
	abstract void update_after_matching(QueryNode[] queries);
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
