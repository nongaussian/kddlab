package gm.data;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;

import cm.cm_data;

import sim.Param;

public class DataGenerator {
	//String graphname;
	int n_node;
	int n_label;
	double density;
	File dir;
	
	
	public static void main(String[] args){
		int n_node = 500;
		double rmr = .05;
		generate_files(n_node, rmr);
		//printDegrees(0.05);
	}
	
	
	
	public static void printDegrees(int n_node, double density){
		cm_data dat = new cm_data(getfilepath(n_node,density), getfilepath(n_node,density));
		
		for(int i =0; i<dat.lnodes[0].size; i++){
			System.out.printf("%d\t%d\n", i, dat.lnodes[0].arr[i].neighbors[0].size);
		}
	}
	public static String getfilepath(int n_node, double density){
		return "data/"+getfilename(n_node, density);
	}
	public static String getfilename(int n_node, double density){
		return "Syn"+n_node+"_"+Param.double2String_filename(density);
	}
	public static void generate_files(int n_node,double rmr){
		//double[] density_list = {0.05,0.1,0.2};
		double[] density_list = {0.005};//,0.01,0.03,0.05};
		
		for(double density:density_list){
			DataGenerator dg = new DataGenerator(n_node, density, getfilepath(n_node,density),1);
			try {
				dg.generate();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			cm_data dat = new cm_data(getfilepath(n_node, density), getfilepath(n_node, density));
			cm_data.removeEdges(dat.lnodes, 0, 0, rmr);
			cm_data.removeEdges(dat.rnodes, 0, 0, rmr);
			
			try {
				dat.saveGraph(dat.rnodes, getfilename(n_node, density)+"_5_r");
				dat.saveGraph(dat.lnodes, getfilename(n_node, density)+"_5_l");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
	}
	public DataGenerator(int n_node, double density, String filename,int n_label){
		//this.graphname = filename;
		this.n_node = n_node;
		this.density = density;
		this.n_label = n_label;
		dir = new File(filename);
		dir.mkdir();
	}
	
	public void generate() throws IOException{
		make_meta();
		make_vertices();
		make_edges();
		make_labels();
	}
	
	public PrintWriter getPrintWriter(String filename) throws IOException{
		PrintWriter bw = new PrintWriter(new BufferedWriter(new FileWriter(new File(dir, filename))));
		return bw;
	}
	public void make_meta() throws IOException{
		PrintWriter pw = getPrintWriter("meta");
		pw.println("vertex");
		pw.println("vertex_vertex");
		pw.close();
	}
	public void make_vertices() throws IOException{
		PrintWriter bw = getPrintWriter("vertex");
		for(int i = 0; i < n_node; i++){
			bw.printf("%d\t%d\n", i,i);
		}
		bw.close();
	}
	
	public void make_edges() throws IOException{
		Random r = new Random();
		PrintWriter bw = getPrintWriter( "vertex_vertex");
		
		
		for(int i = 0; i < n_node; i++){
			for (int j = i+1; j <n_node; j++){
				if(r.nextDouble()>=density){
					continue;
				}
				bw.printf("%d\t%d\n", i,j);
			}
		}
		bw.close();
		
	}
	public void make_labels() throws IOException{
		PrintWriter pw = getPrintWriter("label.vertex");
		
		for(int i =0; i<n_label; i++){
			pw.printf("%d\t%d\n", i,i);
		}
		
		pw.close();
	}
}
