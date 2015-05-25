package cm;

public class QueryNode implements Comparable<QueryNode> {
	public int t;
	public int id;
	public double diff;
	
	
	//matching information
	public int cost = 0;
	public double matching_sim  = 0.0;	//Similarity of the query node and the matched node
	public double max_sim		= 0.0;	//Maximum similarity
	public double entropy		= 0.0;	//Entropy of similarity distribution
	public int 	n_annotates		= 0	 ;	//Number of annotations
	
	public int matched_id = -1;
	public QueryNode (int t, int i){
		this.t = t;
		this.id = i;
	}
	public QueryNode (int t, int i, double diff) {
		this(t,i);
		this.diff = diff;
	}
	public void initKnownLink(){
		setKnownLink(0, null, 0);
	}
	public void setKnownLink(int i, CandNode cn, int n_annotates){
		if(cn==null){
			matched_id = -1;
			cost = 0;
			matching_sim = 0.0;
			return;
		}
		matched_id = cn.id;
		cost = i+1;
		matching_sim = cn.w;
		this.n_annotates = n_annotates+1;
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