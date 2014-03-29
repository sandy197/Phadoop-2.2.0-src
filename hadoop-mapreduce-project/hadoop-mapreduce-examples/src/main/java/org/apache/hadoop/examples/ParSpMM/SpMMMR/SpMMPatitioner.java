package org.apache.hadoop.examples.ParSpMM.SpMMMR;

import org.apache.hadoop.examples.ParSpMM.SpMMMR.SpMMTypes.Key;
import org.apache.hadoop.examples.ParSpMM.SpMMMR.SpMMTypes.Value;
import org.apache.hadoop.mapreduce.Partitioner;


public class SpMMPatitioner extends Partitioner<SpMMTypes.Key, SpMMTypes.Value> {

	@Override
	public int getPartition(Key key, Value value, int numPartitions) {
		return (key.index1 * SpMMDriver.SPMM_PROC_GRID_DIMM_X + key.index3) % numPartitions;
	}
}
