package org.apache.hadoop.ipc;

public class RAPLExecTime {
	private long execTime;
	private int sampleCount;
	
	public long getExecTime() {
		return execTime;
	}

//	public void setExecTime(long execTime) {
//		this.execTime = execTime;
//	}

	public int getSampleCount() {
		return sampleCount;
	}

//	public void setSampleCount(int sampleCount) {
//		this.sampleCount = sampleCount;
//	}

	public RAPLExecTime(long execTime, int sampleCount){
		this.execTime = execTime;
		this.sampleCount = sampleCount;
	}
	
	public void add(RAPLExecTime rExecTime){
		this.execTime = ((this.execTime * this.sampleCount) + (rExecTime.getExecTime() * rExecTime.getSampleCount()))/(this.sampleCount + rExecTime.getSampleCount());
		this.sampleCount = this.sampleCount + rExecTime.getSampleCount();
	}
	
}
