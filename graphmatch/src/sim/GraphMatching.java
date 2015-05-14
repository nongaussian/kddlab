package sim;

import graph.neighbors;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;

import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

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
	BufferedWriter bw;
	PrintWriter pw;
	
	
	long time;
	public static void main(String[] args) throws IOException{
		runall("tiny",-1,-1);
	}
	
	
	public static void runall(String dataname,int sim,int query) throws IOException{
		String[] basic_args = {
				"-maxacc", "0.6",
				//"-maxacc", "0.02",
				"-data", dataname,
				"-nq", "100",
				"-thres", "1.0e-5"};
				//"-thres", "1.0e-3"};
			
			String[] sim_list = null;
			if(sim>0){
				sim_list = new String[1];
				sim_list[0] = Param.sim_str[sim];
			}else{
				sim_list = Arrays.copyOfRange(Param.sim_str, 1, Param.sim_str.length);
			}
			String[] query_list = null;
			if(query>0){
				query_list = new String[1];
				query_list[0] = Param.query_str[query];
			}else{
				query_list = Arrays.copyOfRange(Param.query_str, 1, Param.query_str.length);
			}
			
			
			String[] r_list ={"2"};
			String[] rmr_list = {/*"0.0","0.05","0.1",*/"0.2"};

			int i = 0;
			for(String rmr:rmr_list){
				String[] cmd_rmr = {"-rmr",rmr};
				String[] cmd0 = ArrayUtils.addAll(basic_args, cmd_rmr);
				for(String sim_str: sim_list){
					String[] cmd_sim = {"-sim", sim_str};
					String[] cmd1 = ArrayUtils.addAll(cmd0, cmd_sim);
					for(String query_str: query_list){
						String[] cmd_qry = {"-query",query_str};
						String[] cmd2 = ArrayUtils.addAll(cmd1, cmd_qry);
						for(String r:r_list){
							String[] cmd_r = {"-radius",r};
							String[] cmd = ArrayUtils.addAll(cmd2, cmd_r);
						
							System.out.println((++i)+"  "+StringUtils.join(cmd," "));
							GraphMatching gm = new GraphMatching(cmd);
							gm.init();
							gm.initDB();
							gm.run();
							gm.close();
							
							
						}
					}
				}
			}
	}
	
	
	public GraphMatching(Namespace nes) {
		super(nes);
		param = new Param(nes);
		loadWriter();
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
		expid = sqlunit.executeUpdateGetAutoKey(
			String.format(
				"insert into Experiment" +
				"(data,sim, query, nq, radius, rmr)" +
				"values ('%s','%s', '%s', %d, %d, %f );"
				,dataname, param.getSimString(), param.getQueryString(), nquery, model.getRadius(), rm_ratio));
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
							neighbor_pairs[s].add(x, y,1);
						}
					}
				}
				
			}
		}
	}
	


	@Override
	public void run() throws IOException {
		time = System.currentTimeMillis();
		int cnt = 0;
		int totalcost = 0;
		
		PrintStream out = new PrintStream(new FileOutputStream(outputfile));
		
		QueryNode[] queries = new QueryNode[nquery];
		CandNode[] cand = new CandNode[maxrnodesize];
		for (int i=0; i<maxrnodesize; i++) cand[i] = new CandNode();
		
		
		System.out.println("now, we start to find queries..");
		model.initialize(dat);
		if(em!=null && param.sim != Param.SIM_PROPOSED){
			em.initialize(dat);
		}
		
		if(param.query == Param.QUERY_NEIGHBORPAIR){
			initialize_neighbor_pairs();
		}
		
		int iteration = 0;
		double accuracy = 0.0;
		while ((accuracy<max_accuracy)&&(nmaxquery == -1 || cnt < nmaxquery) && (cnt < dat.totallnode)) {
			iteration++;
			// run model
			model.run();
			if(em!=null && param.sim != Param.SIM_PROPOSED){
				em.run();
			}
			
			// select a query & candidates
			int nq = select_query (nquery, queries);
			
			int tmpcnt = 0;
			for (int i=0; i<nq; i++) {
				QueryNode q = queries[i];
				
				int nc = select_candidates(q, cand);

				double cost = match(q, cand, nc);
				totalcost += cost;
				System.out.println("CNT=" + (cnt + tmpcnt) + ", COST: " + cost + ", ACC: " + totalcost+"   "+q.id+" of type "+dat.nodetypes[q.t]);
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

			logging(iteration, queries);
			model.update_effpairs(queries, dat);
			if(param.sim ==Param.SIM_PROPOSED){
				model.smoothing();
			}
			if(em!=null && param.sim != Param.SIM_PROPOSED){
				em.update_effpairs(queries, dat);
				em.smoothing();
			}
			update_after_matching(queries);
			
			accuracy = computeAccuracy(iteration, totalcost, nmatch);
			System.out.printf("Accuracy:  %f\n", accuracy);
		}
		
		out.close();
		time = System.currentTimeMillis()-time;
		pw.printf("Execution time: %d (ms)\n", time);
		
	}

	
	private void update_after_matching(QueryNode[] queries) {
		if(param.query == Param.QUERY_NEIGHBORPAIR){
			update_neighbor_pairs(queries);
		}
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
				ObjectIterator<Entry> iter = model.sim_next[t].getEntryIterator(i);
				while(iter.hasNext()){
					Entry entry = iter.next();
					if(entry.getValue()>val_max){
						idx_max = entry.getIntKey();
						val_max = entry.getDoubleValue();
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
			//System.out.println("=> selected: " + q.id + " of type " + dat.nodetypes[q.t]);
			res[idx++] = q;
		}
		return idx;
	}
	// choose n candidates for the node i & return the actual number of candidates
	protected int select_candidates(QueryNode q, CandNode[] cand) {
		//System.out.printf("query t: %d, i: %d, diff:%f, #np: %d\n", q.t,q.id, q.diff, neighbor_pairs[q.t].size(q.id));
		
		int idx=0;
		
		
		//Set similarities
		int rsize = dat.rnodes[q.t].size;
		for(int j = 0; j<rsize; j++){
			if(dat.rnodes[q.t].arr[j].label<0){
				cand[idx].id = j;
				cand[idx].w = model.sim_next[q.t].get(q.id, j);
				idx++;
			}
		}
		
				
		int n_cand = idx;
		Arrays.sort(cand, 0,n_cand, new Comparator<CandNode> () {
			@Override
			public int compare(CandNode c1, CandNode c2) {
				return (c1.w > c2.w) ? -1 : ((c1.w < c2.w) ? 1 : 0);
			}
		});
		
		//Set the maximum similarity
		q.max_sim = cand[0].w;
		
		
		//Compute Entropy
		q.entropy = 0;
		if(q.max_sim>0.0){
			int j=0;
			double sum = 0.0;
			double p = 0.0;
			for(; j<n_cand; j++){
				if(cand[j].w==0.0){
					break;
				}
				sum += cand[j].w;
			}
			for(j=0; j<n_cand; j++){
				p =cand[j].w;
				if(p==0.0){
					break;
				}
				p /= sum; 
				q.entropy -= (p*Math.log(p));
			}
		}
		
		
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
	
	
	public  void loadWriter(){
		String filename = String.format("log/%s_%s_%s_rmr%s_nq%d.log",
				dataname,param.getSimString(),param.getQueryString(),Double.toString(rm_ratio).replace(".","_"),nquery);
		try {
			bw = new BufferedWriter(new FileWriter(filename));
			pw = new PrintWriter(bw);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	
	private void logging(int iter, QueryNode[] queries){
		pw.printf("iteration: %d\n",iter);
		for(int q = 0; q <queries.length; q++){
			QueryNode qn = queries[q];
			if(q>0){
				pw.print("|");
			}
			pw.printf("%d,%d:%f,%f,%f,%f,%d",
					qn.t, qn.id,qn.diff, qn.max_sim, qn.matching_sim, qn.entropy, qn.cost);
			
		}
		pw.println();
	}
	public void close(){
		if(sqlunit!=null){
			sqlunit.exit();
		}
		try {
			pw.close();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

