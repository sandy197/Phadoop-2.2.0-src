package org.apache.hadoop.ipc;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.hadoop.io.Writable;

public class RAPLExecTime implements Writable {
	private List<Long> execTime;
	private long maxTime;
	private long minTime;
//	private int sampleCount;
	
	public long getExecTime() {
		long avgPower = 0L;
		if(execTime != null && execTime.size() != 0){
			if(execTime.size() == 1)
				avgPower = execTime.get(0);
			else{
				for(long l : execTime){
					avgPower += l;
				}
				avgPower /= execTime.size();
			}
		}
		return avgPower;
	}
	
	public List<Long> getExecTimeList(){
		return execTime;
	}
	
	public void eliminateOutliers(){
		long removedElement;
		//Change it to a better algo.
		Collections.sort(execTime);
		//naive implementation : Remove the highest values depending on the size
		int arrayLength = execTime.size();
		if(arrayLength > 1){
			if(arrayLength < 6){
				//remove 1 highest value
				removedElement = execTime.remove(arrayLength - 1);
				System.out.println("Remove outlier:"+removedElement);
			}
			else{
				//remove 2 highest values
				removedElement = execTime.remove(arrayLength - 1);
				System.out.println("Remove outlier:"+removedElement);
				arrayLength = execTime.size();
				removedElement = execTime.remove(arrayLength - 1);
				System.out.println("Remove outlier:"+removedElement);
			}
		}
		//update min and max.
		updateMinMax();
	}
	
	private void updateMinMax(){
		this.maxTime = Long.MIN_VALUE;
		this.minTime = Long.MAX_VALUE;
		if(this.execTime != null && this.execTime.size() != 0){
			for(long l : this.execTime){
				if(l < this.minTime){
					this.minTime = l;
				}
				
				if(l > this.maxTime){
					this.maxTime = l;
				}
			}
		}
	}

//	public void setExecTime(long execTime) {
//		this.execTime = execTime;
//	}

	public int getSampleCount() {
		return execTime.size();
	}

//	public void setSampleCount(int sampleCount) {
//		this.sampleCount = sampleCount;
//	}

	public RAPLExecTime(long execTime, int sampleCount){
		this.execTime = new ArrayList<Long>();
		this.execTime.add(execTime);
		this.maxTime = Long.MIN_VALUE;
		this.minTime = Long.MAX_VALUE;
//		this.sampleCount = sampleCount;
	}
	
	public void add(RAPLExecTime rExecTime){
//		this.execTime = ((this.execTime * this.sampleCount) 
//				+ (rExecTime.getExecTime() * rExecTime.getSampleCount()))/(this.sampleCount + rExecTime.getSampleCount());
//		this.sampleCount = this.sampleCount + rExecTime.getSampleCount();
		if(rExecTime != null && rExecTime.getExecTimeList() != null){
			for(Long l : rExecTime.getExecTimeList()){
				this.execTime.add(l);
				//update max and min values
				if(l < this.minTime){
					this.minTime = l;
				}
				if(l > this.maxTime){
					this.maxTime = l;
				}
			}
		}
	}
	
	public boolean isOutlier(long time){
		//TODO : logic to determine if a datapoint is an outlier wrt the execTime list
		// naive algo for now
		long maxDiff = this.maxTime - this.getExecTime();
		boolean isOutlier = time > this.maxTime + maxDiff;
		if(isOutlier) System.out.println("Outlier detected, record is being marked invalid:"+time);
		return isOutlier;
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeInt(execTime.size());
		for(Long l : this.execTime)
			out.writeLong(l);
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		int size = in.readInt();
		for(int i =0; i< size; i++){
			Long l = in.readLong();
			this.execTime.add(l);
		}
	}
	
	public String toString(){
		StringBuilder sb = new StringBuilder();
		for(long l : execTime)
			sb.append(l).append(",");
		sb.append("/t").append("length:").append(execTime.size());
		return sb.toString();
	}
	
}
