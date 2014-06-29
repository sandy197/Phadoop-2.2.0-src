package org.apache.hadoop.mapred;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.Writable;

public class RAPLRecord implements Writable {

	public static final String MAP_TASK_REUSE_JOBTOKEN = "MapReuse.jobToken";
	public static final String REDUCE_TASK_REUSE_JOBTOKEN = "ReduceReuse.jobToken";
	
	private int jobtoken;
	private long exectime;
	private String hostname;
	private short pkg;
	private long targetTime;
	private boolean doCalibration;
	private boolean doPowerMeasurement;
	private int iterationCount;
	private boolean isValid;

	
	public RAPLRecord(){
		// TODO Remove this later
		jobtoken = 197775;
		hostname = "localhost";
		doCalibration = false;
		doPowerMeasurement = false;
		isValid = true;
	}
	
	public RAPLRecord(long exectime){
		// TODO remove this
		this();
		this.exectime = exectime;
	}
	
	public boolean isValid() {
		return isValid;
	}

	public void setValid(boolean isValid) {
		this.isValid = isValid;
	}

	
	public long getTargetTime() {
		return targetTime;
	}

	public void setTargetTime(long targetTime) {
		this.targetTime = targetTime;
	}
	
	public int getJobtoken() {
		return jobtoken;
	}

	public void setJobtoken(int jobtoken) {
		this.jobtoken = jobtoken;
	}

	public long getExectime() {
		return exectime;
	}

	public void setExectime(long exectime) {
		this.exectime = exectime;
	}

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public short getPkg() {
		return pkg;
	}

	public void setPkg(short pkg) {
		this.pkg = pkg;
	}
	
	public int getInterationCount() {
		return iterationCount;
	}

	public void setInterationCount(int interationCount) {
		this.iterationCount = interationCount;
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeInt(jobtoken);
		out.writeLong(exectime);
		out.writeUTF(hostname);
		out.writeShort(pkg);
		out.writeLong(targetTime);
		out.writeBoolean(doCalibration);
		out.writeBoolean(doPowerMeasurement);
		out.writeInt(iterationCount);
		out.writeBoolean(isValid);
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		this.jobtoken = in.readInt();
		this.exectime = in.readLong();
		this.hostname = in.readUTF();
		this.pkg = in.readShort();
		this.targetTime = in.readLong();
		this.doCalibration = in.readBoolean();
		this.doPowerMeasurement = in.readBoolean();
		this.iterationCount = in.readInt();
		this.isValid = in.readBoolean();
	}
	
	public boolean isDoCalibration() {
		return doCalibration;
	}

	public void setDoCalibration(boolean doCalibration) {
		this.doCalibration = doCalibration;
	}

	public boolean isDoPowerMeasurement() {
		return doPowerMeasurement;
	}

	public void setDoPowerMeasurement(boolean doPowerMeasurement) {
		this.doPowerMeasurement = doPowerMeasurement;
	}

	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append("***RAPL Record***"+"\n");
		sb.append("Hostname:"+this.hostname+"\n")
			.append("PkgIdx:"+this.pkg+"\n")
				.append("ExecTime:"+this.exectime+"\n")
					.append("targetTime:"+this.targetTime+"\n")
						.append("iterationCount" + this.iterationCount+"\n")
							.append("isValid" + this.isValid);
		sb.append("*****************");
		
		return sb.toString();
	}

}
