package cm_fact;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Random;

/*
 * manage model parameters
 * - variables
 * - functions to save to & load from files 
 */
public class cm_model {
	// constant
	public double mu							= 1.;
	public int nfact							= 10;
	
	// data
	private cm_data dat 						= null;

	// model parameters
	public w[] w 								= null;
	public w[] w_next							= null;
	public double[] pi							= null;
	public double[] alpha						= null;
	public double[][][] delta					= null;
	public double[] c							= null;
	public double[] c_next						= null;
	
	public int maxrnodesize						= 0;
	public int maxlneighborsize					= 0;
	public int maxrneighborsize					= 0;

	public cm_model(cm_data dat, double mu, int nfact) {
		this.dat = dat;
		this.mu = mu;
		this.nfact = nfact;
		
		c = new double[dat.ntype];
		c_next = new double[dat.ntype];
		pi = new double[nfact];
		
		w = new w[dat.ntype];
		for (int t=0; t<dat.ntype; t++) {
			System.out.println(t + "\t" + dat.lnodes[t].size + "\t" + dat.rnodes[t].size);
			w[t] = new w(nfact, dat.lnodes[t].size, dat.rnodes[t].size);
		}
		
		w_next = new w[dat.ntype];
		for (int t=0; t<dat.ntype; t++) {
			w_next[t] = new w(nfact, dat.lnodes[t].size, dat.rnodes[t].size);
		}
		
		maxrnodesize = 0;
		for (int t=0; t<dat.ntype; t++) {
			if (maxrnodesize < dat.rnodes[t].size) {
				maxrnodesize = dat.rnodes[t].size;
			}
		}
		
		alpha = new double[maxrnodesize];
		
		maxlneighborsize = 0;
		for (int t=0; t<dat.ntype; t++) {
			for (int i=0; i<dat.lnodes[t].size; i++) {
				for (int s=0; s<dat.ntype; s++) {
					if (!dat.rel[t][s]) continue;
					if (maxlneighborsize < dat.lnodes[t].arr[i].neighbors[s].size) {
						maxlneighborsize = dat.lnodes[t].arr[i].neighbors[s].size;
					}
				}
			}
		}
		
		maxrneighborsize = 0;
		for (int t=0; t<dat.ntype; t++) {
			for (int i=0; i<dat.rnodes[t].size; i++) {
				for (int s=0; s<dat.ntype; s++) {
					if (!dat.rel[t][s]) continue;
					if (maxrneighborsize < dat.rnodes[t].arr[i].neighbors[s].size) {
						maxrneighborsize = dat.rnodes[t].arr[i].neighbors[s].size;
					}
				}
			}
		}
		
		delta = new double[dat.ntype][maxlneighborsize][maxrneighborsize];
	}

	/*
	 * setting variational variables
	 */
	public void update_var () {
		// init c
		for (int t=0; t<dat.ntype; t++) {
			c[t] = c_next[t];
			c_next[t] = 0;
		}

		// init w
		for (int t=0; t<dat.ntype; t++) {
			for (int i=0; i<dat.lnodes[t].size; i++) {
				for (int k=0; k<nfact; k++) {
					w[t].f1[i][k] = w_next[t].f1[i][k];
					w_next[t].f1[i][k] = 0;
				}
			}
			for (int k=0; k<nfact; k++) {
				for (int j=0; j<dat.rnodes[t].size; j++) {
					w[t].f2[k][j] = w_next[t].f2[k][j];
					w_next[t].f2[k][j] = 0;
				}
			}
		}
	}
	
	public void initialize_var() {
		// init w
		Random rand = new Random(System.currentTimeMillis());

		for (int t=0; t<dat.ntype; t++) {
			for (int i=0; i<dat.lnodes[t].size; i++) {
				double tmp = 0;
				
				for (int k=0; k<nfact; k++) {
					w_next[t].f1[i][k] = rand.nextDouble();
					tmp += w_next[t].f1[i][k];
				}
				for (int k=0; k<nfact; k++) {
					w_next[t].f1[i][k] /= tmp;
				}
			}
			
			for (int k=0; k<nfact; k++) {
				double tmp = 0;
				
				for (int j=0; j<dat.rnodes[t].size; j++) {
					w_next[t].f2[k][j] = rand.nextDouble();
					tmp += w_next[t].f2[k][j];
				}
				for (int j=0; j<dat.rnodes[t].size; j++) {
					w_next[t].f2[k][j] /= tmp;
				}
			}
		}

		
		// init c
		for (int t=0; t<dat.ntype; t++) {
			c_next[t] = 1. / (double)dat.ntype;
		}
	}
	
	public void normalize_var () {
		// normalize w
		for (int t=0; t<dat.ntype; t++) {
			for (int i=0; i<dat.lnodes[t].size; i++) {
				double z = 0;
				for (int k=0; k<nfact; k++) {
					z += w_next[t].f1[i][k];
				}
				for (int k=0; k<nfact; k++) {
					w_next[t].f1[i][k] /= z;
				}
			}
			
			for (int k=0; k<nfact; k++) {
				double z = 0;
				for (int j=0; j<dat.rnodes[t].size; j++) {
					z += w_next[t].f2[k][j];
				}
				for (int j=0; j<dat.rnodes[t].size; j++) {
					w_next[t].f2[k][j] /= z;
				}
			}
		}
		
		// normalize c
		double z = 0;
		for (int t=0; t<dat.ntype; t++) {
			z += c_next[t];
		}
		for (int t=0; t<dat.ntype; t++) {
			c_next[t] /= z;
		}
	}
	
	public void save_model(String prefix) {
		// print w
		save_w(prefix);

		// print c
		save_c(prefix);
	}

	private void save_w(String prefix) {
		try {
			PrintStream out = new PrintStream(new FileOutputStream(prefix + ".w1"));
			for (int t=0; t<dat.ntype; t++) {
				for(int i = 0 ; i < dat.lnodes[t].size ; i++) {
					for(int k = 0 ; k < nfact ; k++) {
						out.println ("" + t + "\t" + i + "\t" + k + "\t" + w_next[t].f1[i][k]);
					}
				}
			}
			out.close();
			
			out = new PrintStream(new FileOutputStream(prefix + ".w2"));
			for (int t=0; t<dat.ntype; t++) {
				for(int k = 0 ; k < nfact ; k++) {
					for(int j = 0 ; j < dat.rnodes[t].size ; j++) {
						out.println ("" + t + "\t" + k + "\t" + j + "\t" + w_next[t].f1[k][j]);
					}
				}
			}
			out.close();
		}
		catch(IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void save_c(String prefix) {
		try {
			PrintStream out = new PrintStream(new FileOutputStream(prefix + ".c"));
			out.print("" + c[0]);
			for(int t = 1 ; t < dat.ntype ; t++) {
				out.print("\t" + c_next[t]);
			}
			out.println();
			out.close();
		}
		catch(IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}
