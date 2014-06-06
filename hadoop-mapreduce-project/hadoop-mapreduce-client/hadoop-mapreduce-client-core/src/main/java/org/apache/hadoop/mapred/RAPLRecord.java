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
	
	public RAPLRecord(){
		// TODO Remove this later
		jobtoken = 197775;
		hostname = "localhost";
	}
	
	public RAPLRecord(long exectime){
		// TODO remove this
		this.exectime = exectime;
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

	public int getPkg() {
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
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		this.jobtoken = in.readInt();
		this.exectime = in.readLong();
		this.hostname = in.readUTF();
		this.pkg = in.readShort();
	}

}
