package sim;

import gm.data.DataGenerator;
import gm.data.SqlUnit;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

public class Test {
	//static String[] mu_list = {"0.1","1.0","10.0"};
	static String[] mu_list = {"0.1", "1.0","10.0","100.0"};
	public static String default_dbname = "graphmatching3";
	
	public static void main(String[] args) throws IOException{
		if(args.length==1){
			if(!args[0].equals("default")){
				default_dbname = args[0];
			}

		}else{
			System.out.println("enter dbname");
			System.exit(0);
		}
		System.out.println("dbname: "+default_dbname);


		/*/
		try {
			runSynth(500, 0.01, 5);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//*/
		//runSynth(500, 0.03, 5, 0.03);
		run("tiny", -1,-1);
	
		
		
	}
	

	public static void runSynth(int n_node,double density, int nq) throws IOException, SQLException{
		double[] mu_list = {0.1,1,10,100};
		String[] sim_list = Arrays.copyOfRange(Param.sim_str, 1, Param.sim_str.length);
		String[] query_list = Arrays.copyOfRange(Param.query_str, 1, Param.query_str.length);
		double[] rmr_list = {0.05};
		//double[] rmr_list = {0.0, 0.02, 0.05};
		double[] err_list = {0,0.01,0.03,0.05, 0.1, 0.2};
		int radius = 2;
		SqlUnit sqlunit = new SqlUnit(default_dbname);
		
		String[] basic_args = {
				"-maxacc", "0.9",
				"-data", DataGenerator.getfilename(n_node, density),
				"-nq", Integer.toString(nq),	
				"-thres", "1.0e-5",
				"-perfect_ann","false",
				"-radius",Integer.toString(radius),
				"-runtoy","true",
				};
		int i = 0;
		for(double rmr:rmr_list){
			String[] cmd_rmr = {"-rmr",Double.toString(rmr)};
			String[] cmd0 = ArrayUtils.addAll(basic_args, cmd_rmr);
			for(double err: err_list){
				for(String sim_str: sim_list){
					String[] cmd_sim = {"-sim", sim_str};
					String[] cmd1 = ArrayUtils.addAll(cmd0, cmd_sim);
					for(String query_str: query_list){
						if(!Param.compatible(query_str, sim_str)){
							continue;
						}
						for(int mu_idx = 0; mu_idx < mu_list.length; mu_idx++){
							if(mu_idx==1||
									((query_str.equals(Param.query_str[Param.QUERY_EMC])||query_str.equals(Param.query_str[Param.QUERY_MAXENTROPY])) && sim_str.equals(Param.sim_str[Param.SIM_PROPOSED]))
							){
									
							}else{
								continue;
							}
							String mu_str = "";
							if(sim_str.equals(Param.sim_str[Param.SIM_PROPOSED])){
								mu_str = String.format("and mu > %f and mu <%f ", mu_list[mu_idx]-0.0001, mu_list[mu_idx]+0.0001);
							}
							String[] cmd_qry = {
									"-query",query_str,
									"-mu",Double.toString(mu_list[mu_idx]),
									"-err_ann", Double.toString(err)
							};
							
							String[] cmd = ArrayUtils.addAll(cmd1, cmd_qry);
							
							String qry = String.format("" +
									"select count(*) from experiment " +
									"where data = '%s' " +
									"and query = '%s' and sim = '%s' " +
									"and radius = %d " +
									"and rmr > %f and rmr < %f " +
									"%s " +
									"%s;",
									DataGenerator.getfilename(n_node, density),
									query_str, sim_str, 
									radius,
									rmr-0.0001, rmr+0.0001,
									String.format("and err > %f and err < %f",err-0.0001,err+0.0001),
									mu_str);
							ResultSet rs = sqlunit.executeQuery(qry);
							System.out.println((++i)+"  "+StringUtils.join(cmd," "));
							if(rs.first()){
								if(rs.getInt(1)>0){
									System.out.println("Pass");
									continue;
								}
							}
							
							
							
							//*
							GraphMatching gm = new GraphMatching(cmd);
							gm.init();
							gm.initDB(default_dbname);
							gm.run();
							gm.close();	
							//*/
						}
					}
					
				}
			}
		}
		sqlunit.exit();
	}
	public static void runSynth(int n_node, double density,int nq,double err) throws IOException{
		String[] basic_args = {
				"-maxacc", "0.9",
				"-data", DataGenerator.getfilename(n_node, density),
				"-nq", Integer.toString(nq),	
				"-thres", "1.0e-5",
				"-perfect_ann","false",
				"-radius","2",
				"-runtoy","true",
				"-err_ann",Double.toString(err)
				};
		String[] sim_list = Arrays.copyOfRange(Param.sim_str, 1, Param.sim_str.length);
		String[] query_list = Arrays.copyOfRange(Param.query_str, 1, Param.query_str.length);
		
		
		String[] rmr_list = {"0.05"};
		

		int i = 0;
		for(String rmr:rmr_list){
			String[] cmd_rmr = {"-rmr",rmr};
			String[] cmd0 = ArrayUtils.addAll(basic_args, cmd_rmr);
			
			for(String sim_str: sim_list){
				String[] cmd_sim = {"-sim", sim_str};
				String[] cmd1 = ArrayUtils.addAll(cmd0, cmd_sim);
				for(String query_str: query_list){
					if(!Param.compatible(query_str, sim_str)){
						continue;
					}
					for(int mu_idx = 0; mu_idx < mu_list.length; mu_idx++){
						if(mu_idx==1||
								((query_str.equals(Param.query_str[Param.QUERY_EMC])||query_str.equals(Param.query_str[Param.QUERY_MAXENTROPY])) && sim_str.equals(Param.sim_str[Param.SIM_PROPOSED]))
							){
								
							}else{
								continue;
							}
						String[] cmd_qry = {
								"-query",query_str,
								"-mu",mu_list[mu_idx]
						
						};
						
						String[] cmd = ArrayUtils.addAll(cmd1, cmd_qry);
						
						
						System.out.println((++i)+"  "+StringUtils.join(cmd," "));
						//*
						GraphMatching gm = new GraphMatching(cmd);
						gm.init();
						gm.initDB(default_dbname);
						gm.run();
						gm.close();	
						//*/
					}
				}
			}
		}
	}
	

	
	
