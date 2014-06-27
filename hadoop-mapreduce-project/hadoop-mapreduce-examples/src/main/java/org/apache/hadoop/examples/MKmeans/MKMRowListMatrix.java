package org.apache.hadoop.examples.MKmeans;

import java.util.List;

import org.apache.hadoop.examples.MKmeans.Value;
import org.apache.hadoop.ipc.GenericMatrix;
import org.apache.hadoop.ipc.RAPLCalibration;

public class MKMRowListMatrix implements GenericMatrix<List<Value>> {
	
	private List<Value> vectors;
	private boolean isMatrixSet = false;
	private RAPLCalibration calibration = new RAPLCalibration();

	public MKMRowListMatrix(){
		
	}
	
	public MKMRowListMatrix(List<Value> vectors){
		this.vectors = vectors;
		this.isMatrixSet = true;
	}
	
	@Override
	public RAPLCalibration getCalibration() {
		return calibration;
	}

	public void setCalibration(RAPLCalibration calibration) {
		this.calibration = calibration;
	}
	
	@Override
	public void addCalibration(RAPLCalibration calibration){
		this.calibration.add(calibration);
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
