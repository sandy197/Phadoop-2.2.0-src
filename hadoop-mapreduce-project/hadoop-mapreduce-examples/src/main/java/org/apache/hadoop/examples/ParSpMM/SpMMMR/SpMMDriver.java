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
import org.apache.hadoop.mapred.JobConf;
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
import org.ncsu.sys.*;
import org.znerd.xmlenc.Library;

/**
 * Driver class invokes the jobs for matrix multiply based on 
 * the parameters configured.
 * @author sandeep
 *
 */
public class SpMMDriver {
	
	// a logical 2 x 4 core grid
	public static final int SPMM_PROC_GRID_DIMM_X = 2;
	public static final int SPMM_PROC_GRID_DIMM_Y = 3;
	
	public static final int NZ_INCRIMENT = 5;
	
	private static final String SPMM_DATA_DIR = "tmp/spmm/";
	private static final String SPMM_INPUT_PATH_A = SPMM_DATA_DIR + "/A";
	private static final String SPMM_INPUT_PATH_B = SPMM_DATA_DIR + "/B";
	private static final String SPMM_TEMP_DIR_PATH = SPMM_DATA_DIR;
	private static final String SPMM_TEMP_OUTPUT_PATH = SPMM_DATA_DIR + "/tmp/C";
	private static final String SPMM_OUTPUT_PATH = SPMM_DATA_DIR + "/C";
	private static final boolean DEBUG = true;
	
	private static FileSystem fs;
	private static JobConf conf = new JobConf();
	private UseRAPL librapl;
	private List<PowerStatus<Double>> powerStatus;
	
	
	private boolean isASparse;
	private boolean isBSparse;
	
	public SpMMDriver() {
		this.isASparse = true;
		this.isBSparse = true;
		librapl = new UseRAPL();
        powerStatus = new ArrayList<PowerStatus<Double>>();
    	librapl.initRAPL("mmPower");
	}
	
