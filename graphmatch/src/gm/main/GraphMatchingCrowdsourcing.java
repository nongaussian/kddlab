package gm.main;

import gm.graph.Graph;
import gm.graph.Movie;

import java.util.Date;

import net.sourceforge.argparse4j.inf.Namespace;

public class GraphMatchingCrowdsourcing {
	GraphMatching gm;
	double th_matching_stop;
	int MAX_ITERATION = 50;
	SimRank sr;
	public static void main(String[] args){
		System.out.println(new Date());
		long ct = System.currentTimeMillis();
		Namespace nes = GraphMatching.parseArguments(args);
		GraphMatchingCrowdsourcing gmc  = new GraphMatchingCrowdsourcing(nes);
		gmc.run();
		System.out.println(new Date());
		System.out.printf("%d ms\n", System.currentTimeMillis()-ct);
	}
	
	public GraphMatchingCrowdsourcing(Namespace nes){
		gm = new GraphMatching(nes);
		th_matching_stop = nes.getDouble("thm");
		
	}
	
	public void run(){
		int i = 0;
		while(i++<MAX_ITERATION){
			int n_newlinks = 0;
			double[][][] sim = gm.computeSimrank();
			
			int[] matching = gm.hungarian(sim[Graph.VERTEX_TYPE_MOVIE]);
			int[] count = gm.matching_test(matching, Graph.VERTEX_TYPE_MOVIE);
			double precision = ((double)count[1])/(count[1]+count[0]);
			System.out.printf("Precision:  %f  true:%d false:%d\n",precision,count[1],count[0]);
			n_newlinks+=gm.setKnownLinks(matching,Graph.VERTEX_TYPE_MOVIE);
			
			matching = gm.hungarian(sim[Graph.VERTEX_TYPE_ACTOR]);
			n_newlinks+=gm.setKnownLinks(matching,Graph.VERTEX_TYPE_ACTOR);
			
			matching = gm.hungarian(sim[Graph.VERTEX_TYPE_DIRECTOR]);
			n_newlinks+=gm.setKnownLinks(matching, Graph.VERTEX_TYPE_DIRECTOR);
			
			
			int known = 0;
			int unknown = 0;
			for(Movie m: gm.gp.getMovies()){
				if(m.getKnownLink()!=null){
					known++;
				}else{
					unknown++;
				}
			}
			
			System.out.printf("Crowdsourcing iteration (%d/%d) - known: %d, unknown: %d\n",i,MAX_ITERATION, known, unknown);
			
			double matching_ratio = ((double)known)/(known+unknown);
			if(matching_ratio>=th_matching_stop){
				break;
			}
			if(n_newlinks==0){
				//break;
				System.out.println("converged");
				break;
			}

			
		}
		
	}
}
