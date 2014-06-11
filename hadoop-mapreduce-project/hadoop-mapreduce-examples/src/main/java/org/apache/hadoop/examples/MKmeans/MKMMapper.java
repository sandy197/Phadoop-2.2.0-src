package org.apache.hadoop.examples.MKmeans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapred.RAPLRecord;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer.Context;
import org.apache.hadoop.examples.MKmeans.MKMTypes.Values;
import org.apache.hadoop.examples.MKmeans.MKMTypes.VectorType;

public class MKMMapper extends Mapper<Key, Values, IntWritable, PartialCentroid> {
	
	private static final boolean DEBUG = true;
	private int dimension;
	private int k;
	private int R1;
	private boolean isCbuilt, isVbuilt;
	private List<Value> centroids, vectors;
	private RAPLRecord record;
	
	public void setup (Context context) {
		init(context);
		Configuration conf = context.getConfiguration();
		record = context.getRAPLRecord();
		//TODO : JNI call to set the power cap based on the target task time and
	    // the previous execution time.
		//TODO : Decide if this has to be done in setup or the mapTask 
		//since the rapl record has the information about the package already
		// 
	    ThreadPinning rapl = new ThreadPinning(record);
	    rapl.adjustPower(record);
		//read centroids
		//Change this section for Phadoop version
//		FileSystem fs;
//		try {
//			fs = FileSystem.get(conf);
//			Path path = new Path(conf.get("KM.inputCenterPath"));
//			Path filePath = fs.makeQualified(path);
//			centroids = MKMUtils.getCentroidsFromFile(filePath, false);
//			if(centroids == null){
//				throw new IOException("No centroids fetched from the file");
//			}
//			isCbuilt = true;
//			if(DEBUG) System.out.println("************Centroids Read form file*************");
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}
	
	private void init(Context context) {
		Configuration conf = context.getConfiguration();
		dimension = conf.getInt("KM.dimension", 2);
		k = conf.getInt("KM.k", 6);
		R1 = conf.getInt("KM.R1", 6);
//		centroids = new ArrayList<Value>();
//		vectors = new ArrayList<Value>();
		isCbuilt = isVbuilt = false;
	}

	public void map(Key key, Values values, Context context)
			throws IOException, InterruptedException {
		long start2 =System.nanoTime();
		PartialCentroid[] partialCentroids = null;
		if(key.getType() == org.apache.hadoop.examples.MKmeans.MKMTypes.VectorType.CENTROID && !isCbuilt){
			centroids = buildCentroids(values);
			isCbuilt = true;
		}
		else{
			//use build and set here
			vectors = buildCentroidsAndSet(values, context);
			isVbuilt = true;
			//buildVectors(values, vectors);
		}
		
		if(vectors == null && context.getMatrix() != null){
  			if(DEBUG) System.out.println("##Getting vectors already read from fs. Skipping reading the file");
  			MKMRowListMatrix vectorList = (MKMRowListMatrix) context.getMatrix();
  			vectors = vectorList.getMatrix();
  			isVbuilt = true;
  		}
		
		if(isCbuilt && isVbuilt){
			try{
				if(DEBUG) System.out.println("Classifying " + vectors.size() + " vectors among " + centroids.size() + " clusters" );
				System.out.println("$$VectorCount:"+"\t"+vectors.size());
				long start1 =System.nanoTime();
				partialCentroids = (PartialCentroid[]) classify(vectors, centroids);
				long end1 =System.nanoTime();
				System.out.println("$$ClassifyTime:"+"\t" + (end1-start1));
				if(record == null){
					//NOTE : this doesn't work if the classify is done more than once per map task
					record = new RAPLRecord();
				}
				record.setExectime(end1 - start1);
				//TODO:replace the hardcoded value with a JNI call to get the core this thread is pinned to
				record.setPkg((short)1);
				//TODO : add hostname to record either here or in the appmaster (this info is readily available there)
//				record.setHostname(hostname);
				context.setRAPLRecord(record);
				for(PartialCentroid pcent : partialCentroids){
					IntWritable newKey = new IntWritable(pcent.getCentroidIdx());
					context.write(newKey, pcent);
					if(DEBUG) printMapOutput(newKey, pcent);
				}
				long end2 =System.nanoTime();
				System.out.println("MapTime:"+"\t" + (end2-start2));
			}
			catch(Exception ex){
				ex.printStackTrace();
			}
		}
		
	}
	
	private void printMapOutput(IntWritable newKey, PartialCentroid pcent) {
		StringBuilder sb = new StringBuilder();
		sb.append("##### Map output: (" + newKey.get() + ") (" 
					+ pcent.getDimension() + "," + pcent.getCentroidIdx() + "," + pcent.getCount() + "\n");
		for(int coord : pcent.getCoordinates()){
			sb.append(coord + ",");
		}
		sb.append(") ");
		System.out.println(sb.toString());
	}

	private List<Value> buildCentroids(Values values) {
		List<Value> centroidsLoc = new ArrayList<Value>();
		for(Value val : values.getValues()){
			if(DEBUG) System.out.println("Adding value :" + val);
			Value valCopy = VectorFactory.getInstance(VectorType.REGULAR);
			valCopy.copy(val);
			centroidsLoc.add(valCopy);
		}
		return centroidsLoc;
		
	}
	
	private List<Value> buildCentroidsAndSet(Values values, Context context) {
		List<Value> centroidsLoc = buildCentroids(values);
		context.setMatrix(new MKMRowListMatrix(centroidsLoc));
		return centroidsLoc;
	}
	
	private PartialCentroid[] classify(List<Value> vectors2, List<Value> centroids2) throws Exception {
		PartialCentroid[] partialCentroids = new PartialCentroid[centroids2.size()];
		
		Hashtable<Integer, Value> pCentMapping = new Hashtable<Integer, Value>(); 
		for(Value pcent : centroids2){
			pCentMapping.put(pcent.getCentroidIdx(), pcent);
		}
			
		for(Value point : vectors2){
			int idx = getNearestCentroidIndex(point, centroids2);
			if(partialCentroids[idx] == null){
				partialCentroids[idx] = (PartialCentroid)VectorFactory.getInstance(VectorType.PARTIALCENTROID, point.getDimension());
				pCentMapping.remove(idx);
			}
			partialCentroids[idx].addVector(point);
			if(partialCentroids[idx].getCentroidIdx() == MKMTypes.UNDEF_VAL){
				partialCentroids[idx].setCentroidIdx(idx);
			}
			else {
				if(partialCentroids[idx].getCentroidIdx() != idx)
					if(DEBUG) throw new Exception("Fatal: Inconsistent cluster, multiple centroids problem!");
			}
		}
		if(pCentMapping.keySet()!= null && !pCentMapping.keySet().isEmpty()){
			for(Integer key : pCentMapping.keySet()){
				partialCentroids[key] = (PartialCentroid)VectorFactory.getInstance(VectorType.PARTIALCENTROID);
				partialCentroids[key].copy(pCentMapping.get(key));
			}
		}
		return partialCentroids;
	}
	
	private int getNearestCentroidIndex(Value point, List<Value> centroids2) {
		int nearestCidx = -1;
		int shortestDistance = Integer.MAX_VALUE;
		for(Value centroid : centroids2){
			int distance = MKMUtils.getDistance(point, centroid);
			if(distance < shortestDistance){
				nearestCidx  = centroid.getCentroidIdx();
				shortestDistance = distance;
			}
		}
		return nearestCidx;
	}
}
