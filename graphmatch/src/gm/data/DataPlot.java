package gm.data;

import graph.nodelist;

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
import java.util.Collections;
import java.util.HashMap;

import sim.Param;
import sim.Test;
import cm.QueryNode;
import cm.cm_data;

import com.panayotis.gnuplot.JavaPlot;
import com.panayotis.gnuplot.plot.AbstractPlot;
import com.panayotis.gnuplot.plot.DataSetPlot;
import com.panayotis.gnuplot.style.PlotStyle;
import com.panayotis.gnuplot.style.Style;
import com.panayotis.gnuplot.terminal.PostscriptTerminal;

public class DataPlot {
	static final String PLOT_DIR_STRING = "plots";
	static final String SCRIPT_DIR_STRING = "gnuplot_script";
	
	static double[] mu_list = {0.1,1.0,10.0,100.0};
	static double default_mu = 10.0;
	

	static String default_font = "'Arial, 24'";
	static int	default_point_size = 2;
	
	String dataname;
	String plot_dir;
	String script_dir;
	SqlUnit sqlunit;
	
	
	
	public static HashMap<String, String> abb = new HashMap<String, String>();
	public static void main(String[] args){
		String dbname = Test.default_dbname;
		if(args.length>0){
			dbname = args[0];
			System.out.println(dbname);
		}else{
			System.out.println("enter dbname");
			System.exit(0);
		}
		
		initAbb();
		SqlUnit sqlunit = new SqlUnit(dbname);
		
		String dataname = "tiny";
		int[] radius_list = getRadiusList(dataname, sqlunit);
		
		plotAll(dataname,dbname,radius_list);
		
		dataname = "Syn500_0_03";
		radius_list = getRadiusList(dataname, sqlunit);
		plotAll(dataname, dbname,radius_list);
		for(int radius: radius_list){
			plotSynthetic(500,dbname, radius);
		}
		
		dataname = "Syn500_0_01";
		radius_list = getRadiusList(dataname, sqlunit);
		if(radius_list.length>0){
			plotAll(dataname, dbname,radius_list);
			for(int radius: radius_list){
				plotSynthetic(500,dbname, radius);
			}
		}
		
		dataname = "Syn500_0_005";
		radius_list = getRadiusList(dataname, sqlunit);
		if(radius_list.length>0){
			plotAll(dataname, dbname,radius_list);
			for(int radius: radius_list){
				plotSynthetic(500,dbname, radius);
			}
		}
		
		
		sqlunit.exit();
		
	}
	
	
	static void plotSynthetic(int n_node,String dbname, int radius){
		double[] density_list = {0.005,0.01,0.03,0.05};
		double rmr = 0.05;
		double mu  = 1.0;
		double err = 0.03;
		
		String filename = "CostAccSynth"+n_node;
		 
		PlotManager pm = new PlotManager("Synth"+n_node,filename, dbname,radius);
		
		PostscriptTerminal eps = new PostscriptTerminal(pm.getPlotfilename());
		
		eps.setEPS(true);
		eps.set("font", default_font);
		eps.set("enhanced");
		eps.setColor(false);
		
		
		JavaPlot p = new JavaPlot();
		
		p.newGraph();
		p.getAxis("x").setLabel("Accuracy");
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
		for(double density:density_list){
			DataPlot dp = new DataPlot(DataGenerator.getfilename(n_node, density),dbname);
			//dp.plotCostAccuracy(radius, 1.0,0.9);
			double [][][] points = dp.getCostAccuracy(
					radius, 
					Param.query_str[Param.QUERY_EMC], 
					Param.sim_str[Param.SIM_PROPOSED], rmr,mu , false, err);
			if(points!=null&&points.length>0){
				for(int i = 0; i < points.length; i++){
					if(points[i]!=null && points.length>0){
						AbstractPlot data = new DataSetPlot(points[i]);
						data.setTitle(
								"density = "+density);
						
						PlotStyle pStyle = new PlotStyle();
						pStyle.setStyle(Style.LINESPOINTS);
						pStyle.setLineWidth(3);
						pStyle.setPointSize(default_point_size);
						//pStyle.setPointType(PlotParam.getPointType(mu));
						data.setPlotStyle(pStyle);
						p.addPlot(data);
						b_plot = true;
					}
				}
			}
		}
		if(b_plot){
			p.plot();
			pm.saveGnuplotScript(p);
		}
		
	}
	
