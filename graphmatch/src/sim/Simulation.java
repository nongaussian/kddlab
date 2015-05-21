package sim;

import gm.data.SqlUnit;

import java.io.IOException;
import java.sql.SQLException;
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
	//int ncand							= 100;
	int nmaxquery						= -1;
	int maxrnodesize					= 0;
	
	cm_data dat							= null;
	
	String dataname						= "";
	
	//String algorithm					= null;
	
	String lprefix						= null;
	String rprefix						= null;
	String wprefix						= null;
	String outputfile					= null;
	
	double max_accuracy					= 0.8;
	double rm_ratio						= 0.0;
	
	public Simulation(Namespace nes){
		wprefix						= nes.getString("wprefix");
		outputfile					= nes.getString("output");
		nmaxquery					= nes.getInt("nmaxq");
		nquery						= nes.getInt("nq");
		max_accuracy				= nes.getDouble("maxacc");
		rm_ratio					= nes.getDouble("rmr");
		
		dataname 					= nes.getString("data");
		lprefix						= "data/"+dataname;
		rprefix						= "data/"+dataname;
		
		if(outputfile.equals("null")){
			outputfile = dataname 
					+ "_rmr"+Param.double2String_filename(rm_ratio)
					+".out";
		}
		
		
	}
	
	public static ArgumentParser getArgParser(){
		ArgumentParser parser = ArgumentParsers.newArgumentParser("CrowdMatch")
                .description("Graph matching");

		parser.addArgument("-data")
				.type(String.class)
				.required(true)
				.help("Prefix name of the data");
		parser.addArgument("-wprefix")
					.type(String.class)
					.help("Prefix name of model parameters to use for initialization");
		parser.addArgument("-output")
					.type(String.class)
					.setDefault("null")
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
					.setDefault(0.0)
					.help("Edge (Movie-Actor) removing ratio");
		
		return parser;
	
	}
	
	public abstract void init();
	public void initDB(){
		try{
			sqlunit = new SqlUnit("graphmatching","graphmatching","graphmatching");
			regiExp();
		}catch(Exception e){
			sqlunit = null;
		}
	}
	public abstract void regiExp();
	public abstract void run() throws IOException;
	// compute cost (how many entries checked in cands to find the node i by linear search)
	public void finalize(){
		sqlunit.exit();
	}
	static final double P_GEOMETRIC_DIST_BAD_ANNOTATOR = 0.2;

	public void regiResult(int iteration, int cost, int nmatched, int ntrue, int nfalse, int count_miss){
		
		sqlunit.executeUpdate(
				String.format("insert into results " +
						"(exp_id,iteration, cost, nmatched, ntrue, nfalse, miss) " +
						"values (%d,%d,%d,%d,%d,%d);"
						,expid, iteration, cost, nmatched, ntrue, nfalse,count_miss));
		
		String log = String.format("" +
				"iteration: %d, matched: %d, true:%d, false:%d, miss:%d, accuracy:%f ", 
				iteration, nmatched, ntrue, nfalse, count_miss, ((double)ntrue)/(ntrue+nfalse));
		System.out.println(log);
	}

}
