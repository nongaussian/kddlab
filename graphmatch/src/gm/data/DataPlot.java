package gm.data;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
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
	String dataname;
	SqlUnit sqlunit;
	
	public static HashMap<String, String> abb = new HashMap<String, String>();
	public static void main(String[] args){
		initAbb();
		plotAll("tiny");
		
		
	}
	
	static void plotAll(String dataname){
		DataPlot dp = new DataPlot(dataname);
		/**/
		dp.plotCostAccuracy();
		dp.plotEarlyMatching(-1, -1);
		dp.plotRmRCost(-1, -1);
		/**/
		dp.plotIterCost(-1, -1);
		dp.plotDiffCost();
		//dp.plotWCost();
		dp.plotSimCost(true);
		dp.plotSimCost(false);
		for (int s = 1; s < Param.sim_str.length; s++){
			/**/
			dp.plotCostAccuracy(s,-1);
			dp.plotEarlyMatching(s,-1);
			dp.plotRmRCost(s, -1);
			/**/
			dp.plotIterCost(s, -1);
		}
		for (int q = 1; q< Param.query_str.length; q++){
			/**/
			dp.plotCostAccuracy(-1,q);
			dp.plotEarlyMatching(-1,q);
			dp.plotRmRCost(-1, q);
			/**/
			dp.plotIterCost(-1, q);
		}
	}
	
	static void initAbb(){
		abb.put("PROPOSED", "EM");
		abb.put("NEIGHBORPAIR", "NP");
		abb.put("SIMRANK", "SR");
		abb.put("RANDOM", "RAND");
	}
	static String getAbb(String key){
		String out = abb.get(key);
		if(out!=null){
			return out;
		}
		return key;
	}
	
	
	public DataPlot(String dataname){
		this.dataname = dataname;
		sqlunit = new SqlUnit("graphmatching");
	
	}
	
	
	private double[][] getCostAccuracy(String query, String sim, double rmr){
		double[][] out = null;
		try {
			String suffix = String.format(
					"from experiment, results " +
					"where data = '%s' and experiment.id = results.exp_id and query = '%s' " +
					"and sim = '%s' and rmr > %f " +
					"and rmr < %f;",
					dataname, query, sim, rmr-0.01, rmr+0.01);
			
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
		String qry = String.format("select distinct (rmr) from experiment where data = '%s';",dataname);
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
		return list;
	}
	
	public void plotCostAccuracy(){
		plotCostAccuracy(-1, -1);
	}
	public void plotCostAccuracy(int sim, int query){
		double[] rmr_list = getRmrlist();
		
		for( double rmr:rmr_list){
			String opt = "";
			if(sim>0){
				opt = Param.sim_str[sim];
			}else if(query>0){
				opt = Param.query_str[query];
			}
			PostscriptTerminal eps = new PostscriptTerminal(String.format("plots/CostAccuracy%s_%s_rmr%s.eps",dataname,opt,Double.toString(rmr).replace(".","_")));
			
			eps.setColor(true);
			eps.setEPS(true);
			JavaPlot p = new JavaPlot();
			PlotStyle pStyle = new PlotStyle();
			pStyle.setStyle(Style.LINESPOINTS);
			p.newGraph();
			p.getAxis("x").setLabel("Precision");
			p.getAxis("y").setLabel("Cost");
			p.getAxis("y").setLogScale(true);
			p.set("size ratio", "1");
			
			
			p.set("terminal wxt size", "640,640");
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
					double[][] points = getCostAccuracy(Param.query_str[q], Param.sim_str[s], rmr);
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
	}
	
	public void plotEarlyMatching(int sim, int query){
		double[] rmr_list = getRmrlist();
		
		for( double rmr:rmr_list){
			String opt = "";
			if(sim>0){
				opt = Param.sim_str[sim];
			}else if(query>0){
				opt = Param.query_str[query];
			}
			PostscriptTerminal eps = new PostscriptTerminal(String.format("plots/earlymatching%s_%s_rmr%s.eps",dataname,opt,Double.toString(rmr).replace(".","_")));
			
			eps.setColor(true);
			eps.setEPS(true);
			JavaPlot p = new JavaPlot();
			PlotStyle pStyle = new PlotStyle();
			pStyle.setStyle(Style.LINESPOINTS);
			p.newGraph();
			p.getAxis("x").setLabel("Iteration");
			p.getAxis("y").setLabel("EarlyMatchingRatio");
			
			p.setTerminal(eps);
			p.setPersist(false);
			p.set("size ratio", "1");
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
					int TH_EARLY_MATCHING = 10;
					double [][] points = getEarlyMatchings(q, s, rmr, 100,TH_EARLY_MATCHING);
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



	private double[][] getEarlyMatchings(int query, int sim, double rm_ratio, int nquery, int th_em) {
		String filename = Param.logFilePath(dataname, sim, query, rm_ratio, nquery);
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
					
				case '(':
					earlymatching_ratio.add(computeEarlyMatchingRatio(line,th_em));
					break;
				default:
					v_continue = false;
					break;
				}
			}
			if(earlymatching_ratio.size()==0){
				return null;
			}
			double[][] data = new double[earlymatching_ratio.size()][2];
			for(int i = 0; i< data.length; i++){
				data[i][0] = i;
				data[i][1] = earlymatching_ratio.get(i);
			}
			return data;
		} catch (IOException e) {
			return null;
		}
	}
	
	
	private static QueryNode[] getQueryList(String line){
		ArrayList <QueryNode> qn_list = new ArrayList<QueryNode>();
		String[] queries = line.split("\\|");
		for(String query: queries){
			try{
				String[] query_tmp = query.split("\\(|\\)|,");
				QueryNode qn = new QueryNode(Integer.parseInt(query_tmp[1]), Integer.parseInt(query_tmp[2]));
				qn.diff = Double.parseDouble(query_tmp[4]);
				qn.matching_sim = Double.parseDouble(query_tmp[5]);
				qn.cost = Integer.parseInt(query_tmp[6]);
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
	
	
	
	private void plotSimCost(boolean avg){
		double[] rmr_list = getRmrlist();
		
		for( double rmr:rmr_list){
			for(int q = 1; q < Param.query_str.length; q++){
			
				String opt = Param.query_str[q];
				
				PostscriptTerminal eps = new PostscriptTerminal(String.format("plots/%sSimCost%s%s_rmr%s.eps",dataname,opt,avg?"Avg":"",Double.toString(rmr).replace(".","_")));
				
				eps.setColor(true);
				eps.setEPS(true);
				JavaPlot p = new JavaPlot();
				PlotStyle pStyle = new PlotStyle();
				pStyle.setStyle(Style.LINESPOINTS);
				p.newGraph();
				p.getAxis("x").setLabel("Similarity");
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
					double [][] points = getWCost(q, sim, rmr, 100,avg);
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
	private void plotWCost(){
		double[] rmr_list = getRmrlist();
		
		for( double rmr:rmr_list){

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
										
					double [][] points = getWCost(q, sim, rmr, 100);
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
	
	

	private double[][] getWCost(int query, int sim, double rm_ratio, int nquery) {
		return getWCost(query, sim, rm_ratio, nquery, false);
	}
	private double[][] getWCost(int query, int sim, double rm_ratio, int nquery, boolean avg) {
		String filename = Param.logFilePath(dataname, sim, query, rm_ratio, nquery);
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
					data[b][0] = (b+0.5)/nBucket;
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
				case '(':
					QueryNode[] qn_list = getQueryList(line);
					for(QueryNode qn: qn_list){
						if(avg){
							int b = (int) (qn.matching_sim*nBucket);
							if(b == nBucket){b--;}
							
							data[b][1]+= qn.cost;
							count[b]++;
						}else{
							cost.add((double) qn.cost);
							w_list.add(qn.matching_sim);
						}
					}
					break;
				default:
					v_continue = false;
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
					return null;
				}
				data = new double[cost.size()][2];
				for(int i = 0; i< data.length; i++){
					data[i][0] = w_list.get(i);
					data[i][1] = cost.get(i);
				}
			}
			return data;
		}catch(FileNotFoundException e){
			return null;
		}catch (IOException e) {
			System.out.println("Error string: "+line);
			e.printStackTrace();
			return null;
		}
	}

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
				case '(':
					QueryNode[] qn_list = getQueryList(line);
					for(QueryNode qn: qn_list){
						cost.add((double) qn.cost);
						diff_list.add(qn.diff);
					}
					break;
				default:
					v_continue = false;
					break;
				}
			}
			if(cost.size()==0){
				return null;
			}
			double[][] data = new double[cost.size()][2];
			for(int i = 0; i< data.length; i++){
				data[i][0] = diff_list.get(i);
				data[i][1] = cost.get(i);
			}
			return data;
		}catch(FileNotFoundException e){
			return null;
		}catch (IOException e) {
			System.out.println("Error string: "+line);
			e.printStackTrace();
			return null;
		}
	}
	private void plotIterCost(int sim, int query){
		double[] rmr_list = getRmrlist();
		
		for( double rmr:rmr_list){
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
					double [][] points = getIterCost(q, s, rmr, 100);
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

	private double[][] getIterCost(int query, int sim, double rm_ratio, int nquery) {
		String filename = Param.logFilePath(dataname, sim, query, rm_ratio, nquery);
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
				case '(':
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
				default:
					v_continue = false;
					break;
				}
			}
			if(cost.size()==0){
				return null;
			}
			double[][] data = new double[cost.size()][2];
			for(int i = 0; i< data.length; i++){
				data[i][0] = iter_list.get(i);
				data[i][1] = cost.get(i);
			}
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



