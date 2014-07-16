package org.apache.hadoop.examples.MKmeans;

import java.net.URI;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapTask;
import org.apache.hadoop.mapred.RAPLRecord;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.filecache.DistributedCache;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import org.ncsu.sys.*;

public class MKMDriver {
		
		//input/output paths
		private static final String KM_DATA_DIR = "tmp/kmeans/";
		private static final String KM_CENTER_INPUT_PATH = KM_DATA_DIR + "/centerIn";
		private static final String KM_CENTER_OUTPUT_PATH = KM_DATA_DIR + "/centerOut";
		private static final String KM_DATA_INPUT_PATH = KM_DATA_DIR + "/data";
		private static final String KM_TEMP_CLUSTER_DIR_PATH = KM_DATA_DIR + "/tmpC";
		private static final boolean DEBUG = true;
		
		private static FileSystem fs;
		private static JobConf conf = new JobConf();
		private UseRAPL librapl;
		private List<PowerStatus<Double>> powerStatus;
		
		public MKMDriver(){
			librapl = new UseRAPL();
            powerStatus = new ArrayList<PowerStatus<Double>>();
        	librapl.initRAPL("mmPower");
		}

		public static void main(String[] args) throws Exception {
			GenericOptionsParser goParser = new GenericOptionsParser(conf, args);
			fs = FileSystem.get(conf);
			fs.mkdirs(new Path(KM_DATA_DIR));
			MKMDriver driver = new MKMDriver();
			String[] remainingArgs = goParser.getRemainingArgs();
			
			if (remainingArgs.length < 10) {
			     System.out.println("USAGE: <COUNT> <K> <DIMENSION OF VECTORS> <SegmentsPerDimension> "
			     		+ "<MAXITERATIONS> <num of tasks> <convgDelta> <FirstTaskInputCount> <Diff/Ratio> <isLinear(0/1)> <isCalibration> <opt:powerCap>");
			      return;
			}

			int count = Integer.parseInt(remainingArgs[0]);
			int k = Integer.parseInt(remainingArgs[1]);
			int dimension = Integer.parseInt(remainingArgs[2]);
			int segPerDim = Integer.parseInt(remainingArgs[3]);
			int maxIterations = Integer.parseInt(remainingArgs[4]);
			int taskCount = Integer.parseInt(remainingArgs[5]);
			int convergenceDelta = Integer.parseInt(remainingArgs[6]);
			int taskStart = Integer.parseInt(remainingArgs[7]);
			int diffratio = Integer.parseInt(remainingArgs[8]);
			boolean isLinear = Integer.parseInt(remainingArgs[9]) == 1;
			boolean isCalibration = Integer.parseInt(remainingArgs[10]) == 1;
			int powerCap = 115;
			boolean isUniform = false;
			if(isCalibration){
				powerCap = Integer.parseInt(remainingArgs[11]);
				isUniform = Integer.parseInt(remainingArgs[12]) == 1;
			}
			else
				isUniform = Integer.parseInt(remainingArgs[11]) == 1;
			Random rand = new Random(); 
			
			conf.setBoolean("KM.isCalibration", isCalibration);
			conf.setInt(RAPLRecord.MAP_TASK_REUSE_JOBTOKEN, rand.nextInt());
			conf.setInt("KM.maxiterations", maxIterations);		
			conf.setInt("KM.k", k);
			conf.setInt("KM.dimension", dimension);
			conf.setInt("KM.mapTaskCount", taskCount);
//			conf.set("KM.centerIn", center.toString());
//		    conf.set("KM.centerOut", centerOut.toString());
		    String inputDataPath = fs.makeQualified(new Path(KM_DATA_INPUT_PATH)).toString();
		    String inputCenterPath = fs.makeQualified(new Path(KM_CENTER_INPUT_PATH)).toString();
		    String outPath = fs.makeQualified(new Path(KM_CENTER_OUTPUT_PATH)).toString();
		    String tempClusterDirPath = fs.makeQualified(new Path(KM_TEMP_CLUSTER_DIR_PATH)).toString();
		    conf.set("KM.inputDataPath", inputDataPath);
		    conf.set("KM.inputCenterPath", inputCenterPath);
		    conf.set("KM.outputDirPath", outPath);
		    conf.set("KM.tempClusterDir", tempClusterDirPath);
//		    conf.setInt("KM.R1", taskCount);
		    conf.setNumMapTasks(taskCount);
		    
		    fs.delete(new Path(tempClusterDirPath), true);
			fs.delete(new Path(outPath), true);
			
			//write input data and centers to the file paths accordingly
			// NOTE: Make sure centers have a cluster identifier with it.
			Path[] paths = new Path[taskCount];
			for(int pj = 0; pj < paths.length; pj++){
				paths[pj] = new Path(KM_DATA_INPUT_PATH, ""+pj);
			}
			
			URI uri = new URI("hdfs://localhost/libraries/libpapi.so.1#libpapi.so");
			DistributedCache.createSymlink(conf);
			DistributedCache.addCacheFile(uri, conf);
			
			uri = new URI("hdfs://localhost/libraries/librapl.so.1#librapl.so");
			DistributedCache.createSymlink(conf);
			DistributedCache.addCacheFile(uri, conf);
			
			int totalVectorCount = MKMUtils.prepareAstroPhyInput(k, dimension, segPerDim, 1000, taskCount, 
					conf, paths, new Path(KM_CENTER_INPUT_PATH), fs, taskStart, diffratio, isLinear, isUniform);
//			MKMUtils.prepareInput(count, k, dimension, taskCount, conf, paths, new Path(KM_CENTER_INPUT_PATH), fs, ratio);
			long start = System.nanoTime();
			long defaultPowerCap0 = 115;
			long defaultPowerCap1 = 115;
			//Read default power cap and set the input power cap
			if(isCalibration){
				defaultPowerCap0 = driver.librapl.getPowerLimit(0);
				defaultPowerCap1 = driver.librapl.getPowerLimit(1);
				driver.librapl.setPowerLimit(0, powerCap);
				driver.librapl.setPowerLimit(1, powerCap);
				Thread.sleep(2000);
			}
			driver.kmeans(maxIterations, convergenceDelta);
			//reset it back to original power cap
			if(isCalibration){
				driver.librapl.setPowerLimit(0, defaultPowerCap0);
				driver.librapl.setPowerLimit(1, defaultPowerCap1);
			}
			
			
			long end = System.nanoTime();
			System.out.println("totalVectorCount:\t" + totalVectorCount);
			System.out.println("totalTime:\t" + (end -start));
			printAvgPowerConsumption(driver.powerStatus);
			//cleanup
			fs.delete(new Path("tmp/rapl/", "map"), true);
		}
		
