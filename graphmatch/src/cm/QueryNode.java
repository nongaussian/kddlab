package cm;

public class QueryNode implements Comparable<QueryNode> {
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
	@Override
	public int compareTo(QueryNode o) {
		if(t==o.t){
			return id-o.id;
		}
		return t-o.t;
	}
	
}