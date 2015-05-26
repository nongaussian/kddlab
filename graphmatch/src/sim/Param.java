package sim;

import net.sourceforge.argparse4j.inf.Namespace;

public class Param{
	public static final int QUERY_EMC			= 1;
	public static final int QUERY_RANDOM		= 2;
	public static final int QUERY_MAXENTROPY	= 3;
	public static final int QUERY_MINVAR		= 4;

	
	
	public static final int QUERY_NEIGHBORPAIR	= 5;
	public static final int QUERY_NEIGHBORPAIR_OVERLAP	= 6;

	public static final int SIM_PROPOSED	= 1;
	public static final int SIM_SIMRANK 	= 2;
	
	
	public static final String[] query_str = {"","EMC","RANDOM", "MAXENTROPY","MINVAR"};
	public static final String[] sim_str = {"","PROPOSED","SIMRANK"};
	
	public static final boolean[][] compatibility = {
			{false,false,false},
			{false,true,false},	//EMC
			{false,true,true},	//RANDOM
			{false,true,false},	//MAXENTROPY
			{false,false,true}	//MINVAR
			
	};
	
	public int 		sim = 0;
	public int 		query 	= 0;
	
	public boolean runtoy 		= false;
	public boolean perfect_ann 	= true;
	public double  err_ann		= 0.0;	
	public double mu			=0.0;
	
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
		mu = nes.getDouble("mu");
	}
	
	public String getSimString(){
		return sim_str[sim];
	}
	public String getQueryString(){
		return query_str[query];
	}
	
	public String logFilePath(String dataname, double rm_ratio, int nquery,int qmid){
		return logFilePath(dataname, sim, query, rm_ratio, nquery,perfect_ann,err_ann,mu,qmid);
	}
	
	public String optAnn(){
		return optAnn(perfect_ann,err_ann);
	}
	public static String optAnn(boolean perfect_ann, double err_ann){
		String annopt = "";
		if(!perfect_ann){
			annopt = "_ann";
			annopt +=double2String_filename(err_ann);
		}
		return annopt;
	}
	public static String logFilePath(String dataname, int sim, int query, double rm_ratio, int nquery, boolean perfectann, double ann_err,double mu,int qmid){
		String annopt =optAnn(perfectann, ann_err);
		
		return String.format("log/%s/%s_%s_rmr%s_nq%d%s_mu%s.%d.log",
				dataname,sim_str[sim],query_str[query],double2String_filename(rm_ratio),nquery,annopt,double2String_filename(mu),qmid);
	}

	public static String double2String_filename(double d){
		return Double.toString(d).replace(".", "_");
	}
	
	public static boolean compatible(int query, int sim){
		return compatibility[query][sim];
	}
	
	public static boolean compatible(String qry, String sim){
		int q=0;
		for(q = 0; q<query_str.length; q++ ){
			if(query_str[q].equals(qry)){
				break;
			}
		}
		int s=0;
		for(s = 0; s<sim_str.length; s++ ){
			if(sim_str[s].equals(sim)){
				break;
			}
		}
		return compatible(q, s);
	}
}


