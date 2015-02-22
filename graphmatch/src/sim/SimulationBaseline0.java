package sim;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import cm.CM_Learner;
import cm.CrowdMatch_BL;
import cm.QueryNode;
import cm.cm_data;
import cm.neighbors;
import cm.node;

/*
 * We simulate a crowdsourcing process with
 * two exactly the same heterogeneous graphs
 */
public class SimulationBaseline0 {
	int nquery							= 10;
	int nmaxquery						= -1;
	double mu							= 1;
	
	cm_data dat							= null;

	CM_Learner learner					= null;
	CrowdMatch_BL learner_bl				= null;
	
	
	String lprefix						= null;
	String rprefix						= null;
	String wprefix						= null;
	String outputfile					= null;
	
	double[] tmp_w						= null;
	
	public SimulationBaseline0 (Namespace nes) {
		// data parameters
		lprefix						= nes.getString("lprefix");
		rprefix						= nes.getString("rprefix");
		wprefix						= nes.getString("wprefix");
		outputfile					= nes.getString("output");
		nmaxquery					= nes.getInt("nmaxq");
		nquery						= nes.getInt("nq");
		
		
		learner_bl 					= new CrowdMatch_BL(nes);
		learner						= (CM_Learner) learner_bl;
	}

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Namespace nes 				= parseArguments(args);
		SimulationBaseline0 sim 			= new SimulationBaseline0 (nes);
		sim.init();
		sim.run();
	}
	
	public static Namespace parseArguments(String[] args){
		ArgumentParser parser = ArgumentParsers.newArgumentParser("CrowdMatch")
	                .description("Graph matching");
			
		try {


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

		learner.init();
	}

	public void run () throws IOException {
		int cnt = 0;
		int totalcost = 0;
		
		PrintStream out = new PrintStream(new FileOutputStream(outputfile));
		
		QueryNode[] queries = new QueryNode[nquery];
		CandNode[] cand = new CandNode[learner_bl.maxrnodesize];
		
		System.out.println("now, we start to find queries..");
		
		while ((nmaxquery == -1 || cnt < nmaxquery) && (cnt < dat.totallnode)) {
			
			// run model
			learner.run();
			
			// select a query & candidates
			int nq = select_query (nquery, queries);
			
			int tmpcnt = 0;
			for (int i=0; i<nq; i++) {
				QueryNode q = queries[i];
				int nc = select_candidates(q, cand);

				
				
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



	// select the best n queries & return the actual number of selected queries
	private int select_query(int n, QueryNode[] res) {
		
		HashSet<QueryNode> cand_set = new HashSet<QueryNode>();
		for(int t = 0; t< dat.ntype; t++){
			for(node n_q: learner_bl.nodes_queried[t]){
				if(n_q.label<0){
					continue;
				}
				neighbors[] neighbors = n_q.neighbors;
				for(int t_ = 0; t_< dat.ntype; t_++){
					int type = neighbors[t_].type;
					for(int id: neighbors[t_].arr){
						if(dat.lnodes[t_].arr[id].label>0)
							continue;
						cand_set.add(new QueryNode(type, id));
					}
				}
			}
		}
		
		
		
		for(int t = 0; t< dat.ntype; t++){
			if(cand_set.size()>=n){
				break;
			}
			for(int id = 0; id < dat.lnodes[t].arr.length; id++){
				if(cand_set.size()>=n){
					break;
				}
				if(dat.lnodes[t].arr[id].label>0){
					continue;
				}
				cand_set.add(new QueryNode(t,id));	
				
				
			}
		}
		
		
		ArrayList<QueryNode> candidates = new ArrayList<QueryNode>(cand_set);
		ArrayList<QueryNode> selected_queries = new ArrayList<QueryNode>();
		
		
		Random r = new Random();
		n = Math.min(n, candidates.size());
		
		while(selected_queries.size()<n){
			int id = r.nextInt(candidates.size());
			selected_queries.add(candidates.get(id));
			candidates.remove(id);
		}
		selected_queries.toArray(res);
		
		return selected_queries.size();
	}
	
	
	// choose n candidates for the node i & return the actual number of candidates
	class CandNode {
		public int id;
		public double w;
	}
	
	private int select_candidates(QueryNode q, CandNode[] cand) {
		for (int j=0; j<dat.rnodes[q.t].size; j++) {
			cand[j].id = j;
		}
		return dat.rnodes[q.t].size;
	}
	
	
}
