package org.apache.hadoop.examples.MKmeans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.ipc.RAPLCalibration;
import org.apache.hadoop.mapred.RAPLRecord;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer.Context;
import org.apache.hadoop.examples.MKmeans.MKMTypes.Values;
import org.apache.hadoop.examples.MKmeans.MKMTypes.VectorType;
import org.ncsu.sys.*;

public class MKMMapper extends Mapper<Key, Values, IntWritable, PartialCentroid> {
	
	public static int CORES_PER_PKG = 8;
	
	private static final boolean DEBUG = true;
	private int dimension;
	private int k;
	private int R1;
	private boolean isCbuilt, isVbuilt, doCalibrate, useRAPL;
	private List<Value> centroids, vectors;
	private RAPLRecord record;
	private ThreadPinning rapl;
	private UseRAPL urapl;
	private int iterationCount;
	private int jobToken;
	
	public void setup (Context context) {
		init(context);
		Configuration conf = context.getConfiguration();
		rapl = new ThreadPinning();
		urapl = new UseRAPL();
		urapl.initRAPL("maptask");
		if(useRAPL){
			record = context.getRAPLRecord();
			//JNI call to set the power cap based on the target task time and
		    // the previous execution time.
			// record contains prev exec time & the target execution time.
			//TODO : Decide if this has to be done in setup or the mapTask 
			//since the rapl record has the information about the package already
			
	//	    rapl.adjustPower(record);
			if(record != null){
				iterationCount = 1 + record.getInterationCount();
				//TODO : Do this only if the iteration count is more than 4 and a flag to use this feature is set.
//				String testVar = conf.get("conftest");
//				if("test".equals(testVar)){
//					System.out.println("Able to read data from conf files");
//				}
				RAPLCalibration calibration = ((MKMRowListMatrix) context.getMatrix()).getCalibration();
				Map<Integer, Long> cap2time = new HashMap<Integer, Long>();
				for(Integer i : calibration.getCapToExecTimeMap().keySet()){
					cap2time.put(i, calibration.getCapToExecTimeMap().get(i).getExecTime());
				}
				int calibIterCount = conf.getInt("RAPL.calibrationCount", 5);
				if(record.isDoCalibration() && iterationCount != 0 && iterationCount < calibIterCount){
					System.out.println("Setting doCalib to true:"+record.isDoCalibration()+":"+iterationCount);
					doCalibrate = true;
				}
				
				//rapl.adjustPower(record.getExectime(), record.getTargetTime());
				//get the power cap and set it only if this isn't a calibration round
				int powerCap = getPowerCap(record.getTargetTime(), cap2time);
				if(powerCap!=0 && !doCalibrate){
					int pkg = rapl.get_thread_affinity()/8;
					System.out.println("Setting power cap of pkg:"+pkg+", to:"+powerCap+" watts");
					urapl.setPowerLimit(pkg, powerCap);
				}
			}
			else if(iterationCount == 1){
				//Set the power cap to default.i.e. the highest. Can get this from the config file
				int defPowerCap = 115;//watts
				int pkg = rapl.get_thread_affinity()/8;
				System.out.println("Setting default power cap of pkg:"+pkg+", to:"+defPowerCap+" watts");
				urapl.setPowerLimit(pkg, defPowerCap);
			}
		}
		else{
			//just to make sure defaults are set
			int defPowerCap = 115;//watts
			int pkg = rapl.get_thread_affinity()/8;
			System.out.println("Setting default power cap of pkg:"+pkg+", to:"+defPowerCap+" watts");
			urapl.setPowerLimit(pkg, defPowerCap);
		}
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
	
	/**
	 * Can be reused.
	 * @param targetTime
	 * @param cap2time
	 * @return
	 */
	private int getPowerCap(long targetTime, Map<Integer, Long> cap2time) {
		int powerCap = 0;
		long min_diff = Long.MAX_VALUE;
		for(int i : cap2time.keySet()){
			if(Math.abs(cap2time.get(i) - targetTime) < min_diff){
				min_diff = Math.abs(cap2time.get(i) - targetTime);
				powerCap = i;
			}
		}
		return powerCap;
	}

	private void init(Context context) {
		Configuration conf = context.getConfiguration();
		dimension = conf.getInt("KM.dimension", 2);
		k = conf.getInt("KM.k", 6);
		R1 = conf.getInt("KM.R1", 6);
		//this is always 1 when we reuse the tasks.
		iterationCount = conf.getInt("KM.iterationCount", 0);
		jobToken = conf.getInt("KM.jobToken", -1);
//		centroids = new ArrayList<Value>();
//		vectors = new ArrayList<Value>();
		isCbuilt = isVbuilt = false;
		doCalibrate = false;
		useRAPL = conf.getBoolean("RAPL.enable", false);
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
				record.setJobtoken(jobToken);
				record.setExectime(end1 - start1);
				//TODO:replace the hardcoded value with a JNI call to get the core this thread is pinned to
				int pkgIdx = rapl.get_thread_affinity() / CORES_PER_PKG;
				record.setPkg((short)pkgIdx);
				record.setInterationCount(iterationCount);
				//TODO : add hostname to record either here or in the appmaster (this info is readily available there)
//				record.setHostname(hostname);
				context.setRAPLRecord(record);
				/******** Calibration *********/
				//TODO : done for the initial iterations only
				//if(currentIteration < 3){
				if(doCalibrate){
					RAPLCalibration calibration = calibrate(vectors, centroids);
					doCalibrate = false;
					if(calibration != null)
						((MKMRowListMatrix) context.getMatrix()).addCalibration(calibration);
				}
				
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
//			iterationCount++;
		}
	}
	
	/**
	 * This method is invoked on the core of the map/reduce task's execution
	 * And taskes the arguments for this core as its arguments
	 * For kmeans, "classify" is the core.
	 * 
	 * TODO : Check to see if a method can be passed as an argument so that
	 * the calibration method can be reused.
	 * @param vectors2
	 * @param centroids2
	 * @return
	 */
	private RAPLCalibration calibrate(List<Value> vectors2,
			List<Value> centroids2) throws Exception{
		PartialCentroid[] partialCentroids = null;
		RAPLCalibration calibration = new RAPLCalibration();
		UseRAPL urapl = new UseRAPL();
		urapl.initRAPL("maptask");
		int pkg = rapl.get_thread_affinity()/8;
		long origLimit = urapl.getPowerLimit(pkg);
		for(int powerCap = 50; powerCap > 5; powerCap -= 5){
			//set power cap
			urapl.setPowerLimit(pkg, powerCap);
			//wait 2 seconds for the power cap to kick in
			Thread.sleep(2000);
			//execute the core
			long start =System.nanoTime();
			partialCentroids = (PartialCentroid[]) classify(vectors, centroids);
			long end =System.nanoTime();
			//set the execution time in the calibration
			calibration.addRAPLExecTime(powerCap, end - start);
			if(DEBUG) System.out.println(partialCentroids + ":"+ pkg+ ":" + powerCap +":" + (end - start));
		}
		urapl.setPowerLimit(pkg, origLimit);
		return calibration;
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
//			if(DEBUG) System.out.println("Adding value :" + val);
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
