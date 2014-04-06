package org.apache.hadoop.examples.ParSpMM.SpMMMR;

import java.io.*;
import java.net.URI;
import java.util.*;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.examples.ParSpMM.SpMMMR.SpMMTypes.Key;
import org.apache.hadoop.examples.ParSpMM.SpMMMR.SpMMTypes.Value;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.filecache.DistributedCache;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.mapreduce.lib.reduce.IntSumReducer;
import org.apache.hadoop.util.GenericOptionsParser;

/**
 * Driver class invokes the jobs for matrix multiply based on 
 * the parameters configured.
 * @author sandeep
 *
 */
public class SpMMDriver {
	
	// a logical 2 x 4 core grid
	public static final int SPMM_PROC_GRID_DIMM_X = 3;
	public static final int SPMM_PROC_GRID_DIMM_Y = 2;
	
	private static final String SPMM_DATA_DIR = "tmp/spmm/";
	private static final String SPMM_INPUT_PATH_A = SPMM_DATA_DIR + "/A";
	private static final String SPMM_INPUT_PATH_B = SPMM_DATA_DIR + "/B";
	private static final String SPMM_TEMP_DIR_PATH = SPMM_DATA_DIR;
	private static final String SPMM_TEMP_OUTPUT_PATH = SPMM_DATA_DIR + "/tmp/C";
	private static final String SPMM_OUTPUT_PATH = SPMM_DATA_DIR + "/C";
	
	private static FileSystem fs;
	private static Configuration conf = new Configuration();
	
	private boolean isASparse;
	private boolean isBSparse;
	
	public SpMMDriver() {
		this.isASparse = true;
		this.isBSparse = true;
	}
	
	public SpMMDriver(boolean isASparse, boolean isBSparse){
		this.isASparse = isASparse;
		this.isBSparse = isBSparse;
	}
	
