package cm;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;


public class CrowdMatch {
	// constants
	private double em_converged				= 1.0e-6;
	private int em_max_iter					= 100;
		
	public cm_data dat						= null;
	public cm_model model					= null;
	public boolean LOGGING 					= false;
	
	// debug
	private final boolean verbose			= false;
	
	public static void main(String[] args) throws IOException {
		Namespace nes = parseArguments(args);
		CrowdMatch obj = new CrowdMatch(nes);
		
		// run em
		obj.run();
	}
	
	public CrowdMatch(Namespace nes){
		// data parameters
		String lprefix				= nes.getString("lprefix");
		String rprefix				= nes.getString("rprefix");
		em_max_iter					= nes.getInt("niter");
		em_converged				= nes.getDouble("thres");
		
		// model parameters
		double mu					= nes.getDouble("mu");
		
		// data loading
		dat 						= new cm_data(lprefix, rprefix);
		
		// init model parameter
		model						= new cm_model(dat);
		model.mu					= mu;
	}
		
	
	public static Namespace parseArguments(String[] args){
		ArgumentParser parser = ArgumentParsers.newArgumentParser("CrowdMatch")
	                .description("Graph matching");
			
		try {
			parser.addArgument("-mu")
		                .type(Double.class)
		                .setDefault(1.)
		                .help("mu");
			parser.addArgument("-niter")
				        .type(Integer.class)
				        .setDefault(100)
				        .help("Maximum number of iterations");
			parser.addArgument("-thres")
			            .type(Double.class)
			            .setDefault(1.0e-6)
			            .help("Threshold of log-likelihood ratio for convergence test");
			parser.addArgument("-lprefix")
						.type(String.class)
						.required(true)
						.help("Prefix name of left graph");
			parser.addArgument("-rprefix")
						.type(String.class)
						.required(true)
						.help("Prefix name of right graph");
			parser.addArgument("-label")
						.type(String.class)
						.required(true)
						.help("Label file name");

			Namespace res = parser.parseArgs(args);
			System.out.println(res);
			return res;
		} catch (ArgumentParserException e) {
			parser.handleError(e);
		}
		return null;
	}

	public void run() throws IOException{
		// open an output file stream for logging likelihoods
		PrintStream likelihood_file = new PrintStream(new FileOutputStream("res.our/likelihood.dat"));
		
		// initialize variational variables
		model.random_initialize_var();
		
		long t_start = System.currentTimeMillis();
		long t_finish, t_start_iter, t_finish_iter, gap;

		// run expectation maximization
		double likelihood = 0, likelihood_old = 0, converged_ratio = 1;

		int iter = 0;
		while (((converged_ratio < 0) 
				|| (converged_ratio > em_converged) 
				|| (iter <= 2)) && (iter < em_max_iter)) {
			
			/* 1) start an iteration */
			iter++;
			if(verbose) System.out.println("**** em iteration " + iter + " ****");

			// start time
			t_start_iter = System.currentTimeMillis();

			/* 3) m-step : update model parameters (w) */
			em_mle(iter);

			likelihood = compute_likelihood(); 

			/* 4) now, finalize this iteration */
			t_finish_iter = System.currentTimeMillis();
			gap = t_finish_iter - t_start_iter;

			converged_ratio = (likelihood_old - likelihood) / (likelihood_old);
			likelihood_old = likelihood;

			// output model and likelihood
			likelihood_file.println(likelihood + "\t" + converged_ratio + "\t" + gap + "\t" + (t_finish_iter - t_start));
			System.out.println("iteration (" + iter + ") likelihood: " + likelihood + "\t" + converged_ratio + " (" + gap + " ms)");

			if ((iter % 20) == 0) {
				model.save_model(String.format("res/%03d", iter));
			}
		}

		t_finish = System.currentTimeMillis();
		gap = t_finish - t_start;
		likelihood_file.println("execution time: " + gap);
		likelihood_file.close();

		// output the final model
		model.save_model("res.our/final");
	}

