package test;

import java.io.IOException;
import java.util.Arrays;

import sim.Simulation;
import sim.Simulation0;
import sim.Simulation1;
import sim.Simulation1_2;
import sim.Simulation2;
import sim.Simulation2_2;
import sim.Simulation2_3;

public class TestMain {
	
	public static void main(String[] args) throws IOException{
		String[] algonames = {"simul0","simul1","simul1_2","simul2", "simul2_2","simul2_3"};
		//String[] algonames = {"simul1_2"};
		//String[] r_list ={"1","2"};
		String[] r_list ={"2"};
		String[] rmr_list = {"0.05","0.1","0.2"};
		for(String algorithm:algonames){
			args[0] = algorithm;
			for(String r:r_list){
				args[2] = r;
				for(String rmr:rmr_list){
					args[4] = rmr;
					Simulation simulation = setSimulation(args);
					simulation.init();
					simulation.initDB();
					simulation.run();
					simulation.finalize();
				}
			}
		}
	}
	static Simulation setSimulation(String[] args_org){
		String algorithm = args_org[0];
		String[] args = Arrays.copyOfRange(args_org, 1, args_org.length);
		Simulation simulation = null;
		switch (algorithm){
			case "simul0":
				simulation = new Simulation0(args);
				break;
			case "simul1":
				simulation = new Simulation1(args);
				break;
			case "simul1_2":
				simulation = new Simulation1_2(args);
				break;
			case "simul2":
				simulation = new Simulation2(args);
				break;
			case "simul2_2":
				simulation = new Simulation2_2(args);
				break;
			case "simul2_3":
				simulation = new Simulation2_3(args);
				break;
		}
		return simulation;
	}
}
