package org.apache.hadoop.mapred;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.Writable;

public class RAPLRecord implements Writable {

	private int jobtoken;
	private long exectime;
	private String hostname;
	private short pkg;
	private long targetTime;

	public RAPLRecord(){
		// TODO Remove this later
		jobtoken = 197775;
		hostname = "localhost";
	}
	
	public RAPLRecord(long exectime){
		// TODO remove this
		this();
		this.exectime = exectime;
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

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeInt(jobtoken);
		out.writeLong(exectime);
		out.writeUTF(hostname);
		out.writeShort(pkg);
		out.writeLong(targetTime);
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		this.jobtoken = in.readInt();
		this.exectime = in.readLong();
		this.hostname = in.readUTF();
		this.pkg = in.readShort();
		this.targetTime = in.readLong();
	}
	
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append("***RAPL Record***"+"\n");
		sb.append("Hostname:"+this.hostname+"\n")
			.append("PkgIdx:"+this.pkg+"\n")
				.append("ExecTime:"+this.exectime+"\n")
					.append("targetTime:"+this.targetTime+"\n");
		sb.append("*****************");
		
		return sb.toString();
	}

}
