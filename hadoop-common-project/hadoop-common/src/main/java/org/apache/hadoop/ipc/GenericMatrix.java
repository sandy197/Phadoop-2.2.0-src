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
public interface GenericMatrix<MATRIX_TYPE> {
	
//	private int[][] matrix;
//	private boolean isMatrixSet;
	
	public MATRIX_TYPE getMatrix();
	public void setMatrix(MATRIX_TYPE matrix);
	public boolean isMatrixSet();
	public void setMatrixSet(boolean isMatrixSet);
	
	//can add more fields if needed

}