	/**
	 * Implementation is in-line with CombBLAS.PSpGEMM.
	 * 
	 * P[aRows/aRowBlk, bCols/bColBlk] 
	 * 
	 * 
	 * Deals with square matrices only.
	 * @param aRows
	 * @param aColsbRows
	 * @param bCols
	 * @param blocksize		Number of aRows/bCols each block contains
	 */
	@SuppressWarnings("deprecation")
	public void SpMM(int strategy, int aRows, int aColsbRows, int bCols, 
						int aRowBlk, int aColbRowBlk, int bColBlk){
		try {
		if (conf == null) throw new Exception("conf is null");
		FileSystem fs;
		
			fs = FileSystem.get(conf);
		
		String inputPathA = fs.makeQualified(new Path(SPMM_INPUT_PATH_A)).toString();
		String inputPathB = fs.makeQualified(new Path(SPMM_INPUT_PATH_B)).toString();
		String outPath = fs.makeQualified(new Path(SPMM_OUTPUT_PATH)).toString();
	    String tempDirPath = fs.makeQualified(new Path(SPMM_TEMP_DIR_PATH)).toString();
	    tempDirPath = SPMM_TEMP_DIR_PATH + "/SpMM-" +
	          Integer.toString(new Random().nextInt(Integer.MAX_VALUE));

	    //TODO:take these as command line param
	    conf.setBoolean("SpMM.useTaskPool", true);
	    conf.setBoolean("SpMM.isSparseMM", false);
	    
	    conf.set("SpMM.inputPathA", inputPathA);
	    conf.set("SpMM.inputPathB", inputPathB);
	    conf.set("SpMM.outputDirPath", outPath);
	    conf.set("SpMM.tempDirPath", tempDirPath);
	    conf.setInt("SpMM.strategy", strategy);
	    conf.setInt("SpMM.R1", 6);
	    conf.setInt("SpMM.R2", 4);
	    conf.setInt("SpMM.I", aRows);
	    conf.setInt("SpMM.K", aColsbRows);
	    conf.setInt("SpMM.J", bCols);
	    conf.setInt("SpMM.IB", aRowBlk);
	    conf.setInt("SpMM.KB", aColbRowBlk);
	    conf.setInt("SpMM.JB", bColBlk);
	    
		fs.delete(new Path(tempDirPath), true);
		fs.delete(new Path(outPath), true);
		
		int k_max = 0;
		switch(strategy){
		case 1:
			k_max = aColsbRows/aColbRowBlk;
			break;
		case 2:
			k_max = aRows/aRowBlk;
			break;
		default:
			break;
		}
		URI uri = new URI("hdfs://localhost/libraries/libpapi.so.1#libpapi.so");
		DistributedCache.createSymlink(conf); 
		DistributedCache.addCacheFile(uri, conf);
	    for(int k = 0; k < k_max; k++){
	    	conf.setInt("SpMM.iteration", k);
	    	long start = System.nanoTime();
	    	bCastJob(conf, strategy, k, k < 1);
	    	long end = System.nanoTime();
	    	System.out.println("Time taken for bcast execution:"+(end - start));
		}
	    //TODO:implement this
	    long start = System.nanoTime();
	    aggregateJob(conf, k_max);
	    long end = System.nanoTime();
	    System.out.println("Time taken for aggregate execution:"+(end - start));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	

	private void aggregateJob(Configuration conf, int k) {
		Job job;
		try {
			job = Job.getInstance(conf, "Sparse matrix multiplication aggregator");
		
	    job.setJarByClass(SpMMMapper.class);
	    job.setNumReduceTasks(conf.getInt("SpMM.R2", 0));
	    job.setInputFormatClass(SequenceFileInputFormat.class);
	    job.setOutputFormatClass(SequenceFileOutputFormat.class);
	    job.setMapperClass(Mapper.class);
	    //job.setCombinerClass(IntSumReducer.class);
	    job.setReducerClass(SpMMReducer.class);
	    job.setOutputKeyClass(Key.class);
	    job.setOutputValueClass(Value.class);
	    for(int i = 0; i < k; i++)
	    	FileInputFormat.addInputPath(job, new Path(conf.get("SpMM.tempDirPath")+i));
	    FileOutputFormat.setOutputPath(job, new Path(conf.get("SpMM.outputDirPath")));
	    boolean ok;
		ok = job.waitForCompletion(true);
		
	    if (!ok) throw new Exception("Job 2 failed");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e){
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * This job multiplies A[i,k] with B[k,j], which are blocks in matrices 
	 * A and B respectively.
	 *
	 * @param k
	 * @throws Exception 
	 */
	private void bCastJob(Configuration config, int strategy, int k, boolean isFirstIter) throws Exception {
		//TODO:Form keys and read only those pertaining to the block. 
		//Make use of the sorting order.
		config.setInt("SpMM.iteration", k);
		Job job;
		try {
			job = Job.getInstance(conf, "Matrix Multiply Job 1");
		
	    job.setJarByClass(SpMMMapper.class);
	    job.setNumReduceTasks(conf.getInt("SpMM.R1", 0));
	    System.out.println("Number of reduce tasks for job1 set to: "+ conf.getInt("SpMM.R1", 0));
	    job.setInputFormatClass(SequenceFileInputFormat.class);
	    job.setOutputFormatClass(SequenceFileOutputFormat.class);
	    job.setMapperClass(SpMMMapper.class);
	    job.setReducerClass(SpMMReducer.class);
	    switch(strategy){
	    case 1:
	    	job.setPartitionerClass(SpMMPatitioner.class);
	    	break;
	    case 2:
	    	job.setPartitionerClass(SpMMPartitioner2.class);
	    	break;
	    default:
	    	job.setPartitionerClass(SpMMPatitioner.class);
	    	break;
	    }
	    job.setMapOutputKeyClass(Key.class);
	    job.setMapOutputValueClass(Value.class);
	    job.setOutputKeyClass(Key.class);
	    job.setOutputValueClass(Value.class);
	    FileInputFormat.addInputPath(job, new Path(conf.get("SpMM.inputPathA")));
	    if(isFirstIter)
	    	FileInputFormat.addInputPath(job, new Path(conf.get("SpMM.inputPathB")));
	    FileOutputFormat.setOutputPath(job, (new Path(conf.get("SpMM.tempDirPath") + k)));
	    
	    boolean ok = job.waitForCompletion(true);
	    if (!ok) throw new Exception("Job 1 failed");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	  }
		
	
	
	@SuppressWarnings("deprecation")
	public void writeMatrix (int[][] matrix, int rowDim, int colDim, String pathStr)
		    throws IOException
	  {
	    Path path = new Path(pathStr);
	    SequenceFile.Writer writer = SequenceFile.createWriter(fs, conf, path,
	      SpMMTypes.IndexPair.class, IntWritable.class,
	      SequenceFile.CompressionType.NONE);
	    SpMMTypes.IndexPair indexPair = new SpMMTypes.IndexPair();
	    IntWritable el = new IntWritable();
	    for (int i = 0; i < rowDim; i++) {
	      for (int j = 0; j < colDim; j++) {
	        int v = matrix[i][j];
	        if (v != 0) {
	          indexPair.index1 = i;
	          indexPair.index2 = j;
	          el.set(v);
	          writer.append(indexPair, el);
	        }
	      }
	    }
	    writer.close();
	  }
	
	public static void main (String[] args) throws Exception
	{
		//needed for parsing the generic options(jar files and .so files for JNI
		new GenericOptionsParser(conf, args);
		fs = FileSystem.get(conf);
		fs.mkdirs(new Path(SPMM_DATA_DIR));
		SpMMDriver driver = new SpMMDriver(true, true);
		//assumed core grid (2 x 3)\
		int I = 60;
		int K = 60;
		int J = 60;
		
		int IB = 15;
		int KB = 20;
		int JB = 30;
		
		int[][] A = { {0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0},
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0},
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0},
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0},
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0},
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0},
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0},
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0},
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0},
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0},
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0},
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0},
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0},
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0},
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0},
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0},
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0},
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0},
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0},
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0},
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0},
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0},
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0},
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0},
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0},
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0},
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0},
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0},
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0},
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0}
		};
		
		int[][] B = {{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},
				
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0},
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0},
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0},
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0},
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0},
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0},
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0},
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0},
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0},
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0},
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0},
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0},
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0},
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0},
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0},
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0},
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0},
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0},
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0},
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0},
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0},
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0},
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0},
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0},
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0},
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0},
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0},
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0},
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0},
				{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0}
				
		};
		driver.writeMatrix(A, I, K, SPMM_INPUT_PATH_A);
		driver.writeMatrix(B, K, J, SPMM_INPUT_PATH_B);
		long start = System.nanoTime();
		driver.SpMM(2, I, K, J, IB, KB, JB);
		long end = System.nanoTime();
		System.out.println("Time taken for total execution:" + (end - start));
	}
	
	/*

	public static void main(String[] args) throws Exception {
		Configuration conf = new Configuration();
		Job job = Job.getInstance(conf, "Spare MM MR");
		job.setJarByClass(org.ncsu.sys.SpMMMR.SpMMDriver.class);
		job.setMapperClass(org.ncsu.sys.SpMMMR.SpMMMapper.class);
		job.setReducerClass(org.ncsu.sys.SpMMMR.SpMMReducer.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);

		// TODO: specify input and output DIRECTORIES (not files)
		FileInputFormat.setInputPaths(job, new Path("src"));
		FileOutputFormat.setOutputPath(job, new Path("out"));

		if (!job.waitForCompletion(true))
			return;
	}
	*/
}
