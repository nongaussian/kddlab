package sim;

import graph.neighbors;

import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;

import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import cm.CandNode;
import cm.QueryNode;
import cm.cm_data;

import common.SparseIntMatrix;

/*
 * Simulate a crowdsourcing process by selecting neighbors of labeled nodes
 * for queries
 */
public class Simulation2 extends SimulationSimRank {
	SparseIntMatrix[] neighbor_pairs;
	public Simulation2 (String[] args) {
		super(parseArguments(args));
		algorithm = "simul2";
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
		return SimulationSimRank.getArgParser();
	}
	
	public void init() {
		dat							= new cm_data(lprefix, rprefix,rm_ratio);
		maxrnodesize = 0;
		for (int t=0; t<dat.ntype; t++) {
			if (maxrnodesize < dat.rnodes[t].size) {
				maxrnodesize = dat.rnodes[t].size;
			}
		}
		neighbor_pairs = new SparseIntMatrix[dat.ntype];
		for (int t=0; t<dat.ntype; t++) {
			neighbor_pairs[t] = new SparseIntMatrix(dat.lnodes[t].size);
		}
	}

	
	


	void initialize_neighbor_pairs(){
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
							neighbor_pairs[s].setTrue(x, y);
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
							neighbor_pairs[t].setTrue(x, y);
						}
					}
					
				}
			}
		}
	}
	
	// select the best n queries & return the actual number of selected queries
	int select_query(int n, QueryNode[] res) {
		PriorityQueue<QueryNode> topk = new PriorityQueue<QueryNode> (n, new Comparator<QueryNode> () {
			public int compare (QueryNode q1, QueryNode q2) {
				return (q1.diff < q2.diff) ? -1 : ((q1.diff > q2.diff) ? 1 : 0);
			}
		});
		
		for (int t=0; t<dat.ntype; t++) {
			for (int i=0; i<dat.lnodes[t].size; i++) {
				if (dat.lnodes[t].arr[i].label >= 0) continue;
				            
				double val = neighbor_pairs[t].size(i);
				
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
	


	
	int select_candidates(QueryNode q, CandNode[] cand) {
		for (int j=0; j<dat.rnodes[q.t].size; j++) {
			cand[j].id = j;
			cand[j].w = neighbor_pairs[q.t].haspair(q.id, j)?1.0:0.0;
		}
		
		Arrays.sort(cand, 0, dat.rnodes[q.t].size, new Comparator<CandNode> () {
			@Override
			public int compare(CandNode c1, CandNode c2) {
				return (c1.w > c2.w) ? -1 : ((c1.w < c2.w) ? 1 : 0);
			}
		});
		return dat.rnodes[q.t].size;
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
