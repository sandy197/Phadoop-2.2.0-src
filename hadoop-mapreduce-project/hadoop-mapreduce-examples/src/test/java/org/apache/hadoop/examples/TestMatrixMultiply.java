/**	Test program for MatrixMultiply.
 */
package org.apache.hadoop.examples;

import java.io.*;
import java.util.*;

import org.apache.hadoop.conf.*;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.util.*;

public class TestMatrixMultiply {

	private static final String DATA_DIR_PATH = "/tmp/MatrixMultiply";
	private static final String INPUT_PATH_A = DATA_DIR_PATH + "/A";
	//private static final String INPUT_OATH_B = DATA_DIR_PATH + "/B";
	private static final String OUTPUT_DIR_PATH = DATA_DIR_PATH + "/out";
	private static final String TEMP_DIR_PATH = DATA_DIR_PATH;
	
	private static final int NUM_RANDOM_SPARSE_TESTS = 100;
	private static final int NUM_RANDOM_DENSE_TESTS = 100;
	private static final int NUM_RANDOM_BIG_TESTS = 100;
	
	private static Configuration conf = new Configuration();
	private static FileSystem fs;
	
	private static int[][] A;
	private static int[][] B;
	
	private static Random random = new Random();
	
	public static void writeMatrix (int[][] matrixA, int rowDimA, int colDimA, 
			int[][] matrixB,int rowDimB, int colDimB, String pathStr)
		throws IOException
	{
		Path path = new Path(pathStr);
		SequenceFile.Writer writer = SequenceFile.createWriter(fs, conf, path, 
			MatrixMultiply.IndexPair.class, IntWritable.class, 
			SequenceFile.CompressionType.NONE);
		MatrixMultiply.IndexPair indexPair = new MatrixMultiply.IndexPair();
		IntWritable el = new IntWritable();
		
		//A
		for (int i = 0; i < rowDimA; i++) {
			for (int j = 0; j < colDimA; j++) {
				int v = matrixA[i][j];
				if (v != 0) {
					indexPair.index1 = i;
					indexPair.index2 = j;
					indexPair.aFlag = 1;
					el.set(v);
					writer.append(indexPair, el);
				}
			}
		}
		//B
		for (int i = 0; i < rowDimB; i++) {
			for (int j = 0; j < colDimB; j++) {
				int v = matrixB[i][j];
				if (v != 0) {
					indexPair.index1 = i;
					indexPair.index2 = j;
					indexPair.aFlag = 0;
					el.set(v);
					writer.append(indexPair, el);
				}
			}
		}
		writer.close();
	}
	
	private static void fillMatrix (int[][] matrix, Path path)
		throws IOException
	{
		SequenceFile.Reader reader = new SequenceFile.Reader(fs, path, conf);
		MatrixMultiply.IndexPair indexPair = new MatrixMultiply.IndexPair();
		IntWritable el = new IntWritable();
		while (reader.next(indexPair, el)) {
			matrix[indexPair.index1][indexPair.index2] = el.get();
		}
		reader.close();
	}
	
	public static int[][] readMatrix (int rowDim, int colDim, String pathStr)
		throws IOException
	{
		Path path = new Path(pathStr);
		int[][] result = new int[rowDim][colDim];
		for (int i = 0; i < rowDim; i++)
			for (int j = 0; j < colDim; j++)
				result[i][j] = 0;
		if (fs.isFile(path)) {
			fillMatrix(result, path);
		} else {
			FileStatus[] fileStatusArray = fs.listStatus(path);
			for (FileStatus fileStatus : fileStatusArray) {
				fillMatrix(result, fileStatus.getPath());
			}
		}
		return result;
	}
	
	private static int[][] multiply (int[][] A, int[][] B, int I, int K, int J) {
		int[][] C = new int[I][J];
		for (int i = 0; i < I; i++) {
			for (int j = 0; j < J; j++) {
				int sum = 0;
				for (int k = 0; k < K; k++) {
					sum += A[i][k] * B[k][j];
				}
				C[i][j] = sum;
			}
		}
		return C;
	}
	
	public static void checkAnswer (int[][] A, int[][] B, int I, int K, int J)
		throws Exception
	{
		int[][] X = multiply(A, B, I, K, J);
		int[][] Y = readMatrix(I, J, OUTPUT_DIR_PATH);
		for (int i = 0; i < I; i++) {
			for (int j = 0; j < J; j++) {
				if (X[i][j] != Y[i][j]) {
					throw new Exception("Bad answer!");
				}
			}
		}
	}
	
	private static void zero (int[][] matrix, int rowDim, int colDim) {
		for (int i = 0; i < rowDim; i++)
			for (int j= 0; j < colDim; j++)
				matrix[i][j] = 0;
	}
	
