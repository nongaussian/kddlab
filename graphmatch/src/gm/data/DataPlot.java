package gm.data;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import sim.Param;
import cm.QueryNode;

import com.panayotis.gnuplot.JavaPlot;
import com.panayotis.gnuplot.plot.AbstractPlot;
import com.panayotis.gnuplot.plot.DataSetPlot;
import com.panayotis.gnuplot.style.PlotStyle;
import com.panayotis.gnuplot.style.Style;
import com.panayotis.gnuplot.terminal.PostscriptTerminal;

public class DataPlot {
	static final String PLOT_DIR_STRING = "plots";
	static final String SCRIPT_DIR_STRING = "gnuplot_script";
	
	static double[] mu_list = {0.1,1.0,10.0};
	static double default_mu = 1.0;
	
	static String default_font = "'Arial, 24'";
	
	
	String dataname;
	String plot_dir;
	String script_dir;
	SqlUnit sqlunit;
	
	
	
	public static HashMap<String, String> abb = new HashMap<String, String>();
	public static void main(String[] args){
		initAbb();
		plotAll("tiny");
		//plotSynthetic(500);
		
	}
	
	public DataPlot(String dataname){
		File dir 	= new File(PLOT_DIR_STRING);
		dir.mkdir();
		plot_dir = PLOT_DIR_STRING+"/"+dataname;
		File subdir = new File(plot_dir);
		subdir.mkdir();
		
		dir 		= new File(SCRIPT_DIR_STRING);
		dir.mkdir();
		script_dir=SCRIPT_DIR_STRING+"/"+dataname;
		subdir = new File(script_dir);
		subdir.mkdir();
		
		
		this.dataname = dataname;
		sqlunit = new  SqlUnit("graphmatching");
	
	}
	
	
	static void plotSynthetic(int n_node){
		DataPlot dp0_01 = new DataPlot("Syn"+n_node+"_0_01");
		dp0_01.plotCostAccuracy(10.0);
		
		DataPlot dp0_05 = new DataPlot("Syn"+n_node+"_0_05");
		DataPlot dp0_1 = new DataPlot("Syn"+n_node+"_0_1");
		DataPlot dp0_2 = new DataPlot("Syn"+n_node+"_0_2");
		
		dp0_05.plotCostAccuracy(10.0);
		dp0_1.plotCostAccuracy(10.0);
		dp0_2.plotCostAccuracy(10.0);
		
	}
	
	static void plotAll(String dataname){
		DataPlot dp = new DataPlot(dataname);
		
		for(double mu: mu_list){
			dp.plotCostAccuracy(mu);
		}
		
		dp.plotAnnMuAccuracy();
		
		for(double mu: mu_list){
			dp.plotEarlyMatching(-1, -1,mu);
		}
		
		dp.plotErrCost(-1, -1, 0.7);
		dp.plotErrCost(-1, -1, 0.5);
		dp.plotErrCost(-1, -1, 0.3);
	
	}
	
	static void initAbb(){
		abb.put("PROPOSED", "EM");
		abb.put("NEIGHBORPAIR", "NP");
		abb.put("NEIGHBORPAIR_OVERLAP", "NPO");
		abb.put("SIMRANK", "SR");
		abb.put("RANDOM", "RAND");
		abb.put("MAXENTROPY", "MAXENT");
		

		
	}
	static String getAbb(String key){
		String out = abb.get(key);
		if(out!=null){
			return out;
		}
		return key;
	}
	
	
	
	
	/*
	private double[][][] getCostAccuracy_WO_outlier(int query, int sim, double rmr,double mu, boolean perfect_ann, double err_ann, int th_outlier,int id){
		double[][][] costacc = getCostAccuracy(
				Param.query_str[query], Param.sim_str[sim], rmr, mu, perfect_ann, err_ann);
	
		//TODO 
		double[] costs = getCostSums(query, sim, rmr, mu, perfect_ann, err_ann,th_outlier,id);
		if(costs==null){
			return null;
		}
		for(int i = 0; i < costs.length; i++){
			costacc[i+1][1] = costs[i]; 
		}
		return costacc;
	}
	
	private double[] getCostSums(int query, int sim, double rmr, double mu,
			boolean perfect_ann, double err_ann, int th_outlier,int id) {
		String filename = Param.logFilePath(dataname, sim, query, rmr, 100,perfect_ann,err_ann,mu,id);
		String line = null;

		ArrayList<Integer> cost_sum_list = new ArrayList<Integer>();
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			boolean v_continue = true;
			while(v_continue){
				line=br.readLine();
				if(line==null||line.length()==0){
					v_continue = false;
					continue;
				}
				char sc = line.charAt(0);
				switch(sc){
				case 'i':
					continue;
				case 'E':
					v_continue = false;
					break;
				default:
					QueryNode[] queries = getQueryList(line);
					int cost_sum = 0;
					for(QueryNode q:queries){
						if(q.cost <= th_outlier){
							cost_sum+=q.cost;
						}
					}
					cost_sum_list.add(cost_sum);
					break;
				
				}
			}
		}catch(FileNotFoundException fne){
			return null;
		}catch(Exception e){
			e.printStackTrace();
		}
		
		double[] cost_sums = new double[cost_sum_list.size()];
		for(int i = 0; i<cost_sums.length; i++){
			if(i==0){
				cost_sums[i] = cost_sum_list.get(i);
			}else{
				cost_sums[i] = cost_sums[i-1]+cost_sum_list.get(i);
			}
		}
		return cost_sums;
	}
	*/

	

