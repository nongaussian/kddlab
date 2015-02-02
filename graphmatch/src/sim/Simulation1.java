package sim;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

/*
 * Simply, we simulate a crowdsourcing process with
 * two exactly the same heterogeneous graphs
 */
public class Simulation1 {
	double max_iter					= 100;
	
	public Simulation1 (Namespace nes) {
		// data parameters
		String lprefix				= nes.getString("lprefix");
		String rprefix				= nes.getString("rprefix");
		double max_iter				= nes.getInt("niter");
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Namespace nes = parseArguments(args);
		Simulation1 sim = new Simulation1 (nes);
		sim.run ();
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

			Namespace res = parser.parseArgs(args);
			System.out.println(res);
			return res;
		} catch (ArgumentParserException e) {
			parser.handleError(e);
		}
		return null;
	}

	public void run () {
		int iter = 0;
		while (iter < max_iter) {
			iter++;
			
			// select a query & candidates
			
			// compute the cost
			
			// update w
		}
	}
}