	private static void fillRandom (int[][] matrix, int rowDim, int colDim, boolean sparse) {
		if (sparse) {
//			zero(matrix, rowDim, colDim);
//			for (int n = 0; n < random.nextInt(10); n++)
//				matrix[random.nextInt(rowDim)][random.nextInt(colDim)] = random.nextInt(1000);
			int[][] matrix1 = {
		/*	{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,0,35,0,37,38,0,40,0,0,43,0, 0,0,47,0,0,0,51,0,53,0,55,0,0,0,0},
{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,0,35,0,37,38,0,40,0,0,43,0, 0,0,47,0,0,0,51,0,53,0,55,0,0,0,0},
{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,0,35,0,37,38,0,40,0,0,43,0, 0,0,47,0,0,0,51,0,53,0,55,0,0,0,0},
{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,0,35,0,37,38,0,40,0,0,43,0, 0,0,47,0,0,0,51,0,53,0,55,0,0,0,0},
{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,0,35,0,37,38,0,40,0,0,43,0, 0,0,47,0,0,0,51,0,53,0,55,0,0,0,0},
{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,0,35,0,37,38,0,40,0,0,43,0, 0,0,47,0,0,0,51,0,53,0,55,0,0,0,0},
{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,0,35,0,37,38,0,40,0,0,43,0, 0,0,47,0,0,0,51,0,53,0,55,0,0,0,0},
{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,0,35,0,37,38,0,40,0,0,43,0, 0,0,47,0,0,0,51,0,53,0,55,0,0,0,0},
{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,0,35,0,37,38,0,40,0,0,43,0, 0,0,47,0,0,0,51,0,53,0,55,0,0,0,0},
{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,0,35,0,37,38,0,40,0,0,43,0, 0,0,47,0,0,0,51,0,53,0,55,0,0,0,0},
{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,0,35,0,37,38,0,40,0,0,43,0, 0,0,47,0,0,0,51,0,53,0,55,0,0,0,0},
{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,0,35,0,37,38,0,40,0,0,43,0, 0,0,47,0,0,0,51,0,53,0,55,0,0,0,0},
{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,0,35,0,37,38,0,40,0,0,43,0, 0,0,47,0,0,0,51,0,53,0,55,0,0,0,0},
{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,0,35,0,37,38,0,40,0,0,43,0, 0,0,47,0,0,0,51,0,53,0,55,0,0,0,0},
{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,0,35,0,37,38,0,40,0,0,43,0, 0,0,47,0,0,0,51,0,53,0,55,0,0,0,0},
{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,0,35,0,37,38,0,40,0,0,43,0, 0,0,47,0,0,0,51,0,53,0,55,0,0,0,0},
{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,0,35,0,37,38,0,40,0,0,43,0, 0,0,47,0,0,0,51,0,53,0,55,0,0,0,0},
{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,0,35,0,37,38,0,40,0,0,43,0, 0,0,47,0,0,0,51,0,53,0,55,0,0,0,0},
{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,0,35,0,37,38,0,40,0,0,43,0, 0,0,47,0,0,0,51,0,53,0,55,0,0,0,0},
{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,0,35,0,37,38,0,40,0,0,43,0, 0,0,47,0,0,0,51,0,53,0,55,0,0,0,0},
{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,0,35,0,37,38,0,40,0,0,43,0, 0,0,47,0,0,0,51,0,53,0,55,0,0,0,0},
{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,0,35,0,37,38,0,40,0,0,43,0, 0,0,47,0,0,0,51,0,53,0,55,0,0,0,0},
{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,0,35,0,37,38,0,40,0,0,43,0, 0,0,47,0,0,0,51,0,53,0,55,0,0,0,0},
{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,0,35,0,37,38,0,40,0,0,43,0, 0,0,47,0,0,0,51,0,53,0,55,0,0,0,0},
{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,0,35,0,37,38,0,40,0,0,43,0, 0,0,47,0,0,0,51,0,53,0,55,0,0,0,0},
{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,0,35,0,37,38,0,40,0,0,43,0, 0,0,47,0,0,0,51,0,53,0,55,0,0,0,0},
{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,0,35,0,37,38,0,40,0,0,43,0, 0,0,47,0,0,0,51,0,53,0,55,0,0,0,0},
{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,0,35,0,37,38,0,40,0,0,43,0, 0,0,47,0,0,0,51,0,53,0,55,0,0,0,0},
{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,0,35,0,37,38,0,40,0,0,43,0, 0,0,47,0,0,0,51,0,53,0,55,0,0,0,0},
{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,0,35,0,37,38,0,40,0,0,43,0, 0,0,47,0,0,0,51,0,53,0,55,0,0,0,0},

{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 30,0,0,0,0,0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,29,0,0,0,0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,0,28,0,0,0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,0,27,0,0,0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,0,26,0,0,0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,0,25,0,0,0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,0,0,24,0,0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,0,0,23,0,0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,0,0,22,0,0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,0,0,21,0,0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,0,0,0,20,0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,0,0,0,19,0,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,0,0,0,0,18,0,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,0,0,0,0,0,17,0,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,0,0,0,0,0,0,16,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,0,0,0,0,0,0,15,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,0,0,0,0,0,0,14,0,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,0,0,0,0,0,0,0,13,0,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,0,0,0,0,0,0,0,0,12,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,0,0,0,0,0,0,0,0,11,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,0,0,0,0,0,0,0,0,10,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,0,0,0,0,0,0,0,0,9,0,0,0,0,0, 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,0,0,0,0,0,0,0,0,0,8,0,0,0,0, 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,0,0,0,0,0,0,0,0,0,7,0,0,0,0, 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,0,0,0,0,0,0,0,0,0,0,6,0,0,0, 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,0,0,0,0,0,0,0,0,0,0,5,0,0,0, 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,0,0,0,0,0,0,0,0,0,0,0,4,0,0, 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,0,0,0,0,0,0,0,0,0,0,0,3,0,0, 1,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,0,0,0,0,0,0,0,0,0,0,0,0,2,0, 1,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,0,0,0,0,0,0,0,0,0,0,0,0,0,1, 0,0,0,0,0,0,0,0,0,0,0,0,0,0,0}*/
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
{0,1,2,0,4,5,6,7,8,9,10,11,12,13,14, 15,0,17,18,19,20,0,22,23,24,0,26,27,0,29, 0,31,32,0,34,35,0,37,38,0,40,41,0,43,0, 45,0,47,0,0,0,51,0,53,0,55,0,57,0,59},

//last two blocks(30X15) flipped
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
/*
//regular
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59},
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59},
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59},
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59},
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59},
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59},
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59},
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59},
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59},
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59},
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59},
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59},
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59},
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59},
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59},
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59},
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59},
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59},
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59},
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59},
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59},
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59},
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59},
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59},
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59},
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59},
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59},
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59},
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59},
{0,1,0,0,0,5,0,7,0,9,10,0,0,0,14, 15,0,0,18,0,0,0,22,0,0,0,26,0,0,29, 0,0,32,0,34,0,0,0,0,0,0,41,0,43,0, 45,0,0,0,0,0,0,0,0,0,0,0,57,0,59}
*/
};
			for(int i = 0; i < rowDim; i++){
				for(int j  =0; j < colDim; j++){
					matrix[i][j] = matrix1[i][j];
				}
			}
		} else {
			for (int i = 0; i < rowDim; i++) {
				for (int j = 0; j < colDim; j++) {
					matrix[i][j] = random.nextInt(1000);
				}
			}
		}
	}
	
