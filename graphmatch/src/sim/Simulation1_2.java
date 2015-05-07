package sim;

import graph.neighbors;

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

import common.SparseIntMatrix;
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
public class Simulation1_2 extends SimulationProposed{	
	SparseIntMatrix[] neighbor_pairs;

	
	public Simulation1_2 (Namespace nes) {
		// data parameters
		super(nes);
	}
	
	public Simulation1_2 (String[] args) {
		this(parseArguments(args));
		algorithm = "simul1_2";
	}

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Namespace nes 				= parseArguments(args);
		Simulation1_2 sim 			= new Simulation1_2 (nes);
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
	

	void initialize_neighbor_pairs(){
		neighbor_pairs = new SparseIntMatrix[dat.ntype];
		for (int t=0; t<dat.ntype; t++) {
			neighbor_pairs[t] = new SparseIntMatrix(dat.lnodes[t].size);
		}
		for (int t=0; t<dat.ntype; t++) {
			for (int i=0; i<dat.lnodes[t].size; i++) {
				if(dat.lnodes[t].arr[i].label<0) continue;
				for (int s=0; s<dat.ntype; s++) {
					if(!dat.rel[t][s]){
						continue;
					}
					neighbors lneighbors = dat.lnodes[t].arr[i].neighbors[s];
					neighbors rneighbors = dat.rnodes[t].arr[dat.lnodes[t].arr[i].label].neighbors[s];
					for(int x: lneighbors.arr){
						for(int y: rneighbors.arr){
							neighbor_pairs[s].put(x, y,1);
						}
					}
				}
				
			}
		}
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
	



	// choose n candidates for the node i & return the actual number of candidates
	protected int select_candidates(QueryNode q, CandNode[] cand, int maxcand) {
		//System.out.printf("query t: %d, i: %d, diff:%f, #np: %d\n", q.t,q.id, q.diff, neighbor_pairs[q.t].size(q.id));
		
		int idx=0;
		for(int j = 0; j<maxcand; j++){
			if(dat.rnodes[q.t].arr[j].label<0){
				cand[idx].id = j;
				cand[idx].ival = neighbor_pairs[q.t].get(q.id, j);
				cand[idx].w = model.w[q.t].get(q.id, j);
				idx++;
			}
		}
		
		int n_cand = Math.min(maxcand, idx);
		Arrays.sort(cand, 0,n_cand, new Comparator<CandNode> () {
			@Override
			public int compare(CandNode c1, CandNode c2) {
				if(c1.ival==c2.ival){
					return (c1.w > c2.w) ? -1 : ((c1.w < c2.w) ? 1 : 0);
				}else {
					return c2.ival-c1.ival;
				}
				
			}
		});
		
		
		return n_cand;
	}

	@Override
	void initialize() {
		initialize_neighbor_pairs();
		
	}

	@Override
	void update_after_matching(QueryNode[] queries) {
		update_neighbor_pairs(queries);
	}
}



