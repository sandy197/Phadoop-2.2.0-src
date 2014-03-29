package org.apache.hadoop.examples.ParSpMM.SpMM;

import org.ncsu.sys.SpMMMR.SpMMTypes.Pair;

public class StackEntry{
	public int value;
	public Pair key;
	
	public StackEntry(int value, Pair key){
		this.value = value;
		this.key = key;
	}
}