	private double[][][] getCostAccuracy(String query, String sim, double rmr,double mu, boolean perfect_ann, double err_ann){
		String ann_str = String.format("and err > %f and err < %f",err_ann-0.0001,err_ann+0.0001);
		String mu_str = "";
		if(query.equals(Param.query_str[Param.QUERY_EMC])){
			mu_str = String.format("and mu > %f and mu <%f ", mu-0.0001, mu+0.0001);
		}
		
		String qry = String.format(
				"select id " +
				"from experiment " +
				"where data = '%s' and query = '%s' " +
				"and sim = '%s' " +
				"and rmr > %f and rmr < %f " +
				"%s " +
				"%s;",
				dataname, query, sim, rmr-0.0001, rmr+0.0001,ann_str,mu_str);
		
		
		ResultSet rs = sqlunit.executeQuery(qry);
		
		double[][][] out= null;
		try {
			rs.beforeFirst();
			ArrayList<Integer> exp_id_list = new ArrayList<Integer>();
			while(rs.next()){
				exp_id_list.add(rs.getInt("id"));
			}
			
			out = new double[exp_id_list.size()][][];
			for(int i = 0; i < out.length; i++){
				out[i] = getCostAccuracy(exp_id_list.get(i));
			}
			return out;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return out;
		
	}
	//private double[][] getCostAccuracy(String query, String sim, double rmr,double mu, boolean perfect_ann, double err_ann){
	private double[][] getCostAccuracy(int expid){
		double[][] out = null;
		try {
			
			
			String suffix = String.format(
					"from results " +
					"where exp_id =%d;",
					expid);
			
			String select = "select count(*) ";
			ResultSet rs = sqlunit.executeQuery(select+suffix);
			rs.first();
			int count = rs.getInt(1);
			out = new double[count][2];
			if(count==0){
				return out;
			}
			
			select = "select ntrue/(nfalse+ntrue) as accuracy, cost ";

			rs = sqlunit.executeQuery(select+suffix);
			
			rs.beforeFirst();
			for(int i = 0; i < count; i++){
				rs.next();
				out[i][0] = rs.getDouble(1);
				out[i][1] = (double) rs.getInt(2);
			}
			return out;
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(0);
		}
		return out;
		
		
	}
	public double[] getRmrlist(){
		//double[] rmr_list = {0.0};
		//double[] rmr_list = {0.0, 0.05, 0.1, 0.2};
		//return rmr_list;/*
		String qry = String.format("select distinct (rmr) from experiment where data = '%s'" +
				" order by rmr asc;",dataname);
		ResultSet rs = sqlunit.executeQuery(qry);
		
		ArrayList<Double> rmr_list = new ArrayList<Double> ();
		try {
			rs.beforeFirst();
			while(rs.next()){
				rmr_list.add(rs.getDouble(1));
			}
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(0);
		}
		double[] list = new double[rmr_list.size()];
		
		for(int l = 0; l < list.length; l++){
			list[l] = rmr_list.get(l);
		}
		return list;//*/
	}
	
	public void plotCostAccuracy(double mu){
		plotCostAccuracy(-1, -1, mu, -1);
	}
	
	public void plotCostAccuracy(int sim, int query,double mu,int th_outlier){
		boolean except_outlier =  th_outlier>0;
		double[] rmr_list = getRmrlist();
		double[] err_list = getErrList();
		
		for( double rmr:rmr_list){
			for(double err_ann:err_list){
				String opt = "";
				if(sim>0){
					opt = Param.sim_str[sim];
				}else if(query>0){
					opt = Param.query_str[query];
				}
				
				String filename = String.format("CostRecall_%s_rmr%s%s_mu%s%s"
						,opt
						,Double.toString(rmr).replace(".","_")
						,Param.optAnn(false, err_ann)
						,Param.double2String_filename(mu)
						,except_outlier?"eo"+th_outlier:""
						);
				
				String plot_filename = String.format("%s/%s.eps"
						,plot_dir
						,filename);
				PostscriptTerminal eps = new PostscriptTerminal(plot_filename);
				
				
				eps.setEPS(true);
				eps.set("font", default_font);
				eps.set("enhanced");
				eps.setColor(true);
				
				
				JavaPlot p = new JavaPlot();
				PlotStyle pStyle = new PlotStyle();
				pStyle.setStyle(Style.LINESPOINTS);
				pStyle.setLineWidth(3);
				pStyle.setPointSize(3);
				p.newGraph();
				p.getAxis("x").setLabel("Recall");
				p.getAxis("y").setLabel("Cost");
				p.getAxis("y").setLogScale(true);
				p.setTerminal(eps);
				
				p.setPersist(false);
				
				p.set("size ratio", "0.75");
				p.set("lmargin at screen", "0.25");
				p.set("rmargin at screen", "0.9");
				p.set("bmargin at screen", "0.2");
				p.set("tmargin at screen", "0.95");
				
				p.set("key", "right bottom");

				
				boolean b_plot = false;
				for (int s = 1; s < Param.sim_str.length; s++){
					if(sim>0 && sim!=s){
						continue;
					}
					for (int q = 1; q< Param.query_str.length; q++){
						if(query>0 &&query!=q){
							continue;
						}
						
						double[][][] points = null;
						
						if(except_outlier){
							//points = getCostAccuracy_WO_outlier(q, s, rmr, mu, false, err_ann, th_outlier,gmid);
						}else{
							points = getCostAccuracy(Param.query_str[q], Param.sim_str[s], rmr, mu, false, err_ann);
						}
						if(points!=null&&points.length>0){
							for(int i = 0; i < points.length; i++){
								if(points[i]!=null && points.length>0){
									AbstractPlot data = new DataSetPlot(points[i]);
									data.setTitle(getAbb(Param.query_str[q])+"+"+getAbb(Param.sim_str[s])+(points.length>1?i:""));
									data.setPlotStyle(pStyle);
									p.addPlot(data);
									b_plot = true;
								}
							}
						}
					}
				}	
				if(b_plot){
					p.plot();
					saveGnuplotScript(p,filename);
					//System.out.println(p.getCommands());
				}
				
			}
		}
	}
	
	
	
	public void plotAnnMuAccuracy(){
		double[] rmr_list = getRmrlist();
		double[] err_list = getErrList();
		
		
		for( double rmr:rmr_list){
			for(double err_ann:err_list){
				String filename = String.format("CostRecall_varingMu_rmr%s_Ann%s"
						,Double.toString(rmr).replace(".","_")
						,Param.optAnn(false, err_ann));
				String plot_filename = String.format("%s/%s.eps"
						,plot_dir
						,filename);
				
				PostscriptTerminal eps = new PostscriptTerminal(plot_filename);
				
				
				eps.setEPS(true);
				eps.set("font", default_font);
				eps.set("enhanced");
				eps.setColor(true);
				
				
				JavaPlot p = new JavaPlot();
				PlotStyle pStyle = new PlotStyle();
				pStyle.setStyle(Style.LINESPOINTS);
				pStyle.setLineWidth(3);
				pStyle.setPointSize(3);
				p.newGraph();
				p.setTerminal(eps);
				p.set("encoding","utf8");
				
				p.getAxis("x").setLabel("Recall");
				p.getAxis("y").setLabel("Cost");
				
				p.setPersist(false);
				
				p.set("size ratio", "0.75");
				p.set("lmargin at screen", "0.25");
				p.set("rmargin at screen", "0.9");
				p.set("bmargin at screen", "0.2");
				p.set("tmargin at screen", "0.95");
				
				p.getAxis("y").setLogScale(true);
				
				p.set("key", "right bottom");
				
				
				boolean b_plot = false;
				
						
				for(double mu: mu_list){
					double[][][] points = getCostAccuracy(
							Param.query_str[Param.QUERY_EMC], Param.sim_str[Param.SIM_PROPOSED],
							rmr, mu, false, err_ann);
					if(points!=null&&points.length>0){
						for(int i = 0; i < points.length; i++){
							if(points[i]!=null && points.length>0){
								AbstractPlot data = new DataSetPlot(points[i]);
								data.setTitle(
										"{/Symbol m} = "+mu);
								data.setPlotStyle(pStyle);
								p.addPlot(data);
								b_plot = true;
							}
						}
					}
				}
					
				if(b_plot){
					p.plot();
					saveGnuplotScript(p,filename);
				}
			}
		}
	}
	
	public void plotEarlyMatching(int sim, int query, double mu){
		double[] rmr_list = getRmrlist();
		double[] err_list = getErrList();
		
		for( double rmr:rmr_list){
			for(double err_ann:err_list){
				String opt = "";
				if(sim>0){
					opt = Param.sim_str[sim];
				}else if(query>0){
					opt = Param.query_str[query];
				}
				
				String filename = String.format(
						"earlymatching_%s_rmr%s_ann%s_mu%s",
						opt,
						Param.double2String_filename(rmr),
						Param.double2String_filename(err_ann),
						Param.double2String_filename(mu));
				
				String plot_filename = String.format("%s/%s.eps"
						,plot_dir
						,filename);
				PostscriptTerminal eps = new PostscriptTerminal(plot_filename);
				eps.setEPS(true);
				eps.set("font", default_font);
				eps.set("enhanced");
				eps.setColor(true);
				
				
				JavaPlot p = new JavaPlot();
				PlotStyle pStyle = new PlotStyle();
				pStyle.setStyle(Style.LINESPOINTS);
				pStyle.setLineWidth(3);
				pStyle.setPointSize(3);
				p.newGraph();
				p.getAxis("x").setLabel("Round");
				p.getAxis("y").setLabel("EarlyMatchingRatio");
				
				p.setTerminal(eps);
				p.setPersist(false);
				p.set("size ratio", "0.75");
				p.set("lmargin at screen", "0.25");
				p.set("rmargin at screen", "0.9");
				p.set("bmargin at screen", "0.2");
				p.set("tmargin at screen", "0.95");
				
				p.set("key", "right bottom");
								
				boolean b_plot = false;
				for (int s = 1; s < Param.sim_str.length; s++){
					if(sim>0 && sim!=s){
						continue;
					}
					for (int q = 1; q< Param.query_str.length; q++){
						if(query>0 &&query!=q){
							continue;
						}
						if(!Param.compatible(q, s))
							continue;
						int TH_EARLY_MATCHING = 10;
						int gmid = 0;
						double [][] points = getEarlyMatchings(q, s, rmr, 100,TH_EARLY_MATCHING,false,err_ann,
								(s==Param.SIM_PROPOSED?mu:default_mu),gmid);
						if(points !=null && points.length>0){
							AbstractPlot data = new DataSetPlot(points);
							data.setTitle(getAbb(Param.query_str[q])+"+"+getAbb(Param.sim_str[s]));
							data.setPlotStyle(pStyle);
							p.addPlot(data);
							b_plot = true;
						}
					}
				}	
				if(b_plot){
					p.plot();
					saveGnuplotScript(p,filename);
				}

			}
		}
	}



	private double[][] getEarlyMatchings(int query, int sim, double rm_ratio, int nquery, int th_em,boolean perfect_ann, double err_ann,double mu, int gmid) {
		String filename = Param.logFilePath(dataname, sim, query, rm_ratio, nquery,perfect_ann,err_ann, mu,gmid);
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			ArrayList<Double> earlymatching_ratio = new ArrayList<Double>();
			
			String line;
			
			boolean v_continue = true;
			while(v_continue){
				line=br.readLine();
				if(line==null||line.length()==0){
					v_continue = false;
					continue;
				}
				char sc = line.charAt(0);
				switch(sc){
				case 'i':
					continue;
				case 'E':
					v_continue = false;
					break;
				default:
					earlymatching_ratio.add(computeEarlyMatchingRatio(line,th_em));
					break;
				
				}
			}
			if(earlymatching_ratio.size()==0){
				br.close();
				return null;
			}
			double[][] data = new double[earlymatching_ratio.size()][2];
			for(int i = 0; i< data.length; i++){
				data[i][0] = i;
				data[i][1] = earlymatching_ratio.get(i);
			}
			br.close();
			return movingAvg(data, 10);
			
		} catch (IOException e) {
			return null;
		}
	}
	
