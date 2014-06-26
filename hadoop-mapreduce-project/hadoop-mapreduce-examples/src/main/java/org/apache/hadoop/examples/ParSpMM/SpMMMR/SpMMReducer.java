package org.apache.hadoop.examples.ParSpMM.SpMMMR;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.ResettableIterator;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.examples.MKmeans.PartialCentroid;
import org.apache.hadoop.examples.ParSpMM.SpMM.SpDCSC;
import org.apache.hadoop.examples.ParSpMM.SpMM.SpUtils;
import org.apache.hadoop.examples.ParSpMM.SpMM.StackEntry;
import org.apache.hadoop.examples.ParSpMM.SpMM.SyncPrimitive;
import org.apache.hadoop.examples.ParSpMM.SpMM.SyncPrimitive.Barrier;
import org.apache.hadoop.examples.ParSpMM.SpMMMR.SpMMTypes.IndexPair;
import org.apache.hadoop.examples.ParSpMM.SpMMMR.SpMMTypes.Key;
import org.apache.hadoop.examples.ParSpMM.SpMMMR.SpMMTypes.Value;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.SequenceFile.CompressionType;
import org.apache.hadoop.io.SequenceFile.Reader;
import org.apache.hadoop.ipc.GenericMatrix;
import org.apache.hadoop.ipc.RAPLCalibration;
import org.apache.hadoop.ipc.RAPLIterCalibration;
import org.apache.hadoop.mapred.RAPLRecord;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.join.ResetableIterator;
import org.apache.zookeeper.KeeperException;
import org.ncsu.sys.*;
import org.znerd.xmlenc.Library;


public class SpMMReducer extends Reducer<Key, Value, Key, Value> {
	public static int CORES_PER_PKG = 8;
	
	private GenericMatrix<?> A, B;
	private Key indexPair;
	private Value el = new Value();
	
	public static String ZK_ADDRESS = "10.1.255.13:2181";
	private static final boolean DEBUG = true;

	private boolean useTaskPool;
	private boolean isSparseMM;
	private String inputPathA;
	private String inputPathB;
	private String outputDirPath;
	private String tempDirPath;
	private static int R1;
	private static int R2;
	private static int I;
	private static int K;
	private static int J;
	private static int IB;
	private static int KB;
	private static int JB;
	
	private static int NIB;
	private static int NKB;
	private static int NJB;
	
	private static boolean useM;
	
	private static int lastIBlockNum;
	private static int lastIBlockSize;
	private static int lastKBlockNum;
	private static int lastKBlockSize;
	private static int lastJBlockNum;
	private static int lastJBlockSize;
	
	private int sib, skb, sjb;
	private boolean isABuilt, isBBuilt, useRAPL, isCalibrate;
	private int multiplyCount = 0;
	private RAPLRecord record;
	private ThreadPinning rapl;
	private UseRAPL urapl;
	private int iterationCount;
	private int jobToken;
	
	private RAPLIterCalibration iCalibration;


