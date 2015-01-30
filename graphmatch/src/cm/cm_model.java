package cm;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Random;

import org.apache.commons.math3.special.Gamma;


/*
 * manage model parameters
 * - variables
 * - functions to save to & load from files 
 */
public class cm_model {
	// data
	cm_data dat = null;

	// model parameters
	int type
	double 

	public cm_model(int n_seq, int n_word, int n_region, int n_topic, Sequence[] seqs) {
		this.n_seq = n_seq;
		this.n_word = n_word;
		this.n_region = n_region;
		this.n_topic = n_topic;
		this.seqs = seqs;
		
		unif = Math.log(1. / (double)n_region);

		// approximate for theta (n_region by n_topic)
		var_a						= new double[n_region][n_topic];
		next_a						= new double[n_region][n_topic];
		var_asum					= new double[n_region];
		digamma_a					= new double[n_region][n_topic];
		digamma_asum				= new double[n_region];
		
		// approximate for phi (n_topic by n_word)
		var_b						= new double[n_topic][n_word];
		next_b						= new double[n_topic][n_word];
		var_bsum					= new double[n_topic];
		digamma_b					= new double[n_topic][n_word];
		digamma_bsum				= new double[n_topic];
		
		// approximate for lambda (n_seq by 2)
		var_c						= new double[n_seq][2];
		var_csum					= new double[n_seq];
		digamma_c					= new double[2];
		digamma_csum				= 0;

		//var_e						= new double[n_seq][2];
		//var_esum					= new double[n_seq];
		//digamma_e					= new double[2];
		//digamma_esum				= 0;
		
		for(Sequence s : seqs) {
			if (max_seqlen < s.tweets.length) {
				max_seqlen = s.tweets.length;
			}
			for (Tweet t : s.tweets) {
				if (t == null) {
					System.err.println ("empty msg: " + s.sid);
					System.exit(1);
				}
				if (max_msglen < t.len) {
					max_msglen = t.len;
				}
			}
		}

		// approximate for S (max_seqlen by 2)
		var_sigma					= new double[max_seqlen][2];
		
		// approximate for R (max_seqlen by n_region)
		var_rho						= new double[max_seqlen][n_region];

		// approximate for B (max_seqlen by 2)
		//var_omega					= new double[max_seqlen][max_msglen][2];

		// approximate for Z (max_seqlen by max_len by n_topic)
		var_zhi						= new double[max_seqlen][n_topic];
		
		// model parameters for Gaussian distribution (n_region by 2)
		mu							= new double[n_region][2];
		Sigma						= new double[n_region][3];
		next_mu						= new double[n_region][2];
		next_Sigma					= new double[n_region][3];
		next_denom					= new double[n_region];
		
		log_phi0						= new double[n_word];
		
		// transition prob (n_region+1 by n_region)
		delta						= new double[n_region][n_topic][n_region];
		delta0						= new double[n_region];
		log_delta					= new double[n_region][n_topic][n_region];
		log_delta0					= new double[n_region];

		next_delta					= new double[n_region][n_topic][n_region];
		next_delta0					= new double[n_region];

		// priors
		alpha 						= new double[n_topic];
		beta 						= new double[n_word];
		gamma						= new double[2];
		nu							= new double[2];
		
		// temporary
		tmplognormal				= new double[max_seqlen][n_region];

		alpha_sum = 0;
		for(int i = 0 ; i < n_topic ; i++) {
			alpha[i] = init_alpha;
			alpha_sum += init_alpha;
		}
		
		beta_sum = 0;
		for(int i = 0 ; i < n_word ; i++) {
			beta[i] = init_beta;
			beta_sum += init_beta;
		}
		
		gamma[0] = init_gamma[0];
		gamma[1] = init_gamma[1];
		gamma_sum = gamma[0] + gamma[1];
		
		//nu_sum = init_nu[0] + init_nu[1];
		//nu[0] = Math.log(init_nu[0] / nu_sum);
		//nu[1] = Math.log(init_nu[1] / nu_sum);
		

		g = new double[Math.max(n_topic, n_word)];
		h_1 = new double[Math.max(n_topic, n_word)];
		prev = new double[Math.max(n_topic, n_word)];
	}

