package org.apache.hadoop.examples.Kmeans;

import java.io.IOException;
import java.net.URI;
import java.util.Hashtable;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.filecache.DistributedCache;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import org.apache.hadoop.examples.Kmeans.KMTypes.Value;

public class KMDriver {
	
	//input/output paths
	private static final String KM_DATA_DIR = "tmp/kmeans/";
	private static final String KM_CENTER_INPUT_PATH = KM_DATA_DIR + "/centerIn";
	private static final String KM_CENTER_OUTPUT_PATH = KM_DATA_DIR + "/centerOut";
	private static final String KM_DATA_INPUT_PATH = KM_DATA_DIR + "/data";
//	private static final String KM_INPUT_PATH_1 = KM_DATA_DIR + "/1";
//	private static final String KM_INPUT_PATH_2 = KM_DATA_DIR + "/2";
//	private static final String KM_INPUT_PATH_3 = KM_DATA_DIR + "/3";
//	private static final String KM_INPUT_PATH_4 = KM_DATA_DIR + "/4";
	private static final String KM_TEMP_CLUSTER_DIR_PATH = KM_DATA_DIR + "/tmpC";
	
	private static FileSystem fs;
	private static Configuration conf = new Configuration();
	
	@SuppressWarnings("deprecation")
	public static void main(String[] args) throws Exception {
		GenericOptionsParser goParser = new GenericOptionsParser(conf, args);
		fs = FileSystem.get(conf);
		fs.mkdirs(new Path(KM_DATA_DIR));
		KMDriver driver = new KMDriver();
		String[] remainingArgs = goParser.getRemainingArgs();
		
		if (remainingArgs.length < 6) {
		     System.out.println("USAGE: <COUNT> <K> <DIMENSION OF VECTORS> <MAXITERATIONS> <num of tasks> <convgDelta>");
		      return;
		}

		conf = new Configuration();
		int count = Integer.parseInt(remainingArgs[0]);
		int k = Integer.parseInt(remainingArgs[1]);
		int dimension = Integer.parseInt(remainingArgs[2]);
		int iterations = Integer.parseInt(remainingArgs[3]);
		int taskCount = Integer.parseInt(remainingArgs[4]);
		int convergenceDelta = Integer.parseInt(remainingArgs[5]);
		conf.setInt("KM.maxiterations", iterations);		
		conf.setInt("KM.k", k);
		conf.setInt("KM.dimension", dimension);
		conf.setInt("KM.mapTaskCount", taskCount);
//		conf.set("KM.centerIn", center.toString());
//	    conf.set("KM.centerOut", centerOut.toString());
	    String inputDataPath = fs.makeQualified(new Path(KM_DATA_INPUT_PATH)).toString();
	    String inputCenterPath = fs.makeQualified(new Path(KM_CENTER_INPUT_PATH)).toString();
	    String outPath = fs.makeQualified(new Path(KM_CENTER_OUTPUT_PATH)).toString();
	    String tempClusterDirPath = fs.makeQualified(new Path(KM_TEMP_CLUSTER_DIR_PATH)).toString();
	    conf.set("KM.inputDataPath", inputDataPath);
	    conf.set("KM.inputCenterPath", inputCenterPath);
	    conf.set("KM.outputDirPath", outPath);
	    conf.set("KM.tempClusterDir", tempClusterDirPath);
	    conf.setInt("KM.R1", taskCount);
	    
	    fs.delete(new Path(tempClusterDirPath), true);
		fs.delete(new Path(outPath), true);
		
		URI uri = new URI("hdfs://localhost/libraries/libpapi.so.1#libpapi.so");
		DistributedCache.createSymlink(conf);
		DistributedCache.addCacheFile(uri, conf);
		//write input data and centers to the file paths accordingly
		// NOTE: Make sure centers have a cluster identifier with it.
		KMUtils.prepareInput(count, k, dimension, taskCount, conf, new Path(KM_DATA_INPUT_PATH), new Path(KM_CENTER_INPUT_PATH), fs);
		driver.kmeans(iterations, convergenceDelta);
	}
	
