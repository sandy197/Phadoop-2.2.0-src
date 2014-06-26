package org.apache.hadoop.ipc;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.io.Writable;

public class RAPLCalibration implements Writable {
	private Map<Long, RAPLExecTime> capToExecTimeMap;

	public Map<Long, RAPLExecTime> getCapToExecTimeMap() {
		return capToExecTimeMap;
	}

//	public void setCapToExecTimeMap(Map<Integer, RAPLExecTime> capToExecTimeMap) {
//		this.capToExecTimeMap = capToExecTimeMap;
//	}
	
	public RAPLCalibration(){
		this.capToExecTimeMap = new HashMap<Long, RAPLExecTime>();
	}
	
	public void add(RAPLCalibration calibration){
		if(calibration != null){
			Map <Long, RAPLExecTime> calibrationMap = calibration.getCapToExecTimeMap();
			for(Long i : calibrationMap.keySet()){
				this.addRAPLExecTime(i, calibrationMap.get(i));
			}
		}
	}
	
	public void addRAPLExecTime(Long powerCap, RAPLExecTime rExecTime){
		if(this.capToExecTimeMap.containsKey(powerCap)){
			this.capToExecTimeMap.get(powerCap).add(rExecTime);
		}
		else
			this.capToExecTimeMap.put(powerCap, rExecTime);
	}
	
	public void addRAPLExecTime(Long powerCap, long execTime){
		RAPLExecTime rExecTime = new RAPLExecTime(execTime, 1);
		addRAPLExecTime(powerCap, rExecTime);
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeInt(capToExecTimeMap.size());
		for(Long l : capToExecTimeMap.keySet()){
			out.writeLong(l);
			capToExecTimeMap.get(l).write(out);
		}
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		int size = in.readInt();
		for(int i = 0; i < size; i++){
			Long l = in.readLong();
			RAPLExecTime execTime = new RAPLExecTime(0L, 0);
			execTime.readFields(in);
			capToExecTimeMap.put(l, execTime);
		}
	}
	
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append("----XXX----").append("\n");
		
		for(Long l : this.capToExecTimeMap.keySet())
			sb.append(l).append(":")
				.append(this.capToExecTimeMap.get(l).toString())
					.append("\n");
		
		sb.append("----XXX----");
		return sb.toString();
	}
	
}
