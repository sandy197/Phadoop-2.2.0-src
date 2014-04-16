package org.apache.hadoop.examples.ParSpMM.SpMMMR;

import java.util.Set;

import org.apache.hadoop.examples.ParSpMM.SpMM.SpDCSC;
import org.apache.hadoop.ipc.GenericMatrix;

public class SpMMMatrix implements GenericMatrix<SpDCSC> {

	private SpDCSC spMat;
	private boolean isMatrixSet = false;
	
	public SpMMMatrix(SpDCSC matrix){
		spMat = matrix;
		isMatrixSet = true;
	}
	
	@Override
	public SpDCSC getMatrix() {
		if(isMatrixSet)
			return spMat;
		return null;
	}
	@Override
	public void setMatrix(SpDCSC matrix) {
		spMat = matrix;
		
	}
	@Override
	public boolean isMatrixSet() {
		return isMatrixSet;
	}
	@Override
	public void setMatrixSet(boolean isMatrixSet) {
		this.isMatrixSet = isMatrixSet;		
	}
	
	public float getMatrixDensity(){
		return (float)spMat.nz/(spMat.m * spMat.n);
	}
	
	public float getAvgNZperNZC(){
		return (float)spMat.nz/spMat.nzc;
	}
	
	public float getAvgNZperNZR(){
		return (float)spMat.nz/spMat.getnzrIndices().size();
	}
	
	public Set<Integer> getnzrIndices(){
		return spMat.getnzrIndices();
	}
	
	public Set<Integer> getnzcIndices(){
		return spMat.getnzcIndices();
	}
}
