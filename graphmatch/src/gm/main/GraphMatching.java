package gm.main;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import gm.graph.Graph;
import gm.graph.Movie;
import gm.graph.Vertex;
import gm.sql.SqlUnit;

public class GraphMatching {
	Graph g,gp;
	SqlUnit sqlunit;
	double movie_pruning_ratio, relation_pruning_ratio, link_ratio, th_convergence;
	int exp_id;
	int simtype;
	int simweight_type;
	
	int maxMovies;
	
	int nIter;
	SimRank sr;
	public static boolean LOGGING = true;
	
	public static void main(String[] args){
		Namespace nes = parseArguments(args);
		
		GraphMatching gm = new GraphMatching(nes);
		gm.runSimrank();
		gm.close();
		
	}
	
	
	public GraphMatching(Namespace nes){
		String dbname 					= nes.getString("dbname");
		sqlunit = new SqlUnit(dbname);
		System.out.println(dbname);
		
		maxMovies 			= nes.getInt("maxMovies");
		
		movie_pruning_ratio 	= nes.getDouble("mpr");
		relation_pruning_ratio 	= nes.getDouble("epr");
		link_ratio 				= nes.getDouble("lr");
		simweight_type 			= setWeightType(nes.getString("wt"));
		nIter 					= nes.getInt("nIter");
		th_convergence			= nes.getDouble("thcvg");
		simtype 				= setSimType(nes.getString("st"));
		
		g = new Graph(sqlunit, maxMovies);
		gp = new Graph(g,movie_pruning_ratio, relation_pruning_ratio, link_ratio);
		
		sr = new SimRank(g,gp, simtype, simweight_type, nIter,th_convergence);
		
	}
	
		
	
	public static Namespace parseArguments(String[] args){
		 ArgumentParser parser = ArgumentParsers.newArgumentParser("GraphMatching")
	                .description("Graph matching.");
	        parser.addArgument("dbname")
	                //.metavar("-d")
	                .type(String.class)
	                .help("Database name");
	        parser.addArgument("-mpr")
	        		.type(Double.class)
	        		.required(true)
	        		.help("movie prunining ratio");
	        parser.addArgument("-epr")
	        		.type(Double.class)
	        		.required(true)
	        		.help("edge prunining ratio");
	        parser.addArgument("-lr")
	        		.type(Double.class)
	        		.setDefault(0.1)
	        		.help("known link ratio");
	        parser.addArgument("-thcvg")
	        .type(Double.class)
	        .setDefault(1E-7)
	        .help("Convergence threshold");
	        parser.addArgument("-thm")
	        .type(Double.class)
	        .setDefault(0.95)
	        .help("Matching threshold");
	        parser.addArgument("-st")
    		.type(String.class)
    		.setDefault("npairs")
    		.help(String.format("Similarity measures: sr srkl\n" +
    				"\t- sr: simrank " +
    				"\t- srkl: "));
	        parser.addArgument("-wt")
	        		.type(String.class)
	        		.setDefault("npairs")
	        		.help(String.format("Similarity weight function: npairs, uniform\n" +
	        				"\t- npairs: w_i is proportional to |I(u)|*|I(v)|\n" +
	        				"\t- uniform: w_i following a uniform distribution"));
	        parser.addArgument("-mm","--maxMovies")
	        		.type(Integer.class)
	        		.nargs("?")
	        		.setDefault(-1)
	        		.help("Maximum number of movies");
	        parser.addArgument("-niter","--nIter")
	        .type(Integer.class)
	        .nargs("?")
	        .setDefault(100)
	        .help("Maximum number of movies");
	        try {
	            Namespace res = parser.parseArgs(args);
	            System.out.println(res);
	            return res; 
	        } catch (ArgumentParserException e) {
	            parser.handleError(e);
	        }
	    return null;   
	}
	
	
	public static int setWeightType(String arg){
		int weight_type =-1;
		arg = arg.toLowerCase();
		if(arg.equals("npairs")){
			weight_type  =  SimRank.WEIGHT_TYPE_NPAIRS;
		}else if(arg.equals("uniform")){
			weight_type = SimRank.WEIGHT_TYPE_UNIFORM;
		}
		return weight_type;
	}
	
	public static int setSimType(String arg){
		int sim_type =-1;
		arg = arg.toLowerCase();
		if(arg.equals("sr")){
			sim_type  =  SimRank.SIM_TYPE_SR;
		}else if(arg.equals("srkl")){
			sim_type = SimRank.SIM_TYPE_SRKL;
		}
		return sim_type;
	}
	
	
	public void runSimrank(){
		long ct = System.currentTimeMillis();

		double[][][] sim = computeSimrank();
		//printMat(sim);
		int t_sim = (int)(System.currentTimeMillis()-ct);
		exp_id = registerExperiment(t_sim);
		System.out.println("t_sim: "+t_sim+"ms");
		
		ct = System.currentTimeMillis();
		computeAccuracy(sim);
		
		int t_matching = (int)(System.currentTimeMillis()-ct);
		registerTmatching(t_matching);
		System.out.println("t_matching: "+t_matching+"ms");
		
	}
	public double[][][] computeSimrank(){
		
		sr.computeSimRank();
		return  sr.getSimRank();
		
	}

