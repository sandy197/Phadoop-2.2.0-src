package org.apache.hadoop.examples.ParSpMM.SpMMMR;

import org.apache.hadoop.ipc.GenericMatrix;

public class RegMatrix implements GenericMatrix<int[][]> {

	private int[][] matrix;
	private boolean isMatrixSet;
	
	public RegMatrix(int[][] matrix){
		this.matrix = matrix;
		this.isMatrixSet = true;
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
		this.isMatrixSet = true;
	}

	@Override
	public boolean isMatrixSet() {
		return isMatrixSet;
	}

	@Override
	public void setMatrixSet(boolean isMatrixSet) {
		this.isMatrixSet = isMatrixSet;
	}


	@Override
	public float getMatrixDensity() {
		int[][] matrix = this.getMatrix();
		int count = 0;
		for(int i = 0; i < matrix.length; i++)
			for(int j = 0; j < matrix[0].length; j++){
				if(matrix[i][j] != 0){
					count++;
				}
			}
		return (float)count/(matrix.length * matrix[0].length);
	}
}
