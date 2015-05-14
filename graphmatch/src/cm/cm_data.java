package cm;

import graph.node;
import graph.nodelist;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

public class cm_data {
	
	public int ntype;
	public int nrel;
	public int totallnode;
	public int totalrnode;
	
	// names of types
	public String[] nodetypes = null;
	// names of relations
	public int[][] relations = null;
	// has a relation between two types? 
	public boolean[][] rel = null;
	// list of nodes of each type
	public nodelist[] lnodes = null;
	public nodelist[] rnodes = null;
	
	
	public static void main(String[] args){
		cm_data dat = new cm_data("data/tiny", "data/tiny", 0);
		
	}
	public void printStat(String dataname){
		
		
	}
	
	
	// dataname is the prefix to the files
	
	// <dataname>.actor
	//           .director
	//           .movie
	//           .movie_actor
	//           .movie_director
	public cm_data (String ldataname, String rdataname, double rm_ratio) {
		// XXX: meta files of left graph and right graph must be the same
		read_meta_file (ldataname + ".meta");
		
		// read left graph
		totallnode = 0;
		for (int i=0; i<ntype; i++) {
			read_node_file (lnodes, 
					i, 
					ldataname + "." + nodetypes[i]);
			totallnode += lnodes[i].size;
		}
		
		for (int i=0; i<nrel; i++) {
			read_relation_file (lnodes,
					ldataname + "." + nodetypes[relations[i][0]] + "_" + nodetypes[relations[i][1]],
					relations[i][0], relations[i][1]);
		}
		
		// read right graph
		totalrnode = 0;
		for (int i=0; i<ntype; i++) {
			read_node_file (rnodes, 
					i, 
					rdataname + "." + nodetypes[i]);
			totalrnode += rnodes[i].size;
		}
		
		for (int i=0; i<nrel; i++) {
			read_relation_file (rnodes,
					rdataname + "." + nodetypes[relations[i][0]] + "_" + nodetypes[relations[i][1]],
					relations[i][0], relations[i][1]);
		}
		
		// read label file
		for (int i=0; i<ntype; i++) {
			read_label_file(i,
					ldataname + ".label." + nodetypes[i]);
		}
		
		if(rm_ratio>0){
			removeActings(rm_ratio);
		}
		
	}
	
	public void removeActings(double rm_ratio){
		int t_movie = 0;
		int t_actor = 0;
		
		for(int t = 0; t < nodetypes.length ; t++){
			if (nodetypes[t].equals("movie")){
				t_movie = t;
			}else if(nodetypes[t].equals("actor")){
				t_actor = t;
			}
		}
		
		
		for(int i = 0; i<lnodes[t_movie].size; i++){
			node movie = lnodes[t_movie].arr[i];
			int[] remove_list = movie.neighbors[t_actor].remove(rm_ratio);
			if(remove_list!=null){
				for(int idx_actor:remove_list){
					lnodes[t_actor].arr[idx_actor].neighbors[t_movie].remove(i);
				}
			}
		}
		for(int i = 0; i<lnodes[t_actor].size; i++){
			node actor = lnodes[t_actor].arr[i];
			actor.trim(t_movie);
		}
	}
	

