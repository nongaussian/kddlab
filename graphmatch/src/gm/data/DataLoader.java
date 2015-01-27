package gm.data;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import gm.graph.Graph;

public class DataLoader {
	public static void main(String[] args){
		Namespace nes = parseArguments(args);
	
		String databasename 			= nes.getString("dbname");
		double movie_pruning_ratio 		= nes.getDouble("mpr");
		double relation_pruning_ratio 	= nes.getDouble("epr");
		double link_ratio 				= nes.getDouble("lr");
		
		int maxMovies 					= nes.getInt("maxMovies");
		int nPrunedGraphs 				= nes.getInt("npg");
		
		makeGraphsfromRDB(databasename, maxMovies, nPrunedGraphs, movie_pruning_ratio, relation_pruning_ratio, link_ratio);
	}
	
	public static void makeGraphsfromRDB(String databasename, int maxMovies, int nPrunedGraphs, double movie_pruning_ratio, double relation_pruning_ratio, double link_ratio){
		SqlUnit sqlunit = new SqlUnit(databasename);
		
		Graph g = loadRDBdata(sqlunit, maxMovies);
		
		g.writeGraph(g.graphName());
		//g.writeGraphText(g.graphName());
		
		for(int gid = 0; gid < nPrunedGraphs; gid++){
			Graph gp = makePrunedGraph(g, sqlunit, movie_pruning_ratio, relation_pruning_ratio, link_ratio);
			gp.writeGraph(gp.graphName()+"_"+gid);
			gp.writeKnownLinks(gid);
			g.clearKnownLink();
			//gp.writeGraphText(gp.graphName()+"_"+gid);
		}
		
	}
	
	public static Graph loadRDBdata(SqlUnit sqlunit, int maxMovies){
		Graph g = new Graph(sqlunit, maxMovies);
		return g;
	}
	
	public static Graph makePrunedGraph(Graph g, SqlUnit sqlunit, double movie_pruning_ratio, double relation_pruning_ratio, double link_ratio){
		Graph gp = new Graph(g, sqlunit, movie_pruning_ratio, relation_pruning_ratio, link_ratio);
		return gp;
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
	        parser.addArgument("-mm","--maxMovies")
	        		.type(Integer.class)
	        		.nargs("?")
	        		.setDefault(-1)
	        		.help("Maximum number of movies");
	        parser.addArgument("-npg")
	        .type(Integer.class)
	        .help("Number of pruned graphs");
	        try {
	            Namespace res = parser.parseArgs(args);
	            //System.out.println(res);
	            return res; 
	        } catch (ArgumentParserException e) {
	            parser.handleError(e);
	        }
	    return null;   
	}
	

}
