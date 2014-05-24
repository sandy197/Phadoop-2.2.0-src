package org.apache.hadoop.examples.MKmeans;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.examples.MKmeans.MKMTypes.VectorType;

public class Key implements WritableComparable {

	private int TaskIndex;
	private VectorType type;
	
	public Key(){
		
	}

	public Key(int TaskIndex, VectorType type) {
		this.TaskIndex = TaskIndex;
		this.type = type;
	}

	public VectorType getType() {
		return type;
	}

	public void setType(VectorType type) {
		this.type = type;
	}

	public int getTaskIndex() {
		return TaskIndex;
	}

	public void setTaskIndex(int taskIndex) {
		TaskIndex = taskIndex;
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		TaskIndex = in.readInt();
		type = VectorType.getType(in.readInt());
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeInt(this.TaskIndex);
		out.writeInt(type.getTypeVal());
	}

	@Override
	public int compareTo(Object o) {
		Key key = (Key)o;
		if(this.TaskIndex < key.getTaskIndex())
			return -1;
		else if(this.TaskIndex > key.getTaskIndex())
			return 1;
		//CENTROID types are greater than REGULAR types
		else if(this.getType().getTypeVal() < key.getType().getTypeVal())
			return -1;
		else if(this.getType().getTypeVal() < key.getType().getTypeVal())
			return 1;
		else
			return 0;
	}

}