	public static double[][] movingAvg(double[][] dat,int window_size){
		double[][] out = new double[dat.length][2];
		double window_sum = 0.0;
		for(int i = 0; i<out.length; i++){
			window_sum += dat[i][1];
			if(i>=window_size){
				window_sum -= dat[i-window_size][1];
			}
			out[i][0] = dat[i][0];
			out[i][1] = window_sum/Math.min(window_size, i+1);
		}
		
		return out;
	}
	
	private static QueryNode[] getQueryList(String line){
		ArrayList <QueryNode> qn_list = new ArrayList<QueryNode>();
		String[] queries = line.split("\\|");
		for(String query: queries){
			try{
				String[] query_tmp = query.split(":|,");
				QueryNode qn = new QueryNode(Integer.parseInt(query_tmp[0]), Integer.parseInt(query_tmp[1]));
				qn.diff = Double.parseDouble(query_tmp[2]);
				qn.max_sim = Double.parseDouble(query_tmp[3]);
				qn.matching_sim = Double.parseDouble(query_tmp[4]);
				qn.entropy = Double.parseDouble(query_tmp[5]);
				qn.cost = Integer.parseInt(query_tmp[6]);
				if(query_tmp.length>7){
					qn.n_annotates = Integer.parseInt(query_tmp[7]);
				}
				qn_list.add(qn);
			}catch(NumberFormatException e){
				System.out.println(query);
				e.printStackTrace();
				continue;
			}catch(ArrayIndexOutOfBoundsException e){
				System.out.println(query);
				e.printStackTrace();
				continue;
			}
		}
		QueryNode[] out = qn_list.toArray(new QueryNode[0]);
		return out;
	}
	private static double computeEarlyMatchingRatio(String line,int th){
		int nEarly = 0;
		int nLate = 0;
		
		QueryNode[] qn_list = getQueryList(line);
		for(QueryNode qn: qn_list){
			if(qn.cost>th){
				nLate++;
			}else{
				nEarly++;
			}
		}
				
		return ((double)nEarly)/(nEarly+nLate);		
	}
	
