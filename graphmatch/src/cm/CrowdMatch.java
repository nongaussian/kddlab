package cm;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import gm.data.SqlUnit;
import gm.graph.Actor;
import gm.graph.Director;
import gm.graph.Graph;
import gm.graph.Movie;
import gm.graph.Vertex;

public class CrowdMatch {
	Graph g,gp;
	SqlUnit sqlunit;
	double movie_pruning_ratio, relation_pruning_ratio, link_ratio, th_convergence;
	int exp_id;
	int simtype;
	int simweight_type;
	int maxMovies;
	
	int nIter;
	int gid;
	String dbname;

	public boolean LOGGING = false;
	
	public static void main(String[] args){
		Namespace nes = parseArguments(args);
		CrowdMatch gm = new CrowdMatch(nes);
		
		double[][][] sim=null;
		
		//Compute similarity
		String algorithm = nes.getString("algo");
		gm.run();
	}
	
	
	public CrowdMatch(Namespace nes){
		//Data parameters
		dbname 						= nes.getString("dbname");
		if(LOGGING&&dbname.length()>0){
			sqlunit = new SqlUnit(dbname);
		}else{
			LOGGING = false;
		}
		maxMovies 			= nes.getInt("maxMovies");
		movie_pruning_ratio 	= nes.getDouble("mpr");
		relation_pruning_ratio 	= nes.getDouble("epr");
		link_ratio 				= nes.getDouble("lr");
		
		//algorithm parameters
		String algorithm				= nes.getString("algo");
		
		//Data loading
		g = Graph.readGraph(getGname());
		gp = Graph.readGraph(getGPname());
		loadKnownLinks();
		
	}
	
	public String getGname(){
		return String.format("g_%s_mm%d", dbname, maxMovies);
	}
	public String getGPname(){
		return getGname()+String.format("_mpr%s_rpr%s_lr%s_%d", Double.toString(movie_pruning_ratio).replace(".", "_"), Double.toString(relation_pruning_ratio).replace(".", "_"), Double.toString(link_ratio).replace(".", "_"),gid);
	}
		
	
	public static Namespace parseArguments(String[] args){
		 ArgumentParser parser = ArgumentParsers.newArgumentParser("GraphMatching")
	                .description("Graph matching.");
	        parser.addArgument("-dbname")
	                .required(false)
	                .type(String.class)
	                .setDefault("")
	                .help("Database name");
	        parser.addArgument("-mpr")
	        		.type(Double.class)
	        		.help("movie prunining ratio");
	        parser.addArgument("-epr")
	        		.type(Double.class)
	        		.help("edge prunining ratio");
	        parser.addArgument("-lr")
	        		.type(Double.class)
	        		.setDefault(0.1)
	        		.help("known link ratio");
	        parser.addArgument("-algo")
	            .type(String.class)
	            .help("Algorithm [simrank,]");
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
	        parser.addArgument("-gid")
			        .type(Integer.class)
			        .help("Graph id");
	        parser.addArgument("-niter","--nIter")
			        .type(Integer.class)
			        .nargs("?")
			        .setDefault(100)
			        .help("Maximum number of iterations");
	        try {
	            Namespace res = parser.parseArgs(args);
	            System.out.println(res);
	            return res; 
	        } catch (ArgumentParserException e) {
	            parser.handleError(e);
	        }
	    return null;   
	}

	public void loadKnownLinks(){
		try {
			BufferedReader in = new BufferedReader(new FileReader(Graph.getKnownLinkPath(getGPname())));
			String line = in.readLine();
			
			
			//Read movie link
			boolean read = false;
			Movie[] movies = g.getMovies();
			Movie[] movies_p = gp.getMovies();
			while((line = in.readLine()).length()>0){
				if(!read){
					if(line.startsWith("Movies")){
						read = true;
					}
					continue;
				}
				String[] ids = line.split("-");
				int id_p = Integer.parseInt(ids[0]);
				int id = Integer.parseInt(ids[1]);
				
				movies[id].setKnownLink(movies_p[id_p]);
				movies_p[id_p].setKnownLink(movies[id]);
				
			}
			
			//Read actor links
			read = false;
			Actor[] actors = g.getActors();
			Actor[] actors_p = gp.getActors();
			while((line = in.readLine()).length()>0){
				if(!read){
					if(line.startsWith("Actors")){
						read = true;
					}
					continue;
				}
				String[] ids = line.split("-");
				int id_p = Integer.parseInt(ids[0]);
				int id = Integer.parseInt(ids[1]);
				
				actors[id].setKnownLink(actors_p[id_p]);
				actors_p[id_p].setKnownLink(actors[id]);
			}
			
			//Read director links
			
			read = false;
			Director[] directors = g.getDirectors();
			Director[] directors_p = gp.getDirectors();
			while((line = in.readLine())!=null){
				if(!read){
					if(line.startsWith("Directors")){
						read = true;
					}
					continue;
				}
				String[] ids = line.split("-");
				int id_p = Integer.parseInt(ids[0]);
				int id = Integer.parseInt(ids[1]);
				
				directors[id].setKnownLink(directors_p[id_p]);
				directors_p[id_p].setKnownLink(directors[id]);
			}
			
			
			in.close();
			
			
			
		} catch ( IOException e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	public void run(){
	}

}
