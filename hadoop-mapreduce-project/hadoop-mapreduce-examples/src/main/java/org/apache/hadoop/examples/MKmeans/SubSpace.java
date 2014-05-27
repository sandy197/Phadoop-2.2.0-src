package org.apache.hadoop.examples.MKmeans;

import java.util.ArrayList;
import java.util.List;

public class SubSpace {
	private int id;
	private int capacity;
	private List<Value> vectors;
	
	public SubSpace(){
		this.vectors = new ArrayList<Value>();
	}
	
	public SubSpace(int id, int capacity){
		this.id = id;
		this.capacity = capacity;
		this.vectors = new ArrayList<Value>();
	}
	
	public int getId() {
		return id;
	}
//
//	public void setId(int id) {
//		this.id = id;
//	}

	public int getCapacity() {
		return capacity;
	}
	
	public List<Value> getVectors() {
		return vectors;
	}
	
	public void setVectors(List<Value> vectors) {
		this.vectors = vectors;
	}
	
	/**
	 *	Inserts the element if the subspace is not full. 
	 * 
	 * @return true		If inserted successfully
	 * 			false	If the capacity is full
	 */
	public boolean offer(Value val){
		boolean isInserted = false;
		if(this.vectors.size() < this.capacity){
			this.vectors.add(val);
			isInserted = true;
		}
		return isInserted;
	}
	
	public boolean isFull(){
		return this.vectors.size() == this.capacity;
	}
}
