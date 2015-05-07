package sim;

import graph.neighbors;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.PriorityQueue;

import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import similarity.EM;
import similarity.SimRankD;
import similarity.Similarity;
import cm.CandNode;
import cm.QueryNode;
import cm.cm_data;

import common.SparseIntMatrix;

public class GraphMatching  extends Simulation{
	Similarity model;
	EM em;
	
	public SparseIntMatrix[] neighbor_pairs		= null;
	
	Param param;
	
	
	
	public GraphMatching(Namespace nes) {
		super(nes);
		param = new Param(nes);
	}
	
	public GraphMatching(String[] args) {
		this(parseArguments(args));
	}

	
	public static Namespace parseArguments(String[] args){
		ArgumentParser parser = getArgParser();
		try{
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
		parser.addArgument("-niter")
				.type(Integer.class)
				.setDefault(100)
				.help("Maximum number of iterations");
		parser.addArgument("-thres")
				.type(Double.class)
				.setDefault(1.0e-5)
				.help("Threshold for convergence test");
		parser.addArgument("-radius")
			.type(Integer.class)
			.setDefault(-1);
		parser.addArgument("-query")
			.type(String.class);
		parser.addArgument("-sim")
			.type(String.class);
		return parser;
	}

	@Override
	public void init() {
		dat	= new cm_data(lprefix, rprefix,rm_ratio);
		
		maxrnodesize 				= 0;
		for (int t=0; t<dat.ntype; t++) {
			if (maxrnodesize < dat.rnodes[t].size) {
				maxrnodesize = dat.rnodes[t].size;
			}
		}
		
		switch(param.sim){
		case Param.SIM_PROPOSED:
			model = new EM(param.nes);
			break;
		case Param.SIM_SIMRANK:
			model = new SimRankD(param.nes);
			break;
		}
		switch(param.query){
		case Param.QUERY_EMC:
			if(param.sim == Param.SIM_PROPOSED){
				em = (EM)model;
			}else{
				em = new EM(param.nes);
			}
			break;
		case Param.QUERY_NEIGHBORPAIR:
			break;
		case Param.QUERY_RANDOM:
			break;
		}
		
		
		
	}

	@Override
	public void regiExp() {
		
	}

	@Override
	public void run() throws IOException {
		int cnt = 0;
		int totalcost = 0;
		
		PrintStream out = new PrintStream(new FileOutputStream(outputfile));
		
		QueryNode[] queries = new QueryNode[nquery];
		CandNode[] cand = new CandNode[maxrnodesize];
		for (int i=0; i<maxrnodesize; i++) cand[i] = new CandNode();
		//tmp_w = new double[model.maxrneighborsize];
		
		System.out.println("now, we start to find queries..");
		model.initialize(dat);
		
		
		int iteration = 0;
		double accuracy = 0.0;
		while ((accuracy<max_accuracy)&&(nmaxquery == -1 || cnt < nmaxquery) && (cnt < dat.totallnode)) {
			iteration++;
			// run model
			model.run();
			
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

			//model.save_label("res/test");
			
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
			
			accuracy = computeAccuracy(iteration, totalcost, nmatch);
			System.out.printf("Accuracy:  %f\n", accuracy);
		}
		
		out.close();
		
	}

	
	private void update_after_matching(QueryNode[] queries) {
		update_neighbor_pairs(queries);
	}

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
				Iterator<Entry<Integer,Double>> iter = model.sim_next[t].getEntryIterator(i);
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

	
	// select the best n queries & return the actual number of selected queries
	int select_query(int n, QueryNode[] res) {
		Comparator<QueryNode> comp = new Comparator<QueryNode> () {
			public int compare (QueryNode q1, QueryNode q2) {
				return (q1.diff < q2.diff) ? -1 : ((q1.diff > q2.diff) ? 1 : 0);
			}
		};
		
		PriorityQueue<QueryNode> topk  = new PriorityQueue<QueryNode> (n, comp);
		double val = 0.0;
		for (int t=0; t<dat.ntype; t++) {
			for (int i=0; i<dat.lnodes[t].size; i++) {
				if (dat.lnodes[t].arr[i].label >= 0) continue;
				
				switch (param.query){
					case Param.QUERY_EMC:
						val = em.compute_expected_model_change(t, i);
						break;
					case Param.QUERY_NEIGHBORPAIR:
						val = (double) neighbor_pairs[t].size(i);
						break;
					case Param.QUERY_RANDOM:
						val = rand.nextDouble();
						break;
				}
				 
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
		
		
		int idx=0;
		for (QueryNode q : topk) {
			System.out.println("=> selected: " + q.id + " of type " + dat.nodetypes[q.t]);
			res[idx++] = q;
		}
		return idx;
	}
	// choose n candidates for the node i & return the actual number of candidates
	protected int select_candidates(QueryNode q, CandNode[] cand, int maxcand) {
		//System.out.printf("query t: %d, i: %d, diff:%f, #np: %d\n", q.t,q.id, q.diff, neighbor_pairs[q.t].size(q.id));
		
		int idx=0;
		for(int j = 0; j<maxcand; j++){
			if(dat.rnodes[q.t].arr[j].label<0){
				cand[idx].id = j;
				cand[idx].w = model.sim[q.t].get(q.id, j);
				idx++;
			}
		}
		
		int n_cand = Math.min(maxcand, idx);
		Arrays.sort(cand, 0,n_cand, new Comparator<CandNode> () {
			@Override
			public int compare(CandNode c1, CandNode c2) {
				return (c1.w > c2.w) ? -1 : ((c1.w < c2.w) ? 1 : 0);
			}
		});
		
		
		return n_cand;
	}
	
	void update_neighbor_pairs(QueryNode[] queries){
		for (QueryNode q : queries) {
			if(q.matched_id >= 0){
				for(int t = 0; t < dat.ntype; t++){
					if(!dat.rel[q.t][t])
						continue;
					neighbors lneighbors = dat.lnodes[q.t].arr[q.id].neighbors[t];
					neighbors rneighbors = dat.rnodes[q.t].arr[q.matched_id].neighbors[t];
					for(int x: lneighbors.arr){
						for(int y: rneighbors.arr){
							neighbor_pairs[t].add(x, y,1);
						}
					}
					
				}
			}
		}
	}
}




class Param{
	public static final int SIM_PROPOSED	= 1;
	public static final int SIM_SIMRANK 	= 2;
	
	public static final int QUERY_EMC			= 1;
	public static final int QUERY_RANDOM		= 2;
	public static final int QUERY_NEIGHBORPAIR	= 3;
	
	public static final String[] sim_str = {"","PROPOSED","SIMRANK"};
	public static final String[] query_str = {"","EMC","RANDOM", "NEIGHBORPAIR"};
	
	public int sim = 0;
	public int query 	= 0;
	
	public Namespace nes;
	
	
	public Param(Namespace nes){
		for(int i = 1; i<sim_str.length; i++){
			if(nes.getString("sim").toUpperCase().equals(sim_str[i])){
				sim = i;
				break;
			}
		}
		for(int i = 1; i<query_str.length; i++){
			if(nes.getString("query").toUpperCase().equals(query_str[i])){
				query = i;
				break;
			}
		}
		this.nes = nes;
	}
	

}