	public void reduce(SpMMTypes.Key key, Iterable<SpMMTypes.Value> values, Context context)	
			throws IOException, InterruptedException {
		
		 if (DEBUG) printReduceInputKey(key);
	      int ib, kb, jb, nz1, nz2;
	      int sum = 0;
	      //job 2 reduce function
	      if(key.index2 < 0){
	    	  //sum up all the values and write
	    	  for(Value val : values){
	    		  sum += val.v;
	    	  }
	    	  el.set(sum);
	    	  context.write(key, el);
	    	  
	    	  //Dummy record, just to avoid exceptions
	    	  if(record == null){
					//NOTE : this doesn't work if the classify is done more than once per map task
					record = new RAPLRecord();
				}
				record.setJobtoken(jobToken);
				record.setExectime(0);
				//TODO:replace the hardcoded value with a JNI call to get the core this thread is pinned to
				int pkgIdx = rapl.get_thread_affinity() / CORES_PER_PKG;
				record.setPkg((short)pkgIdx);
				record.setInterationCount(iterationCount);
				//TODO : add hostname to record either here or in the appmaster (this info is readily available there)
//				record.setHostname(hostname);
				context.setRAPLRecord(record);
				
	    	  return;
	      }
	      
          ib = key.index1;
          kb = key.index2;
          jb = key.index3;
          if (key.m == 0) {
            sib = ib;
            skb = kb;
            A = build(values, IB, KB, context);
            isABuilt = true;
            
          } else {
            //if (ib != sib || kb != skb) return;
            //bColDim = getDim(jb, lastJBlockNum, JB, lastJBlockSize);
            B = buildAndSet(values, KB, JB, context);
            isBBuilt = true;
          }
          //multiply & emit
          //support building normal matrix as well.
          //check if the matrix is already read if B is null
          if(B == null && useTaskPool && context.getMatrix() != null){
      			System.out.println("**Getting matrix already read from fs. Skipping reading the file");
      			B = context.getMatrix();
      			isBBuilt = true;
      		}
          //multiply only of both A and B are populated
          if(isABuilt && isBBuilt){
        	  multiplyCount++;
        	  ThreadPinning tp = new ThreadPinning();
        	  tp.start_counters();
        	  long start = System.nanoTime();
        	  long multiplyTime = multiplyAndEmit(context, ib, jb, false);
//        	  Barrier b = new Barrier(ZK_ADDRESS, "/b1", 6);
//              try{
//                  boolean flag = b.enter();
//                  System.out.println("Entered barrier: " + 6);
//                  if(!flag) System.out.println("Error when entering the barrier");
//              } catch (KeeperException e){
//            	  e.printStackTrace();
//              } catch (InterruptedException e){
//            	  e.printStackTrace();
//              }
        	  long end = System.nanoTime();
        	  long[] counterValues = tp.stop_counters();
      		// density(B),nnzc(B),x*y*Intersect(nnzc(A), nnzr(B)),execTime
        	  StringBuilder sb = new StringBuilder();
        	  sb.append("$$\t");
        	  if(A instanceof SpMMMatrix && B instanceof SpMMMatrix){
        		  int intersectCount = SpUtils.setIntersectionCount(((SpMMMatrix)A).getnzcIndices(), ((SpMMMatrix)B).getnzrIndices());
            		//avg nz elements per nzc of A
        		  float x = (float)((SpMMMatrix)A).getAvgNZperNZC();
            		//avg nz elements per nzr of B
        		  float y = (float)((SpMMMatrix)B).getAvgNZperNZR();
            		// density(A), density(B), nnzc(B), x*y*Intersect(nnzc(A), nnzr(B)),execTime        	  
              	  
              	  sb.append(((float)((SpMMMatrix)A).getMatrixDensity())+"\t"+
      						((float)((SpMMMatrix)B).getMatrixDensity())+"\t"+((SpMMMatrix)B).getMatrix().jc.size()+"\t"+
      						(x*y*intersectCount)+"\t"+intersectCount+"\t"+multiplyTime+"\t"+(end - start)+"\t");
        	  }
        	  if(record == null){
					//NOTE : this doesn't work if the classify is done more than once per map task
					record = new RAPLRecord();
				}
				record.setJobtoken(jobToken);
				record.setExectime(multiplyTime);
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
//				if(doCalibrate){
//					RAPLCalibration calibration = calibrate(context, ib, jb);
//					doCalibrate = false;
//					if(calibration != null)
//						((SpMMMatrix) context.getMatrix()).addCalibration(calibration);
//				}
				if(isCalibrate){
					SequenceFile.Writer dataWriter = null;
					RAPLCalibration calibration = new RAPLCalibration();
					calibration.addRAPLExecTime(urapl.getPowerLimit(pkgIdx), multiplyTime);
					RAPLIterCalibration iCalib;
					GenericMatrix<?> cachedMat = context.getMatrix();
					if(cachedMat instanceof SpMMMatrix){
						iCalib = ((SpMMMatrix) cachedMat).getIterCalibration();
					}
					else
						iCalib = ((RegMatrix) cachedMat).getIterCalibration();
					System.out.println("Adding calibration value:"+calibration+"for iteration:"+iterationCount);
					iCalib.addCalibration(iterationCount, calibration);
					for(Integer i : iCalib.getItrToCalibMap().keySet()){
						if(DEBUG) System.out.println(i + ":\n" + iCalib.getItrToCalibMap().get(i));
					}
					Configuration conf = context.getConfiguration();
					if(iterationCount == conf.getInt("RAPL.calibrationCount", 4)){
						//Write calibration data to file
						int filename = context.getTaskAttemptID().getTaskID().getId();
						FileSystem fs = FileSystem.get(conf);
						Path filePath = new Path("tmp/rapl/SpMM/calib", filename+"");
						System.out.println("Writing calibration data to:"+filePath);
						dataWriter = SequenceFile.createWriter(fs, conf,
							    filePath, IntWritable.class, RAPLCalibration.class, CompressionType.NONE);
						for(Integer i : iCalib.getItrToCalibMap().keySet()){
							if(DEBUG) System.out.println(i + ":\n" + iCalib.getItrToCalibMap().get(i));
							dataWriter.append(new IntWritable(i), iCalib.getItrToCalibMap().get(i));
						}
						dataWriter.close();
					}
				}
        	  //sb.append(multiplyCount +"\t");
        	  for(int i = 0; i < counterValues.length; i++){
                  //System.out.println("Counter Values");
                  sb.append(counterValues[i]+ "\t");
        	  }
        	  System.out.println(sb.toString());
          }
	}

//	private RAPLCalibration calibrate(Context context, int ib2,
//			int jb2) throws InterruptedException{
//		long multiplyTime;
//		RAPLCalibration calibration = new RAPLCalibration();
//		UseRAPL urapl = new UseRAPL();
//		urapl.initRAPL("maptask");
//		int pkg = rapl.get_thread_affinity()/8;
//		long origLimit = urapl.getPowerLimit(pkg);
//		for(int powerCap = 50; powerCap > 5; powerCap -= 5){
//			//set power cap
//			urapl.setPowerLimit(pkg, powerCap);
//			//wait 2 seconds for the power cap to kick in
//			Thread.sleep(2000);
//			//execute the core
//			long start =System.nanoTime();
//			multiplyTime = multiplyAndEmit(context, ib2, jb2, true);
//			long end =System.nanoTime();
//			//set the execution time in the calibration
//			calibration.addRAPLExecTime(powerCap, multiplyTime);
//			if(DEBUG) System.out.println("Lorg:" + multiplyTime + ":"+ pkg+ ":" + powerCap +":" + (end - start));
//		}
//		urapl.setPowerLimit(pkg, origLimit);
//		return calibration;
//	}

