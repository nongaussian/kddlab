package gm.data;

import net.sourceforge.argparse4j.inf.Namespace;
import gm.graph.Graph;

public class DataLoader {
	
	
	public void main(String[] args){
		//Namespace nes = GraphMatching.parseArguments(args);
		
		String databasename = "imdb_small";
		int[] maxMovies_list = {100, 250, 500, 1000};
		int nPrunedGraphs = 5;
		
		for(int maxMovies: maxMovies_list){
			//makeGraphsfromRDB(databasename, maxMovies, nPrunedGraphs);
		}
	}
	
	public static void makeGraphsfromRDB(String databasename, int maxMovies, int nPrunedGraphs, double movie_pruning_ratio, double relation_pruning_ratio, double link_ratio){
		SqlUnit sqlunit = new SqlUnit(databasename);
		
		Graph g = loadRDBdata(sqlunit, maxMovies);
		
		g.writeGraph(g.graphName());
		g.writeGraphText(g.graphName());
		
		for(int gid = 0; gid <= nPrunedGraphs; gid++){
			Graph gp = makePrunedGraph(g, sqlunit, movie_pruning_ratio, relation_pruning_ratio, link_ratio);
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
		
	public static void saveGraph(){
		
	}
	
	

}