	private void read_label_file(int t, String filename) {
		try {
			File f = new File(filename);
			if (!f.exists()) return;
			
			System.out.println("read: " + filename);
			
			String line;
			BufferedReader reader = new BufferedReader(new FileReader(filename));
			
			// the second line lists the edge types
			while ((line = reader.readLine()) != null) {
				String[] arr = line.trim().split("\t", 2);
				if (arr == null || arr.length != 2) {
					System.err.println ("syntax error in meta file");
					System.exit(1);
				}
				int lidx = Integer.parseInt(arr[0]);
				int ridx = Integer.parseInt(arr[1]);
				
				lnodes[t].arr[lidx].label = ridx;
				rnodes[t].arr[ridx].label = lidx;
			}

			reader.close();
		}
		catch(IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	// read meta file
	// format of meta file:
	// e.g., imdb database
	//   movie	actor	director
	//   movie_actor	movie_director
	protected void read_meta_file(String filename) {
		System.out.println("read: " + filename);
		try {
			String line;
			BufferedReader reader = new BufferedReader(new FileReader(filename));

			// the first line lists the names of types
			if ((line = reader.readLine()) == null) {
				System.err.println ("syntax error in meta file");
				System.exit(1);
			}
			
			String[] arr = line.trim().split("\t");
			if (arr == null || arr.length == 0) {
				System.err.println ("syntax error in meta file");
				System.exit(1);
			}
			
			nodetypes = arr;
			ntype = arr.length;
			
			// allocate node list
			lnodes = new nodelist [ntype];
			for (int i=0; i<ntype; i++) lnodes[i] = new nodelist (i, ntype);
			
			rnodes = new nodelist [ntype];
			for (int i=0; i<ntype; i++) rnodes[i] = new nodelist (i, ntype);
			
			// the second line lists the edge types
			if ((line = reader.readLine()) == null) {
				System.err.println ("syntax error in meta file");
				System.exit(1);
			}
			
			arr = line.trim().split("\t");
			if (arr == null || arr.length == 0) {
				System.err.println ("syntax error in meta file");
				System.exit(1);
			}
			
			nrel = arr.length;
			relations = new int [nrel][2];
			
			rel = new boolean[ntype][ntype];
			for (int i=0; i<ntype; i++)
				for (int j=0; j<ntype; j++)
					rel[i][j] = false;
			
			for (int i=0; i<nrel; i++) {
				String[] tmp = arr[i].split("_");
				int id1 = Arrays.asList(nodetypes).indexOf(tmp[0]);
				int id2 = Arrays.asList(nodetypes).indexOf(tmp[1]);
				rel[id1][id2] = true;
				rel[id2][id1] = true;
				relations[i][0] = id1;
				relations[i][1] = id2;
			}

			reader.close();
		}
		catch(IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	// XXX: we simply count the entries of each type only for now
	private void read_node_file(nodelist[] nodes, int type, String filename) {
		System.out.println("read: " + filename);
		try {
			String line;
			BufferedReader reader = new BufferedReader(new FileReader(filename));

			while((line = reader.readLine()) != null) {
				String[] arr = line.trim().split("\t", 2);
				if (arr.length != 2) {
					System.err.println ("[warn] arr.length != 2: " + line);
					System.exit(1);
				}
				//int oid = Integer.parseInt(arr[0]);
				//int aid = Integer.parseInt(arr[1]);
				
				nodes[type].increase_count();
			}
			
			System.out.println (nodetypes[type] + ": " + nodes[type].size);
			reader.close();
			
			nodes[type].alloc();
			
			reader = new BufferedReader(new FileReader(filename));

			while((line = reader.readLine()) != null) {
				String[] arr = line.trim().split("\t", 2);
				if (arr.length != 2) {
					System.err.println ("[warn] arr.length != 2: " + line);
					System.exit(1);
				}
				//int oid = Integer.parseInt(arr[0]);
				int aid = Integer.parseInt(arr[1]);
				
				nodes[type].arr[aid] = new node(aid, ntype, rel[type]);
			}
			reader.close();
		}
		catch(IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private void read_relation_file(nodelist[] nodes, String filename, int type1, int type2) {
		System.out.println("read: " + filename);
		try {
			String line;
			BufferedReader reader = new BufferedReader(new FileReader(filename));

			while((line = reader.readLine()) != null) {
				String[] arr = line.trim().split("\t", 2);
				if (arr.length != 2) {
					System.err.println ("[warn] arr.length != 2: " + line);
					System.exit(1);
				}
				int id1 = Integer.parseInt(arr[0]);
				int id2 = Integer.parseInt(arr[1]);
				
				nodes[type1].arr[id1].increase_neighbor_count(type2);
				nodes[type2].arr[id2].increase_neighbor_count(type1);
			}

			reader.close();
			
			for (int i=0; i<nodes[type1].size; i++) {
				nodes[type1].arr[i].alloc(type2);
			}
			
			for (int i=0; i<nodes[type2].size; i++) {
				nodes[type2].arr[i].alloc(type1);
			}

			reader = new BufferedReader(new FileReader(filename));
			
			while((line = reader.readLine()) != null) {
				String[] arr = line.trim().split("\t", 2);
				if (arr.length != 2) {
					System.err.println ("[warn] arr.length != 2: " + line);
					System.exit(1);
				}
				int id1 = Integer.parseInt(arr[0]);
				int id2 = Integer.parseInt(arr[1]);
				
				nodes[type1].arr[id1].neighbors[type2].add_neighbor(id2);
				nodes[type2].arr[id2].neighbors[type1].add_neighbor(id1);
			}

			reader.close();
		}
		catch(IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public void print_data (nodelist[] nodes) {
		// print relations
		for (int i=0; i<nrel; i++) {
			System.out.println("" + nodetypes[relations[i][0]] + "_" + nodetypes[relations[i][1]]);
			
			int type1 = relations[i][0];
			int type2 = relations[i][1];
			
			int totalline = 0;
			int line = 0;
			for (int j=0; j<nodes[type1].size; j++) {
				int node1 = nodes[type1].arr[j].id;
				
				for (int k=0; k<nodes[type1].arr[j].neighbors[type2].size
						&& line<100; k++, line++) {
					System.out.println("" + node1 + "\t" + nodes[type1].arr[j].neighbors[type2].arr[k]);
				}
				totalline += nodes[type1].arr[j].neighbors[type2].size;
				
				if (nodes[type1].arr[j].label >= 0) {
					System.out.println("" + node1 + " -> " + nodes[type1].arr[j].label);
				}
			}
			
			System.out.println("size = " + totalline);
		}
	}
	
	public void print_data () {
		System.out.println("left graph:");
		print_data(lnodes);
		System.out.println("right graph:");
		print_data(rnodes);
	}
}
