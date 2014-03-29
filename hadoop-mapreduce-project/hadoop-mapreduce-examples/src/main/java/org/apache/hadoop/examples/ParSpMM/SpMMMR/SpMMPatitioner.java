package org.apache.hadoop.examples.ParSpMM.SpMMMR;

import org.apache.hadoop.mapreduce.Partitioner;
import org.ncsu.sys.SpMMMR.SpMMTypes.Key;
import org.ncsu.sys.SpMMMR.SpMMTypes.Value;

public class SpMMPatitioner extends Partitioner<SpMMTypes.Key, SpMMTypes.Value> {

	@Override
	public int getPartition(Key key, Value value, int numPartitions) {
		return (key.index1 * SpMMDriver.SPMM_PROC_GRID_DIMM_X + key.index3) % numPartitions;
	}
}