	public SpMMDriver(boolean isASparse, boolean isBSparse){
		this.isASparse = isASparse;
		this.isBSparse = isBSparse;
		librapl = new UseRAPL();
        powerStatus = new ArrayList<PowerStatus<Double>>();
    	librapl.initRAPL("mmPower");
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
						int aRowBlk, int aColbRowBlk, int bColBlk, boolean isCalibration){
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

	    Random rand = new Random();
	    conf.setBoolean("SpMM.isCalibration", isCalibration);
		conf.setInt("SpMM.jobToken", rand.nextInt());
	    //TODO:take these as command line param
	    conf.setBoolean("SpMM.useTaskPool", true);
	    conf.setBoolean("SpMM.isSparseMM", true);
	    
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
		
		uri = new URI("hdfs://localhost/libraries/librapl.so.1#librapl.so");
		DistributedCache.createSymlink(conf);
		DistributedCache.addCacheFile(uri, conf);
		
		boolean isCalcStarted = false;
		int calcStartIter = conf.getInt("RAPL.calibrationIterationCount", 4) + 1;
	    for(int k = 0; k < k_max; k++){
	    	if(!isCalcStarted && k == calcStartIter){
	    		this.powerCalculator.start();
	    		isCalcStarted = true;
	    	}
	    	
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
		finally{
			if(this.powerCalculator.isAlive())
				this.powerCalculator.interrupt();
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
		GenericOptionsParser goParser = new GenericOptionsParser(conf, args);
		fs = FileSystem.get(conf);
		fs.mkdirs(new Path(SPMM_DATA_DIR));
		SpMMDriver driver = new SpMMDriver(true, true);
//		assumed core grid (2 x 3)\
//		int I = 60;
//		int K = 60;
//		int J = 60;
//		
//		int IB = 15;
//		int KB = 20;
//		int JB = 30;
		String[] remainingArgs = goParser.getRemainingArgs();
		int I = Integer.parseInt(remainingArgs[0]);
		int K = Integer.parseInt(remainingArgs[1]);
		int J = Integer.parseInt(remainingArgs[2]);
		
		int IB = Integer.parseInt(remainingArgs[3]);
		int KB = Integer.parseInt(remainingArgs[4]);
		int JB = Integer.parseInt(remainingArgs[5]);
		
		int nzc = Integer.parseInt(remainingArgs[6]);
		int nzr = Integer.parseInt(remainingArgs[7]);
		int diff = Integer.parseInt(remainingArgs[8]);
		
		boolean isAUniform = Integer.parseInt(remainingArgs[9]) == 0 ? true : false;
		
		boolean isCalibration = Integer.parseInt(remainingArgs[10]) == 1 ? true : false;
		
		int[][] A = new int[I][K];
		int[][] B = new int[K][J];
		
		buildBlockedMatrix(A, I, K, IB, KB, nzc, nzr, diff,false, isAUniform);
		System.out.println("Built blocked A");
		buildBlockedMatrix(B, K, J, KB, JB, nzc, nzr, diff,true, !isAUniform);
		System.out.println("Built blocked B");
		
		driver.writeMatrix(A, I, K, SPMM_INPUT_PATH_A);
		driver.writeMatrix(B, K, J, SPMM_INPUT_PATH_B);
		
		if(isCalibration){
			long defaultPowerCap0 = driver.librapl.getPowerLimit(0);
			long defaultPowerCap1 = driver.librapl.getPowerLimit(1);
			for(int powerCap = 50; powerCap > 10; powerCap -= 10){
				driver.librapl.setPowerLimit(0, powerCap);
				driver.librapl.setPowerLimit(1, powerCap);
				Thread.sleep(2000);
				
				driver.SpMM(2, I, K, J, IB, KB, JB, isCalibration);
			}
			driver.librapl.setPowerLimit(0, defaultPowerCap0);
			driver.librapl.setPowerLimit(1, defaultPowerCap1);
		}
		else {
			long start = System.nanoTime();
			System.out.println("I="+ I+", K="+ K+", J,"+J+" IB="+ IB+", KB="+KB+", JB="+JB);
			driver.SpMM(2, I, K, J, IB, KB, JB, isCalibration);
			long end = System.nanoTime();
			//recursive delete of data dir
			fs.delete(new Path(SPMM_DATA_DIR), true);
			System.out.println("Time taken for total execution:" + (end - start));
			printAvgPowerConsumption(driver.powerStatus);
		}
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
	
	private static void buildMatrix(int[][] a2, int rows, int cols) {
		Random rand = new Random();
		for(int i = 0; i < rows; i++){
			for(int j = 0; j < cols; j++){
				a2[i][j] = rand.nextInt(1000);
			}
		}		
	}
	
	private static int calcIndex(HashSet<Integer> hset, Random colIndx, int rangeStart, int rangeEnd){
		int cidx = 0;
		do {
			if(DEBUG) System.out.println("Contains cidx:"+cidx);
		if(cidx != 0){
			cidx = (cidx + 1)% rangeEnd;
		}
		cidx = colIndx.nextInt(rangeEnd - rangeStart) % rangeEnd;
		cidx = cidx < 0 ? cidx * -1:cidx;
		} while(hset.contains(cidx));
		hset.add(cidx);
		return cidx;
	}
	
	private static void buildBlockedMatrix(int [][]A, int rows, int cols, int brows, int bcols, int nzc, int nzr, int diff, boolean isRowMajor, boolean isUniform){
		HashSet<Integer> rhset, chset;
		Random randNum = new Random(1);
		Random colIndx = new Random(2);	
		Random rowIndx = new Random(3);
		for(int i = 0; i < rows; i++){
			for(int j = 0; j < cols; j++){
				A[i][j] = 0;
			}
		}
		for(int roffset = 0; roffset < rows; roffset += brows){
			for(int coffset = 0; coffset < cols; coffset += bcols){
				int nzr_d, nzc_d;
//				nzr_d = nzr + ((roffset/brows)*(cols/bcols)+(coffset/bcols)) * NZ_INCRIMENT;
//				nzc_d = nzc + ((roffset/brows)*(cols/bcols)+(coffset/bcols)) * NZ_INCRIMENT;
				nzr_d = nzr;
				nzc_d = nzc;
				
				
				int rowCount =0, colCount = 0;
				if(isRowMajor){
//					nzr_d = nzr + ((roffset/brows)) * NZ_INCRIMENT;
//					nzc_d = nzc + ((roffset/brows)) * NZ_INCRIMENT;
					if(!isUniform){
						nzr_d = nzr + ((roffset/brows)*(cols/bcols)+(coffset/bcols)) * diff;
						nzc_d = nzc + ((roffset/brows)*(cols/bcols)+(coffset/bcols)) * diff;
					}
					rhset = new HashSet<Integer>();
					while(rowCount < nzr_d){
						if(DEBUG) System.out.println("loop1:"+rowCount+":"+nzr_d);
						int ridx = roffset + calcIndex(rhset, rowIndx, 0, brows);
						colCount = 0;
						chset = new HashSet<Integer>();
						while(colCount < nzc_d){
							if(DEBUG) System.out.println("loop2:"+colCount+":"+nzc_d);
							int cidx = coffset + calcIndex(chset, colIndx, 0, bcols);
							A[ridx][cidx] = randNum.nextInt();
							colCount++;
						}
						rowCount++;
					}
				}
				else{
					if(!isUniform){
						nzr_d = nzr + (coffset/bcols) * NZ_INCRIMENT;
						nzc_d = nzc + (coffset/bcols) * NZ_INCRIMENT;
					}
					chset = new HashSet<Integer>();
					while(colCount < nzc_d){
						if(DEBUG) System.out.println("loop3:"+ colCount+":"+nzc_d);
						int cidx = coffset + calcIndex(chset, colIndx, 0, bcols);
						rowCount = 0;
						rhset = new HashSet<Integer>();
						while(rowCount < nzr_d){
							if(DEBUG) System.out.println("loop4:"+rowCount+":"+nzr_d);
							int ridx =  roffset + calcIndex(rhset, rowIndx, 0, brows);
							A[ridx][cidx] = randNum.nextInt();
							rowCount++;
						}
						colCount++;
					}
				}
			}
		}
		
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
