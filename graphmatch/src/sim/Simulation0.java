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
import cm.CandNode;
import cm.QueryNode;
import cm.cm_data;

/*
 * Simulate a crowdsourcing process with random selection
 */
public class Simulation0 extends SimulationSimRank{
	public Simulation0 (String[] args) {
		super(parseArguments(args));
		algorithm = "simul0";
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
		
		maxrnodesize 				= 0;
		for (int t=0; t<dat.ntype; t++) {
			if (maxrnodesize < dat.rnodes[t].size) {
				maxrnodesize = dat.rnodes[t].size;
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
				            
				double val = rand.nextDouble();
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
		int idx=0;
		for (int j=0; j<dat.rnodes[q.t].size; j++) {
			if(dat.rnodes[q.t].arr[j].label<0){
				cand[idx].id = j;
				cand[idx].w = rand.nextDouble();
				idx++;
			}
		}
		
		Arrays.sort(cand,0,idx, new Comparator<CandNode> () {
			@Override
			public int compare(CandNode c1, CandNode c2) {
				return (c1.w > c2.w) ? -1 : ((c1.w < c2.w) ? 1 : 0);
			}
		});
		return idx;
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