	static void plotAll(String dataname,String dbname,int[] radius_list){
		DataPlot dp = new DataPlot(dataname,dbname);

		
		double max_x = 1.0;
		if(dataname.toLowerCase().startsWith("tiny")){
			max_x = 0.7;
		}else if(dataname.toLowerCase().startsWith("syn")){
			max_x =0.9;
		}
		for(double mu: mu_list){
			dp.plotVaryingRadius(mu, max_x);
			
		}
		//*
		
		for(int radius: radius_list){

			dp.plotAnnMuAccuracy(radius,max_x);
			for(double mu: mu_list){
				dp.plotCostAccuracy(radius,mu,max_x);
				dp.plotEarlyMatching(radius,-1, -1,mu);
				dp.plotRmRCost(radius, -1, -1, mu);
			}
			
			double[] mu1_0 = {10.0};
			if(dataname.toLowerCase().startsWith("syn")){
				dp.plotErrCost(radius, -1, -1, 0.9,mu1_0,true);
				dp.plotErrCost(radius, -1, -1, 0.9,mu1_0,false);
				dp.plotErrCost(radius, Param.SIM_PROPOSED, -1, 0.9,mu_list,false);
				/*
				dp.plotErrCost(radius, -1, -1, 0.85,mu1_0,true);
				dp.plotErrCost(radius, -1, -1, 0.85,mu1_0,false);
				dp.plotErrCost(radius, Param.SIM_PROPOSED, -1, 0.9,mu_list,false);
				*/
			}else{
				dp.plotErrCost(radius, -1, -1, 0.7,mu1_0,true);
				dp.plotErrCost(radius, -1, -1, 0.7,mu1_0,false);
				dp.plotErrCost(radius, Param.SIM_PROPOSED, -1, 0.7,mu_list,false);
			}
			
			dp.plotCoverage(radius, Param.SIM_PROPOSED, Param.QUERY_EMC, 0.1, 0.03);
			//dp.plotCoverage(radius, Param.SIM_PROPOSED, Param.QUERY_EMC, 0.1, 0.05);
			
			dp.plotDegreeDistribution(radius, Param.SIM_PROPOSED, Param.QUERY_EMC, 0.1, 0.03);
			//dp.plotDegreeDistribution(radius, Param.SIM_PROPOSED, Param.QUERY_EMC, 0.1, 0.05);
		}
		//*/
	}
	
