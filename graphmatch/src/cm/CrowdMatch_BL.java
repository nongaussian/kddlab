package cm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

public class CrowdMatch_BL extends CM_Learner{
	public ArrayList<node>[] nodes_queried;
	
	
	public CrowdMatch_BL(Namespace nes) {
		// data parameters
		setArgs(nes);
		
		init_query_nodes(nodes_queried, dat.ntype);
	}
	
	public static void init_query_nodes(ArrayList<node>[] query_nodes, int ntype){
		query_nodes = new ArrayList[ntype];
		for(int t = 0; t<ntype; t++){
			query_nodes[t] = new ArrayList<node>();
		}
	}
	public static void main(String[] args) throws IOException {
		Namespace nes = parseArguments(args);
		CrowdMatch_BL obj = new CrowdMatch_BL(nes);
		
		
	}
	public static Namespace parseArguments(String[] args){
		ArgumentParser parser = ArgumentParsers.newArgumentParser("CrowdMatch_Baseline")
	                .description("Graph matching baseline");
		
		try {
			parser.addArgument("-lprefix")
						.type(String.class)
						.required(true)
						.help("Prefix name of left graph");
			parser.addArgument("-rprefix")
						.type(String.class)
						.required(true)
						.help("Prefix name of right graph");

			Namespace res = parser.parseArgs(args);
			System.out.println(res);
			return res;
		} catch (ArgumentParserException e) {
			parser.handleError(e);
		}
		return null;
	}
	@Override
	public void init() {
		// data loading
		dat 						= new cm_data(lprefix, rprefix);
	}
	@Override
	public void run() {
		
		
	}
	
	
	class IntDouble{
		int v1;
		int v2;
		public IntDouble(int v1, int v2){
			this.v1 = v1;
			this.v2 = v2;
		}
	}
	private ArrayList<IntDouble> select_query_nodes(int n){
		//IntDouble[][] 
		
		HashSet<IntDouble> cand_set = new HashSet<IntDouble>();
		for(int t = 0; t< dat.ntype; t++){
			for(node n_q: nodes_queried[t]){
				if(n_q.label<0){
					continue;
				}
				neighbors[] neighbors = n_q.neighbors;
				for(int t_ = 0; t_< dat.ntype; t_++){
					int type = neighbors[t_].type;
					for(int id: neighbors[t_].arr){
						if(dat.lnodes[t_].arr[id].label>0)
							continue;
						cand_set.add(new IntDouble(type, id));
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
				cand_set.add(new IntDouble(t,id));	
				
				
			}
		}
		
		
		ArrayList<IntDouble> candidates = new ArrayList<IntDouble>(cand_set);
		ArrayList<IntDouble> selected_queries = new ArrayList<IntDouble>();
		
		
		Random r = new Random();
		n = Math.min(n, candidates.size());
		
		while(selected_queries.size()<n){
			int id = r.nextInt(candidates.size());
			selected_queries.add(candidates.get(id));
			candidates.remove(id);
		}
		
		
		return selected_queries;
	}
	
	private int select_query(int n, QueryNode[] res) {
		ArrayList<IntDouble > selected_queries = select_query_nodes(n);
		
		selected_queries.toArray(res);
		
		return selected_queries.size();
	}
	
}
