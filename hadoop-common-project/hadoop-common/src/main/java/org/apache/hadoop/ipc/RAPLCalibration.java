package org.apache.hadoop.ipc;

import java.util.HashMap;
import java.util.Map;

public class RAPLCalibration {
	private Map<Integer, RAPLExecTime> capToExecTimeMap;

	public Map<Integer, RAPLExecTime> getCapToExecTimeMap() {
		return capToExecTimeMap;
	}

//	public void setCapToExecTimeMap(Map<Integer, RAPLExecTime> capToExecTimeMap) {
//		this.capToExecTimeMap = capToExecTimeMap;
//	}
	
	public RAPLCalibration(){
		this.capToExecTimeMap = new HashMap<Integer, RAPLExecTime>();
	}
	
	public void add(RAPLCalibration calibration){
		if(calibration != null){
			Map <Integer, RAPLExecTime> calibrationMap = calibration.getCapToExecTimeMap();
			for(Integer i : calibrationMap.keySet()){
				this.addRAPLExecTime(i, calibrationMap.get(i));
			}
		}
	}
	
	public void addRAPLExecTime(int powerCap, RAPLExecTime rExecTime){
		if(this.capToExecTimeMap.containsKey(powerCap)){
			this.capToExecTimeMap.get(powerCap).add(rExecTime);
		}
		else
			this.capToExecTimeMap.put(powerCap, rExecTime);
	}
	
	public void addRAPLExecTime(int powerCap, long execTime){
		RAPLExecTime rExecTime = new RAPLExecTime(execTime, 1);
		addRAPLExecTime(powerCap, rExecTime);
	}
	
}
