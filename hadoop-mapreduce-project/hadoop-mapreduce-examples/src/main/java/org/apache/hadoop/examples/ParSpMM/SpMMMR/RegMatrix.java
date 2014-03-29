package org.apache.hadoop.examples.ParSpMM.SpMMMR;

import org.apache.hadoop.ipc.GenericMatrix;

public class RegMatrix implements GenericMatrix<int[][]> {

	private int[][] matrix;
	private boolean isMatrixSet;
	
	public RegMatrix(int[][] matrix){
		this.matrix = matrix;
	}
	
	
	@Override
	public int[][] getMatrix() {
		if(isMatrixSet)
			return matrix;
		return null;
	}

	@Override
	public void setMatrix(int[][] matrix) {
		this.matrix = matrix;		
	}

	@Override
	public boolean isMatrixSet() {
		return isMatrixSet;
	}

	@Override
	public void setMatrixSet(boolean isMatrixSet) {
		this.isMatrixSet = isMatrixSet;
	}

}
