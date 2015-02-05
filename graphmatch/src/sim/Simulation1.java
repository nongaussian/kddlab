package sim;

import java.util.Comparator;
import java.util.PriorityQueue;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import cm.cm_data;
import cm.cm_model;

/*
 * We simulate a crowdsourcing process with
 * two exactly the same heterogeneous graphs
 */
public class Simulation1 {
	int nquery							= 10;
	int ncandidate						= -1;
	int nmaxquery						= -1;
	double mu							= 1;
	
	cm_data dat							= null;
	cm_model model						= null;
	
	String outputfile					= null;
	
	public Simulation1 (Namespace nes) {
		// data parameters
		String lprefix				= nes.getString("lprefix");
		String rprefix				= nes.getString("rprefix");
		String oprefix				= nes.getString("oprefix");
		outputfile					= nes.getString("output");
		nmaxquery					= nes.getInt("nmaxq");
		nquery						= nes.getInt("nq");
		ncandidate					= nes.getInt("nc");
		
		dat							= new cm_data(lprefix, rprefix);
		model						= new cm_model();
		model.init_tester(dat, mu);
		model.load_model(oprefix);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Namespace nes = parseArguments(args);
		Simulation1 sim = new Simulation1 (nes);
		sim.run ();
	}
	
	public static Namespace parseArguments(String[] args){
		ArgumentParser parser = ArgumentParsers.newArgumentParser("CrowdMatch")
	                .description("Graph matching");
			
		try {
			parser.addArgument("-mu")
						.type(Double.class)
						.setDefault(1.)
						.help("mu");
			parser.addArgument("-lprefix")
						.type(String.class)
						.required(true)
						.help("Prefix name of left graph");
			parser.addArgument("-rprefix")
						.type(String.class)
						.required(true)
						.help("Prefix name of right graph");
			parser.addArgument("-oprefix")
						.type(String.class)
						.required(true)
						.help("Prefix name of .w and .c files");
			parser.addArgument("-output")
						.type(String.class)
						.required(true)
						.help("Output file for new labels");
			parser.addArgument("-nq")
		                .type(Integer.class)
		                .setDefault(10)
		                .help("Number of queries in each iteration");
			parser.addArgument("-nc")
			            .type(Integer.class)
			            .setDefault(-1)
			            .help("Number of candidates for each query (default: infinity)");
			parser.addArgument("-nmaxq")
				        .type(Integer.class)
				        .setDefault(-1)
				        .help("Maximum number of queries (default: infinity)");

			Namespace res = parser.parseArgs(args);
			System.out.println(res);
			return res;
		} catch (ArgumentParserException e) {
			parser.handleError(e);
		}
		return null;
	}

	public void run () {
		int cnt = 0;
		int[] queries = new int[nquery];
		int[] cands = new int[ncandidate];
		int cost = 0;
		
		while ((nmaxquery == -1 || cnt < nmaxquery) && (cnt < dat.totallnode)) {
			
			// run model?
			
			// select a query & candidates
			int nq = select_query (queries, nquery);
			cnt += nq;
			
			for (int i=0; i<nq; i++) {
				int nc = select_candidates(queries[i], cands, ncandidate);

				cost += compute_cost(queries[i], cands, nc);
			}
		}
		
		output_labels(queries, nq);
		System.out.println(cost);
	}
	
	class QueryNode {
		public int t;
		public int i;
		public double diff;
		public QueryNode (int t, int i, double diff) {
			this.t = t;
			this.i = i;
			this.diff = diff;
		}
	}

	// select the best n queries & return the actual number of selected queries
	private int select_query(int[] qs, int n) {
		PriorityQueue<QueryNode> topk = new PriorityQueue<QueryNode> (n, new Comparator<QueryNode> () {
			public int compare (QueryNode q1, QueryNode q2) {
				return (q1.diff < q2.diff) ? -1 : ((q1.diff > q2.diff) ? 1 : 0);
			}
		});
		
		for (int t=0; t<dat.ntype; t++) {
			for (int i=0; i<dat.lnodes[t].size; i++) {
				if (dat.lnodes[t].arr[i].label >= 0) continue;
				            
				double val = compute_expected_model_change(t, i);
				if (topk.size() < n) {
					topk.add(new QueryNode(t, i, val));
				}
				else if (topk.peek().diff < val) {
					QueryNode tmp = topk.remove();
					tmp.t = t;
					tmp.i = i;
					tmp.diff = val;
					topk.add(tmp);
				}
			}
		}
		return 0;
	}
	
	private double compute_expected_model_change(int t, int i) {
		double sum = 0;
		for (int j=0; j<dat.rnodes[t].size; j++) {
			double diff = 0;

			// change of the node i
			diff += -Math.log(model.w[t].val[i][j]) / 2.;
			
			// change of the neighbors of i
			for (int s=0; s<dat.ntype; s++) {
				if (!dat.rel[t][s]) continue;
				
				double tmpdiff = 0;
				for (int x=0; x<dat.lnodes[t].arr[i].neighbors[s].size; x++) {
					for (int v=0; v<dat.rnodes[t].arr[j].neighbors[s].size; v++) {
						int u = dat.lnodes[t].arr[i].neighbors[s].arr[x];
						                         
						
					}
				}
				
				diff += model.c[s] * tmpdiff;
			}
			sum += diff * model.w[t].val[i][j];
		}
		
		return sum;
	}

	// choose n candidates for the node i & return the actual number of candidates
	private int select_candidates(int i, int[] cand, int n) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	// compute cost (how many entries checked in cands to find the node i by linear search)
	private int compute_cost(int i, int[] cands, int nc) {
		// TODO Auto-generated method stub
		return 0;
	}
}
