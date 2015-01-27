package gm.main;

import gm.graph.Actor;
import gm.graph.Director;
import gm.graph.Graph;
import gm.graph.Movie;
import gm.graph.Vertex;

import java.util.ArrayList;

public class SimRank {
	double[][][] sim;
	public double C=0.8;
	
	
	public int nIter;
	public double th_convergence;
	

	public int stype;
	public static final int SIM_TYPE_SR = 0;
	public static final int SIM_TYPE_SRKL= 1;
	
	
	public int wtype;
	public static final int WEIGHT_TYPE_NPAIRS = 0;
	public static final int WEIGHT_TYPE_UNIFORM = 1;
	public static final int WEIGHT_TYPE_FLOODING = 2;
	

	
	Graph g0;
	Graph g1;
	
	
	
	public SimRank(Graph g0, Graph g1, int stype, int wtype, int nIter, double th_convergence){
		this.g0 = g0;
		this.g1 = g1;
		this.stype = stype;
		this.wtype = wtype;
		this.nIter = nIter;
		this.th_convergence = th_convergence;
		sim = new double[3][][];
		//for(int vtype = 0; vtype < sim.length; vtype++){}
		sim[0] = new double[g0.getMovies().length][g1.getMovies().length];
		sim[1] = new double[g0.getActors().length][g1.getActors().length];
		sim[2] = new double[g0.getDirectors().length][g1.getDirectors().length];
	}
	public double[][][] getSimRank(){
		if(!GraphMatching.LOGGING){
			int n0 = 0;
			int n1 = 0;
			for(int i = 0; i< sim.length; i++){
				for(int j = 0; j<sim[i].length; j++){
					for(int k = 0; k<sim[i][j].length; k++){
						if(sim[i][j][k]>0){
							n1++;
						}else{
							n0++;
						}
					}
				}
			}
			
			System.out.println("0: "+n0+" 1:"+n1);
		}
		return sim;
	}
	