	private long multiplyAndEmit(Context context, int ib2,
			int jb2, boolean isCalibration) {
		if(isSparseMM){
			SpDCSC a, b;
			a = (SpDCSC) A.getMatrix();
			b = (SpDCSC) B.getMatrix();
			System.out.println(a);
			System.out.println(b);
			long start = System.nanoTime();
			List<StackEntry> multStack = a.SpMatMultiply(b);
			long end = System.nanoTime();
			if(!isCalibration){
				for(StackEntry se : multStack){
					if(se.value != 0){
						indexPair = new Key();
						indexPair.index1 = ib2*IB + se.key.first;
						indexPair.index2 = -1;
						indexPair.index3 = jb2*JB + se.key.second;
						el.set(se.value);
						try {
							context.write(indexPair, el);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
			return end - start;
		}
		else{
			//regular matrix multiply
			int[][] a,b;
			a = (int[][]) A.getMatrix();
			b = (int[][]) B.getMatrix();
			int ibase = ib2*IB;
			int jbase = jb2*JB;
			long multiplyTime = 0;
			long writingTime = 0;
			for (int i = 0; i < IB; i++) {
				for (int j = 0; j < JB; j++) {
					int sum = 0;
					long start_m = System.nanoTime();
					//increasing the number of iterations to check 
					//for a 5X increase in multiplication time
					for (int i_jff = 0; i_jff < 1000; i_jff++){
					for (int k = 0; k < KB; k++) {
						//srkandul
						if(a[i][k] != 0 && b[k][j] != 0){
							sum += a[i][k] * b[k][j];
						}
					}
					}
					long end_m = System.nanoTime();
					multiplyTime += end_m - start_m;
					long start = System.nanoTime();
					if (!isCalibration && sum != 0) {
						indexPair = new Key();
						indexPair.index1 = ibase + i;
						indexPair.index2 = -1;
						indexPair.index3 = jbase + j;
						el.set(sum);
						try {
							context.write(indexPair, el);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
					long end = System.nanoTime();
					writingTime += end - start;
				}
			}
			return multiplyTime;
		}
	}
	
	private GenericMatrix<?> buildAndSet(Iterable<Value> values, int kB2,
			int jB2, Context context) {
		GenericMatrix<?> genMatrix = build(values, kB2, jB2, context);
		System.out.println("**Setting matrix");
		if(!(isCalibrate && context.getMatrix() != null))
			context.setMatrix(genMatrix);
		return genMatrix;
	}

	private GenericMatrix<?> build(Iterable<Value> values, int m, int n, Context context) {
		if(isSparseMM)
			return new SpMMMatrix(new SpDCSC(values, m, n));
		else
			return new RegMatrix(build_orig(m, n, values));
	}
	
	private int[][] build_orig(int rowDim, int colDim,
            Iterable<Value> valueList)
    {
		int[][] matrix = new int[rowDim][colDim];
		int nonZeros = 0;
		for (int rowIndex = 0; rowIndex < rowDim; rowIndex++)
			for (int colIndex = 0; colIndex < colDim; colIndex++)
				matrix[rowIndex][colIndex] = 0;
		for (Value value : valueList) {
			if (DEBUG) printReduceInputValue(value);
			matrix[value.index1][value.index2] = value.v;
			if(value.v != 0){
				nonZeros++;
			}
		}
		return matrix;
    }

	public void setup (Context context) {
		init(context);
		Configuration conf = context.getConfiguration();
		System.loadLibrary("papi");
		if (DEBUG) {
			System.out.println("##### Reduce setup");
			System.out.println("   I = " + I);
			System.out.println("   K = " + K);
			System.out.println("   J = " + J);
			System.out.println("   IB = " + IB);
			System.out.println("   KB = " + KB);
			System.out.println("   JB = " + JB);
		}
//		A = new int[IB][KB];
//		B = new int[KB][JB];
		
		sib = -1;
		skb = -1;
		sjb = -1;
		
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
	
			if(record != null){
				iterationCount = 1 + record.getInterationCount();
				//TODO : Do this only if the iteration count is more than 4 and a flag to use this feature is set.
//				String testVar = conf.get("conftest");
//				if("test".equals(testVar)){
//					System.out.println("Able to read data from conf files");
//				}
				if(!isCalibrate && record.getExectime() != record.getTargetTime()){
					//get calibration data from the file
					GenericMatrix<?> cachedMat = context.getMatrix();
					if(cachedMat != null){
						if(cachedMat instanceof SpMMMatrix){
							iCalibration = ((SpMMMatrix) cachedMat).getIterCalibration();
						}
						else {
							iCalibration = ((RegMatrix) cachedMat).getIterCalibration();
						}
					}
					if(iCalibration == null || iCalibration.getItrToCalibMap().isEmpty()){
						iCalibration = readCalibrationFile(context);
						System.out.println("Read calibration data");
					}
					
					
					
					RAPLCalibration calibration = iCalibration.getItrToCalibMap().get(iterationCount);
					if(calibration != null){
						Map<Long, Long> cap2time = new HashMap<Long, Long>();
						for(Long i : calibration.getCapToExecTimeMap().keySet()){
							cap2time.put(i, calibration.getCapToExecTimeMap().get(i).getExecTime());
						}
		//				int calibIterCount = conf.getInt("RAPL.calibrationCount", 5);
		//				if(record.isDoCalibration() && iterationCount != 0 && iterationCount < calibIterCount){
		//					System.out.println("Setting doCalib to true:"+record.isDoCalibration()+":"+iterationCount);
		//					doCalibrate = true;
		//				}
						
						//rapl.adjustPower(record.getExectime(), record.getTargetTime());
						//get the power cap and set it only if this isn't a calibration round
						long powerCap = getPowerCap(record.getTargetTime(), cap2time);
						if(powerCap!=0){
							int pkg = rapl.get_thread_affinity()/8;
							System.out.println("Setting power cap of pkg:"+pkg+", to:"+powerCap+" watts");
							urapl.setPowerLimit(pkg, powerCap);
						}
					}
				}
			}
			else if(iterationCount == 0 && !isCalibrate){
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
	}
	
	private RAPLIterCalibration readCalibrationFile(Context context){
		RAPLIterCalibration iCalib = new RAPLIterCalibration();
		try{
			FileSystem fs = FileSystem.get(context.getConfiguration());
			
			int taskId = context.getTaskAttemptID().getTaskID().getId();
			IntWritable key;
			RAPLCalibration value;
			Reader calibReader = new SequenceFile.Reader(fs, new Path("tmp/rapl/SpMM/calib", taskId+""), context.getConfiguration());
			try {
				key = calibReader.getKeyClass().asSubclass(IntWritable.class).newInstance();
			} catch (InstantiationException e) { // Should not be possible
				throw new IllegalStateException(e);
			} catch (IllegalAccessException e) {
					throw new IllegalStateException(e);
			}
			value = new RAPLCalibration();
			while (calibReader.next(key, value)) {
				iCalib.getItrToCalibMap().put(key.get(), value);
				value = new RAPLCalibration();
			}
		}
		catch (IOException ex){
			ex.printStackTrace();
		}
		
		return iCalib;
	}

	/**
	 * Can be reused.
	 * @param targetTime
	 * @param cap2time
	 * @return
	 */
	private long getPowerCap(long targetTime, Map<Long, Long> cap2time) {
		long powerCap = 0;
		long min_diff = Long.MAX_VALUE;
		for(Long i : cap2time.keySet()){
			if(Math.abs(cap2time.get(i) - targetTime) < min_diff){
				min_diff = Math.abs(cap2time.get(i) - targetTime);
				powerCap = i;
			}
		}
		return powerCap;
	}
	
	private void init(JobContext context) {
		Configuration conf = context.getConfiguration();
		useTaskPool = conf.getBoolean("SpMM.useTaskPool", false);
		isSparseMM = conf.getBoolean("SpMM.isSparseMM", false);
		inputPathA = conf.get("SpMM.inputPathA");
		inputPathB	 = conf.get("SpMM.inputPathB");
		outputDirPath = conf.get("SpMM.outputDirPath");
		tempDirPath = conf.get("SpMM.tempDirPath");
		R1 = conf.getInt("SpMM.R1", 0);
		R2 = conf.getInt("SpMM.R2", 0);
		I = conf.getInt("SpMM.I", 0);
		K = conf.getInt("SpMM.K", 0);
		J = conf.getInt("SpMM.J", 0);
		IB = conf.getInt("SpMM.IB", 0);
		KB = conf.getInt("SpMM.KB", 0);
		JB = conf.getInt("SpMM.JB", 0);
		iterationCount = conf.getInt("SpMM.iteration", 0);
		System.out.println("Got iteration count as :"+iterationCount + "from the conf");
		jobToken = conf.getInt("SpMM.jobToken", -1);
		useRAPL = conf.getBoolean("RAPL.enable", false);
		isCalibrate = conf.getBoolean("SpMM.isCalibration", false);
		NIB = (I-1)/IB + 1;
		NKB = (K-1)/KB + 1;
		NJB = (J-1)/JB + 1;
		lastIBlockNum = NIB-1;
		lastIBlockSize = I - lastIBlockNum*IB;
		lastKBlockNum = NKB-1;
		lastKBlockSize = K - lastKBlockNum*KB;
		lastJBlockNum = NJB-1;
		lastJBlockSize = J - lastJBlockNum*JB;
		isABuilt = false;
		isBBuilt = false;
//		doCalibrate = false;
	}
	      
  private void printReduceInputKey (SpMMTypes.Key key) {
		System.out.println("##### Reduce input: key = (" + key.index1 + "," + 
			key.index2 + "," + key.index3 + "," + key.m + ")");
	}
	
	private void printReduceInputValue (SpMMTypes.Value value) {
		System.out.println("##### Reduce input: value = (" + value.index1 + "," +
			value.index2 + "," + value.v + ")");
	}
	
	private void printReduceOutput (IndexPair indexPair, IntWritable el) {
		System.out.println("##### Reduce output: (" + indexPair.index1 + "," + 
			indexPair.index2 + ") " + el.get());
	}

}
