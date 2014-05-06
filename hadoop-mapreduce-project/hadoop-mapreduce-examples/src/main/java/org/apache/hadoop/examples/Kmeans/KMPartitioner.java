package org.apache.hadoop.examples.Kmeans;

import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.examples.Kmeans.KMTypes.Key;
import org.apache.hadoop.examples.Kmeans.KMTypes.Value;

public class KMPartitioner extends Partitioner<Key, Value> {

	@Override
	public int getPartition(Key key, Value value, int reduceTaskCount) {
		return key.getTaskIndex() % reduceTaskCount;
	}

}