	/*
	 * setting variational variables
	 */
	public void random_initialize_var_outer() {
		Random rand = new Random(System.currentTimeMillis());

		for (int r=0; r<n_region; r++) {
			var_asum[r] = 0;
			for(int k=0; k<n_topic; k++) {
				var_a[r][k] = alpha[k] + rand.nextDouble() * 160;
				digamma_a[r][k] = Gamma.digamma(var_a[r][k]);
				var_asum[r] += var_a[r][k];
			}
			digamma_asum[r] = Gamma.digamma(var_asum[r]);
		}
		
		for (int k=0; k<n_topic; k++) {
			var_bsum[k] = 0;
			for (int w=0; w<n_word; w++) {
				var_b[k][w] = beta[w] + rand.nextDouble() * 160;
				digamma_b[k][w] = Gamma.digamma(var_b[k][w]);
				var_bsum[k] += var_b[k][w]; 
			}
			digamma_bsum[k] = Gamma.digamma(var_bsum[k]);
		}		
		
		for (int t=0; t<n_seq; t++) {
			var_c[t][0] = gamma[0] + rand.nextDouble() * 16;
			var_c[t][1] = gamma[1] + rand.nextDouble() * 16;
			var_csum[t] = var_c[t][0] + var_c[t][1];
		}
		
		//for (int t=0; t<n_seq; t++) {
		//	var_e[t][0] = nu[0] + rand.nextDouble() * 16;
		//	var_e[t][1] = nu[1] + rand.nextDouble() * 16;
		//	var_esum[t] = nu[0] + nu[1];
		//}
		
		// delta
		double tmp1 = 0;
		for (int r1=0; r1<n_region; r1++) {
			for (int k=0; k<n_topic; k++) {
				delta0[r1] = rand.nextDouble();
				tmp1 += delta0[r1];
				
				double tmp2 = 0;
				for (int r2=0; r2<n_region; r2++) {
					delta[r1][k][r2] = rand.nextDouble();
					tmp2 += delta[r1][k][r2];
				}
				for (int r2=0; r2<n_region; r2++) {
					delta[r1][k][r2] /= tmp2;
					if (delta[r1][k][r2] > 0) log_delta[r1][k][r2] = Math.log(delta[r1][k][r2]);
				}
			}
		}
		for (int r1=0; r1<n_region; r1++) {
			delta0[r1] /= tmp1;
			if (delta0[r1] > 0) log_delta0[r1] = Math.log(delta0[r1]);
		}
		
		// mu and Sigma (init using samples)
		double s0 = 0, s1 = 0, ss0 = 0, ss1 = 0;
		for (int i=0; i<64; i++) {
			int rnd1 = rand.nextInt(n_seq);
			int rnd2 = rand.nextInt(seqs[rnd1].tweets.length);
			s0 += seqs[rnd1].tweets[rnd2].lat;
			s1 += seqs[rnd1].tweets[rnd2].lon;
			ss0 += seqs[rnd1].tweets[rnd2].lat * seqs[rnd1].tweets[rnd2].lat;
			ss1 += seqs[rnd1].tweets[rnd2].lon * seqs[rnd1].tweets[rnd2].lon;
		}

		for (int r=0; r<n_region; r++) {
			
			//if (r == n_region) {
			//	mu0[0] = s0 / (double)64.;
			//	mu0[1] = s1 / (double)64.;
				//Sigma0[0] = ss0 / 64. - mu0[0] * mu0[0];
				//Sigma0[1] = ss1 / 64. - mu0[1] * mu0[1];
			//	Sigma0[0] = 1.0e+8;
			//	Sigma0[1] = 1.0e+8;
			//}
			//else {
				mu[r][0] = s0 / (double)64.;
				mu[r][1] = s1 / (double)64.;
				Sigma[r][0] = ss0 / (double)64. - mu[r][0] * mu[r][0];
				Sigma[r][1] = ss1 / (double)64. - mu[r][1] * mu[r][1];
				Sigma[r][2] = 0;
				//Sigma[r][0] *= 10;
				//Sigma[r][1] *= 10;
			//}
		}
		
		/*
		double maxlat = -360, minlat = 360;
		double maxlon = -360, minlon = 360;
		for (int t=0; t<n_seq; t++) {
			for (int i=0; i<seqs[t].tweets.length; i++) {
				if (maxlat < seqs[t].tweets[i].lat)
					maxlat = seqs[t].tweets[i].lat;
				if (minlat > seqs[t].tweets[i].lat)
					minlat = seqs[t].tweets[i].lat;
				if (maxlon < seqs[t].tweets[i].lon)
					maxlon = seqs[t].tweets[i].lon;
				if (minlon > seqs[t].tweets[i].lon)
					minlon = seqs[t].tweets[i].lon;
			}
		}
		*/
		
		//f0 = -10000;
		//log_f0 = 3; //Math.log((maxlat - minlat) * (maxlon - minlon));
		//System.out.println (f0);
		//System.exit(1);
		//f0 = 5;
	}
	
