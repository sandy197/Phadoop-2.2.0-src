package org.apache.hadoop.examples.MKmeans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.SequenceFile.CompressionType;
import org.apache.hadoop.io.SequenceFile.Reader;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.examples.MKmeans.MKMTypes.Values;
import org.apache.hadoop.examples.MKmeans.MKMTypes.VectorType;

public class MKMUtils {

	private static final boolean DEBUG = true;

	public static int getDistance(Value point, Value centroid) {
		int distance = 0;
		int[] pointCoords = point.getCoordinates();
		int[] centCoords = centroid.getCoordinates();
		for(int i = 0; i < pointCoords.length; i++){
			distance += (pointCoords[i] - centCoords[i]) * (pointCoords[i] - centCoords[i]); 
		}
		return distance;
	}

	public static List<Value> getPartialCentroidsFromFile(Path filePath) {
		List<Value> partialCentroids = new ArrayList<Value>();
		Configuration conf = new Configuration();
		Reader reader = null;
		try {
			FileSystem fs = filePath.getFileSystem(conf);
			reader = new SequenceFile.Reader(fs, filePath, conf);
			Class<?> valueClass = reader.getValueClass();
			IntWritable key;
			try {
				key = reader.getKeyClass().asSubclass(IntWritable.class).newInstance();
			} catch (InstantiationException e) { // Should not be possible
				throw new IllegalStateException(e);
			} catch (IllegalAccessException e) {
					throw new IllegalStateException(e);
			}
			Value value = new Value();
			while (reader.next(key, value)) {
				partialCentroids.add(value);
				value = new Value();
			}
        } catch (IOException e) {
			e.printStackTrace();
		} finally {
        	try{
        		if(reader != null)
        			reader.close();
        	} catch (IOException e) {
        		e.printStackTrace();
        	}
        }
		return partialCentroids;
	}
	
	public static List<Value> getCentroidsFromFile(Path filePath, boolean isReduceOutput) {
		List<Value> partialCentroids = new ArrayList<Value>();
		Configuration conf = new Configuration();
		Reader reader = null;
		try {
			FileSystem fs = filePath.getFileSystem(conf);
			if(isReduceOutput){
				FileStatus[] parts = fs.listStatus(filePath);
			    for (FileStatus part : parts) {
			      String name = part.getPath().getName();
			      if (name.startsWith("part") && !name.endsWith(".crc")) {
			        reader = new SequenceFile.Reader(fs, part.getPath(), conf);
			        try {
			          Key key = reader.getKeyClass().asSubclass(Key.class).newInstance();
			          Values values = new Values();
			          while (reader.next(key, values)) {
			        	  for(Value value : values.getValues())
								partialCentroids.add(value);
			        	  values = new Values();
			          }
			        } catch (InstantiationException e) { // shouldn't happen
			          e.printStackTrace();
			          throw new IllegalStateException(e);
			        } catch (IllegalAccessException e) {
			        	e.printStackTrace();
			          throw new IllegalStateException(e);
			        }
			      }
			    }
			}
			else{
				reader = new SequenceFile.Reader(fs, filePath, conf);
				Class<?> valueClass = reader.getValueClass();
				Key key;
				try {
					key = reader.getKeyClass().asSubclass(Key.class).newInstance();
				} catch (InstantiationException e) { // Should not be possible
					throw new IllegalStateException(e);
				} catch (IllegalAccessException e) {
						throw new IllegalStateException(e);
				}
				Values values = new Values();
				while (reader.next(key, values)) {
					for(Value value : values.getValues())
						partialCentroids.add(value);
					values = new Values();
				}
			}
        } catch (IOException e) {
			e.printStackTrace();
		} finally {
        	try{
        		if(reader != null)
        			reader.close();
        	} catch (IOException e) {
        		e.printStackTrace();
        	}
        }
		return partialCentroids;
	}
	
	public static int getDistance(int[] point, int[] centroid) {
		int distance = 0;
		for(int i = 0; i < point.length; i++){
			distance += (point[i] - centroid[i]) * (point[i] - centroid[i]); 
		}
		return distance;
	}
	