		private static void printAvgPowerConsumption(
				List<PowerStatus<Double>> powerStatus2) {int count = 0;
		        Double avgPow_0 = 0.0, avgPow_1 = 0.0;
		        for(PowerStatus<Double> ps : powerStatus2){
		        	avgPow_0 += ps.pkgPower.get(0);
		        	avgPow_1 += ps.pkgPower.get(1);
		        	count++;
		        }
		        avgPow_0 /= count;
		        avgPow_1 /= count;
		        System.out.println("From power calculator thread");
		        System.out.println("pkg_0:\t"+ avgPow_0 + "\n" + "pkg_1:\t" + avgPow_1);
		}

		private class PowerStatus<POWER_TYPE>{
        	List<POWER_TYPE> pkgPower;
        	public PowerStatus(int arrayLen){
        		pkgPower = new ArrayList<POWER_TYPE>(arrayLen);
        	}
        }
		
		public Thread powerCalculator = new Thread(
				new Runnable() {
					
					@Override
					public void run() {
						PowerStatus<Double> ps;
						while(!Thread.currentThread().isInterrupted()){
							try {
								double power_0 = librapl.getPowerStatus(0);
								double power_1 = librapl.getPowerStatus(1);
								ps = new PowerStatus<Double>(2);
								ps.pkgPower.add(power_0);
								ps.pkgPower.add(power_1);
								powerStatus.add(ps);
								Thread.sleep(500);
							} catch (InterruptedException e) {
								e.printStackTrace();
								break;
							}
						}
					}
				}
			);
		