	public void computeSimRank(){
		//System.out.println("Compute simrank");
		System.out.print("Compute simrank: ");
		for(int i = 0; i < nIter; i++){
			//System.out.println((i+1)+"/"+nIter+" iterations");
			System.out.print((i+1)+" ");
			if(updateSimRank()&&i>0){
				break;
			}
		}
		System.out.println();
	}
	public boolean updateSimRank(){
		boolean converge = true;
		
		double diff_ratio = updateSimRankMovies();
		//System.out.printf("%10s diff ratio: %.10f, th: %.10f\n", "movie", diff_ratio, th_convergence);
		if(diff_ratio > th_convergence){
			converge = false;
		}
		diff_ratio = updateSimRankActors();
		//System.out.printf("%10s diff ratio: %.10f, th: %.10f\n", "actor", diff_ratio, th_convergence);
		if(diff_ratio > th_convergence){
			converge = false;
		}
		diff_ratio = updateSimRankDirectors();
		//System.out.printf("%10s diff ratio: %.10f, th: %.10f\n", "director", diff_ratio, th_convergence);
		if(diff_ratio > th_convergence){
			converge = false;
		}
		return converge;
	}
	public double updateSimRankMovies(){
		double[][] sim_actors = sim[1];
		double[][] sim_directors = sim[2];
		
		Movie[] movies0 = g0.getMovies(); 
		Movie[] movies1 = g1.getMovies(); 
		double[][] newsim = new double[movies0.length][movies1.length];
		
		double abs_diff = 0.0;
		double sum = 0.0;
		for(int m0 = 0; m0 < movies0.length; m0++ ){
			for(int m1 = 0; m1 <movies1.length; m1++){
				if(movies0[m0].same(movies1[m1])){
					newsim[m0][m1] = 1.0;
					sum += newsim[m0][m1];
					continue;
				}
				//TODO: Weight 결정
				double w_a = 0.5;
				double w_d = 0.5;
				if(wtype==WEIGHT_TYPE_NPAIRS){
					w_a = movies0[m0].getActors().size()*movies1[m1].getActors().size();
					w_d = movies0[m0].getDirectors().size()*movies1[m1].getDirectors().size();
					double wsum = w_a + w_d;
					if(wsum==0.0){
						newsim[m0][m1] = 0.0;
						continue;
					}
					w_a /=wsum;
					w_d /=wsum;
				}
				if(stype==SIM_TYPE_SR){
					newsim[m0][m1] = w_a*simrank(movies0[m0].getActors(), movies1[m1].getActors(), sim_actors, C);
					newsim[m0][m1] += w_d*simrank(movies0[m0].getDirectors(), movies1[m1].getDirectors(), sim_directors, C);
				}else if(stype==SIM_TYPE_SRKL){
					if(movies0[m0].getKnownLink()!=null||movies1[m1].getKnownLink()!=null){
						newsim[m0][m1] = 0.0;
						continue;
					}
					newsim[m0][m1] = w_a*simrank_kl(movies0[m0].getActors(), movies1[m1].getActors(), sim_actors, C);
					newsim[m0][m1] += w_d*simrank_kl(movies0[m0].getDirectors(), movies1[m1].getDirectors(), sim_directors, C);
					
				}
				sum += newsim[m0][m1];
				abs_diff += Math.abs(newsim[m0][m1] - sim[0][m0][m1]);
				if(Double.isNaN(newsim[m0][m1])){
					System.out.println("NaN");
				}
			}
		}
		sim[0] = newsim;
		
		return abs_diff/sum;
	}
	public double  updateSimRankActors(){
		double[][] sim_movies = sim[Graph.VERTEX_TYPE_MOVIE];
		double[][] sim_actors = sim[Graph.VERTEX_TYPE_ACTOR];
			
		Actor[] actors0 = g0.getActors(); 
		Actor[] actors1 = g1.getActors(); 
		double[][] newsim = new double[actors0.length][actors1.length];
		
		double abs_diff = 0.0;
		double sum = 0.0;
		for(int a0 = 0; a0 < actors0.length; a0++ ){
			
			for(int a1 = 0; a1 <actors1.length; a1++){
				if(actors0[a0].same(actors1[a1])){
					newsim[a0][a1] = 1.0;
					sum += 1.0;
					continue;
				}
				
				
				if(stype==SIM_TYPE_SR){
					newsim[a0][a1] = simrank(actors0[a0].getMovies(), actors1[a1].getMovies(), sim_movies, C);
				}else if(stype==SIM_TYPE_SRKL){
					if(actors0[a0].getKnownLink()!=null||actors1[a1].getKnownLink()!=null){
						newsim[a0][a1] = 0.0;
						continue;
					}
					newsim[a0][a1] = simrank_kl(actors0[a0].getMovies(), actors1[a1].getMovies(), sim_movies, C);
				}
				sum += newsim[a0][a1];
				abs_diff += Math.abs(newsim[a0][a1]-sim_actors[a0][a1]);
			}
		}
		sim[Graph.VERTEX_TYPE_ACTOR] = newsim;
		
		return abs_diff/sum;
	}
	public double updateSimRankDirectors(){
		double[][] sim_movies = sim[Graph.VERTEX_TYPE_MOVIE];
		double[][] sim_directors = sim[Graph.VERTEX_TYPE_DIRECTOR];
		
		Director[] directors0 = g0.getDirectors(); 
		Director[] directors1 = g1.getDirectors(); 
		double[][] newsim = new double[directors0.length][directors1.length];
		
		double abs_diff = 0.0;
		double sum = 0.0;
		for(int d0 = 0; d0 < directors0.length; d0++ ){
			for(int d1 = 0; d1 <directors1.length; d1++){
				if(directors0[d0].same(directors1[d1])){
					newsim[d0][d1] = 1.0;
					sum += 1.0;
					continue;
				}
				if(stype==SIM_TYPE_SR){
					newsim[d0][d1] = simrank(directors0[d0].getMovies(), directors1[d1].getMovies(), sim_movies, C);
				}else if(stype==SIM_TYPE_SRKL){
					if(directors0[d0].getKnownLink()!=null || directors1[d1].getKnownLink()!=null){
						newsim[d0][d1] = 0.0;
						continue;
					}
					newsim[d0][d1] = simrank_kl(directors0[d0].getMovies(), directors1[d1].getMovies(), sim_movies, C);
					
				}
				
				sum+= newsim[d0][d1];
				abs_diff += Math.abs(newsim[d0][d1]-sim_directors[d0][d1]);
				
			}
		}
		sim[Graph.VERTEX_TYPE_DIRECTOR] = newsim;
		return abs_diff/sum;
	}
	
	public static double simrank(ArrayList<Vertex> neighbors0, ArrayList<Vertex> neighbors1, double[][] sim_, double C){
		double rank = 0.0;
		if(neighbors0.size()==0||neighbors1.size()==0){
			return rank;
		}
		for(Vertex v0:neighbors0){
			for(Vertex v1:neighbors1){
				try{
					rank += sim_[v0.getId()][v1.getId()];
				}catch(ArrayIndexOutOfBoundsException e){
					e.printStackTrace();
					System.exit(0);
				}
			}
		}
		
		rank=  C*rank/(neighbors0.size()*neighbors1.size());
		return rank;
	}
	
	public static double simrank_kl(ArrayList<Vertex> neighbors0, ArrayList<Vertex> neighbors1, double[][] sim_, double C){
		double rank = 0.0;
		if(neighbors0.size()==0||neighbors1.size()==0){
			return rank;
		}
		for(Vertex v0:neighbors0){
			for(Vertex v1:neighbors1){
				if(v0.same(v1)){
					rank += Math.min(neighbors0.size(), neighbors1.size());
					continue;
				}
				
				try{
					rank += sim_[v0.getId()][v1.getId()];
				}catch(ArrayIndexOutOfBoundsException e){
					e.printStackTrace();
					System.exit(0);
				}
			}
		}
		
		rank=  C*rank/(neighbors0.size()*neighbors1.size());
		return rank;
	}
}
