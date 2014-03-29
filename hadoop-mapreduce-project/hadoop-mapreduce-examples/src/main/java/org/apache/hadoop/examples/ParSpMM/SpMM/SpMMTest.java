package org.apache.hadoop.examples.ParSpMM.SpMM;

import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.examples.ParSpMM.SpMMMR.SpMMTypes.Value;


public class SpMMTest {
	
	public static void main(String[] args){
		SpMat A, B;
		
		List<Value> matAVals = new ArrayList<Value>();
		// 3 x 3
		matAVals.add(new Value(0,0,1));
		matAVals.add(new Value(1,1,1));
		matAVals.add(new Value(2,2,1));
		
		A = new SpDCSC(matAVals, 3, 3);
		
		List<Value> matBVals = new ArrayList<Value>();
		// 3 x 3
		matBVals.add(new Value(0,0,1));
		matBVals.add(new Value(1,0,2));
		matBVals.add(new Value(2,0,3));
		matBVals.add(new Value(2,1,3));
		
		B = new SpDCSC(matBVals, 3, 3);
		System.out.println("Matrix A");
		System.out.println(A);
		
		System.out.println("Matrix B");
		System.out.println(B);
		
		A.SpMatMultiply(B);
	}

}
