package sim;

import graph.neighbors;
import graph.node;
import it.unimi.dsi.fastutil.ints.Int2DoubleMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
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
	
	
	@Deprecated
	public SparseIntMatrix[] neighbor_pairs		= null;
	
	Param param;
	BufferedWriter bw;
	PrintWriter pw;
	
	
	long time;

	
	
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
		parser.addArgument("-runtoy")
			.type(Boolean.class)
			.setDefault(false)
			.help("Save the model (similarities) if enable this option");
		parser.addArgument("-perfect_ann")
			.type(Boolean.class)
			.setDefault(true)
			.help("Enable the perfect annotator assumption");
		parser.addArgument("-err_ann")
			.type(Double.class)
			.setDefault(0.0)
			.help("Annotator error ratio\n" +
					"Disable -perfect_ann if you want to simulate annotator error");
		parser.addArgument("-mu")
		.type(Double.class)
		.setDefault(1.0);
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
		case Param.QUERY_MAXVAR:
		case Param.QUERY_MINVAR:
			break;
		case Param.QUERY_NEIGHBORPAIR:
		case Param.QUERY_NEIGHBORPAIR_OVERLAP:
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
				"(data,sim, query, nq, radius, rmr,err,mu)" +
				"values ('%s','%s', '%s', %d, %d, %f ,%f,%f);"
				,dataname, param.getSimString(), param.getQueryString(), nquery, model.getRadius(), rm_ratio,param.err_ann,param.nes.getDouble("mu")));
	}
	
	
	
	@Deprecated
	void initialize_neighbor_pairs(){
		neighbor_pairs = new SparseIntMatrix[dat.ntype];
		for (int t=0; t<dat.ntype; t++) {
			neighbor_pairs[t] = new SparseIntMatrix(dat.lnodes[t].size);
		}
		for (int t=0; t<dat.ntype; t++) {
			for (int i=0; i<dat.lnodes[t].size; i++) {
				if(dat.lnodes[t].arr[i].getNAnnotatios()==0) continue;
				for (int s=0; s<dat.ntype; s++) {
					if(!dat.rel[t][s]){
						continue;
					}
					neighbors lneighbors = dat.lnodes[t].arr[i].neighbors[s];
					neighbors rneighbors = dat.rnodes[t].arr[dat.lnodes[t].arr[i].getLastLabel()].neighbors[s];
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
		
		if(param.query == Param.QUERY_NEIGHBORPAIR||param.query == Param.QUERY_NEIGHBORPAIR_OVERLAP){
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
			
			if(param.runtoy){
				model.saveSimilarity(param, dataname, rm_ratio, nquery, iteration);
			}
			
			
			accuracy = computeAccuracy(iteration, totalcost, nmatch);
			System.out.printf("Accuracy:  %f\n", accuracy);
			if (accuracy>max_accuracy){
				break;
			}
			
			// select a query & candidates
			int nq = select_query (nquery, queries,iteration);
			
			int tmpcnt = 0;
			for (int i=0; i<nq; i++) {
				QueryNode q = queries[i];
				
				int nc = select_candidates(q, cand);

				double cost = match(q, cand, nc,param);
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
					dat.lnodes[q.t].arr[q.id].annotate(q.matched_id);
					dat.rnodes[q.t].arr[q.matched_id].annotate(q.id);
					//System.out.printf("match node (t,i,j,w)=(%2d,%5d,%5d,%5f)\n",q.t,q.id,q.matched_id,model.w[q.t].get(q.id, q.matched_id));
					nmatch++;
				}
			}

			logging(iteration, queries);
			model.update_effpairs(queries, dat, param);
			if(param.sim ==Param.SIM_PROPOSED){
				model.smoothing();
			}
			if(em!=null && param.sim != Param.SIM_PROPOSED){
				em.update_effpairs(queries, dat, param);
				em.smoothing();
			}
			update_after_matching(queries);
			
			
		}
		
		out.close();
		
		model.saveSimilarity(param, dataname, rm_ratio, nquery, -1);
		
		time = System.currentTimeMillis()-time;
		pw.printf("Execution time: %d (ms)\n", time);
		
	}

	
	private void update_after_matching(QueryNode[] queries) {
		if(param.query == Param.QUERY_NEIGHBORPAIR||param.query ==Param.QUERY_NEIGHBORPAIR_OVERLAP){
			update_neighbor_pairs(queries);
		}
	}

	private static final double TH_SIM_MATCHING = 0.0;
	protected final double computeAccuracy(int iteration,int cost, int nmatched){
		int count_correct 	= 0;
		int count_tot		= 0;
		int count_pass	= 0;
		//TODO non overlapping matching
		
		
		if(param.perfect_ann){
			for (int t=0; t<dat.ntype; t++) {
				count_tot += dat.lnodes[t].size;
				for (int i=0; i<dat.lnodes[t].size; i++) {
					if(dat.lnodes[t].arr[i].getNAnnotatios()>0){
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
			
		}else{
			ObjectArrayList<EntryWrapper> links = new ObjectArrayList<EntryWrapper>();
			for (int t=0; t<dat.ntype; t++) {
				links.clear();
				count_tot+=dat.lnodes[t].size;
				boolean[] flag_lmatched = new boolean[dat.lnodes[t].size];
				boolean[] flag_rmatched = new boolean[dat.rnodes[t].size];
				Arrays.fill(flag_lmatched, false);
				Arrays.fill(flag_rmatched, false);
				for (int i=0; i<dat.lnodes[t].size; i++) {
					node n_ti = dat.lnodes[t].arr[i];
					if(n_ti.getNAnnotatios()>0){
						if(n_ti.getWbar(n_ti.getLastLabel())==1.0){
							flag_lmatched[i] = true;
							flag_rmatched[n_ti.getLastLabel()] = true;
							if(n_ti.getLastLabel()==i){
								count_correct++;
								count_pass--;
							}
							continue;
						}
					}
					ObjectIterator<Entry> iter = model.sim_next[t].getEntryIterator(i);
					while(iter.hasNext()){
						Entry entry = iter.next();
						if(entry.getDoubleValue()>TH_SIM_MATCHING){
							links.add(new EntryWrapper(i, entry));
						}
					}
				}
				Collections.sort(links, new Comparator<EntryWrapper>() {
					@Override
					public int compare(EntryWrapper o1, EntryWrapper o2) {
						double comp = o2.w-o1.w;
						if(comp==0.0)return 0;
						return comp>0.0?1:-1;
					}
				});
				for(EntryWrapper ew: links){
					if(flag_lmatched[ew.id]||flag_rmatched[ew.j])
						continue;
					if(ew.id==ew.j){
						count_correct++;
					}
					count_pass--;
					flag_lmatched[ew.id] = true;
					flag_rmatched[ew.j] = true;
				}
			}
			count_pass+=count_tot;
		}
		regiResult(iteration, cost, nmatched, count_correct, count_tot-count_correct,count_pass);
		return ((double)count_correct)/count_tot;
	}
	
	// select the best n queries & return the actual number of selected queries
	int select_query(int n, QueryNode[] res,int iteration) {
		Comparator<QueryNode> comp = new Comparator<QueryNode> () {
			public int compare (QueryNode q1, QueryNode q2) {
				return (q1.diff < q2.diff) ? -1 : ((q1.diff > q2.diff) ? 1 : 0);
			}
		};
		
		
		PriorityQueue<QueryNode> topk  = new PriorityQueue<QueryNode> (n, comp);
		double val = 0.0;
		//int tie_count = 0;
		StringBuilder qry_sb = new StringBuilder();
		for (int t=0; t<dat.ntype; t++) {
			for (int i=0; i<dat.lnodes[t].size; i++) {
				if (dat.lnodes[t].arr[i].getNAnnotatios() > 0){
					if(param.perfect_ann){
						continue;
					}
					
										
					boolean b_cont = false;
					switch (param.query){
					case Param.QUERY_MAXVAR:
					case Param.QUERY_MINVAR:
					case Param.QUERY_NEIGHBORPAIR:
						b_cont = true;
						break;
					
					}
					if (b_cont){
						continue;
					}
				}
				

				switch (param.query){
					case Param.QUERY_EMC:
						val = em.compute_expected_model_change(t, i);
						break;
					case Param.QUERY_MAXVAR:
					case Param.QUERY_MINVAR:
						if(model.eff_pairs[t][i].size()==0){
							continue;
						}else{
							val = model.similarity_variance(t, i);
							if (param.query ==Param.QUERY_MINVAR){
								val = -val;
							}
						}
						break;
					case Param.QUERY_NEIGHBORPAIR:
					case Param.QUERY_NEIGHBORPAIR_OVERLAP:
						val = (double) neighbor_pairs[t].size(i);
						break;
					case Param.QUERY_RANDOM:
						val = rand.nextDouble();
						break;
				}
				
				
				
				if(param.runtoy){
					if(i==0&&t==0){
						qry_sb.append(String.format("%d,%d,%f", t,i,val));
					}else{
						qry_sb.append(String.format("|%d,%d,%f", t,i,val));
					}
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
				}else if(topk.peek().diff == val){
					
				}
			}
		}
		
		
		int idx=0;
		for (QueryNode q : topk) {
			//System.out.println("=> selected: " + q.id + " of type " + dat.nodetypes[q.t]);
			res[idx++] = q;
		}
		
		if(param.runtoy){
			writeQueries(iteration, qry_sb.toString(), res);
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
			if(param.perfect_ann&& dat.rnodes[q.t].arr[j].getNAnnotatios()>0)
				continue;
			cand[idx].id = j;
			cand[idx].w = model.sim_next[q.t].get(q.id, j);
			idx++;
			
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
	
	
	@Deprecated
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
	
	public void loadWriter(){
		File dir  = new File("log/"+dataname);
		dir.mkdir();
		String filename = param.logFilePath(dataname, rm_ratio, nquery);
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
			pw.printf("%d,%d:%f,%f,%f,%f,%d,%d",
					qn.t, qn.id,qn.diff, qn.max_sim, qn.matching_sim, qn.entropy, qn.cost,qn.n_annotates);
			
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
	
	private void writeQueries(int iteration,String qry_str, QueryNode[] queries){
		String dir = "query";
		File f = new File(dir);
		f.mkdir();
		dir += ("/"+dataname);
		f = new File(dir);
		f.mkdir();
		
		String filename = String.format("%s/%s_%s_rmr%s_nq%d%s", dir, param.getQueryString(), param.getSimString(),Param.double2String_filename(rm_ratio),nquery,param.optAnn());
		
		if(iteration>0){
			filename += ("."+iteration);
		}
		try {
			PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(filename)));
			pw.println(qry_str);
			for(int q = 0; q<queries.length; q++){
				if(q!=0){
					pw.print("|");
				}
				pw.printf("%d,%d,%f",queries[q].t,queries[q].id, queries[q].diff);
			}
			pw.println();
			pw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	protected int match(QueryNode q, CandNode[] cand, int nc, Param param) {
		q.initKnownLink();
		if(param.perfect_ann||rand.nextDouble()>param.err_ann){
			int i=0;
			while(cand[i].w>0.0) {
				if (q.id == cand[i].id) {
					q.setKnownLink(i,cand[i],dat.lnodes[q.t].arr[q.id].getNAnnotatios());
					return (i+1);
				}
				i++;
			}
			
			//Return expected cost if the similarity is zero
			int cost =  i+(int)Math.ceil((nc-i)/2.0);
			while (i<nc) {
				if (q.id == cand[i].id) {
					q.setKnownLink(i,cand[i],dat.lnodes[q.t].arr[q.id].getNAnnotatios());
					break;
				}
				i++;
			}
			return cost;
		}
		
		int i = 0;
		if(q.id==cand[i].id){
			i++;
		}
		q.setKnownLink(i, cand[i],dat.lnodes[q.t].arr[q.id].getNAnnotatios());
		/*
		//Geometric distribution
		int i = Math.min(nc-1, (int)Math.floor(Math.log(rand.nextDouble())/Math.log((1.0-P_GEOMETRIC_DIST_BAD_ANNOTATOR))));
		q.setKnownLink(i, cand[i]);
		*/
		return i+1;
	}
	
}


class EntryWrapper{
	int id;
	int j;
	double w;
	public EntryWrapper(int id,Entry e){
		this.id = id;
		this.j = e.getIntKey();
		this.w = e.getDoubleValue();
	}
}



