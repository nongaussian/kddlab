package cm;

public class node {
	public int id;
	
	public int label = -1;
	
	
	
	public neighbors[] neighbors = null;
	
	public node (int id, int ntype, boolean[] rel) {
		this.id = id;
		this.neighbors = new neighbors[ntype];
		for (int i=0; i<ntype; i++) {
			if (rel[i])
				neighbors[i] = new neighbors(i);
			else
				neighbors[i] = null;
		}
	}
	public void increase_neighbor_count(int type) {
		neighbors[type].increase_count();
	}
	public void alloc(int type) {
		neighbors[type].alloc();
	}
	public void add_neighbor(int type, int id) {
		neighbors[type].add_neighbor(id);
	}
}