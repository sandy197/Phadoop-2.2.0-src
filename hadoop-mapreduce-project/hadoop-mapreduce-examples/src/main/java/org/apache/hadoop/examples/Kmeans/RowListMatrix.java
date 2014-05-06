package org.apache.hadoop.examples.Kmeans;

import java.util.List;

import org.apache.hadoop.examples.Kmeans.KMTypes.Value;
import org.apache.hadoop.ipc.GenericMatrix;

public class RowListMatrix implements GenericMatrix<List<Value>> {
	
	private List<Value> vectors;
	private boolean isMatrixSet = false;

	public RowListMatrix(){
		
	}
	
	public RowListMatrix(List<Value> vectors){
		this.vectors = vectors;
		this.isMatrixSet = true;
	}
	
	@Override
	public List<Value> getMatrix() {
		return this.vectors;
	}

	@Override
	public void setMatrix(List<Value> vectors) {
		this.vectors = vectors;
		this.isMatrixSet = true;
	}

	@Override
	public boolean isMatrixSet() {
		return this.isMatrixSet;
	}

	@Override
	public void setMatrixSet(boolean isMatrixSet) {
		this.isMatrixSet = isMatrixSet;
	}

	@Override
	public float getMatrixDensity() {
		return -1;
	}

}
