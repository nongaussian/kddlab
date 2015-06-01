package graph;

import java.util.Arrays;
import java.util.Random;

public class neighbors {
	static Random r = new Random();
	
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
		//if (size == 0) return;
		arr = new int[size];
		tmp = 0;
	}
	public void add_neighbor (int id) {
		arr[tmp++] = id;
	}
	
	public void remove(int target_id){
		boolean matched = false;
		for(int i = 0; i < size-1; i++){
			if(!matched){
				if(arr[i]==target_id){
					arr[i] = arr[i+1];
					matched = true;
				}
			}else{
				arr[i] = arr[i+1];
			}
		}
		size--;
		arr[size] = -1;
	}
	public int[] remove(double rm_ratio,int start){
		tmp = size;
		for(int i = 0; i < size; i++){
			if(arr[i]>=start&&r.nextDouble()<rm_ratio){
				arr[i] = -1-arr[i];
				tmp--;
			}
		}
		
		int[] rm_list = null;
		if(tmp<size){
			rm_list = new int[size-tmp];
			int[] arr_new = new int[tmp];
			tmp = 0;
			int idx_rm = 0;
			for(int i = 0; i < size; i++){
				if(arr[i]>=0){
					arr_new[tmp++] = arr[i];
				}else{
					rm_list[idx_rm++] = -arr[i]-1;
				}
			}
			size = tmp;
			arr = arr_new;
		}

		return rm_list;
	}
	
	public boolean join(neighbors n){
		if(this.size!=n.size){
			return false;
		}
		
		Arrays.sort(this.arr);
		Arrays.sort(n.arr);
		
		
		for(int i = 0; i<arr.length;i++){
			if(arr[i]!=n.arr[i]){
				return false;
			}
		}
		return true;
	}
}