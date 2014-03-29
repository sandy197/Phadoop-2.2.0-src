package org.apache.hadoop.examples.ParSpMM.SpMM;

import java.util.List;
import java.util.Map.Entry;

public abstract class SpMat {
	public final int m;
	public final int n;
	
	public SpMat(int m, int n){
		this.m = m;
		this.n = n;
	}
	
	public abstract List<StackEntry> SpMatMultiply(SpMat B);
	public abstract SpMat SpMatMerge(SpMat B);
}