	public int registerExperiment(int t_sim){
		if(!LOGGING)
			return -1;
		return sqlunit.executeUpdateGetAutoKey(String.format("insert into experiments (mm, opr, rpr, lr,stype, wtype,nIter, t_sim) values (%d, %f,%f,%f,%d, %d,%d,%d)", maxMovies, movie_pruning_ratio, relation_pruning_ratio, link_ratio, simtype, simweight_type,nIter,t_sim));
	}
	public void registerTmatching(int t_matching){
		if(!LOGGING)
			return;
		sqlunit.executeUpdate(String.format("update experiments set t_match = %d where id=%d", t_matching,exp_id));
	}
	
	public int[] matching_test(int [] matching, int t){
		int[] count = new int[3];
		for(int id = 0; id <matching.length; id++){
			Vertex v  = g.get(id, t);
			if(v.getKnownLink()!=null){
				continue;
			}
			if(matching[id]<0){
				count[2]++;
				continue;
			}
			
			Vertex vp = gp.get(matching[id], t);
			if(vp==null){
				count[0]++;
				continue;
			}
			
			count[v.getOriginalId()==vp.getOriginalId()?1:0]++;
		}
		return count;
	}
	
	public int[] hungarian(double[][] sim){
		Hungarian hungarian = new Hungarian(sim);
		return hungarian.execute();
	}
	
	
	public void computeAccuracy(double[][][] sim){
		
		int[] count_acc = new int[3];	//[vertex type][true:1/false:0/nomatch:2]
		int[][] count = new int[3][3];
		for(int t = 0; t< 3; t++){
			int[] matching = hungarian(sim[t]);
			count[t] = matching_test(matching, t);
			System.out.printf("Type: %d  (correct: %d, incorrect: %d, nomatch: %d) precision: %f\n",t,count[t][1],count[t][0],count[t][2],((double)count[t][1])/(count[t][1]+count[t][0]));
			if(LOGGING){
				String result_qry = String.format("insert into results (exp_id, vtype, n_true, n_false, n_nomatch) values (%d, %d, %d, %d, %d);",exp_id, t, count[t][1],count[t][0],count[t][2]);
				sqlunit.executeUpdate(result_qry);
			}
			for(int b = 0; b< 3; b++){count_acc[b] += count[t][b];}	
		}
		
		System.out.printf("Total  (correct: %d, incorrect: %d, nomatch: %d) precision: %f\n",count_acc[1],count_acc[0],count_acc[2],((double)count_acc[1])/(count_acc[1]+count_acc[0]));
		if(LOGGING){
			String result_qry = String.format("insert into results (exp_id, vtype, n_true, n_false, n_nomatch) values (%d,-1, %d, %d, %d);",exp_id,  count_acc[1],count_acc[0],count_acc[2]);
			sqlunit.executeUpdate(result_qry);
		}
	}
	public static void printMat(double[][][]in){
		for(int i = 0; i< in.length; i ++){
			System.out.println(i+" -----------------------------------------------------------");
			if(in[i].length>100){
				continue;
			}
			for(int j = 0; j<in[i].length; j++){
				System.out.print("|");
				for(int k = 0; k<in[i][j].length; k++){
					System.out.printf("%5.3f  ", in[i][j][k]);
				}
				System.out.println("|");
			}
		}
		System.out.println();
	}
	
	public void close(){
		sqlunit.exit();
	}
	
	
	public int setKnownLinks(int[] matching,int type){
		Vertex[] vertices = {};
		Vertex[] vertices_p = {};
		
		if(type==Graph.VERTEX_TYPE_MOVIE){
			vertices  = g.getMovies();
			vertices_p = gp.getMovies();
		}else if(type == Graph.VERTEX_TYPE_ACTOR){
			vertices = g.getActors();
			vertices_p = gp.getActors();
		}else if(type == Graph.VERTEX_TYPE_DIRECTOR){
			vertices = g.getDirectors();
			vertices_p = gp.getDirectors();
		}
		
		int newlink_count = 0;
		for(int v = 0; v<matching.length; v++){
			if(matching[v]>=0&&vertices[v].getKnownLink()==null){
				if(vertices[v].getOriginalId()==vertices_p[matching[v]].getOriginalId()){
					Vertex v1 = vertices[v];
					Vertex v0 = vertices_p[matching[v]];
					v0.setKnownLink(v1);
					v1.setKnownLink(v0);
					newlink_count++;
				}
			}
		}
		System.out.println("Made "+newlink_count+" new links");
		return newlink_count;
	}
	
	
	
}
