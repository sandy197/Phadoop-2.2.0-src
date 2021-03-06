package org.apache.hadoop.examples.ParSpMM.SpMMMR;

import org.apache.hadoop.examples.ParSpMM.SpMMMR.SpMMTypes.Key;
import org.apache.hadoop.examples.ParSpMM.SpMMMR.SpMMTypes.Value;
import org.apache.hadoop.mapreduce.Partitioner;

public class SpMMPartitioner2 extends Partitioner<Key, Value> {

	@Override
	public int getPartition(Key key, Value value, int numPartitions) {
		System.out.println("Partition count : " + numPartitions);
		//strategy 2
		if(key.m == 0) //A
			return (key.index2 * SpMMDriver.SPMM_PROC_GRID_DIMM_X + key.index3) % numPartitions;
		else
			return (key.index1 * SpMMDriver.SPMM_PROC_GRID_DIMM_X + key.index3) % numPartitions;
	}

	

}
