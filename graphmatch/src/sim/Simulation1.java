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
public class Simulation1 extends SimulationProposed{	

	
	public Simulation1 (Namespace nes) {
		// data parameters
		super(nes);
	}
	
	public Simulation1 (String[] args) {
		this(parseArguments(args));
		algorithm = "simul1";
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
		ArgumentParser parser =SimulationProposed.getArgParser();
		return parser;
	}
	





	// choose n candidates for the node i & return the actual number of candidates
	protected int select_candidates(QueryNode q, CandNode[] cand, int maxcand) {
			
		//Iterator<Integer> iter_j = model.eff_pairs[q.t][q.id].iterator();
		//int j;
		int idx=0;
		//while(iter_j.hasNext()){
		//	j = iter_j.next();
		
		for(int j = 0; j<maxcand; j++){
			if(dat.rnodes[q.t].arr[j].label<0){
				cand[idx].id = j;
				cand[idx].w = model.w[q.t].get(q.id, j);
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
		
		
		/*//if the candidate size is limited
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
		*/
		
		
		
		return n_cand;
	}

	@Override
	void initialize() {
		// TODO Auto-generated method stub
		
	}

	@Override
	void update_after_matching(QueryNode[] queries) {
		// TODO Auto-generated method stub
		
	}
	
	

	
}



