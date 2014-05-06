package org.apache.hadoop.examples.Kmeans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import org.apache.commons.collections.map.HashedMap;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CreateFlag;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.SequenceFile.Reader;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.compress.DefaultCodec;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.apache.zookeeper.KeeperException;
import org.apache.hadoop.examples.Kmeans.KMTypes.Key;
import org.apache.hadoop.examples.Kmeans.KMTypes.Value;
import org.apache.hadoop.examples.Kmeans.KMTypes.VectorType;
import org.apache.hadoop.examples.Kmeans.SyncPrimitive.Barrier;

public class KMReducer extends Reducer<Key, Value, Key, Value> {

	private static final boolean DEBUG = true;
	public static String ZK_ADDRESS = "10.1.255.13:2181";
	private int dimension;
	private int k;
	private int R1;
	private boolean isCbuilt, isVbuilt;
	private List<Value> centroids, vectors;
	
	public void setup (Context context) {
		init(context);
	}
	
	private void init(Context context) {
		Configuration conf = context.getConfiguration();
		dimension = conf.getInt("KM.dimension", 2);
		k = conf.getInt("KM.k", 6);
		R1 = conf.getInt("KM.R1", 6);
		centroids = new ArrayList<Value>();
		vectors = new ArrayList<Value>();
		isCbuilt = isVbuilt = false;
	}

	public void reduce(Key _key, Iterable<Value> values, Context context)
			throws IOException, InterruptedException {
		
		if(DEBUG){ 
			printReduceInputKey(_key);
			//printReduceInputValues(values);
		}
		//populate clusters and data
		Value[] partialCentroids = null;
		
		if(_key.getType() == VectorType.CENTROID){
			buildCentroids(values, centroids);
			isCbuilt = true;
		}
		else{
			//use build and set here
			buildCentroidsAndSet(values, vectors, context);
			isVbuilt = true;
			//buildVectors(values, vectors);
		}
		
		if(vectors == null && context.getMatrix() != null){
  			if(DEBUG) System.out.println("##Getting vectors already read from fs. Skipping reading the file");
  			RowListMatrix vectorList = (RowListMatrix) context.getMatrix();
  			vectors = vectorList.getMatrix();
  			isVbuilt = true;
  		}
		
		//compute the partial clusters
		if(isCbuilt && isVbuilt){
			try{
				if(DEBUG) System.out.println("Classifying " + vectors.size() + " vectors among " + centroids.size() + " clusters" );
				System.out.println("$$VectorCount:"+"\t"+vectors.size());
				long start =System.nanoTime();
				partialCentroids = classify(vectors, centroids);
				long end =System.nanoTime();
				System.out.println("$$ClassifyTime:"+"\t" + (end-start));
			}
			catch(Exception ex){
				ex.printStackTrace();
			}
			
			//TODO: check the case where a cluster doesn't contain any points. Refer to the fix pointed out on the site.
			Configuration conf = context.getConfiguration();
			FileSystem fs = FileSystem.get(conf);
			int taskId = context.getTaskAttemptID().getTaskID().getId();
			//TODO: write the partial centroids to files
			Path path = new Path(conf.get("KM.tempClusterDir") + "/" + taskId);
			System.out.println("##Writing to:" + path.toString());
			SequenceFile.Writer writer = SequenceFile.createWriter(fs, conf, path,
				      IntWritable.class, Value.class,
				      SequenceFile.CompressionType.NONE);
			for(int i = 0; i < partialCentroids.length ; i++){
				Value centroid = (Value)centroids.get(i);
				IntWritable el = new IntWritable();
				if(partialCentroids[i] != null){
					el.set(partialCentroids[i].getCentroidIdx());
					writer.append(el, partialCentroids[i]);
					if(DEBUG) System.out.println("Partial centroid : " + el.get() + partialCentroids[i]);
				}
				else{
					if(DEBUG) System.out.println("No partial centroid for cluster index:"+i);
				}
			}
			writer.close();
			System.out.println("##Writing to:" + path.toString() +" Done");
			//sync
			Barrier b = new Barrier(ZK_ADDRESS, "/b-kmeans", R1);
			try{
				long start = System.nanoTime();
			    boolean flag = b.enter();
			    System.out.println("Entered barrier: " + 6);
			    long end = System.nanoTime();
			    System.out.println("$$BarrierTime:"+"\t" + (end-start));
			if(!flag) System.out.println("Error when entering the barrier");
			} catch (KeeperException e){
				e.printStackTrace();
			} catch (InterruptedException e){
				e.printStackTrace();
			}
			
			//read files and compute newClusters only if its the task 0
			if(taskId == 0){
				//add partial centers of task 0 to the hashmap.
				Hashtable<Integer, Value> auxCentroids = new Hashtable<Integer, Value>();
				for(Value partialCentroid : partialCentroids){
					if(partialCentroid != null)
						auxCentroids.put(partialCentroid.getCentroidIdx(), partialCentroid);
				}
				
				//add partial centers of other tasks to the hashmap
				for(int i = 1; i < R1; i++){
					//configureWithClusterInfo for reading a file
					path = new Path(conf.get("KM.tempClusterDir") + "/" + i);
					Path filePath = fs.makeQualified(path);
					System.out.println("##Reading from:" + path.toString());
					List<Value> partCentroidsFromFile = KMUtils.getPartialCentroidsFromFile(filePath);
					for(Value partialCentroid : partCentroidsFromFile){
						if(auxCentroids.containsKey(partialCentroid.getCentroidIdx())){
							//TODO: clarify changes to count and consider corner cases
							auxCentroids.get(partialCentroid.getCentroidIdx()).addVector(partialCentroid);
						}
						else{
							auxCentroids.put(partialCentroid.getCentroidIdx(), partialCentroid);
						}
					}
				}
				
				int centroidCount = auxCentroids.keySet().size();
				if(DEBUG) System.out.println("## partial centroids accumulated:" + centroidCount);
				Hashtable<Integer, Value> centroidsMap = new Hashtable<Integer, Value>(); 
				
				//need to identify which centroid has not been assigned any points/vectors
				HashSet<Integer> centroidIndices = new HashSet<Integer>();
				for(Value centroid : centroids){
					centroidsMap.put(centroid.getCentroidIdx(), centroid);
					centroidIndices.add(centroid.getCentroidIdx());
				}
				
				for(Integer key : auxCentroids.keySet()){
					//compute new clusters
					Value newCentroid = computeNewCentroid(auxCentroids.get(key));
					newCentroid.setCentroidIdx(key);
					// write it back as a standard reducer output !
					// make sure all the k-cluster centroids are written even the ones with size-zero
					if(newCentroid != null && centroidIndices.contains(key)){
						context.write(new Key(key, VectorType.CENTROID), newCentroid);
						if(DEBUG) printReduceOutput(new Key(key, VectorType.CENTROID), newCentroid);
						centroidIndices.remove(key);
					}
					else{
						if(newCentroid != null)
							throw new InterruptedException("FATAL: multiple centroids with same id !!");
					}
				}
				
				if(!centroidIndices.isEmpty()){
					for(Integer key : centroidIndices) {
						context.write(new Key(key, VectorType.CENTROID), centroidsMap.get(key));
						if(DEBUG) printReduceOutput(new Key(key, VectorType.CENTROID), centroidsMap.get(key));
					}
				}
			}
		}
	}

