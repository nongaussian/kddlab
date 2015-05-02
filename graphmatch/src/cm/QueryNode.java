package cm;

public class QueryNode implements Comparable<QueryNode> {
	public int t;
	public int id;
	public double diff;
	
	//public CandNode knownlink;
	public int matched_id = -1;
	public QueryNode (int t, int i){
		this.t = t;
		this.id = i;
	}
	public QueryNode (int t, int i, double diff) {
		this(t,i);
		this.diff = diff;
	}
	public void setKnownLink(CandNode cn){
		if(cn==null){
			matched_id = -1;
			return;
		}
		matched_id = cn.id;
	}
	
	@Override
	public int compareTo(QueryNode o) {
		if(t==o.t){
			return id-o.id;
		}
		return t-o.t;
	}
	
	@Override
	public String toString(){
		return String.format("t:%d, id:%d, diff:%f", t,id,diff);
	}
}