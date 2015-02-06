package sim;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import cm.CrowdMatch;
import cm.cm_data;
import cm.cm_model;

/*
 * We simulate a crowdsourcing process with
 * two exactly the same heterogeneous graphs
 */
public class Simulation1 {
	int nquery							= 10;
	int nmaxquery						= -1;
	double mu							= 1;
	
	cm_data dat							= null;
	cm_model model						= null;
	CrowdMatch learner					= null;
	
	String lprefix						= null;
	String rprefix						= null;
	String outputfile					= null;
	
	double[] tmp_w						= null;
	
	public Simulation1 (Namespace nes) {
		// data parameters
		lprefix						= nes.getString("lprefix");
		rprefix						= nes.getString("rprefix");
		outputfile					= nes.getString("output");
		nmaxquery					= nes.getInt("nmaxq");
		nquery						= nes.getInt("nq");
		
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
						.setDefault(1.0e-6)
						.help("Threshold of log-likelihood ratio for convergence test");
			parser.addArgument("-lprefix")
						.type(String.class)
						.required(true)
						.help("Prefix name of left graph");
			parser.addArgument("-rprefix")
						.type(String.class)
						.required(true)
						.help("Prefix name of right graph");
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
		model						= new cm_model();
		model.init_learner(dat, mu);
		model.is_cont = true;
		
		learner.set_dat(dat);
		learner.set_model(model);
	}

	public void run () throws IOException {
		int cnt = 0;
		int totalcost = 0;
		
		PrintStream out = new PrintStream(new FileOutputStream(outputfile));
		
		CandNode[] cand = new CandNode[model.maxrnodesize];
		tmp_w = new double[model.maxrnodesize];
		
		while ((nmaxquery == -1 || cnt < nmaxquery) && (cnt < dat.totallnode)) {
			
			// run model
			learner.run();
			
			// select a query & candidates
			QueryNode[] queries = select_query (nquery);
			
			int tmpcnt = 0;
			for (QueryNode q : queries) {
				int nc = select_candidates(q, cand);

				double cost = compute_cost(q, cand, nc);
				totalcost += cost;
				System.out.println("CNT=" + (cnt + tmpcnt) + ", COST: " + cost + ", ACC: " + totalcost);
				out.println("" + (cnt + tmpcnt) + "\t" + cost + "\t" + totalcost);
				
				tmpcnt++;
			}
			
			cnt += queries.length;

			// set labels in dat.lnodes
			for (QueryNode q : queries) {
				dat.lnodes[q.t].arr[q.id].label = q.id;
				System.out.println("match node : " + q.id);
			}
		}
		
		out.close();
	}

	class QueryNode {
		public int t;
		public int id;
		public double diff;
		public QueryNode (int t, int i, double diff) {
			this.t = t;
			this.id = i;
			this.diff = diff;
		}
	}

	// select the best n queries & return the actual number of selected queries
	private QueryNode[] select_query(int n) {
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
					tmp.id = i;
					tmp.diff = val;
					topk.add(tmp);
				}
			}
		}
		
		return (QueryNode[]) topk.toArray();
	}
	
	// compute the (approximate) expected model change when we ask to annotate for node i of type t
	private double compute_expected_model_change(int t, int i) {
		double sum = 0;
		for (int j=0; j<dat.rnodes[t].size; j++) {
			double diff = 0;

			// change of the node i
			diff += -Math.log(model.w[t].val[i][j]) / 2.;
			
			// change of the neighbors of i
			for (int s=0; s<dat.ntype; s++) {
				if (!dat.rel[t][s]) continue;
				
				for (int x=0; x<dat.lnodes[t].arr[i].neighbors[s].size; x++) {
					int u = dat.lnodes[t].arr[i].neighbors[s].arr[x];
					double tmp = 0;
					
					estimate_w(tmp_w, t, i, j, s, u);
					
					for (int v=0; v<dat.rnodes[t].size; v++) {
						tmp += Math.sqrt(model.w[s].val[u][v] * tmp_w[v]);
					}
					diff += tmp;
				}
			}
			sum += diff * model.w[t].val[i][j];
		}
		
		return sum;
	}

	// estimate w[s].arr[u]
	private void estimate_w(double[] w, int t, int i, int j, int s, int u) {
		double tmp = 0;
		
		for (int v=0; v<dat.rnodes[t].size; v++) {
			w[v] = model.w[s].val[u][v];
		}
		// for j's neighborhood,
		for (int y=0; y<dat.rnodes[t].arr[j].neighbors[s].size; y++) {
			int v = dat.rnodes[t].arr[j].neighbors[s].arr[y];
			estimate_w(w, t, i, j, s, u, v);
		}
		
		for (int v=0; v<dat.rnodes[t].size; v++) {
			tmp += w[v];
		}
		for (int v=0; v<dat.rnodes[t].size; v++) {
			w[v] /= tmp;
		}
	}

	// estimate w[s].arr[u][v]
	private void estimate_w(double[] w, int t, int i, int j, int s, int u, int v) {
		double sum = 0;
		
		for (int o=0; o<dat.ntype; o++) {
			if (!dat.rel[s][o]) continue;
			
			double tmp = 0;
			for (int xx=0; xx<dat.lnodes[s].arr[u].neighbors[o].size; xx++) {
				int uu = dat.lnodes[s].arr[u].neighbors[o].arr[xx];
				for (int yy=0; yy<dat.rnodes[s].arr[v].neighbors[o].size; yy++) {
					int vv = dat.rnodes[s].arr[v].neighbors[o].arr[yy];
					
					if (o == t && uu == i && vv == j) {
						tmp += 1. 
								/ (double)dat.lnodes[s].arr[u].neighbors[o].size
								/ (double)dat.rnodes[o].arr[vv].neighbors[s].size;
					}
					else {
						tmp += model.w[o].val[uu][vv]
								/ (double)dat.lnodes[s].arr[u].neighbors[o].size
								/ (double)dat.rnodes[o].arr[vv].neighbors[s].size;
					}
				}
			}
			
			sum += model.c[o] * tmp;
		}

		w[v] = sum;
	}

	// choose n candidates for the node i & return the actual number of candidates
	class CandNode {
		public int id;
		public double w;
	}
	private int select_candidates(QueryNode q, CandNode[] cand) {
		for (int j=0; j<dat.rnodes[q.t].size; j++) {
			cand[j].id = j;
			cand[j].w = model.w[q.t].val[q.id][j];
		}
		
		Arrays.sort(cand, new Comparator<CandNode> () {
			@Override
			public int compare(CandNode c1, CandNode c2) {
				return (c1.w > c2.w) ? -1 : ((c1.w < c2.w) ? 1 : 0);
			}
		});
		return dat.rnodes[q.t].size;
	}
	
	// compute cost (how many entries checked in cands to find the node i by linear search)
	private int compute_cost(QueryNode q, CandNode[] cand, int nc) {
		for (int i=0; i<nc; i++) {
			if (q.id == cand[i].id) {
				return (i+1);
			}
		}
		return nc;
	}
}
