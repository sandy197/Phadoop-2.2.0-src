package org.apache.hadoop.ipc;

import java.util.HashMap;
import java.util.Map;

public class RAPLIterCalibration {
	private Map<Integer, RAPLCalibration> itrToCalibMap;

	public Map<Integer, RAPLCalibration> getItrToCalibMap() {
		return itrToCalibMap;
	}

//	public void setCapToExecTimeMap(Map<Integer, RAPLExecTime> capToExecTimeMap) {
//		this.capToExecTimeMap = capToExecTimeMap;
//	}
	
	public RAPLIterCalibration(){
		this.itrToCalibMap = new HashMap<Integer, RAPLCalibration>();
	}
	
	public void addCalibration(int iter, RAPLCalibration calibration){
		if(this.itrToCalibMap.containsKey(iter)){
			this.itrToCalibMap.get(iter).add(calibration);
		}
		else {
			this.itrToCalibMap.put(iter, calibration);
		}
	}
	
	public void addCalibrationEntry(int iter, long powerCap, long execTime){
		RAPLCalibration calib = new RAPLCalibration();
		calib.addRAPLExecTime(powerCap, execTime);
		addCalibration(iter, calib);
	}
}