	public static void runOneTest (int strategy, int R1, int R2, int I, int K, int J,
		int IB, int KB, int JB)
			throws Exception
	{
		MatrixMultiply.runJob(conf, INPUT_PATH_A, OUTPUT_DIR_PATH, TEMP_DIR_PATH,
			strategy, R1, R2, I, K, J, IB, KB, JB);
		//checkAnswer(A, B, I, K, J);
	}
	
	private static void testIdentity ()
		throws Exception
	{
		A = new int[][] { {1,0}, {0,1}};
		B = new int[][] { {1,0}, {0,1}};
//		writeMatrix(A, 2, 2, INPUT_PATH_A, true);
//		writeMatrix(B, 2, 2, INPUT_OATH_B, false);
		System.out.println();
		System.out.println("Identity test");
		runOneTest(1, 1, 1, 2, 2, 2, 2, 2, 2);
	}
	
	private static void testTwoByTwo ()
		throws Exception
	{
		A = new int[][] { {1,2}, {3,4}};
		B = new int[][] { {5,6}, {7,8}};
//		writeMatrix(A, 2, 2, INPUT_PATH_A);
//		writeMatrix(B, 2, 2, INPUT_OATH_B);
		int strategy = 2;
		int IB = 2;
		int KB = 1;
		int JB = 2;
//		for (int strategy = 1; strategy <= 4; strategy++) {
//			for (int IB = 1; IB <= 2; IB++) {
//				for (int KB = 1; KB <= 2; KB++) {
//					for (int JB = 1; JB <= 2; JB++) {
						System.out.println();
						System.out.println("Two by two test");
						System.out.println("   strategy = " + strategy);
						System.out.println("   IB = " + IB);
						System.out.println("   KB = " + KB);
						System.out.println("   JB = " + JB);
						runOneTest(strategy, 1, 2, 2, 2, 2, IB, KB, JB);
//					}
//				}
//			}
//		}
	}
	
