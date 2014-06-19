package org.apache.hadoop.examples.Kmeans;

import java.util.List;

import org.apache.hadoop.examples.Kmeans.KMTypes.Value;
import org.apache.hadoop.ipc.GenericMatrix;
import org.apache.hadoop.ipc.RAPLCalibration;

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

	@Override
	public RAPLCalibration getCalibration() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addCalibration(RAPLCalibration calibration) {
		// TODO Auto-generated method stub
		
	}

}