	public void kmeans(int maxIterations, int convergenceDelta){
		boolean converged = false;
		int iteration = 1;
		Path centersIn = fs.makeQualified(new Path(KM_CENTER_INPUT_PATH));
//		try {
//			fs.delete(new Path(conf.get("KM.tempClusterDir")), true);
//		} catch (IllegalArgumentException e1) {
//			e1.printStackTrace();
//		} catch (IOException e1) {
//			e1.printStackTrace();
//		}
		try {
			while(!converged && iteration <= maxIterations){
				
					Path centersOut = fs.makeQualified(new Path(KM_CENTER_OUTPUT_PATH, "iteration-" + iteration));
					this.kmeansJob(centersIn, centersOut, iteration);
					converged = isConverged(centersIn, centersOut, convergenceDelta, iteration == 1);
					if(!converged){
						centersIn = centersOut;
						System.out.println("## not converged, going for the next iteration with input from "+ centersIn.toString());
					}
					iteration++;
					fs.delete(new Path(conf.get("KM.tempClusterDir")), true);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private boolean isConverged(Path centersIn, Path centersOut, int convergenceDelta, boolean isFirstIter) {
		boolean converged = true;
		List<Value> oldCentroids = KMUtils.getCentroidsFromFile(centersIn, !isFirstIter);
		List<Value> newCentroids = KMUtils.getCentroidsFromFile(centersOut, true);
		Hashtable<Integer, Value> oldCentroidMap = new Hashtable<Integer,Value>();
		Hashtable<Integer, Value> newCentroidMap = new Hashtable<Integer,Value>();
		
		for(Value centroid : oldCentroids){
			oldCentroidMap.put(centroid.getCentroidIdx(), centroid);
		}
		
		for(Value centroid : newCentroids){
			newCentroidMap.put(centroid.getCentroidIdx(), centroid);
		}
		
		for(Integer key : oldCentroidMap.keySet()){
			if(!isConverged(oldCentroidMap.get(key), newCentroidMap.get(key), convergenceDelta)){
				converged = false;
				break;
			}
		}
		
		return converged;
	}

	private boolean isConverged(Value oldCentroid, Value newCentroid, int convergenceDelta) {
		return KMUtils.getDistance(oldCentroid.getCoordinates(), 
					newCentroid.getCoordinates()) <= convergenceDelta;
	}

	public void kmeansJob(Path centersIn, Path centersOut, int iteration) throws Exception{
		Job job = Job.getInstance(conf, "kmeans");
		job.setJarByClass(org.apache.hadoop.examples.Kmeans.KMDriver.class);
		
		job.setNumReduceTasks(conf.getInt("KM.R1", 6));
	    System.out.println("Number of reduce tasks for job1 set to: "+ conf.getInt("KM.R1", 0));
	    job.setInputFormatClass(SequenceFileInputFormat.class);
	    job.setOutputFormatClass(SequenceFileOutputFormat.class);
 		job.setMapperClass(KMMapper.class);
 		job.setReducerClass(KMReducer.class);
	    job.setPartitionerClass(KMPartitioner.class);
	    job.setMapOutputKeyClass(org.apache.hadoop.examples.Kmeans.KMTypes.Key.class);
	    job.setMapOutputValueClass(org.apache.hadoop.examples.Kmeans.KMTypes.Value.class);
	    job.setOutputKeyClass(org.apache.hadoop.examples.Kmeans.KMTypes.Key.class);
	    job.setOutputValueClass(org.apache.hadoop.examples.Kmeans.KMTypes.Value.class);
	    
	    //provide input data only for the initial iteration.
	    if(iteration == 1)
	    	FileInputFormat.addInputPath(job, new Path(conf.get("KM.inputDataPath")));
	    
	    FileInputFormat.addInputPath(job, centersIn);
	    FileOutputFormat.setOutputPath(job, centersOut);	    
		if (!job.waitForCompletion(true))
			return;
	}

}