	private static void testThreeByThree ()
		throws Exception
	{
		A = new int[][] { {1,2,3}, {4,5,6}, {7,8,9}};
		B = new int[][] { {10,11,12}, {13,14,15}, {16,17,18}};
//		writeMatrix(A, 3, 3, INPUT_PATH_A);
//		writeMatrix(B, 3, 3, INPUT_OATH_B);
		for (int strategy = 1; strategy <= 4; strategy++) {
			for (int IB = 1; IB <= 3; IB++) {
				for (int KB = 1; KB <= 3; KB++) {
					for (int JB = 1; JB <= 3; JB++) {
						System.out.println();
						System.out.println("Three by three test");
						System.out.println("   strategy = " + strategy);
						System.out.println("   IB = " + IB);
						System.out.println("   KB = " + KB);
						System.out.println("   JB = " + JB);
						runOneTest(strategy, 1, 1, 3, 3, 3, IB, KB, JB);
					}
				}
			}
		}
	}
	
	private static void testVerySparse ()
		throws Exception
	{
		A = new int[10][7];
		B = new int[7][12];
		zero(A, 10, 7);
		zero(B, 7, 12);
		A[5][6] = 1;
		B[6][7] = 1;
//		writeMatrix(A, 10, 7, INPUT_PATH_A);
//		writeMatrix(B, 7, 12, INPUT_OATH_B);
		System.out.println();
		System.out.println("Very sparse test");
		runOneTest(1, 1, 1, 10, 7, 12, 3, 3, 3);
	}
	
	private static void testRandom (boolean sparse, boolean big)
		throws Exception
	{
		/*int strategy = random.nextInt(4) + 1;
		int dimMin = big ? 100 : 10;
		int dimRandom = big ? 100 : 10;
		int I = random.nextInt(dimRandom) + dimMin;
		int K = random.nextInt(dimRandom) + dimMin;
		int J = random.nextInt(dimRandom) + dimMin;
		int IB = random.nextInt(I) + 1;
		int KB = random.nextInt(K) + 1;
		int JB = random.nextInt(J) + 1;*/

		int strategy = 2;
		int dimMin = big ? 100 : 10;
		int dimRandom = big ? 100 : 10;
		int I = 60;//random.nextInt(dimRandom) + dimMin;
		int K = 60;//random.nextInt(dimRandom) + dimMin;
		int J = 60;//random.nextInt(dimRandom) + dimMin;
		int IB = 30;//random.nextInt(I) + 1;
		int KB = 30;//random.nextInt(K) + 1;
		int JB = 30;//random.nextInt(J) + 1;
		A = new int[I][K];
		B = new int[K][J];
		fillRandom(A, I, K, sparse);
		fillRandom(B, K, J, !sparse);
		writeMatrix(A, I, K, B, K, J, INPUT_PATH_A);
		//writeMatrix(B, K, J, INPUT_OATH_B);
		System.out.println("   strategy = " + strategy);
		System.out.println("   I = " + I);
		System.out.println("   K = " + K);
		System.out.println("   J = " + J);
		System.out.println("   IB = " + IB);
		System.out.println("   KB = " + KB);
		System.out.println("   JB = " + JB);
		runOneTest(strategy, 4, 4, I, K, J, IB, KB, JB);
	}

	public static void main (String[] args)
		throws Exception
	{
		new GenericOptionsParser(conf, args);
		fs = FileSystem.get(conf);
		fs.mkdirs(new Path(DATA_DIR_PATH));
		try {
			long start = System.nanoTime();
			testRandom(true, false);
			long end = System.nanoTime();
			//testIdentity();
			//testTwoByTwo();
			/*testThreeByThree();
			testVerySparse();
			for (int i = 1; i <= NUM_RANDOM_SPARSE_TESTS; i++) {
				System.out.println();
				System.out.println("Random sparse test " + i + " of " + 
					NUM_RANDOM_SPARSE_TESTS);
				testRandom(true, false);
			}
			for (int i = 1; i <= NUM_RANDOM_DENSE_TESTS; i++) {
				System.out.println();
				System.out.println("Random dense test " + i + " of " + 
					NUM_RANDOM_DENSE_TESTS);
				testRandom(false, false);
			}
			for (int i = 1; i <= NUM_RANDOM_BIG_TESTS; i++) {
				System.out.println();
				System.out.println("Random big test " + i + " of " + 
					NUM_RANDOM_BIG_TESTS);
				testRandom(false, true);
			}*/
			System.out.println("Time taken for execution:"+ (end - start));
			System.out.println("================");
			System.out.println("All tests passed");
			System.out.println("================");
			System.out.println();
		} finally {
			fs.delete(new Path(DATA_DIR_PATH), true);
		}
	}

}