	private void em_mle(int iter) {
		for (int t=0; t<dat.ntype; t++) {
			for (int i=0; i<dat.lnodes[t].size; i++) {
				// compute variables use in this iteration only
				compute_alpha(t, i, model.alpha);
				
				double sum = 0;
				
				// compute a w distribution
				for (int j=0; j<dat.rnodes[t].size; j++) {
					// add alpha
					model.w_next[t].val[i][j] += model.alpha[j];
					
					for (int s=0; s<dat.ntype; s++) {
						// distribute delta
						if (!dat.rel[t][s]) continue;
						
						compute_delta(t, s, i, j, model.delta);
						
						int n_i = dat.lnodes[t].arr[i].neighbors[s].size;
						int n_j = dat.rnodes[t].arr[j].neighbors[s].size;
						
						for (int x=0; x<n_i; x++) {
							for (int y=0; y<n_j; y++) {
								model.w_next[s].val[x][y] += model.alpha[j] * model.delta[x][y];
							}
						}
					}
				}
				
				// add beta
				if (dat.lnodes[t].arr[i].label > 0) {
					model.w_next[t].val[i][dat.lnodes[t].arr[i].label] += model.mu * model.c[t];
				}
				
				// normalize the w distribution
				for (int j=0; j<dat.rnodes[t].size; j++) {
					model.w_next[t].val[i][j] /= sum;
				}
				for (int j=0; j<dat.rnodes[t].size; j++) {
					model.w_next[t].val[i][j] /= sum;
				}
			}
		}
		
		for (int t=0; t<dat.ntype; t++) {
			for (int i=0; i<dat.lnodes[t].size; i++) {
				
			}
		}
	}
	
	// XXX: for now, we assume each query has only one label 
	private void compute_beta(int t, int i, double[] beta) {
		/*
		double sum = 0;
		for (int j=0; j<dat.rnodes[t].size; j++) {
			beta[j] = model.w[t].val[i][j] * w_bar[j];
			sum += beta[j];
		}
		for (int j=0; j<dat.rnodes[t].size; j++) {
			beta[j] /= sum;
		}
		*/
	}

	// XXX: check the equation (2015. 2. 2.)
	private void compute_delta(int t, int s, int i, int j, double[][] delta) {
		int n_i = dat.lnodes[t].arr[i].neighbors[s].size;
		int n_j = dat.rnodes[t].arr[j].neighbors[s].size;
		double sum = 0;
		for (int x=0; x<n_i; x++) {
			for (int y=0; y<n_j; y++) {
				delta[x][y] = model.w[s].val[x][y] / (double) dat.rnodes[s].arr[y].neighbors[t].size;
				sum += delta[x][y]; 
			}
		}
		for (int x=0; x<n_i; x++) {
			for (int y=0; y<n_j; y++) {
				delta[x][y] /= sum; 
			}
		}
	}

	private void compute_alpha(int t, int i, double[] alpha) {
		double z = 0;
		for (int j=0; j<dat.rnodes[t].size; j++) {
			double tmp = 0;
			for (int s=0; s<dat.ntype; s++) {
				int n_i = dat.lnodes[t].arr[i].neighbors[s].size;
				int n_j = dat.rnodes[t].arr[j].neighbors[s].size;
				
				double sum = 0;
				for (int x=0; x<n_i; x++) {
					for (int y=0; y<n_j; y++) {
						sum += model.w[s].val[x][y] / (double) dat.rnodes[s].arr[y].neighbors[t].size;
					}
				}
				
				tmp += model.c[s] * sum;
			}
			
			alpha[j] = Math.sqrt(model.w[t].val[i][j] * tmp);
			z += alpha[j];
		}
		
		for (int j=0; j<dat.rnodes[t].size; j++) {
			alpha[j] /= z;
		}
	}
	
	public void compute_c () {
		double z = 0;
		
		for (int t=0; t<dat.ntype; t++) {
			double sum = 0;
			for (int i=0; i<dat.lnodes[t].size; i++) {
				double tmp = 0;
				
				for (int j=0; j<dat.lnodes[t].size; j++) {
					tmp += Math.sqrt(model.w[t].val[i][dat.lnodes[t].arr[i].label]);
				}
				
				sum += Math.log(tmp);
			}
			
			model.c[t] = 2 + model.mu * sum;
			z += model.c[t];
		}
		
		for (int t=0; t<dat.ntype; t++) {
			model.c[t] /= z;
		}
	}
	
	public double compute_likelihood () {
		return 0;
	}
}