	private void plotErrCost(int sim, int query,double th){
		double[] err_list = getErrList();
		
		String opt = "";
		if(sim>0){
			opt = Param.sim_str[sim];
		}else if(query>0){
			opt = Param.query_str[query];
		}
		for(double mu:mu_list){
			String filename = String.format("ErrCost_%s_Mu%s_th%s"
					,opt,Param.double2String_filename(mu),Param.double2String_filename(th));
			String plot_filename = String.format("%s/%s.eps", plot_dir,filename);
			PostscriptTerminal eps = new PostscriptTerminal(plot_filename);
			
			eps.setColor(true);
			eps.set("font", default_font);
			eps.set("enhanced");
			eps.setEPS(true);
			JavaPlot p = new JavaPlot();
			PlotStyle pStyle = new PlotStyle();
			pStyle.setStyle(Style.LINESPOINTS);
			pStyle.setLineWidth(3);
			pStyle.setPointSize(3);
			p.newGraph();
			p.setTerminal(eps);
			p.setPersist(false);
			p.getAxis("x").setLabel("Annotation Error");
			p.getAxis("y").setLabel("Cost");
			p.getAxis("y").setLogScale(true);

			p.set("size ratio", "0.75");
			p.set("lmargin at screen", "0.25");
			p.set("rmargin at screen", "0.9");
			p.set("bmargin at screen", "0.2");
			p.set("tmargin at screen", "0.95");
			p.set("xrange", String.format("[%f:%f]", 0.0, err_list[err_list.length-1]+0.01));
			p.set("key", "right bottom");
			
			
			
			for (int s = 1; s < Param.sim_str.length; s++){
				if(sim>0 && sim!=s){
					continue;
				}
				for (int q = 1; q< Param.query_str.length; q++){
					if(query>0 &&query!=q){
						continue;
					}
					if(q==Param.QUERY_NEIGHBORPAIR_OVERLAP||!Param.compatible(q, s))
						continue;
					double[][] points = getErrCost(Param.query_str[q], Param.sim_str[s],err_list,mu,th);
					if(points!=null && points.length>0){
						AbstractPlot data = new DataSetPlot(points);
						data.setTitle(getAbb(Param.query_str[q])+"+"+getAbb(Param.sim_str[s]));
						data.setPlotStyle(pStyle);
						p.addPlot(data);
					}
				}
			}	
			p.plot();
			saveGnuplotScript(p,filename);
		}
	}

	
	private double[][] getErrCost(String query, String sim, double[] err_list,double mu, double th) {
		double[][] tmp = new double[err_list.length][2];
		int idx = 0;
		for(int i = 0; i<err_list.length; i++){
			double mincost = (double)getMinCost(query, sim, err_list[i], mu, th);
			if(mincost>-1.0){
				tmp[idx][0] = err_list[i];
				tmp[idx][1] = mincost;
				idx++;
			}
		}
		if(idx==0){
			return null;
		}
		double[][] out = new double[idx][2];
		for(int i = 0; i<idx; i++){
			out[i][0] = tmp[i][0];
			out[i][1] = tmp[i][1];
		}
		return out;
	}
	