	public void random_initialize_var_inner(Sequence s) {
		//var_c[s.sid][0] = gamma[0] + (double)s.tweets.length / (double) 2;
		//var_c[s.sid][1] = gamma[1] + (double)s.tweets.length / (double) 2;
		//var_csum[s.sid] = var_c[s.sid][0] + var_c[s.sid][1];
		
		//var_e[s.sid][0] = nu[0] + (double)s.tweets.length / (double) 2;
		//var_e[s.sid][1] = nu[1] + (double)s.tweets.length / (double) 2;
		//var_esum[s.sid] = var_e[s.sid][0] + var_e[s.sid][1];

		digamma_c[0] = Gamma.digamma(var_c[s.sid][0]);
		digamma_c[1] = Gamma.digamma(var_c[s.sid][1]);
		digamma_csum = Gamma.digamma(var_csum[s.sid]);
		
		//digamma_e[0] = Gamma.digamma(var_e[s.sid][0]);
		//digamma_e[1] = Gamma.digamma(var_e[s.sid][1]);
		//digamma_esum = Gamma.digamma(var_esum[s.sid]);

		double tmp = (double) 1. / (double) n_region;
		for(int i = 0 ; i < s.tweets.length ; i++) {
			var_sigma[i][0] = gamma[0] / gamma_sum;
			var_sigma[i][1] = gamma[1] / gamma_sum;
			
			//for (int j=0; j<s.tweets[i].len; j++) {
			//	var_omega[i][j][0] = init_nu[0] / nu_sum;
			//	var_omega[i][j][1] = init_nu[1] / nu_sum;
			//}
			
			for (int r=0; r<n_region; r++) {
				var_rho[i][r] = tmp;
			}
		}

		tmp = (double) 1. / (double) n_topic;
		for (int i = 0 ; i < s.tweets.length ; i++) {
			//for (int j=0; j<s.tweets[i].len; j++) {
				for (int k=0; k<n_topic; k++) {
					var_zhi[i][k] = tmp;
				}
			//}
		}
	}
	
	public void initialize_next_vars() {
		// var_a
		for(int r = 0 ; r < n_region ; r++) {
			for(int k = 0 ; k < n_topic ; k++) {
				next_a[r][k] = alpha[k];
			}
		}
		
		// var_b
		for(int k = 0 ; k < n_topic ; k++) {
			for(int w = 0 ; w < n_word ; w++) {
				next_b[k][w] = beta[w];
			}
		}

		// delta
		for(int r = 0 ; r < n_region ; r++) {
			next_delta0[r] = 0;
		}
		for(int r1 = 0 ; r1 < n_region ; r1++) {
			for (int k=0; k<n_topic; k++) {
				for(int r2 = 0 ; r2 < n_region ; r2++) {
					next_delta[r1][k][r2] = 0;
				}
			}
		}
		
		// mu, Sigma
		for(int r = 0 ; r < n_region ; r++) {
			next_mu[r][0] = 0;
			next_mu[r][1] = 0;
			next_Sigma[r][0] = 0;
			next_Sigma[r][1] = 0;
			next_Sigma[r][2] = 0;
			next_denom[r] = 0;
		}
	}


	public void save_lda_model(String prefix) {
		// print alpha
		save_alpha(prefix);

		// print beta
		save_beta(prefix);

		// print gamma
		//save_gamma(prefix);

		// print epsilon
		//save_epsilon(prefix);

		// print transition prob
		save_delta(prefix);

		// print theta
		save_theta(prefix);
		
		// print lambda
		save_lambda(prefix);
		
		// print epsilon
		//save_epsilon(prefix);
		
		// print phi
		save_phi(prefix);
		
		// print location
		save_loc(prefix);
	}

