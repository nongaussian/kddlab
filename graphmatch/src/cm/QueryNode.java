package cm;

public class QueryNode {
	public int t;
	public int id;
	public double diff;
	public QueryNode (int t, int i){
		this.t = t;
		this.id = i;
	}
	public QueryNode (int t, int i, double diff) {
		this(t,i);
		this.diff = diff;
	}
}