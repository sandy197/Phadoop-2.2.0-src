package org.apache.hadoop.examples.Kmeans;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.apache.hadoop.examples.Kmeans.KMTypes.Key;
import org.apache.hadoop.examples.Kmeans.KMTypes.Value;
import org.apache.hadoop.examples.Kmeans.KMTypes.VectorType;

public class KMMapper extends Mapper<Key, Value, Key, Value> {
	
	private static boolean DEBUG = true;
	
	private int dimension;
	private int R1;
	private int centroidIdxSeq;
	
	public void setup (Context context) {
		init(context);
	}

	private void init(Context context) {
		Configuration conf = context.getConfiguration();
		dimension = conf.getInt("KM.dimension", 2);
		R1 = conf.getInt("KM.R1", 4);
		centroidIdxSeq = 0;
	}

	// TODO : set the input path to only files containing 
	// newcentroids from the second iteration
	
	public void map(Key ikey, Value ivalue, Context context)
			throws IOException, InterruptedException {
		Key newKey = ikey;
		if(ikey.getType() == VectorType.REGULAR){
			if(context.getTaskAttemptID().getTaskID().getId() == 0)
			context.write(newKey, ivalue);
			if(DEBUG) printMapOutput(newKey, ivalue);
		}
		if(ikey.getType() == VectorType.CENTROID){
			//send it to all reduce tasks
			int centroidId = centroidIdxSeq++;
			for(int i = 0; i < R1; i++){
				//ivalue.setCentroidIdx(centroidId);
				newKey = new Key(i, ikey.getType());
				context.write(new Key(i, ikey.getType()), ivalue);
				if(DEBUG) printMapOutput(newKey, ivalue);
			}
		}
		//context.write(newKey, ivalue);
		//if(DEBUG) printMapOutput(newKey, ivalue);
	}
	
	private void printMapOutput (Key key, Value value) {
		StringBuilder sb = new StringBuilder();
		sb.append("##### Map output: (" + key.getTaskIndex() + "," + 
			key.getType() + ") (" + value.getDimension() + "," + value.getCentroidIdx() + "," + value.getCount() + "\n");
		for(int coord : value.getCoordinates()){
			sb.append(coord + ",");
		}
		sb.append(") ");
		System.out.println(sb.toString());
	}
}
