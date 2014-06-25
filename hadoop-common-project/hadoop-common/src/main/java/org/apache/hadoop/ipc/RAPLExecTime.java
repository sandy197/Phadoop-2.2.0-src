package org.apache.hadoop.ipc;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.Writable;

public class RAPLExecTime implements Writable {
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

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeLong(execTime);
		out.writeInt(sampleCount);
		
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		this.execTime = in.readLong();
		this.sampleCount = in.readInt();
	}
	
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append(execTime).append(",").append(sampleCount);
		return sb.toString();
	}
	
}