	public static void run(String dataname,int sim,int query) throws IOException{
		String[] sim_list = null;
		if(sim>0){
			sim_list = new String[1];
			sim_list[0] = Param.sim_str[sim];
		}else{
			sim_list = Arrays.copyOfRange(Param.sim_str, 1, Param.sim_str.length);
		}
		String[] query_list = null;
		if(query>0){
			query_list = new String[1];
			query_list[0] = Param.query_str[query];
		}else{
			query_list = Arrays.copyOfRange(Param.query_str, 1, Param.query_str.length);
		}
		try {
			run(dataname, sim_list, query_list);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public static void run(String dataname,String[] sim_list,String[] query_list) throws IOException, SQLException{
		String[] basic_args = {
				"-maxacc", "0.7",
				//"-maxacc", "0.2",
				"-nq", "100",
				"-data", dataname,
				//"-nmaxq","10000",
				"-perfect_ann","false",
				"-thres", "1.0e-5"};
				//"-thres", "1.0e-2"};

		String[] r_list ={"2"};
		String[] rmr_list = {"0.02","0.0","0.05"};

		//String[] ann_list = {"0.03","0.01"};
		//String[] ann_list = {"0.05","0.00"};
		String[] ann_list = {"0.0","0.01","0.03","0.05","0.1"};//,"0.2"};
		SqlUnit sqlunit = new SqlUnit(default_dbname);
		int i = 0;
		for(String rmr:rmr_list){
			String[] cmd_rmr = {"-rmr",rmr};
			String[] cmd0 = ArrayUtils.addAll(basic_args, cmd_rmr);
			for(String ann:ann_list){
				String[] cmd_ann = {"-err_ann",ann};
				String[] cmd0_2 = ArrayUtils.addAll(cmd0,cmd_ann);
				for(String sim_str: sim_list){
					String[] cmd_sim = {"-sim", sim_str};
					String[] cmd1 = ArrayUtils.addAll(cmd0_2, cmd_sim);
					for(String query_str: query_list){
						if(!Param.compatible(query_str, sim_str)){
							continue;
						}
						
						String[] cmd_qry = {"-query",query_str};
						String[] cmd2 = ArrayUtils.addAll(cmd1, cmd_qry);
						for(String r:r_list){
							String[] cmd_r = {"-radius",r};
							String[] cmd3 = ArrayUtils.addAll(cmd2, cmd_r);
						
							for(int mu_idx = 0; mu_idx < mu_list.length; mu_idx++){
								if(mu_idx==2||sim_str.equals(Param.sim_str[Param.SIM_PROPOSED])	){
									
								}else{
									continue;
								}
								
								String[] cmd_mu = new String[2];
								cmd_mu[0] = "-mu";
								cmd_mu[1] = mu_list[mu_idx];
																
								
								
								String[] cmd = ArrayUtils.addAll(cmd3, cmd_mu);
								
								
								String mu_str = "";
								if(sim_str.equals(Param.sim_str[Param.SIM_PROPOSED])){
									mu_str = String.format("and mu > %f and mu <%f ",
											Double.parseDouble(mu_list[mu_idx])-0.0001, Double.parseDouble(mu_list[mu_idx])+0.0001);
								}
								String qry = String.format("" +
										"select count(*) from experiment " +
										"where data = '%s' " +
										"and query = '%s' and sim = '%s' " +
										"and radius = %s " +
										"and rmr > %f and rmr < %f " +
										"%s " +
										"%s;",
										dataname,
										query_str, sim_str, 
										r,
										Double.parseDouble(rmr)-0.0001, Double.parseDouble(rmr)+0.0001,
										String.format("and err > %f and err < %f",Double.parseDouble(ann)-0.0001,Double.parseDouble(ann)+0.0001),
										mu_str);
								ResultSet rs = sqlunit.executeQuery(qry);
								System.out.println((++i)+"  "+StringUtils.join(cmd," "));
								if(rs.first()){
									if(rs.getInt(1)>0){
										System.out.println("Pass");
										continue;
									}
								}
								
								
								
								/*/
								GraphMatching gm = new GraphMatching(cmd);
								gm.init();
								gm.initDB(default_dbname);
								gm.run();
								gm.close();
								//*/
							}
							
						}
					}
				}
			}
		}
		sqlunit.exit();
	}
}
