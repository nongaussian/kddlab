package cm;

import graph.neighbors;
import graph.node;
import graph.nodelist;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

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
	
	
	public static void main(String[] args) throws IOException{
		
		cm_data dat = new cm_data("data/tiny_2_l", "data/tiny_2_r");
		dat.stat("data/tiny");
		//cm_data dat = new cm_data("data/ex_4_l", "data/ex_4_r");
		//dat.stat("data/ex_4");
		/*
		cm_data dat = new cm_data("data/tiny", "data/tiny");
		//dat.stat("data/tiny");
		dat.removeActings(0.02);
		dat.saveGraph(dat.lnodes, "tiny_2_r");
		dat.saveGraph(dat.rnodes, "tiny_2_l");
		//*/
	}
	public void printStat(String dataname){
		
		
	}
	
	
	// dataname is the prefix to the files
	
	// <dataname>.actor
	//           .director
	//           .movie
	//           .movie_actor
	//           .movie_director
	public cm_data (String ldataname, String rdataname) {
		// XXX: meta files of left graph and right graph must be the same
		read_meta_file (ldataname + "/meta");
		
		// read left graph
		totallnode = 0;
		for (int i=0; i<ntype; i++) {
			read_node_file (lnodes, 
					i, 
					ldataname + "/" + nodetypes[i]);
			totallnode += lnodes[i].size;
		}
		
		for (int i=0; i<nrel; i++) {
			read_relation_file (lnodes,
					ldataname + "/" + nodetypes[relations[i][0]] + "_" + nodetypes[relations[i][1]],
					relations[i][0], relations[i][1]);
		}
		
		// read right graph
		totalrnode = 0;
		for (int i=0; i<ntype; i++) {
			read_node_file (rnodes, 
					i, 
					rdataname + "/" + nodetypes[i]);
			totalrnode += rnodes[i].size;
		}
		
		for (int i=0; i<nrel; i++) {
			read_relation_file (rnodes,
					rdataname + "/" + nodetypes[relations[i][0]] + "_" + nodetypes[relations[i][1]],
					relations[i][0], relations[i][1]);
		}
		
		// read label file
		for (int i=0; i<ntype; i++) {
			read_label_file(i,
					ldataname + "/label." + nodetypes[i]);
		}
		
		for (int t = 0; t<ntype;t++){
			lnodes[t].init();
			rnodes[t].init();
		}
	}
	
	public void stat(String dataname){
		System.out.println("Original");
		nodelist[] nodes = new nodelist[ntype];
		for (int t = 0; t<ntype;t++){
			nodes[t] = new nodelist(t, ntype);
		}
		for (int i=0; i<ntype; i++) {
			read_node_file (nodes, 
					i, 
					dataname + "/" + nodetypes[i]);
		}
		
		for (int i=0; i<nrel; i++) {
			read_relation_file (nodes,
					dataname + "/" + nodetypes[relations[i][0]] + "_" + nodetypes[relations[i][1]],
					relations[i][0], relations[i][1]);
		}
		for (int t = 0; t<ntype;t++){
			nodes[t].init();
		}
		stat(nodes);
		System.out.println("L");
		stat(this.lnodes);
		System.out.println("R");
		stat(this.rnodes);
	}
	public void stat(nodelist[] nodes){
		for (int t = 0; t<ntype;t++){
			System.out.printf("#%s: %d\n",nodetypes[t],nodes[t].size_real);
		}
		
		System.out.printf("# indistinquishable: %d\n",indistinguishableness(nodes,ntype,rel,nodetypes));
	}
	
	private static int indistinguishableness(nodelist[] nodes,int ntype,boolean [][]rel,String[] nodetypes){
		int n_indis = 0;
		for (int t = 0; t<ntype;t++){
			int tot_size =0;
			Int2ObjectOpenHashMap<ArrayList<node>> degree_node_map = new Int2ObjectOpenHashMap<ArrayList<node>>();
			for (int i=0; i<nodes[t].size; i++) {
				int degree = nodes[t].arr[i].getNeighorCount();
				ArrayList<node> list =	degree_node_map.get(degree);
				if(list==null){
					list = new ArrayList<node>();
				}
				list.add(nodes[t].arr[i]);
				degree_node_map.put(degree, list);
			}
			ObjectIterator<Entry<ArrayList<node>>> iter= degree_node_map.int2ObjectEntrySet().fastIterator();
		
			HashSet<Integer> indistinquishable_set = new HashSet<Integer>();
			while(iter.hasNext()){
				Entry<ArrayList<node>> entry = iter.next();
				node[] cands = entry.getValue().toArray(new node[0]);
				tot_size +=cands.length;
				
				for(int i = 0; i <cands.length; i++){
					if(cands[i].no_neighbor)
						continue;
					node n_i = cands[i];
					neighbors[] i_neighbors = n_i.neighbors;
					for(int j = i+1; j < cands.length; j++){
						if(cands[j].no_neighbor)
							continue;
						node n_j = cands[j];
						/*if((n_i.id==4988||n_j.id==4988)&&t==2){
							System.out.printf("%d\n", n_j.id);
						}*/
						boolean indis = true;						
						neighbors[] j_neighbors = n_j.neighbors;
						for(int s = 0; s<ntype; s++){
							if(!rel[t][s]){
								continue;
							}
							if(!i_neighbors[s].join(j_neighbors[s])){
								indis = false;
								break;
							}
						}
						if(indis){
							indistinquishable_set.add(n_i.id);
							indistinquishable_set.add(n_j.id);
							//System.out.printf("%s: %d,%d\n",nodetypes[t],n_i.id,n_j.id);
						}
					}
				}
			}
			System.out.printf("%s  indis: %d\n",nodetypes[t],indistinquishable_set.size());
			n_indis += indistinquishable_set.size();
			System.out.println(tot_size);
		}
		return n_indis;
		
	}
	
	public void saveGraph(nodelist[] nodes, String dataname) throws IOException{
		File dir = new File("data/"+dataname);
		dir.mkdir();
		
		saveMeta(nodes, dir);
		saveVertices(nodes, dir);
		saveEdges(nodes, dir);
		
	}
	
	public void saveMeta(nodelist[] nodes,File dir) throws IOException{
		PrintWriter pw = getPrintWriter(dir, "meta");
		//save nodetypes
		for(int t =0; t < ntype; t++){
			pw.print(nodetypes[t]);
			if(t<ntype-1){
				pw.print("\t");
			}else{
				pw.println();
			}
		}
		
		//save edge types
		
		boolean first = true;
		for(int t =0; t < ntype; t++){
			for(int s = t; s < ntype; s++){
				if(!rel[t][s]){
					continue;
				}
				if(first){
					first = false;
				}else{
					pw.print("\t");
				}
				pw.print(nodetypes[t]+"_"+nodetypes[s]);
			}
		}
		pw.close();
	}
	public void saveVertices(nodelist[] nodes, File dir) throws IOException{
		for(int t =0; t < ntype; t++){
			PrintWriter pw = getPrintWriter(dir, nodetypes[t]);
			for(int i = 0; i < nodes[t].size; i++){
				pw.printf("%d\t%d\n",nodes[t].arr[i].id,i);
			}		
			pw.close();
		}
	}
	public void saveEdges(nodelist[] nodes, File dir) throws IOException{
		for(int t =0; t < ntype; t++){
			for(int s = t; s < ntype; s++){
				if(!rel[t][s]){
					continue;
				}
				PrintWriter pw = getPrintWriter(dir, nodetypes[t]+"_"+nodetypes[s]);
				for(int i = 0; i < nodes[t].size; i++){
					for(int j: nodes[t].arr[i].neighbors[s].arr){
						if(t!=s||j>=i){
							pw.printf("%d\t%d\n", i,j);
						}
					}
				}
				pw.close();
			}
		}
	}
	
	public static PrintWriter getPrintWriter(File dir,String filename) throws IOException{
		String filepath = dir.getPath()+'/'+filename;
		PrintWriter bw = new PrintWriter(new BufferedWriter(new FileWriter(new File(filepath))));
		return bw;
	}
	
	public static void removeEdges(nodelist[] nodes, int t, int s, double rm_ratio){
		for(int i = 0; i<nodes[t].size; i++){
			node node_t = nodes[t].arr[i];
			int[] remove_list = node_t.neighbors[s].remove(rm_ratio,t==s?(i+1):0);
			if(remove_list!=null){
				for(int idx_s:remove_list){
					nodes[s].arr[idx_s].neighbors[t].remove(i);
				}
			}
		}
		for(int i = 0; i<nodes[t].size; i++){
			node node_t = nodes[t].arr[i];
			node_t.trim(s);
		}
	}
	public void removeActings(double rm_ratio){
		int t_movie = 0;
		int t_actor = 0;
		int t_director = 0;
		
		for(int t = 0; t < nodetypes.length ; t++){
			if (nodetypes[t].equals("movie")){
				t_movie = t;
			}else if(nodetypes[t].equals("actor")){
				t_actor = t;
			}else if(nodetypes[t].equals("director")){
				t_director = t;
			}
		}
		removeEdges(lnodes,t_movie, t_actor, rm_ratio);
		removeEdges(rnodes,t_movie, t_actor, rm_ratio);
		
		
		/*
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
		*/
		for(int i = 0; i<lnodes[t_actor].size; i++){
			node actor = lnodes[t_actor].arr[i];
			if(actor.neighbors[t_movie].size==0){
				System.out.println("actor 0");
			}
		}
		for(int i = 0; i<lnodes[t_movie].size; i++){
			node movie = lnodes[t_movie].arr[i];
			if(movie.neighbors[t_actor].size==0){
				System.out.println("movie a0:"+i);
			}
			if(movie.neighbors[t_director].size==0){
				System.out.println("movie d0:"+i);
			}
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
				
				lnodes[t].arr[lidx].annotate(ridx);
				rnodes[t].arr[ridx].annotate(lidx);
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
				
				if (nodes[type1].arr[j].getNAnnotatios() >= 0) {
					//TODO print all annotations?
					System.out.println("" + node1 + " -> " + nodes[type1].arr[j].getLastLabel());
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
