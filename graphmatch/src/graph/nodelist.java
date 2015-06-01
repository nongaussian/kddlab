package graph;

public class nodelist {
	public int type;
	public int ntype;
	public int size;
	public int size_real; //number of nodes who has at least a neighbor
	public node[] arr = null;
	public nodelist (int type, int ntype) {
		this.type = type;
		this.ntype = ntype;
		this.size = 0;
	}
	public void increase_count() {
		size++;
	}
	public void alloc() {
		assert size > 0 : "node type " + type + " has no instance";
		arr = new node[size];
	}
	
	public void init(){
		size_real = 0;
		for(node d:arr){
			d.init();
			if(!d.no_neighbor){
				size_real++;
			}
		}
	}
	
	
}
