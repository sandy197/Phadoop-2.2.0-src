package org.apache.hadoop.examples.ParSpMM.SpMM;

import java.util.Comparator;


public class HeapEntry {
	int key;
	int runr;
	int num;
	
	public HeapEntry(int key, int runr, int num){
		this.key = key;
		this.runr = runr;
		this.num = num;
	}
}

class HeapEntryComp implements Comparator<HeapEntry>{

	@Override
	public int compare(HeapEntry o1, HeapEntry o2) {
		if(o1.key < o2.key)
			return -1;
		else if(o1.key > o2.key)
			return 1;
		else
			return 0;
	}
	
}


