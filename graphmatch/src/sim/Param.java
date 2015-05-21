package sim;

import net.sourceforge.argparse4j.inf.Namespace;

public class Param{
	public static final int SIM_PROPOSED	= 1;
	public static final int SIM_SIMRANK 	= 2;
	
	public static final int QUERY_EMC			= 1;
	public static final int QUERY_RANDOM		= 2;
	public static final int QUERY_NEIGHBORPAIR	= 3;
	
	public static final String[] sim_str = {"","PROPOSED","SIMRANK"};
	public static final String[] query_str = {"","EMC","RANDOM", "NEIGHBORPAIR"};
	
	public int sim = 0;
	public int query 	= 0;
	
	public boolean runtoy 		= false;
	public boolean perfect_ann 	= true;
	public double err_ann		= 0.0;	
	
	
	public Namespace nes;
	
	
	public Param(Namespace nes){
		for(int i = 1; i<sim_str.length; i++){
			if(nes.getString("sim").toUpperCase().equals(sim_str[i])){
				sim = i;
				break;
			}
		}
		for(int i = 1; i<query_str.length; i++){
			if(nes.getString("query").toUpperCase().equals(query_str[i])){
				query = i;
				break;
			}
		}
		this.nes = nes;
		
		runtoy = nes.getBoolean("runtoy");
		perfect_ann = nes.getBoolean("perfect_ann");
		err_ann	= nes.getDouble("err_ann");
		
	}
	
	public String getSimString(){
		return sim_str[sim];
	}
	public String getQueryString(){
		return query_str[query];
	}
	
	public String logFilePath(String dataname, double rm_ratio, int nquery){
		return logFilePath(dataname, sim, query, rm_ratio, nquery);
	}
	public static String logFilePath(String dataname, int sim, int query, double rm_ratio, int nquery){
		return String.format("log/%s/%s_%s_rmr%s_nq%d.log",
				dataname,sim_str[sim],query_str[query],double2String_filename(rm_ratio),nquery);
	}

	public static String double2String_filename(double d){
		return Double.toString(d).replace(".", "_");
	}
}