	private void printReduceInputValues(Iterable<Value> values) {
		StringBuilder sb = new StringBuilder();
		sb.append("##### Reduce input values: "+"\n" );
		for(Value value : values){
			sb.append( "[" + value.getDimension() + "," + value.getCentroidIdx() + "," + value.getCount() + "(");
			for(int coord : value.getCoordinates()){
				sb.append(coord + ",");
			}
			sb.append(")] " + "\n");
		}
		System.out.println(sb.toString());		
	}

	private Value computeNewCentroid(Value value) {
		if(value.getCount() == 0)
			return null;
		else {
			Value newCentroid = new Value(value.getDimension());
			int[] coords = value.getCoordinates();
			int[] newCoords = newCentroid.getCoordinates();
			for(int i = 0; i < coords.length; i++){
				newCoords[i] = coords[i]/value.getCount();
			}
			return newCentroid;
		}
			
	}

	
	private Value[] classify(List<Value> vectors2, List<Value> centroids2) throws Exception {
		Value[] partialCentroids = new Value[centroids2.size()];
		for(Value point : vectors2){
			int idx = getNearestCentroidIndex(point, centroids2);
			if(partialCentroids[idx] == null){
				partialCentroids[idx] = new Value(point.getDimension());
			}
			partialCentroids[idx].addVector(point);
			if(partialCentroids[idx].getCentroidIdx() == -1){
				partialCentroids[idx].setCentroidIdx(idx);
			}
			else {
				if(partialCentroids[idx].getCentroidIdx() != idx)
					if(DEBUG) throw new Exception("Fatal: Inconsistent cluster, multiple centroids problem!");
			}
		}
		return partialCentroids;
	}

	private int getNearestCentroidIndex(Value point, List<Value> centroids2) {
		int nearestCidx = -1;
		int shortestDistance = Integer.MAX_VALUE;
		for(Value centroid : centroids2){
			int distance = KMUtils.getDistance(point.getCoordinates(), centroid.getCoordinates());
			if(distance < shortestDistance){
				nearestCidx  = centroid.getCentroidIdx();
				shortestDistance = distance;
			}
		}
		return nearestCidx;
		
	}

	private void buildCentroids(Iterable<Value> values, List<Value> centroidsLoc) {
		for(Value val : values){
			if(DEBUG) System.out.println("Adding value :" + val);
			Value valCopy = new Value(val.getDimension());
			valCopy.addVector(val);
			valCopy.setCentroidIdx(val.getCentroidIdx());
			centroidsLoc.add(valCopy);
		}
		
	}
	
	private void buildCentroidsAndSet(Iterable<Value> values, List<Value> centroidsLoc, Context context) {
		buildCentroids(values, centroidsLoc);
		context.setMatrix(new RowListMatrix(centroidsLoc));
	}
	
	private void printReduceInputKey (Key key) {
		System.out.println("##### Reduce input: key = (" + key.getTaskIndex() + "," + 
			key.getType() +")");
	}
	
	private void printReduceOutput (Key key, Value value) {
		StringBuilder sb = new StringBuilder();
		sb.append("##### Reduce output: (" + key.getTaskIndex() + "," + 
			key.getType() + ") (" + value.getDimension() + "," + value.getCentroidIdx() + "," + value.getCount() + "\n");
		for(int coord : value.getCoordinates()){
			sb.append(coord + ",");
		}
		sb.append(") ");
		System.out.println(sb.toString());
	}

}