		public void kmeans(int maxIterations, int convergenceDelta){
			boolean converged = false;
			int iteration = 0;
			Path centersIn = fs.makeQualified(new Path(KM_CENTER_INPUT_PATH));
//			try {
//				fs.delete(new Path(conf.get("KM.tempClusterDir")), true);
//			} catch (IllegalArgumentException e1) {
//				e1.printStackTrace();
//			} catch (IOException e1) {
//				e1.printStackTrace();
//			}
			try {
				List<Value> oldCenters = null;
				//get any of the datapath
				Path dataIn = new Path(conf.get("KM.inputDataPath"), ""+0);
				boolean isCalcStarted = false;
				//start from the 1st iteration not the 0th iteration
				int calcStartIter = conf.getInt("RAPL.calibrationIterationCount", 0) + 1;
				while(!converged && iteration <= maxIterations){
						if(!isCalcStarted && iteration == calcStartIter){
							this.powerCalculator.start();
							isCalcStarted = true;
						}
						Path centersOut = fs.makeQualified(new Path(KM_CENTER_OUTPUT_PATH, "iteration-" + iteration));
						fs.delete(centersOut, true);
						if(oldCenters == null)
							oldCenters = MKMUtils.getCentroidsFromFile(centersIn, false);
						else{
							oldCenters = MKMUtils.getCentroidsFromFile(dataIn, false);
						}
						conf.setInt("KM.iterationCount", iteration);
						this.kmeansJob(centersIn, centersOut, iteration);
						List<Value> newCenters = MKMUtils.getCentroidsFromFile(dataIn, false);
						converged = isConverged(oldCenters, newCenters, convergenceDelta);
						if(!converged){
//							centersIn = centersOut;
							System.out.println("## not converged, going for the next iteration with input from "+ centersIn.toString());
						}
						iteration++;
						fs.delete(new Path(conf.get("KM.tempClusterDir")), true);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			finally{
				if(this.powerCalculator.isAlive())
					this.powerCalculator.interrupt();
			}
		}
		
		private boolean isConverged(List<Value> oldCentroids, List<Value> newCentroids, int convergenceDelta) throws Exception {
			boolean converged = true;
//			if(newCentroids.isEmpty()){
//				newCentroids = KMUtils.getCentroidsFromFile(centersOut, true);
//			}
//			if(newCentroids.isEmpty()){
//				if(DEBUG) System.out.println("Screw this! I am trying again with a normal read");
//				newCentroids = KMUtils.getCentroidsFromFile(centersOut, false);
//			}
			Hashtable<Integer, Value> oldCentroidMap = new Hashtable<Integer,Value>();
			Hashtable<Integer, Value> newCentroidMap = new Hashtable<Integer,Value>();
			
			if(DEBUG) System.out.println("***OldCentroids***");
			for(Value centroid : oldCentroids){
				oldCentroidMap.put(centroid.getCentroidIdx(), centroid);
				if(DEBUG) System.out.println(centroid);
			}
			
			if(DEBUG) System.out.println("***NewCentroids***");
			for(Value centroid : newCentroids){
				newCentroidMap.put(centroid.getCentroidIdx(), centroid);
				if(DEBUG) System.out.println(centroid);
			}
			
			for(Integer key : oldCentroidMap.keySet()){
				if(!isConverged(oldCentroidMap.get(key), newCentroidMap.get(key), convergenceDelta)){
					converged = false;
					break;
				}
			}
			
			return converged;
		}
		
		private boolean isConverged(Path centersIn, Path centersOut, int convergenceDelta, boolean isFirstIter) throws Exception{
			boolean converged = true;
			List<Value> oldCentroids = MKMUtils.getCentroidsFromFile(centersIn, !isFirstIter);
			List<Value> newCentroids = MKMUtils.getCentroidsFromFile(centersOut, true);
			Hashtable<Integer, Value> oldCentroidMap = new Hashtable<Integer,Value>();
			Hashtable<Integer, Value> newCentroidMap = new Hashtable<Integer,Value>();
			
			if(DEBUG) System.out.println("***OldCentroids***");
			for(Value centroid : oldCentroids){
				oldCentroidMap.put(centroid.getCentroidIdx(), centroid);
				if(DEBUG) System.out.println(centroid);
			}
			
			if(DEBUG) System.out.println("***NewCentroids***");
			for(Value centroid : newCentroids){
				newCentroidMap.put(centroid.getCentroidIdx(), centroid);
				if(DEBUG) System.out.println(centroid);
			}
			
			for(Integer key : oldCentroidMap.keySet()){
				if(!isConverged(oldCentroidMap.get(key), newCentroidMap.get(key), convergenceDelta)){
					converged = false;
					break;
				}
			}
			
			return converged;
		}

		private boolean isConverged(Value oldCentroid, Value newCentroid, int convergenceDelta) throws Exception{
			if(oldCentroid == null){
				throw new Exception("Old centroid is null");
			}
			else if(newCentroid == null){
				throw new Exception("New centroid is null");
			}
			return MKMUtils.getDistance(oldCentroid.getCoordinates(), 
						newCentroid.getCoordinates()) <= convergenceDelta;
		}

		public void kmeansJob(Path centersIn, Path centersOut, int iteration) throws Exception{
			Job job = Job.getInstance(conf, "kmeans");
			job.setJarByClass(org.apache.hadoop.examples.MKmeans.MKMDriver.class);
			
			job.setNumReduceTasks(1);
//		    System.out.println("Number of reduce tasks for job1 set to: "+ conf.getInt("KM.R1", 0));
		    job.setInputFormatClass(SequenceFileInputFormat.class);
		    job.setOutputFormatClass(SequenceFileOutputFormat.class);
	 		job.setMapperClass(MKMMapper.class);
	 		job.setReducerClass(MKMReducer.class);
//		    job.setPartitionerClass(MKMPartitioner.class);
		    job.setMapOutputKeyClass(IntWritable.class);
		    job.setMapOutputValueClass(org.apache.hadoop.examples.MKmeans.PartialCentroid.class);
		    job.setOutputKeyClass(IntWritable.class);
		    job.setOutputValueClass(org.apache.hadoop.examples.MKmeans.PartialCentroid.class);
		    
		    
		    //uncomment the following line when using Phadoop
		    //if(iteration == 1)
		    for(int i = 0; i < conf.getInt("KM.mapTaskCount", 2); i++){
		    	Path inputFilePath = new Path(conf.get("KM.inputDataPath"), ""+i);
		    	if(DEBUG) System.out.println("Adding input path :" + inputFilePath.toString());
			    FileInputFormat.addInputPath(job, inputFilePath);
		    }
		    //No need to add the centers path
//			    FileInputFormat.addInputPath(job, centersIn);
			    
		    
		    FileOutputFormat.setOutputPath(job, centersOut);
		    
		    //TODO: fix all the paths and implement the algo as indicated in the site.
		    
			if (!job.waitForCompletion(true))
				return;
		}

}