	private void save_loc(String prefix) {
		try {
			PrintStream out = new PrintStream(new FileOutputStream(prefix + ".loc"));
			for(int r = 0 ; r < n_region ; r++) {
				out.println("" + mu[r][0] + "\t" + mu[r][1] + "\t" 
						+ Sigma[r][0] + "\t" + Sigma[r][1] + "\t" + Sigma[r][2]);
			}
			out.close();
		}
		catch(IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void save_delta(String prefix) {
		try {
			PrintStream out = new PrintStream(new FileOutputStream(prefix + ".delta"));
			out.print("" + delta0[0]);
			for(int r = 1 ; r < n_region ; r++) {
				out.print("\t" + delta0[r]);
			}
			out.println();
			for(int r1 = 0 ; r1 < n_region  ; r1++) {
				for(int k = 0 ; k < n_topic ; k++) {
					out.print("" + delta[r1][k][0]);
					for(int r2 = 1 ; r2 < n_region ; r2++) {
						out.print("\t" + delta[r1][k][r2]);
					}
					out.println();
				}
			}
			out.close();
		}
		catch(IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void save_theta(String prefix) {
		try {
			PrintStream out = new PrintStream(new FileOutputStream(prefix + ".theta"));
			double sum = 0;

			out.print("0");
			for(int k = 1 ; k < n_topic ; k++) {
				out.print("\t0");
			}
			out.println();

			for(int r = 0 ; r < n_region ; r++) {
				sum = 0;
				for(int k = 0 ; k < n_topic ; k++) {
					sum += var_a[r][k];
				}

				out.print("" + (var_a[r][0] / sum));
				for(int k = 1 ; k < n_topic ; k++) {
					out.print("\t" + (var_a[r][k] / sum));
				}
				out.println();
			}
			out.close();
		}
		catch(IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public void save_phi(String prefix) {
		try {
			PrintStream out = new PrintStream(new FileOutputStream(prefix + ".phi"));
			for(int k = 0 ; k < n_topic ; k++) {
				double sum = 0;
				for(int w = 0 ; w < n_word ; w++) {
					sum += var_b[k][w];
				}

				out.print("" + (var_b[k][0] / sum));
				for(int w = 1 ; w < n_word ; w++) {
					out.print("\t" + (var_b[k][w] / sum));
				}
				out.println();
			}
			out.close();
		}
		catch(IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void save_lambda(String prefix) {
		try {
			PrintStream out = new PrintStream(new FileOutputStream(prefix + ".lambda"));
			for(int t = 0 ; t < n_seq ; t++) {
				double sum = var_c[t][0] + var_c[t][1];
				out.println("" + (var_c[t][0] / sum) + "\t" + (var_c[t][1] / sum));
			}
			out.close();
		}
		catch(IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	/*
	public void save_epsilon(String prefix) {
		try {
			PrintStream out = new PrintStream(new FileOutputStream(prefix + ".epsilon"));
			for(int t = 0 ; t < n_seq ; t++) {
				double sum = var_e[t][0] + var_e[t][1];
				out.println("" + (var_e[t][0] / sum) + "\t" + (var_e[t][1] / sum));
			}
			out.close();
		}
		catch(IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	*/

	public void save_alpha(String prefix) {
		try {
			PrintStream out = new PrintStream(new FileOutputStream(prefix + ".alpha"));
			for(int i = 0 ; i < n_topic ; i++) {
				out.println(alpha[i]);
			}
			out.close();
		}
		catch(IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void save_beta(String prefix) {
		try {
			PrintStream out = new PrintStream(new FileOutputStream(prefix + ".beta"));
			for(int i = 0 ; i < n_word ; i++) {
				out.println(beta[i]);
			}
			out.close();
		}
		catch(IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public void save_gamma(String prefix) {
		try {
			PrintStream out = new PrintStream(new FileOutputStream(prefix + ".gamma"));
			out.println(gamma[0]);
			out.println(gamma[1]);
			out.close();
		}
		catch(IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	// background word distribution
	public void compute_phi0() {
		for (int w=0; w<n_word; w++) {
			log_phi0[w] = 0;
		}
		for (int i=0; i<n_seq; i++) {
			for (int j=0; j<seqs[i].tweets.length; j++) {
				for (int k=0; k<seqs[i].tweets[j].len; k++) {
					log_phi0[seqs[i].tweets[j].words[k]] += 1.;
				}
			}
		}
		double sum = 0;
		for (int w=0; w<n_word; w++) {
			sum += log_phi0[w];
		}
		for (int w=0; w<n_word; w++) {
			log_phi0[w] = Math.log(log_phi0[w] / sum);
		}
		
		// compute sum of log(phi0) for every tweet
		for (int i=0; i<n_seq; i++) {
			for (int j=0; j<seqs[i].tweets.length; j++) {
				sum = 0;
				for (int k=0; k<seqs[i].tweets[j].len; k++) {
					sum += log_phi0[seqs[i].tweets[j].words[k]];
				}
				
				seqs[i].tweets[j].tmp = sum;
			}
		}
	}
}