	private int getMinCost(String query, String sim, double err, double mu, double th){
		try {
			String mu_option = "";
			if(sim.equals(Param.sim_str[Param.SIM_PROPOSED])){
				mu_option = String.format("and mu > %f and mu <%f  ", mu-0.0001,mu+0.0001);
			}
			
			
			String qry_where_common = 
					String.format("data = '%s' and experiment.id = results.exp_id and query = '%s' " +
							"and sim = '%s'  " +
							"and err > %f and err < %f " +
							"%s ",
							dataname, query, sim,err-0.0001,err+0.0001,mu_option);
			
						
			String qry =
					"select ntrue/(ntrue+nfalse) as recall, cost "+
							"from experiment, results " +
							"where "+ qry_where_common+
							"order by recall desc;";
			
			ResultSet rs = sqlunit.executeQuery(qry);
			
			rs.beforeFirst();
			int cost = 0;
			double recall;
			boolean exist = false;
			while(rs.next()){
				recall = rs.getDouble(1);
				if(recall < th){
					break;
				}
				cost = rs.getInt(2);
				exist = true;
			}
			
			
			if(exist){
				return cost;
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(0);
		}
		return -1;
	}
	
	private void saveGnuplotScript(JavaPlot p, String name){
		String filename = script_dir+"/"+name+".gp";
		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(filename));
			bw.write(p.getCommands());
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	/*
	private void plotRmRCost(int sim, int query){
		double[] rmr_list = getRmrlist();
		

		String opt = "";
		if(sim>0){
			opt = Param.sim_str[sim];
		}else if(query>0){
			opt = Param.query_str[query];
		}
		PostscriptTerminal eps = new PostscriptTerminal(String.format("plots/RmrCost%s_%s.eps",dataname,opt));
		
		eps.setColor(true);
		eps.setEPS(true);
		JavaPlot p = new JavaPlot();
		PlotStyle pStyle = new PlotStyle();
		pStyle.setStyle(Style.LINESPOINTS);
		p.newGraph();
		p.getAxis("x").setLabel("Precision");
		p.getAxis("y").setLabel("");
		p.getAxis("y").setLogScale(true);
		p.set("size ratio", "1");

		p.set("lmargin at screen", "0.15");
		p.set("rmargin at screen", "0.9");
		p.set("bmargin at screen", "0.1");
		p.set("tmargin at screen", "0.95");
		//p.set("terminal wxt size", "640,640");
		p.setTerminal(eps);
		p.setPersist(false);
		
		for (int s = 1; s < Param.sim_str.length; s++){
			if(sim>0 && sim!=s){
				continue;
			}
			for (int q = 1; q< Param.query_str.length; q++){
				if(query>0 &&query!=q){
					continue;
				}
				double[][] points = getRmRCost(Param.query_str[q], Param.sim_str[s],rmr_list);
				if(points.length>0){
					AbstractPlot data = new DataSetPlot(points);
					data.setTitle(getAbb(Param.query_str[q])+"+"+getAbb(Param.sim_str[s]));
					data.setPlotStyle(pStyle);
					p.addPlot(data);
				}
			}
		}	
		p.plot();
	}

	private double[][] getRmRCost(String query, String sim, double[] rmr_list) {
		double[][] out = new double[rmr_list.length][2];
		try {
			String qry = String.format(
					"select rmr, max(cost) "+
					"from experiment, results " +
					"where data = '%s' and experiment.id = results.exp_id and query = '%s' " +
					"and sim = '%s'  " +
					"group by rmr " +
					"order by rmr asc;",
					dataname, query, sim);
			
		
			ResultSet rs = sqlunit.executeQuery(qry);
			
			
			rs.beforeFirst();
			int i = 0;
			while(i<out.length){
				if(!rs.next()){
					break;
				}
				out[i][0] = rs.getDouble(1);
				out[i][1] = (double) rs.getInt(2);
				i++;
			}
			
			return out;
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(0);
		}
		return out;
	}
	*/
	
	/*
	private void plotQueryInfoCost(PlotParam pp, boolean avg){
		double[] rmr_list = getRmrlist();
		double[] err_ann_list = getErrList();
		for(int infotype: pp.infotypes){
			for( double rmr:rmr_list){
				for(double err_ann:err_ann_list){
					for(int q = 1; q < Param.query_str.length; q++){
					
						String opt = Param.query_str[q];
						
						PostscriptTerminal eps = new PostscriptTerminal(String.format("plots/%s%sCost%s%s_rmr%s.eps",dataname,opt,PlotParam.info_str[infotype],avg?"Avg":"",Double.toString(rmr).replace(".","_")));
						
						eps.setColor(true);
						eps.setEPS(true);
						JavaPlot p = new JavaPlot();
						PlotStyle pStyle = new PlotStyle();
						pStyle.setStyle(Style.LINESPOINTS);
						p.newGraph();
						
						p.getAxis("x").setLabel(PlotParam.label(infotype));
						p.getAxis("y").setLabel("Cost");
						p.getAxis("y").setLogScale(true);
						
						p.setTerminal(eps);
						p.setPersist(false);
						p.set("size ratio", "1");
						p.set("lmargin at screen", "0.15");
						p.set("rmargin at screen", "0.9");
						p.set("bmargin at screen", "0.1");
						p.set("tmargin at screen", "0.95");
						
						p.set("terminal wxt size", "640,640");
						
						for(int sim = 1; sim < Param.sim_str.length; sim++){
							double [][] points = getInfoCost(infotype, q, sim, rmr, 100,avg,infotype==PlotParam.INFO_TYPE_ENTROPY?5.0:1.0,false,err_ann,0);
							if(points !=null && points.length>0){
								AbstractPlot data = new DataSetPlot(points);
								data.setTitle(getAbb(Param.query_str[q])+"+"+getAbb(Param.sim_str[sim]));
								data.setPlotStyle(pStyle);
								p.addPlot(data);
							}
							
						}	
						p.plot();
					}
				}
			}
		}
	
	}
	
	
	private void plotWCost(){
		double[] rmr_list = getRmrlist();
		double[] err_ann_list = getErrList();
		for( double rmr:rmr_list){
			for(double err_ann:err_ann_list){
				for(int sim = 1; sim < Param.sim_str.length; sim++){
					String opt = Param.sim_str[sim];
					
					PostscriptTerminal eps = new PostscriptTerminal(String.format("plots/%sWCost%s_rmr%s.eps",dataname,opt,Double.toString(rmr).replace(".","_")));
					
					eps.setColor(true);
					eps.setEPS(true);
					JavaPlot p = new JavaPlot();
					PlotStyle pStyle = new PlotStyle();
					pStyle.setStyle(Style.POINTS);
					p.newGraph();
					p.getAxis("x").setLabel(opt);
					p.getAxis("y").setLabel("Cost");
					p.getAxis("y").setLogScale(true);
					
					p.setTerminal(eps);
					p.setPersist(false);
					p.set("size ratio", "1");
					p.set("lmargin at screen", "0.05");
					p.set("rmargin at screen", "0.9");
					p.set("bmargin at screen", "0.1");
					p.set("tmargin at screen", "0.95");
					
					p.set("terminal wxt size", "640,640");
					
					for(int q = 1; q < Param.query_str.length; q++){
											
						double mu=1.0;
						int gmid=0;
						double [][] points = getWCost(q, sim, rmr, 100,false, err_ann,mu,gmid);
						if(points !=null && points.length>0){
							AbstractPlot data = new DataSetPlot(points);
							data.setTitle(getAbb(Param.query_str[q]));
							data.setPlotStyle(pStyle);
							p.addPlot(data);
						}
						
					}	
					p.plot();
				}		
			}
			
		}
	}*/
	
	

	private double[] getErrList() {
		//double[] errlist = {0.0,0.01,0.05,0.1};
		//return err_list;
		String qry = String.format("select distinct (err) from experiment where data = '%s' " +
				"order by err asc;",dataname);
		ResultSet rs = sqlunit.executeQuery(qry);
		
		ArrayList<Double> err_list = new ArrayList<Double> ();
		try {
			rs.beforeFirst();
			while(rs.next()){
				err_list.add(rs.getDouble(1));
			}
		} catch (SQLException e) {
			e.printStackTrace();
			System.exit(0);
		}
		double[] list = new double[err_list.size()];
		
		for(int l = 0; l < list.length; l++){
			list[l] = err_list.get(l);
		}
		return list;		
		
	}

	private double[][] getWCost(int query, int sim, double rm_ratio, int nquery,boolean perfect_ann, double err_ann,double mu, int gmid) {
		return getInfoCost(PlotParam.INFO_TYPE_MATCHING_SIM, query, sim, rm_ratio, nquery, false, 1.0,perfect_ann, err_ann,mu,gmid);
	}

	
	
	private double[][] getInfoCost(int infotype, int query, int sim, double rm_ratio, int nquery, boolean avg, double max,boolean perfect_ann, double err_ann,int gmid) {
		return getInfoCost(infotype, query, sim, rm_ratio, nquery, avg, max, perfect_ann, err_ann, 10.0, gmid);
	}
	private double[][] getInfoCost(int infotype, int query, int sim, double rm_ratio, int nquery, boolean avg, double max,boolean perfect_ann, double err_ann,double mu,int gmid) {
		String filename = Param.logFilePath(dataname, sim, query, rm_ratio, nquery,perfect_ann,err_ann,mu,gmid);
		String line = null;
		double[][] data = null;
		int[] count = null;
		int nBucket = 100;
		ArrayList<Double> cost = null;
		ArrayList<Double> w_list = null;
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			
			if (avg){
				data = new double[nBucket][2];
				count = new int[nBucket];
				for(int b = 0; b <nBucket; b++){
					data[b][0] = max*(b+0.5)/nBucket;
				}
				
			}else{
				cost = new ArrayList<Double>();
				w_list = new ArrayList<Double>();
			}
			
			
			boolean v_continue = true;
			while(v_continue){
				line=br.readLine();
				if(line==null||line.length()==0){
					v_continue = false;
					continue;
				}
				char sc = line.charAt(0);
				switch(sc){
				case 'i':
					continue;
				case 'E':
					v_continue = false;
					break;
				default:
					QueryNode[] qn_list = getQueryList(line);
					for(QueryNode qn: qn_list){
						double val = 0.0;
						switch(infotype){
						case PlotParam.INFO_TYPE_DIFF:
							val = qn.diff;
							break;
						case PlotParam.INFO_TYPE_MATCHING_SIM:
							val = qn.matching_sim;
							break;
						case PlotParam.INFO_TYPE_MAX_SIM:
							val = qn.max_sim;
							break;
						case PlotParam.INFO_TYPE_ENTROPY:
							val = qn.entropy;
						case PlotParam.INFO_TYPE_COST:
							val = qn.cost;
						}
						if(avg){
							int b = (int) (val*nBucket/max);
							b = Math.min(b, nBucket-1);
							
							data[b][1]+= qn.cost;
							count[b]++;
						}else{
							cost.add((double) qn.cost);
							w_list.add(val);
						}
					}
					break;
				
				}
			}
			
			if(avg){
				for(int b = 0; b <nBucket; b++){
					if(count[b]>0){
						data[b][1] /= count[b]; 
					}
				}
			}else{
				if(cost.size()==0){
					br.close();
					return null;
				}
				data = new double[cost.size()][2];
				for(int i = 0; i< data.length; i++){
					data[i][0] = w_list.get(i);
					data[i][1] = cost.get(i);
				}
			}
			br.close();
			return data;
		}catch(FileNotFoundException e){
			return null;
		}catch (IOException e) {
			System.out.println("Error string: "+line);
			e.printStackTrace();
			return null;
		}
	}