	public DataPlot(String dataname,String dbname){
		File dir,subdir;
		
		plot_dir = PLOT_DIR_STRING+"/"+dataname;
		subdir = new File(plot_dir);
		if(!subdir.exists()){
			dir	= new File(PLOT_DIR_STRING);
			dir.mkdir();
			subdir.mkdir();
		}
		
		
		script_dir=SCRIPT_DIR_STRING+"/"+dataname;
		subdir = new File(script_dir);
		if(!subdir.exists()){
			dir = new File(SCRIPT_DIR_STRING);
			dir.mkdir();
			subdir.mkdir();
		}
		
		this.dataname = dataname;
		sqlunit = new  SqlUnit(dbname);
		
		int[] radius_list = getRadiusList();
		for(int radius:radius_list){
			dir = new File(plot_dir+"/radius"+radius);
			dir.mkdir();
			dir = new File(script_dir+"/radius"+radius);
			dir.mkdir();
		}
	
	}
	
	
	void plotCoverage(int radius, int sim, int query,double mu,double err){
		double[] rmr_list = getRmrlist(radius);
		
		
		for( double rmr:rmr_list){
			for (int s = 1; s < Param.sim_str.length; s++){
				if(sim>0 && sim!=s){
					continue;
				}
				for (int q = 1; q< Param.query_str.length; q++){
					if(query>0 &&query!=q){
						continue;
					}
					if(!Param.compatible(query, sim))
						continue;
					String filename = String.format("Coverage_%s_%s_%s_mu%s_r%d"
							,Param.sim_str[s]
							,Param.query_str[q]
							,Param.optAnn(false, err)
							,Param.double2String_filename(mu)
							,radius
					);
					PlotManager pm = new PlotManager(this, filename,radius);
					PostscriptTerminal eps = new PostscriptTerminal(pm.getPlotfilename());
					
					eps.setEPS(true);
					eps.set("font", default_font);
					eps.set("enhanced");
					eps.setColor(false);
					
					
					JavaPlot p = new JavaPlot();
					
					p.newGraph();
					p.getAxis("x").setLabel("Iteration");
					p.getAxis("y").setLabel("Coverage");
					
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
					double[][][] points = getCoverage(dataname, radius, s, q, rmr, 100, err, mu);
					if(points!=null&&points.length>0){
						for(int i = 0; i < points.length; i++){
							if(points[i]!=null && points.length>0){
								AbstractPlot data = new DataSetPlot(points[i]);
								data.setTitle(getAbb(Param.query_str[q])+"+"+getAbb(Param.sim_str[s])+(points.length>1?i:""));
								PlotStyle pStyle = new PlotStyle();
								pStyle.setStyle(Style.LINESPOINTS);
								pStyle.setLineWidth(3);
								pStyle.setPointSize(default_point_size);
								pStyle.setPointType(PlotParam.getPointType(s, q));
								data.setPlotStyle(pStyle);
								pm.addPlot(points[i][points[i].length-1][1], data);
								
								b_plot = true;
							}
						}
					}
					if(b_plot)
						pm.plot(p);
					
				}
			}
			
		}
	}
	void plotDegreeDistribution(int radius, int sim, int query,double mu,double err){
		double[] rmr_list = getRmrlist(radius);
		
		
		for( double rmr:rmr_list){
			for (int s = 1; s < Param.sim_str.length; s++){
				if(sim>0 && sim!=s){
					continue;
				}
				for (int q = 1; q< Param.query_str.length; q++){
					if(query>0 &&query!=q){
						continue;
					}
					if(!Param.compatible(query, sim))
						continue;
					String filename = String.format("DegreeDist_%s_%s_%s_mu%s"
						,Param.sim_str[s]
						,Param.query_str[q]
						,Param.optAnn(false, err)
						,Param.double2String_filename(mu)
					);
					PlotManager pm = new PlotManager(this, filename,radius);
					PostscriptTerminal eps = new PostscriptTerminal(pm.getPlotfilename());
					
					eps.setEPS(true);
					eps.set("font", default_font);
					eps.set("enhanced");
					eps.setColor(false);
					
					
					JavaPlot p = new JavaPlot();
					
					p.newGraph();
					p.getAxis("x").setLabel("Degree");
					p.getAxis("y").setLabel("Proportion");
					
					//p.getAxis("y").setLogScale(true);
					p.setTerminal(eps);
					
					p.setPersist(false);
					
					p.set("size ratio", "0.75");
					p.set("lmargin at screen", "0.25");
					p.set("rmargin at screen", "0.9");
					p.set("bmargin at screen", "0.2");
					p.set("tmargin at screen", "0.95");
					
					
					
					boolean b_plot = false;
					double[][][] points = getDegreeDistribution("tiny", radius, s, q, rmr, 100, err, mu);
					if(points!=null&&points.length>0){
						for(int i = 0; i < points.length; i++){
							if(points[i]!=null && points.length>0){
								AbstractPlot data = new DataSetPlot(points[i]);
								if(i%2==0){
									data.setTitle("Correct");
								}else{
									data.setTitle("Incorrect");
								}
								PlotStyle pStyle = new PlotStyle();
							
								pStyle.setStyle(Style.LINESPOINTS);
								pStyle.setLineWidth(3);
								pStyle.setPointSize(default_point_size);
								//pStyle.setPointType(PlotParam.getPointType(s, q));
								data.setPlotStyle(pStyle);
								pm.addPlot(0.0, data);
								
								b_plot = true;
							}
						}
					}
					if(b_plot)
						pm.plot(p);
					
				}
			}
			
		}
	}
	static double[][][] getDegreeDistribution(String dataname, int radius, int sim, int query, double rm_ratio, int nquery, double err_ann, double mu){
		int gmid = 0;
		double out[][][] = null;
		
		
		while(true){
			String filename = Param.logFilePath(dataname, radius, sim, query, rm_ratio, nquery,false,err_ann, mu,gmid)+"_dd";
			try {
				BufferedReader br = new BufferedReader(new FileReader(filename));
				br.close();
				gmid++;
			}catch(FileNotFoundException e){
				break;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				break;
			}
		}
		if(gmid==0){
			return out;
		}
		out = new double[gmid*2][][];
		
		for(gmid = 0; gmid< out.length; gmid+=2){
			double[][][] tmp = getDegreeDistribution(dataname, radius, sim, query, rm_ratio, nquery, err_ann, mu, gmid);
			
			out[2*gmid] = tmp[0];
			out[2*gmid+1] = tmp[1];
		}
		
		return out;
	}
	static double[][][] getDegreeDistribution(String dataname, int radius, int sim, int query, double rm_ratio, int nquery, double err_ann, double mu,int gmid){
		double out[][][]= new double[2][][];

		
		String filename = Param.logFilePath(dataname, radius, sim, query, rm_ratio, nquery,false,err_ann, mu,gmid)+"_dd";
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line = br.readLine();

			ArrayList<IntDoubleEntry> list = new ArrayList<IntDoubleEntry>();
			int max_degree = 0;
			while(!(line=br.readLine()).startsWith("incorrect")){
				String[] pair= line.split("\t");
				int degree = Integer.parseInt(pair[0]);
				list.add(new IntDoubleEntry(degree, Integer.parseInt(pair[1])));
				if(max_degree<degree){
					max_degree = degree;
				}
			}
			Collections.sort(list);
			out[0] = new double[max_degree][2];
			for(int i = 0; i<out[0].length;i++){
				out[0][i][0] = i+1;
				out[0][i][1] = 0;
			}
			double sum =0.0;
			for(int i = 0; i<list.size();i++){
				out[0][list.get(i).key-1][1] = (int)list.get(i).val;
				sum += out[0][list.get(i).key-1][1];
			}
			if(sum>0.0){
				for(int i = 0; i<list.size();i++){
					out[0][list.get(i).key-1][1] /= sum;
				}
			}
			
			list.clear();
			max_degree = 0;
			while((line=br.readLine())!=null && line.length()>0){
				String[] pair= line.split("\t");
				int degree = Integer.parseInt(pair[0]);
				list.add(new IntDoubleEntry(degree, Integer.parseInt(pair[1])));
				if(max_degree<degree){
					max_degree = degree;
				}
			}
			br.close();
			
			out[1] = new double[max_degree][2];
			sum = 0.0;
			for(int i = 0; i<out[1].length;i++){
				out[1][i][0] = i+1;
				out[1][i][1] = 0;
				
			}
			for(int i = 0; i<list.size();i++){
				out[1][list.get(i).key-1][1] = (int)list.get(i).val;
				sum += out[1][list.get(i).key-1][1];
			}
			if(sum>0.0){
				for(int i = 0; i<list.size();i++){
					out[1][list.get(i).key-1][1] /= sum;
				}
			}
			
		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.exit(0);
		}
		
		
		return out;
	}
