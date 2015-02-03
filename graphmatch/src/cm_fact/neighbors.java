package cm_fact;

public class neighbors {
	public int type;
	public int size;
	public int tmp;
	public int[] arr = null;
	public neighbors (int type) {
		this.type = type;
		this.size = 0;
		this.tmp = 0;
	}
	public void increase_count() {
		size++;
	}
	public void alloc () {
		if (size == 0) return;
		arr = new int[size];
		tmp = 0;
	}
	public void add_neighbor (int id) {
		arr[tmp++] = id;
	}
}