	/*
	private void plotDiffCost(){
		double[] rmr_list = getRmrlist();
		
		for( double rmr:rmr_list){
			for(int query = 1; query < Param.query_str.length; query++){
				String opt = Param.query_str[query];
				
				PostscriptTerminal eps = new PostscriptTerminal(String.format("plots/%sDiffCost%s_rmr%s.eps",dataname,opt,Double.toString(rmr).replace(".","_")));
				
				eps.setColor(true);
				eps.setEPS(true);
				JavaPlot p = new JavaPlot();
				PlotStyle pStyle = new PlotStyle();
				pStyle.setStyle(Style.POINTS);
				p.newGraph();
				p.getAxis("x").setLabel(opt);
				p.getAxis("y").setLabel("Cost");
				p.getAxis("y").setLogScale(true);
				
				p.setTerminal(eps);
				p.setPersist(false);
				//p.set("size", "1 1");
				p.set("lmargin at screen", "0.15");
				p.set("rmargin at screen", "0.9");
				p.set("bmargin at screen", "0.1");
				p.set("tmargin at screen", "0.95");
				
				
				
				for (int s = 1; s < Param.sim_str.length; s++){
										
					double [][] points = getDiffCost(query, s, rmr, 100);
					if(points !=null && points.length>0){
						AbstractPlot data = new DataSetPlot(points);
						data.setTitle(getAbb(Param.sim_str[s]));
						data.setPlotStyle(pStyle);
						p.addPlot(data);
					}
					
				}	
				p.plot();
			}		
		}
	}
	*/
	