static double[][][] getCoverage(String dataname, int radius, int sim, int query, double rm_ratio, int nquery, double err_ann, double mu){
	cm_data dat = new cm_data("data/"+dataname+"_5_l", "data/"+dataname+"_5_r");
	int tot_nodes = 0;
	for(nodelist n: dat.lnodes){
		tot_nodes+=n.size_real;
	}
	
	
	int gmid = 0;
	double[][][] out;
	
	
	while(true){
		String filename = Param.logFilePath(dataname, radius, sim, query, rm_ratio, nquery,false,err_ann, mu,gmid);
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			br.close();
			gmid++;
		}catch(FileNotFoundException e){
			break;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			break;
		}
	}
	out = new double[gmid][][];
	
	gmid = 0;
	while(true){
		String filename = Param.logFilePath(dataname, radius, sim, query, rm_ratio, nquery,false,err_ann, mu,gmid);
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line;
			boolean v_continue = true;
			
			ArrayList<IntDoubleEntry> coverage_list = new ArrayList<IntDoubleEntry>();
			while(v_continue){
				line=br.readLine();
				if(line==null||line.length()==0){
					v_continue = false;
					continue;
				}
				char sc = line.charAt(0);
				switch(sc){
				case 'i':
					String[] pair = line.split(",");					
					int iteration = Integer.parseInt(pair[0].split(":")[1].trim()); 
					int covered = Integer.parseInt(pair[1].split(":")[1].trim()); 
					coverage_list.add(new IntDoubleEntry(iteration, ((double)covered)/tot_nodes));
							
					break;
				case 'E':
					v_continue = false;
					break;
				default:
					continue;
				}
				
			}
			br.close();
			if(coverage_list.size()==0){
				return null;
			}
			Collections.sort(coverage_list);
			out[gmid] = new double[coverage_list.size()][2];
			for(int i = 0; i<out[gmid].length; i++){
				out[gmid][i][0] = i;
				out[gmid][i][1] = coverage_list.get(i).val;
			}
			gmid++;
		}catch(FileNotFoundException e){
			break;
		} catch (IOException e) {
			// TODO Auto-generated catch block
				e.printStackTrace();
				break;
			}
		}
		
		return out;
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



	

	private double[][][] getCostAccuracy(int radius, String query, String sim, double rmr,double mu, boolean perfect_ann, double err_ann){
		String ann_str = String.format("and err > %f and err < %f",err_ann-0.0001,err_ann+0.0001);
		String mu_str = "";
		if(sim.equals(Param.sim_str[Param.SIM_PROPOSED])){
			mu_str = String.format("and mu > %f and mu <%f ", mu-0.0001, mu+0.0001);
		}
		
		String qry = String.format(
				"select id " +
				"from experiment " +
				"where data = '%s' and query = '%s' " +
				"and sim = '%s' " +
				"and radius = %d " +
				"and rmr > %f and rmr < %f " +
				"%s " +
				"%s;",
				dataname, query, sim, radius,rmr-0.0001, rmr+0.0001,ann_str,mu_str);
		
		
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
					"where exp_id =%d " +
					"and cost>0;",
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
	public double[] getRmrlist(int radius){
		//double[] rmr_list = {0.0};
		//double[] rmr_list = {0.0, 0.05, 0.1, 0.2};
		//return rmr_list;/*
		String qry = String.format("" +
				"select distinct (rmr) from experiment " +
				"where data = '%s' " +
				"and radius = %d" +
				" order by rmr asc;",dataname, radius);
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
	
	public void plotCostAccuracy(int radius, double mu,double max_x){
		plotCostAccuracy(radius, -1, -1, mu, -1, max_x);
	}
	
	public void plotCostAccuracy(int radius, int sim, int query,double mu,int th_outlier,double max_x){
		boolean except_outlier =  th_outlier>0;
		double[] rmr_list = getRmrlist(radius);
		double[] err_list = getErrList(radius);
		
		for( double rmr:rmr_list){
			for(double err_ann:err_list){
				String opt = "";
				if(sim>0){
					opt = Param.sim_str[sim];
				}else if(query>0){
					opt = Param.query_str[query];
				}
				
				String filename = String.format("CostAccuracy_%s_rmr%s%s_mu%s%s_r%d"
						,opt
						,Double.toString(rmr).replace(".","_")
						,Param.optAnn(false, err_ann)
						,Param.double2String_filename(mu)
						,except_outlier?"eo"+th_outlier:""
						,radius
						);
				
				
				PlotManager pm = new PlotManager(this, filename,radius);
				
				PostscriptTerminal eps = new PostscriptTerminal(pm.getPlotfilename());
				
				eps.setEPS(true);
				eps.set("font", default_font);
				eps.set("enhanced");
				eps.setColor(false);
				
				
				JavaPlot p = new JavaPlot();
				
				p.newGraph();
				p.getAxis("x").setLabel("Accuracy");
				p.getAxis("y").setLabel("Cost");
				
				p.getAxis("y").setLogScale(true);
				p.setTerminal(eps);
				
				p.setPersist(false);
				
				p.set("size ratio", "0.75");
				p.set("lmargin at screen", "0.25");
				p.set("rmargin at screen", "0.9");
				p.set("bmargin at screen", "0.2");
				p.set("tmargin at screen", "0.95");

				p.set("xrange",String.format("[0.0:%.2f]", max_x));
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
							points = getCostAccuracy(radius, Param.query_str[q], Param.sim_str[s], rmr, mu, false, err_ann);
						}
						if(points!=null&&points.length>0){
							for(int i = 0; i < points.length; i++){
								if(points[i]!=null && points.length>0){
									AbstractPlot data = new DataSetPlot(points[i]);
									data.setTitle(getAbb(Param.query_str[q])+"+"+getAbb(Param.sim_str[s])+(points.length>1?i:""));
									
									PlotStyle pStyle = new PlotStyle();
									pStyle.setStyle(Style.LINESPOINTS);
									pStyle.setLineWidth(3);
									pStyle.setPointSize(default_point_size);
									pStyle.setPointType(PlotParam.getPointType(s, q));
									data.setPlotStyle(pStyle);
									if(dataname.startsWith("tiny")){
										pm.addPlot(points[i][Math.max(0, points[i].length-5)][1], data);
									}else{
										pm.addPlot(points[i][points[i].length-1][1], data);
									}
									//p.addPlot(data);
									if(s==Param.SIM_PROPOSED)
										b_plot = true;
								}
							}
						}
					}
				}	
				if(b_plot){
					pm.plot(p);
					//System.out.println(p.getCommands());
				}
				
			}
		}
	}
	
	
	public void plotVaryingRadius(double mu, double max_x){
		//boolean except_outlier =  false;
		int DEFAULT_RADIUS = 2;
		double[] rmr_list = getRmrlist(DEFAULT_RADIUS);
		double[] err_list = getErrList(DEFAULT_RADIUS);
		int[] radius_list = getRadiusList();
		if(radius_list.length<=1){
			return;
		}
		for( double rmr:rmr_list){
			for(double err_ann:err_list){
				String filename = String.format("CostAccuracyVaryingRad_rmr%s%s_mu%s"
						,Double.toString(rmr).replace(".","_")
						,Param.optAnn(false, err_ann)
						,Param.double2String_filename(mu)
						);
				
				
				PlotManager pm = new PlotManager(this, filename, -1);
				
				PostscriptTerminal eps = new PostscriptTerminal(pm.getPlotfilename());
				
				eps.setEPS(true);
				eps.set("font", default_font);
				eps.set("enhanced");
				eps.setColor(false);
				
				
				JavaPlot p = new JavaPlot();
				
				p.newGraph();
				p.getAxis("x").setLabel("Accuracy");
				p.getAxis("y").setLabel("Cost");
				
				p.getAxis("y").setLogScale(true);
				p.setTerminal(eps);
				
				p.setPersist(false);
				
				p.set("size ratio", "0.75");
				p.set("lmargin at screen", "0.25");
				p.set("rmargin at screen", "0.9");
				p.set("bmargin at screen", "0.2");
				p.set("tmargin at screen", "0.95");

				p.set("xrange",String.format("[0.0:%.2f]", max_x));
				p.set("key", "right bottom");

				int q = Param.QUERY_EMC;
				int s = Param.SIM_PROPOSED;
				boolean b_plot = false;
				for(int radius:radius_list){
					double[][][] points = null;
					points = getCostAccuracy(radius, Param.query_str[q], Param.sim_str[s], rmr, mu, false, err_ann);
					if(points!=null&&points.length>0){
						for(int i = 0; i < points.length; i++){
							if(points[i]!=null && points.length>0){
								AbstractPlot data = new DataSetPlot(points[i]);
								data.setTitle("Radius: "+radius);
								
								PlotStyle pStyle = new PlotStyle();
								pStyle.setStyle(Style.LINESPOINTS);
								pStyle.setLineWidth(3);
								pStyle.setPointSize(default_point_size);
								//pStyle.setPointType(PlotParam.getPointType(s, q));
								data.setPlotStyle(pStyle);
								pm.addPlot(-radius, data);
								//p.addPlot(data);
								b_plot = true;
							}
						}
					}
					
				}	
				if(b_plot){
					pm.plot(p);
					//System.out.println(p.getCommands());
				}
				
			}
		}
	}
	


	public void plotAnnMuAccuracy(int radius,double max_x){
		double[] rmr_list = getRmrlist(radius);
		double[] err_list = getErrList(radius);
		
		
		for( double rmr:rmr_list){
			for(double err_ann:err_list){
				String filename = String.format("CostAccuracy_varyingMu_rmr%s_Ann%s_r%d"
						,Double.toString(rmr).replace(".","_")
						,Param.optAnn(false, err_ann)
						,radius);
				
				PlotManager pm = new PlotManager(this, filename, radius);
				
				PostscriptTerminal eps = new PostscriptTerminal(pm.getPlotfilename());
				
				eps.setEPS(true);
				eps.set("font", default_font);
				eps.set("enhanced");
				eps.setColor(false);
				
				
				JavaPlot p = new JavaPlot();
				
				
				p.newGraph();
				p.setTerminal(eps);
				p.set("encoding","utf8");
				
				p.getAxis("x").setLabel("Accuracy");
				p.getAxis("y").setLabel("Cost");
				
				p.setPersist(false);
				
				p.set("size ratio", "0.75");
				p.set("lmargin at screen", "0.25");
				p.set("rmargin at screen", "0.9");
				p.set("bmargin at screen", "0.2");
				p.set("tmargin at screen", "0.95");
				p.set("xrange",String.format("[0.0:%.2f]", max_x));
				p.getAxis("y").setLogScale(true);
				
				p.set("key", "right bottom");
				
				
				boolean b_plot = false;
				
						
				for(double mu: mu_list){
					double[][][] points = getCostAccuracy(radius,
							Param.query_str[Param.QUERY_EMC], Param.sim_str[Param.SIM_PROPOSED],
							rmr, mu, false, err_ann);
					if(points!=null&&points.length>0){
						for(int i = 0; i < points.length; i++){
							if(points[i]!=null && points.length>0){
								AbstractPlot data = new DataSetPlot(points[i]);
								data.setTitle(
										"{/Symbol m} = "+mu);
								
								PlotStyle pStyle = new PlotStyle();
								pStyle.setStyle(Style.LINESPOINTS);
								pStyle.setLineWidth(3);
								pStyle.setPointSize(default_point_size);
								pStyle.setPointType(PlotParam.getPointType(mu));
								data.setPlotStyle(pStyle);
								pm.addPlot(points[i][points[i].length-1][1], data);
								b_plot = true;
							}
						}
					}
				}
					
				if(b_plot){
					pm.plot(p);
				}
			}
		}
	}
	
	public void plotEarlyMatching(int radius, int sim, int query, double mu){
		double[] rmr_list = getRmrlist(radius);
		double[] err_list = getErrList(radius);
		
		for( double rmr:rmr_list){
			for(double err_ann:err_list){
				String opt = "";
				if(sim>0){
					opt = Param.sim_str[sim];
				}else if(query>0){
					opt = Param.query_str[query];
				}
				
				String filename = String.format(
						"earlymatching_%s_rmr%s_ann%s_mu%s_r%d",
						opt,
						Param.double2String_filename(rmr),
						Param.double2String_filename(err_ann),
						Param.double2String_filename(mu),
						radius);
				
				PlotManager pm = new PlotManager(this, filename, radius);
				
				PostscriptTerminal eps = new PostscriptTerminal(pm.getPlotfilename());
				eps.setEPS(true);
				eps.set("font", default_font);
				eps.set("enhanced");
				eps.setColor(false);
				
				
				JavaPlot p = new JavaPlot();
				
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
						double [][] points = getEarlyMatchings(radius, q, s, rmr, 100,TH_EARLY_MATCHING,false,err_ann,
								(s==Param.SIM_PROPOSED?mu:default_mu),gmid);
						if(points !=null && points.length>0){
							AbstractPlot data = new DataSetPlot(points);
							data.setTitle(getAbb(Param.query_str[q])+"+"+getAbb(Param.sim_str[s]));
							
							PlotStyle pStyle = new PlotStyle();
							pStyle.setStyle(Style.LINESPOINTS);
							pStyle.setLineWidth(3);
							pStyle.setPointSize(default_point_size);
							pStyle.setPointType(PlotParam.getPointType(s, q));
							
							data.setPlotStyle(pStyle);
							pm.addPlot(points[points.length-1][1], data);
							b_plot = true;
						}
					}
				}	
				if(b_plot){
					pm.plot(p);
				}

			}
		}
	}



	private double[][] getEarlyMatchings(int radius, int query, int sim, double rm_ratio, int nquery, int th_em,boolean perfect_ann, double err_ann,double mu, int gmid) {
		String filename = Param.logFilePath(dataname, radius, sim, query, rm_ratio, nquery,perfect_ann,err_ann, mu,gmid);
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
				System.out.println("GetqueryNFE "+query);
				//e.printStackTrace();
				continue;
			}catch(ArrayIndexOutOfBoundsException e){
				System.out.println("GetqueryABE "+query);
				//e.printStackTrace();
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
	
	private void plotErrCost(int radius, int sim, int query, double th, double[] mu_list,boolean include_random){
		double[] err_list = getErrList(radius);
		double[] rmr_list = getRmrlist(radius);
		String opt = "";
		if(sim>0){
			opt = Param.sim_str[sim];
		}else if(query>0){
			opt = Param.query_str[query];
		}
		String mu_str = "";
		if(mu_list.length==0){
			mu_str = "_"+Param.double2String_filename(mu_list[0]);
			
		}
		boolean b_plot = false;
		for(double rmr:rmr_list){
			String filename = String.format("ErrCost_%s_rmr%s_%s_th%s%s_r%d"
					,opt,Param.double2String_filename(rmr),mu_str,Param.double2String_filename(th),include_random?"":"nr",radius);
			PlotManager pm = new PlotManager(this, filename, radius);
			
			PostscriptTerminal eps = new PostscriptTerminal(pm.getPlotfilename());
			
			eps.setColor(false);
			eps.set("font", default_font);
			eps.set("enhanced");
			eps.setEPS(true);
			JavaPlot p = new JavaPlot();
	
			p.newGraph();
			p.setTerminal(eps);
			p.setPersist(false);
			p.getAxis("x").setLabel("Annotation Error");
			p.getAxis("y").setLabel("Cost");
			//if(include_random){
				p.getAxis("y").setLogScale(true);
			//}
			p.set("size ratio", "0.75");
			p.set("lmargin at screen", "0.25");
			p.set("rmargin at screen", "0.9");
			p.set("bmargin at screen", "0.2");
			p.set("tmargin at screen", "0.95");
			p.set("xrange", String.format("[%f:%f]", 0.0, err_list[err_list.length-1]+0.01));
			p.set("key", "right bottom");
			
			
			for(double mu:mu_list){
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
						
						if(!include_random&&q==Param.QUERY_RANDOM){
							continue;
						}
						
						
						double[][] points = getErrCost(radius, Param.query_str[q], Param.sim_str[s], rmr, err_list,mu,th);
						if(points!=null && points.length>0){
							AbstractPlot data = new DataSetPlot(points);
							String title = getAbb(Param.query_str[q])+"+"+getAbb(Param.sim_str[s]);
							if(mu_list.length>1){
								title += String.format("({/Symbol m} = %.1f)", mu);
							}
							data.setTitle(title);
							
							PlotStyle pStyle = new PlotStyle();
							pStyle.setStyle(Style.LINESPOINTS);
							pStyle.setLineWidth(3);
							pStyle.setPointSize(default_point_size);
							if(mu_list.length==1){
								pStyle.setPointType(PlotParam.getPointType(s, q));
							}
							
							data.setPlotStyle(pStyle);
							pm.addPlot(points[points.length-1][1], data);
							b_plot =true;
						}
					}
				}	
			}
			if(b_plot){
				pm.plot(p);
			}
		}
	}

	
	private double[][] getErrCost(int radius, String query, String sim, double rmr, double[] err_list,double mu, double th) {
		double[][] tmp = new double[err_list.length][2];
		int idx = 0;
		for(int i = 0; i<err_list.length; i++){
			double mincost = (double)getMinCost(radius, query, sim, rmr, err_list[i], mu, th);
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
	
	private int getMinCost(int radius, String query, String sim, double rmr, double err, double mu, double th){
		try {
			String mu_option = "";
			if(sim.equals(Param.sim_str[Param.SIM_PROPOSED])){
				mu_option = String.format("and mu > %f and mu <%f  ", mu-0.0001,mu+0.0001);
			}
			
			
			String qry_where_common = 
					String.format("data = '%s' and experiment.id = results.exp_id and query = '%s' " +
							"and sim = '%s'  " +
							"and rmr > %f and rmr < %f " +
							"and err > %f and err < %f " +
							"and radius =  %d " +
							"%s ",
							dataname, query, sim,rmr-0.00001, rmr+0.00001, err-0.00001,err+0.00001, radius, mu_option);
			
						
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
	
	
	
	
	private void plotRmRCost(int radius, int sim, int query,double mu){
		double[] rmr_list = getRmrlist(radius);
		double[] err_list = getErrList(radius);

		String opt = "";
		if(sim>0){
			opt = Param.sim_str[sim];
		}else if(query>0){
			opt = Param.query_str[query];
		}
		
		for(double err:err_list){
			String filename = 
					String.format("RmrCost%s_mu%s"
							,opt,Param.double2String_filename(mu));
			
			
			
			PlotManager pm = new PlotManager(this, filename,radius);
			
			PostscriptTerminal eps = new PostscriptTerminal(pm.getPlotfilename());
			
			eps.setEPS(true);
			eps.set("font", default_font);
			eps.set("enhanced");
			eps.setColor(false);
			
			
			JavaPlot p = new JavaPlot();
			
			p.newGraph();
			p.getAxis("x").setLabel("Edge deletion ratio");
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
					if(!Param.compatible(q, s))
						continue;
					double[][] points = getRmRCost(radius, Param.query_str[q], Param.sim_str[s],rmr_list, err);
					if(points!=null && points.length>0){
						AbstractPlot data = new DataSetPlot(points);
						data.setTitle(getAbb(Param.query_str[q])+"+"+getAbb(Param.sim_str[s]));
	
						PlotStyle pStyle = new PlotStyle();
						pStyle.setStyle(Style.LINESPOINTS);
						pStyle.setLineWidth(3);
						pStyle.setPointSize(default_point_size);
						pStyle.setPointType(PlotParam.getPointType(s, q));
						data.setPlotStyle(pStyle);
						pm.addPlot(points[points.length-1][1], data);
						//p.addPlot(data);
						if(s==Param.SIM_PROPOSED)
							b_plot = true;
					}
				}
			}	
			if(b_plot){
				pm.plot(p);
			}
		}
	}
	
	private double[][] getRmRCost(int radius, String query, String sim, double[] rmr_list, double err) {
		double[][] out = new double[rmr_list.length][2];
		try {
			String qry = String.format(
					"select rmr, max(cost) "+
					"from experiment, results " +
					"where " +
					"data = '%s' and experiment.id = results.exp_id " +
					"and query = '%s' " +
					"and sim = '%s'  " +
					"and err > %f and err < %f " +
					"and radius =  %d " +
					"group by rmr " +
					"order by rmr asc;",
					dataname, query, sim, err-0.00001,err+0.00001, radius);
			
		
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
						
						eps.setColor(false);
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
					
					eps.setColor(false);
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
	
	

	private double[] getErrList(int radius) {
		//double[] errlist = {0.0,0.01,0.05,0.1};
		//return err_list;
		String qry = String.format("select distinct (err) from experiment " +
				"where data = '%s' " +
				"and radius = %d " +
				"order by err asc;",dataname,radius);
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
	private int[] getRadiusList(){
		return getRadiusList(dataname, sqlunit);	
	}
	private static int[] getRadiusList(String dataname, SqlUnit sqlunit){
		String qry = String.format("select distinct (radius) from experiment " +
				"where data = '%s' " +
				"order by err asc;",dataname);
		ResultSet rs = sqlunit.executeQuery(qry);
		
		ArrayList<Integer> radius_list = new ArrayList<Integer> ();
		try {
			rs.beforeFirst();
			while(rs.next()){
				radius_list.add(rs.getInt(1));
			}
		} catch(NullPointerException e){
			e.printStackTrace();
			System.out.println(qry);
			System.exit(0);
			
		}catch (SQLException e) {
		
			e.printStackTrace();
			System.exit(0);
		}
		int[] list = new int[radius_list.size()];
		
		for(int l = 0; l < list.length; l++){
			list[l] = radius_list.get(l);
		}
		return list;		
	}
	/*//
	private double[][] getWCost(int query, int sim, double rm_ratio, int nquery,boolean perfect_ann, double err_ann,double mu, int gmid) {
		return getInfoCost(PlotParam.INFO_TYPE_MATCHING_SIM, query, sim, rm_ratio, nquery, false, 1.0,perfect_ann, err_ann,mu,gmid);
	}
	//*/

	
	/*//
	private double[][] getInfoCost(int infotype, int query, int sim, double rm_ratio, int nquery, boolean avg, double max,boolean perfect_ann, double err_ann,int gmid) {
		return getInfoCost(infotype, query, sim, rm_ratio, nquery, avg, max, perfect_ann, err_ann, 10.0, gmid);
	}
	//*/
	private double[][] getInfoCost(int infotype, int radius, int query, int sim, double rm_ratio, int nquery, boolean avg, double max,boolean perfect_ann, double err_ann,double mu,int gmid) {
		String filename = Param.logFilePath(dataname, radius, sim, query, rm_ratio, nquery,perfect_ann,err_ann,mu,gmid);
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


	

}



class PlotDataContainer implements Comparable<PlotDataContainer>{
	double key;
	AbstractPlot dsp;
	public PlotDataContainer(double key, AbstractPlot dsp) {
		this.key = key;
		this.dsp = dsp;
	}
	@Override
	public int compareTo(PlotDataContainer o) {
		if (o.key == key)
			return 0;
		else if (o.key > key)
			return 1;
		else
			return -1;
	}
	
	
	
}
	
class PlotManager{	
	ArrayList<PlotDataContainer> pdc_list = new ArrayList<PlotDataContainer>();
	
	DataPlot dataplot;
	String plot_filename;
	String script_filename;
	String filename;
	int radius;
	
	public PlotManager(DataPlot dataplot,String filename, int radius){
		this.dataplot = dataplot;
		this.filename = filename;
		this.radius = radius;
	}
	public PlotManager(String dirname, String filename,String dbname, int radius){
		this.filename = filename;
		
		
		
		plot_filename = DataPlot.PLOT_DIR_STRING+"/"+dirname+"/";
		script_filename = DataPlot.SCRIPT_DIR_STRING+"/"+dirname+"/";
		
		File dir = new File(plot_filename);
		dir.mkdir();
		dir = new File(script_filename);
		dir.mkdir();
		if(radius>0){
			plot_filename  = plot_filename+"radius"+radius+"/";
			dir = new File(plot_filename);
			dir.mkdir();
			script_filename  = script_filename+"radius"+radius+"/";
			dir = new File(script_filename);
			dir.mkdir();
		}
		
		plot_filename = plot_filename+filename+".eps";
		script_filename = script_filename+filename+".gp";
	}
	
	void addPlot(double key, AbstractPlot dsp){
		pdc_list.add(new PlotDataContainer(key, dsp));
	}
	void plot(JavaPlot p){
		Collections.sort(pdc_list);
		
		
		if(filename.startsWith("ErrCost")){
			setErrCostOption(p);
		}else if(filename.startsWith("CostAcc")){
			setCostAccOption(p);
		}
		
		for(PlotDataContainer pdc:pdc_list){
			p.addPlot(pdc.dsp);
		}
		p.plot();
		
		saveGnuplotScript(p);
	}
	String getRadiusDirStr(){
		if(radius>0){
			return "/radius"+radius;
		}
		return "";
	}
	String getPlotfilename(){
		if(this.plot_filename!=null)
			return this.plot_filename;
		String plot_filename = String.format("%s%s/%s.eps"
				,dataplot.plot_dir
				,getRadiusDirStr()
				,filename);
		return plot_filename;
	}
	String getScriptfilename(){
		if(this.script_filename!=null)
			return this.script_filename;
		String script_filename = String.format("%s%s/%s.gp"
				,dataplot.script_dir
				,getRadiusDirStr()
				,filename);
		return script_filename;
	}
	
	void saveGnuplotScript(JavaPlot p){
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(getScriptfilename()));
			bw.write(p.getCommands());
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	void setCostAccOption(JavaPlot p){
		if(pdc_list.size()>3&&dataplot.dataname.startsWith("tiny")){
			double yrange_min = 100;
			p.set("yrange",String.format("[%f:]",yrange_min));
		}
			//p.set("yrange",String.format("[%f:]",yrange_min));
		
	}
	void setErrCostOption(JavaPlot p){
		if(pdc_list.size()>3){
			double min_key = pdc_list.get(pdc_list.size()-1).key;
			double yrange_min = Math.pow(10, Math.floor(Math.log10(min_key)-1.5));
			p.set("yrange",String.format("[%f:]",yrange_min));
		}
		else{
			double min_key = pdc_list.get(pdc_list.size()-1).key;
			double yrange_min = Math.pow(10, Math.floor(Math.log10(min_key)-1.0));
			//p.set("yrange",String.format("[%f:]",yrange_min));
		}
	}
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
	
	static int getPointType (int sim, int query){
		if(sim==Param.SIM_PROPOSED){
			switch(query){
			case Param.QUERY_EMC:
				return 1;
			case Param.QUERY_MAXENTROPY:
				return 2;
			case Param.QUERY_RANDOM:
				return 3;
			}
		}else if(sim== Param.SIM_SIMRANK){
			switch(query){
			case Param.QUERY_MINVAR:
				return 4;
			case Param.QUERY_RANDOM:
				return 6;
	
			}
		}
		return 6;
	}
	
	static int getPointType(double mu){
		if(mu== 0.1)
			return 1;
		if(mu== 1.0)
			return 2;
		if(mu== 10.0)
			return 4;
		if(mu== 50.0)
			return 7;
		
		return 8;
	}
	
}

class IntDoubleEntry implements Comparable<IntDoubleEntry>{
	int key;
	double val;
	public IntDoubleEntry(int key, double val){
		this.key = key;
		this.val  = val;
	}

	@Override
	public int compareTo(IntDoubleEntry o) {
		return key- o.key;
	}
}

