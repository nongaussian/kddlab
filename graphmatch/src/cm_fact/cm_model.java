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
	
	// data
	private cm_data dat 						= null;

	// model parameters
	public w[] w 								= null;
	public w[] w_next							= null;
	public double[] alpha						= null;
	public double[][][] delta					= null;
	public double[] c							= null;
	public double[] c_next						= null;
	
	public int maxrnodesize						= 0;
	public int maxlneighborsize					= 0;
	public int maxrneighborsize					= 0;

	public cm_model(cm_data dat) {
		this.dat = dat;
		
		c = new double[dat.ntype];
		c_next = new double[dat.ntype];
		
		w = new w[dat.ntype];
		for (int t=0; t<dat.ntype; t++) {
			System.out.println(t + "\t" + dat.lnodes[t].size + "\t" + dat.rnodes[t].size);
			w[t] = new w(dat.lnodes[t].size, dat.rnodes[t].size);
		}
		
		w_next = new w[dat.ntype];
		for (int t=0; t<dat.ntype; t++) {
			w_next[t] = new w(dat.lnodes[t].size, dat.rnodes[t].size);
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
	public void random_initialize_var() {
		// init w
		Random rand = new Random(System.currentTimeMillis());

		/*
		for (int t=0; t<dat.ntype; t++) {
			for (int i=0; i<dat.lnodes[t].size; i++) {
				double tmp = 0;
				
				for (int j=0; j<dat.rnodes[t].size; j++) {
					w_next[t].val[i][j] = rand.nextDouble();
					tmp += w_next[t].val[i][j];
				}
				for (int j=0; j<dat.rnodes[t].size; j++) {
					w_next[t].val[i][j] /= tmp;
				}
			}
		}
		
		// init c
		for (int t=0; t<dat.ntype; t++) {
			c_next[t] = 1. / (double)dat.ntype;
		}
		*/
		for (int t=0; t<dat.ntype; t++) {
			for (int i=0; i<dat.lnodes[t].size; i++) {
				double tmp = 1. / (double)dat.rnodes[t].size;
				
				for (int j=0; j<dat.rnodes[t].size; j++) {
					w_next[t].val[i][j] = tmp;
				}
			}
		}
		
		// init c
		for (int t=0; t<dat.ntype; t++) {
			c_next[t] = 1. / (double)dat.ntype;
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
			PrintStream out = new PrintStream(new FileOutputStream(prefix + ".w"));
			for (int t=0; t<dat.ntype; t++) {
				for(int i = 0 ; i < dat.lnodes[t].size ; i++) {
					for(int j = 0 ; j < dat.rnodes[t].size ; j++) {
						out.println ("" + t + "\t" + i + "\t" + j + "\t" + w[t].val[i][j]);
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
				out.print("\t" + c[t]);
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