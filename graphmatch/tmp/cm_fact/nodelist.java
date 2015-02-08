package cm_fact;

public class nodelist {
	public int type;
	public int ntype;
	public int size;
	
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
}
