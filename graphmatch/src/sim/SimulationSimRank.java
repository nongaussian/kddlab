package sim;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.Map.Entry;

import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;
import cm.CandNode;
import cm.QueryNode;
import cm.SimRank;

import common.SparseMatrix;

public abstract class SimulationSimRank extends Simulation {
	SimRank simrank;

	public SimulationSimRank(Namespace nes) {
		super(nes);
		simrank = new SimRank(nes);
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
		return parser;
	}
	
	
	protected final double computeAccuracy(int iteration,int cost, int nmatched){
		int count_correct 	= 0;
		int count_tot		= 0;
		
		SparseMatrix[] sim  = simrank.getNewSimRank();
		for (int t=0; t<dat.ntype; t++) {
			count_tot += dat.lnodes[t].size;
			for (int i=0; i<dat.lnodes[t].size; i++) {
				if(dat.lnodes[t].arr[i].label>=0){
					count_correct++;
					continue;
				}
				
				int idx_max = 0;
				double val_max = 0;
				Iterator<Entry<Integer,Double>> iter = sim[t].getEntryIterator(i);
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
	
	
	public final void run () throws IOException {
		int cnt = 0;
		int totalcost = 0;
		
		PrintStream out = new PrintStream(new FileOutputStream(outputfile));
		
		QueryNode[] queries = new QueryNode[nquery];
		CandNode[] cand = new CandNode[maxrnodesize];
		for (int i=0; i<maxrnodesize; i++) cand[i] = new CandNode();
		System.out.println("now, we start to find queries..");

		simrank.initialize(dat);
		initialize();
		int iteration = 0;
		double accuracy = 0.0;
		while ((accuracy<max_accuracy)&&(nmaxquery == -1 || cnt < nmaxquery) && (cnt < dat.totallnode)) {
			iteration++;
			// select a query & candidates
			int nq = select_query (nquery, queries);
			
			int tmpcnt = 0;
			for (int i=0; i<nq; i++) {
				QueryNode q = queries[i];
				int nc = select_candidates(q, cand);

				double cost = match(q, cand, nc);
				totalcost += cost;
				System.out.println("CNT=" + (cnt + tmpcnt) + ", COST: " + cost + ", ACC: " + totalcost);
				out.println("" + (cnt + tmpcnt) + "\t" + cost + "\t" + totalcost + "\t" + q.t + "\t" + q.id);
				
				tmpcnt++;
			}
			
			cnt += queries.length;

			// set labels in dat.lnodes
			for (QueryNode q : queries) {
				dat.lnodes[q.t].arr[q.id].label = q.matched_id;
				try{
					dat.rnodes[q.t].arr[q.matched_id].label = q.id;
				}catch(ArrayIndexOutOfBoundsException e){
					e.printStackTrace();
					throw e;
				}
				nmatch++;
				//System.out.println("match node : " + q.id);
			}
			update_after_matching(queries);
			simrank.update_effpairs(queries, dat);
			
			simrank.run();
			accuracy = computeAccuracy(iteration,totalcost,nmatch);
			System.out.printf("Accuracy:  %f\n", accuracy);
			
		}
		
		out.close();
	}


	abstract int select_candidates(QueryNode q, CandNode[] cand) ;
	abstract int select_query(int nquery, QueryNode[] queries);

	abstract void initialize();
	abstract void update_after_matching(QueryNode[] queries);
	
	@Override
	public void regiExp() {
		expid = sqlunit.executeUpdateGetAutoKey(String.format("insert into experiment (algorithm,nq,radius, rmr) values ('%s',%d,%d,%f);",algorithm,nquery,simrank.getRadius(),rm_ratio));	
	}

}
