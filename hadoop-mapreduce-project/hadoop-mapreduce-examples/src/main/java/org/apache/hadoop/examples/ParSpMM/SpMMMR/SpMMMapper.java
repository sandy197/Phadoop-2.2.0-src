package org.apache.hadoop.examples.ParSpMM.SpMMMR;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.examples.ParSpMM.SpMMMR.SpMMTypes.IndexPair;
import org.apache.hadoop.examples.ParSpMM.SpMMMR.SpMMTypes.Key;
import org.apache.hadoop.examples.ParSpMM.SpMMMR.SpMMTypes.Value;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;

public class SpMMMapper extends Mapper<SpMMTypes.IndexPair, IntWritable, SpMMTypes.Key, SpMMTypes.Value> {

	private static final boolean DEBUG = true;
	
	private boolean useTaskPool;
	private Path path;
	private boolean matrixA;
	private Key key = new Key();
	private Value value = new Value();
	
	// the value of k in mapper algo(docs)
	private int iteration;
	
	private String inputPathA;
	private String inputPathB;
	private String outputDirPath;
	private String tempDirPath;
	private int strategy;
	private int R1;
	

	private int R2;
	private int I;
	private int K;
	private int J;
	private int IB;
	private int KB;
	private int JB;
	
	private int NIB;
	private int NKB;
	private int NJB;
	
	
	private int lastIBlockNum;
	private int lastIBlockSize;
	private int lastKBlockNum;
	private int lastKBlockSize;
	private int lastJBlockNum;
	private int lastJBlockSize;
	
	public void setup (Context context) {
		init(context);
		FileSplit split = (FileSplit)context.getInputSplit();
		path = split.getPath();
		matrixA = path.toString().startsWith(inputPathA);
		
		if (DEBUG) {
			System.out.println("##### Map setup: matrixA = " + matrixA + " for " + path);
			System.out.println("   R1 = " + R1);
			System.out.println("   I = " + I);
			System.out.println("   K = " + K);
			System.out.println("   J = " + J);
			System.out.println("   IB = " + IB);
			System.out.println("   KB = " + KB);
			System.out.println("   JB = " + JB);
		}
	}
	
	@Override
	public void map(IndexPair indexPair, IntWritable el, Context context)
			throws IOException, InterruptedException {
		if (DEBUG) printMapInput(indexPair, el);
		int i = 0;
		int k = 0;
		int j = 0;
		if (matrixA) {
			i = indexPair.index1;
			if (i < 0 || i >= I) badIndex(i, I, "A row index");
			k = indexPair.index2;
			if (k < 0 || k >= K) badIndex(k, K, "A column index");
		} else {
			k = indexPair.index1;
			if (k < 0 || k >= K) badIndex(k, K, "B row index");
			j = indexPair.index2;
			if (j < 0 || j >= J) badIndex(j, J, "B column index");
		}
		value.v = el.get();
		switch (strategy) {
		case 1:
			if(matrixA){
				// only allow A[;iteration] blocks
				int colRangeStart = iteration * KB;
				int colRangeEnd = colRangeStart + (KB -1);
				if(k >= colRangeStart && k <= colRangeEnd ){
					key.index1 = i/IB;
					key.index2 = k/KB;
					key.m = 0; //for A
					value.index1 = i % IB;
					value.index2 = k % KB;
					for (int jb = 0; jb < NJB; jb++) {
						key.index3 = jb;
						context.write(key, value);
						if (DEBUG) printMapOutput(key, value);
					}
					if (DEBUG) printMapOutput(key, value);
				}
			}
			//matrix B
			else{
				// only allow B[iteration;] blocks
				int rowRangeStart = iteration * KB;
				int rowRangeEnd = rowRangeStart + (KB -1);
				if(k >= rowRangeStart && k <= rowRangeEnd ){
					key.index2 = k/KB;
					key.index3 = j/JB;
					key.m = 1; // for B
					value.index1 = k % KB;
					value.index2 = j % JB;
					for (int ib = 0; ib < NIB; ib++) {
						key.index1 = ib;
						context.write(key, value);
						if (DEBUG) printMapOutput(key, value);
					}
				}
			}
			break;
		case 2:
			if(matrixA){
				// only allow A[iteration;] blocks
				int rowRangeStart = iteration * IB;
				int rowRangeEnd = rowRangeStart + (IB -1);
				if(i >= rowRangeStart && i <= rowRangeEnd ){
					key.index1 = i/IB;
					key.index2 = k/KB;
					key.m = 0; //for A
					value.index1 = i % IB;
					value.index2 = k % KB;
					for (int jb = 0; jb < NJB; jb++) {
						key.index3 = jb;
						context.write(key, value);
						if (DEBUG) printMapOutput(key, value);
					}
					//if (DEBUG) printMapOutput(key, value);
				}
			}
			//matrix B
			else{
				// send each block to a particular process
//				int rowRangeStart = iteration * KB;
//				int rowRangeEnd = rowRangeStart + (KB -1);
//				if(k >= rowRangeStart && k <= rowRangeEnd ){
				key.index2 = k/KB;
				key.index3 = j/JB;
				key.m = 1; // for B
				value.index1 = k % KB;
				value.index2 = j % JB;
				//for (int ib = 0; ib < NIB; ib++) {
				key.index1 = key.index2;
				context.write(key, value);
				if (DEBUG) printMapOutput(key, value);
					
//				}
			}
			break;
		default:
			break;
		}
			//matrix A
			
	}
	
	private void printMapInput (SpMMTypes.IndexPair indexPair, IntWritable el) {
		System.out.println("##### Map input: (" + indexPair.index1 + "," + 
			indexPair.index2 + ") " + el.get());
	}
	
	private void printMapOutput (SpMMTypes.Key key, SpMMTypes.Value value) {
		System.out.println("##### Map output: (" + key.index1 + "," + 
			key.index2 + "," + key.index3 + "," + key.m + ") (" + 
			value.index1 + "," + value.index2 + "," + value.v + ") ");
	}
	
	private void badIndex (int index, int dim, String msg) {
		System.err.println("Invalid " + msg + " in " + path + ": " + index + " " + dim);
		System.exit(1);
	}
	
	private void init (JobContext context) {
		Configuration conf = context.getConfiguration();
		useTaskPool = conf.getBoolean("SpMM.useTaskPool", false);
		inputPathA = conf.get("SpMM.inputPathA");
		inputPathB = conf.get("SpMM.inputPathB");
		//outputDirPath = conf.get("SpMM.outputDirPath");
		tempDirPath = conf.get("SpMM.tempDirPath");
		strategy = conf.getInt("SpMM.strategy", 1);
		
		iteration = conf.getInt("SpMM.iteration", 0);
		R1 = conf.getInt("SpMM.R1", 0);
		R2 = conf.getInt("SpMM.R2", 0);
		I = conf.getInt("SpMM.I", 0);
		K = conf.getInt("SpMM.K", 0);
		J = conf.getInt("SpMM.J", 0);
		IB = conf.getInt("SpMM.IB", 0);
		KB = conf.getInt("SpMM.KB", 0);
		JB = conf.getInt("SpMM.JB", 0);
		NIB = (I-1)/IB + 1;
		NKB = (K-1)/KB + 1;
		NJB = (J-1)/JB + 1;
		lastIBlockNum = NIB-1;
		lastIBlockSize = I - lastIBlockNum*IB;
		lastKBlockNum = NKB-1;
		lastKBlockSize = K - lastKBlockNum*KB;
		lastJBlockNum = NJB-1;
		lastJBlockSize = J - lastJBlockNum*JB;
	}
}
