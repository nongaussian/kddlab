package sim;

import gm.data.SqlUnit;

import java.io.IOException;
import java.util.Random;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;
import cm.CandNode;
import cm.QueryNode;
import cm.cm_data;

public abstract class Simulation {
	static int CONST_MAX_MATCH	= 2*1000;
	static Random rand = new Random();
	public SqlUnit sqlunit = new SqlUnit();
	int expid;
	
	int nmatch							= 0;
	
	int nquery							= 10;
	int ncand							= 100;
	int nmaxquery						= -1;
	int maxrnodesize					= 0;
	
	cm_data dat							= null;
	
	String algorithm					= null;
	
	String lprefix						= null;
	String rprefix						= null;
	String wprefix						= null;
	String outputfile					= null;
	
	double max_accuracy					= 0.8;
	double rm_ratio						= 0.0;
	
	public Simulation(Namespace nes){
		lprefix						= nes.getString("lprefix");
		rprefix						= nes.getString("rprefix");
		wprefix						= nes.getString("wprefix");
		outputfile					= nes.getString("output");
		nmaxquery					= nes.getInt("nmaxq");
		nquery						= nes.getInt("nq");
		ncand						= nes.getInt("ncand");
		max_accuracy				= nes.getDouble("maxacc");
		rm_ratio					= nes.getDouble("rmr");
		
	}
	
	public static ArgumentParser getArgParser(){
		ArgumentParser parser = ArgumentParsers.newArgumentParser("CrowdMatch")
                .description("Graph matching");
		parser.addArgument("-lprefix")
					.type(String.class)
					.required(true)
					.help("Prefix name of left graph");
		parser.addArgument("-rprefix")
					.type(String.class)
					.required(true)
					.help("Prefix name of right graph");
		parser.addArgument("-wprefix")
					.type(String.class)
					.help("Prefix name of model parameters to use for initialization");
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
		parser.addArgument("-maxacc")
					.type(Double.class)
					.setDefault(1.0)
					.help("Maximum accuracy (default: 1.0)");
		parser.addArgument("-rmr")
					.type(Double.class)
					.setDefault(0.0);
		parser.addArgument("-ncand")
					.type(Integer.class)
					.setDefault(-1);
		
		return parser;
	
	}
	
	public abstract void init();
	public void initDB(){
		sqlunit = new SqlUnit("graphmatching","graphmatching","graphmatching");
		regiExp();
	}
	public abstract void regiExp();
	public abstract void run() throws IOException;
	// compute cost (how many entries checked in cands to find the node i by linear search)
	public void finalize(){
		sqlunit.exit();
	}
	protected int match(QueryNode q, CandNode[] cand, int nc) {
		q.setKnownLink(null);
		for (int i=0; i<nc; i++) {
			if (q.id == cand[i].id) {
				q.setKnownLink(cand[i]);
				return (i+1);
			}
		}
		return nc;
	}
	
	public void regiResult(int iteration, int cost, int nmatched, int ntrue, int nfalse){
		sqlunit.executeUpdate(
				String.format("insert into results " +
						"(exp_id,iteration, cost, nmatched, ntrue,nfalse) " +
						"values (%d,%d,%d,%d,%d,%d);"
						, expid,iteration,cost,nmatched,ntrue,nfalse));
		
		
	}

}
