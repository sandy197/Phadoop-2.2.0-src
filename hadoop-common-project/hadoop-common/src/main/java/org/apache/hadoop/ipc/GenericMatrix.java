package org.apache.hadoop.ipc;

/**
 * The matrix implementation. 
 * 
 * TODO : change this to 
 * something similar to a Reducer/Mapper using generics
 * 
 * @author sandeep
 *
 */
public class GenericMatrix {
	
	private int[][] matrix;
	private boolean isMatrixSet;
	public int[][] getMatrix() {
		return matrix;
	}
	public void setMatrix(int[][] matrix) {
		if(matrix != null){
			this.matrix = matrix;
			this.isMatrixSet = true;
		}
	}
	public boolean isMatrixSet() {
		return isMatrixSet;
	}
	public void setMatrixSet(boolean isMatrixSet) {
		this.isMatrixSet = isMatrixSet;
	}
	
	//can add more fields if needed

}