	/*
	private double[][] getDiffCost(int query, int sim, double rm_ratio, int nquery) {
		String filename = Param.logFilePath(dataname, sim, query, rm_ratio, nquery);
		String line = null;
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			ArrayList<Double> cost = new ArrayList<Double>();
			ArrayList<Double> diff_list = new ArrayList<Double>();
			
			
			boolean v_continue = true;
			while(v_continue){
				line=br.readLine();
				if(line==null||line.length()==0){
					v_continue = false;
					continue;
				}
				char sc = line.charAt(0);
				switch(sc){
				case 'i':
					continue;
				case 'E':
					v_continue = false;
					break;
				default:
					QueryNode[] qn_list = getQueryList(line);
					for(QueryNode qn: qn_list){
						cost.add((double) qn.cost);
						diff_list.add(qn.diff);
					}
					break;

				}
			}
			if(cost.size()==0){
				br.close();
				return null;
			}
			double[][] data = new double[cost.size()][2];
			for(int i = 0; i< data.length; i++){
				data[i][0] = diff_list.get(i);
				data[i][1] = cost.get(i);
			}
			br.close();
			return data;
		}catch(FileNotFoundException e){
			return null;
		}catch (IOException e) {
			System.out.println("Error string: "+line);
			e.printStackTrace();
			return null;
		}
	}
	*/
	/*
	private void plotIterCost(int sim, int query){
		double[] rmr_list = getRmrlist();
		double[] err_list = getErrList();
		for( double rmr:rmr_list){
			for(double err_ann:err_list){
				String opt = "";
				if(sim>0){
					opt = Param.sim_str[sim];
				}else if(query>0){
					opt = Param.query_str[query];
				}
				PostscriptTerminal eps = new PostscriptTerminal(String.format("plots/IterCost%s_%s_rmr%s.eps",dataname,opt,Double.toString(rmr).replace(".","_")));
				
				eps.setColor(true);
				eps.setEPS(true);
				JavaPlot p = new JavaPlot();
				PlotStyle pStyle = new PlotStyle();
				pStyle.setStyle(Style.POINTS);
				p.newGraph();
				p.getAxis("x").setLabel("Iteration");
				p.getAxis("y").setLabel("EarlyMatchingRatio");
				p.getAxis("y").setLogScale(true);
				
				p.setTerminal(eps);
				p.setPersist(false);
				p.set("lmargin at screen", "0.15");
				p.set("rmargin at screen", "0.9");
				p.set("bmargin at screen", "0.1");
				p.set("tmargin at screen", "0.95");
				
				p.set("terminal wxt size", "640,640");
				
				for (int s = 1; s < Param.sim_str.length; s++){
					if(sim>0 && sim!=s){
						continue;
					}
					for (int q = 1; q< Param.query_str.length; q++){
						if(query>0 &&query!=q){
							continue;
						}
						double [][] points = getIterCost(q, s, rmr, 100,false, err_ann);
						if(points !=null && points.length>0){
							AbstractPlot data = new DataSetPlot(points);
							data.setTitle(getAbb(Param.query_str[q])+"+"+getAbb(Param.sim_str[s]));
							data.setPlotStyle(pStyle);
							p.addPlot(data);
						}
					}
				}	
				p.plot();
			}
		}
		
	}

	
	private double[][] getIterCost(int query, int sim, double rm_ratio, int nquery, boolean perfect_ann, double err_ann) {
		String filename = Param.logFilePath(dataname, sim, query, rm_ratio, nquery, perfect_ann, err_ann,10.0);
		String line = null;
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			ArrayList<Double> cost = new ArrayList<Double>();
			ArrayList<Integer> iter_list = new ArrayList<Integer>();
			
			
			boolean v_continue = true;
			int iteration = 0;
			while(v_continue){
				line=br.readLine();
				if(line==null||line.length()==0){
					v_continue = false;
					continue;
				}
				char sc = line.charAt(0);
				switch(sc){
				case 'i':
					iteration = Integer.parseInt(line.split(":")[1].trim());
					continue;
				case 'E':
					v_continue = false;
					break;
				default:
					QueryNode[] qn_list = getQueryList(line);
					double avg = 0.0;
					for(QueryNode qn: qn_list){
						avg += qn.cost;
						//cost.add(qn.cost);
						//iter_list.add(iteration);
					}
					avg /= qn_list.length;
					cost.add(avg);
					iter_list.add(iteration);
					break;
				}
			}
			if(cost.size()==0){
				br.close();
				return null;
			}
			double[][] data = new double[cost.size()][2];
			for(int i = 0; i< data.length; i++){
				data[i][0] = iter_list.get(i);
				data[i][1] = cost.get(i);
			}
			br.close();
			return data;
		}catch(FileNotFoundException e){
			return null;
		}catch (IOException e) {
			System.out.println("Error string: "+line);
			e.printStackTrace();
			return null;
		}
	}
	
	*/
}

class PlotParam{
	static final int INFO_TYPE_DIFF 		= 1;
	static final int INFO_TYPE_MAX_SIM 		= 2;
	static final int INFO_TYPE_MATCHING_SIM = 3;
	static final int INFO_TYPE_ENTROPY 		= 4; 
	static final int INFO_TYPE_COST			=5;
	
	static final String[] info_str = {"","DIFF","MAX_SIM","MATCHING_SIM","ENTROPY","COST"};
	static final String[] info_label = {"","Diff","Max Similarity","Matching Similarity","Entropy","Cost"};
	
	
	ArrayList<Integer> infotypes = new ArrayList<Integer>();
	public PlotParam(){
		append(PlotParam.INFO_TYPE_ENTROPY);
		append(PlotParam.INFO_TYPE_MAX_SIM);
		append(PlotParam.INFO_TYPE_MATCHING_SIM);
	}
	void append(int i){
		infotypes.add(i);
	}
	
	static String label(int t){
		return info_label[t];
	}
	
}