	//public static int[] ratio = {2, 4, 8, 16, 32, 64};
	
	public static void prepareInput(int count, int k, int dimension, int taskCount,
		      Configuration conf, Path[] in, Path center, FileSystem fs, int[] ratio)
		      throws IOException {
		int cIdxSeq = 0;
//		int rSigma = 0;
//		int[] distribution = new int[taskCount];
//		for(int i = 0; i < taskCount; i++){
//			rSigma += ratio[i];
//		}
//		int singlePart = count/rSigma;
//		for(int i=0; i < taskCount; i++){
//			if(i == 0)
//				distribution[i] = ratio[i];
//			else
//				distribution[i] = distribution[i-1] + ratio[i];
//		}
		
//		if (fs.exists(out))
//			fs.delete(out, true);
		if (fs.exists(center))
			fs.delete(center, true);
		final SequenceFile.Writer centerWriter = SequenceFile.createWriter(fs,
		        conf, center, Key.class, Value.class,
		        CompressionType.NONE);
		Values centers = new Values(k);
		List<Value> centerArray = centers.getValues();
		int ki = 0;
		for(int i =0 ; i < in.length; i++){
			if (fs.exists(in[i]))
				fs.delete(in[i], true);
			final SequenceFile.Writer dataWriter = SequenceFile.createWriter(fs, conf,
			        in[i], Key.class, Values.class, CompressionType.NONE);
			Random r = new Random(1000);
			//maximum index in this file
			int maxIdx = count < (i+1)*count/in.length ? count : (i+1)*count/in.length;
			Values values = new Values(maxIdx - (i * count/in.length));
			List<Value> valArray = new ArrayList<Value>();
			
			for (int j = i*count/in.length ; j < maxIdx ; j++) {
				int[] arr = new int[dimension];
				for (int d = 0; d < dimension; d++) {
					arr[d] = r.nextInt(count);
				}
				Value vector = new Value(dimension);
				vector.setCoordinates(arr);
				valArray.add(vector);
				if (k > ki) {
					vector.setCentroidIdx(cIdxSeq++);
					//Need this line for Phadoop and write centers along with the data and comment the one below it.
					centerArray.add(vector);
					//Need this for Phadoop version, so that the centers can be read for the first time
					centerWriter.append(new Key(r.nextInt(taskCount), VectorType.CENTROID),vector);
					ki++;
				}
			}
			values.setValues(valArray);
			dataWriter.append(new Key(i, VectorType.REGULAR), values);
			dataWriter.append(new Key(i, VectorType.CENTROID), centers);
			dataWriter.close();
			if(DEBUG) System.out.println("Done writing to :"+ in[i].toString());
		}
		centerWriter.close();
	}
	
	
	private static int getTaskIndex(int vectorNumber, int singlePart, 
								int taskCount, int[] distribution) {
		int taskIdx = -1;
		int quo = vectorNumber / singlePart;
		for(int i = 0; i < taskCount; i++){
			if(quo < distribution[i]){
				taskIdx = i;
				break;
			}
		}
		//if not less than any ratio term then assign it to the last task
		return (taskIdx == -1) ? (taskCount-1) : taskIdx;
	}

	//picked up from mahout library
	public static List<Value> chooseRandomPoints(Collection<Value> vectors, int k) {
	    List<Value> chosenPoints = new ArrayList<Value>(k);
	    Random random = new Random();
	    for (Value value : vectors) {
	      int currentSize = chosenPoints.size();
	      if (currentSize < k) {
	        chosenPoints.add(value);
	      } else if (random.nextInt(currentSize + 1) == 0) { // with chance 1/(currentSize+1) pick new element
	        int indexToRemove = random.nextInt(currentSize); // evict one chosen randomly
	        chosenPoints.remove(indexToRemove);
	        chosenPoints.add(value);
	      }
	    }
	    return chosenPoints;
	}
}
