package org.apache.hadoop.examples.MKmeans;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class PartialCentroid extends Value {

	private int count;
	
	public PartialCentroid(){
		super();
		this.count = 0;
	}
	
	public PartialCentroid(int dimension) {
		super(dimension);
		this.count = 0;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}
	
	@Override
	public void readFields(DataInput in) throws IOException {
		super.readFields(in);
		this.count = in.readInt();
	}

	@Override
	public void write(DataOutput out) throws IOException {
		super.write(out);
		out.writeInt(this.count);
	}

	@Override
	public void copy(Value val) {
		super.copy(val);
		if(val instanceof PartialCentroid){
			this.count = ((PartialCentroid) val).getCount();
		}
		else{
			this.count = 1;
		}
	}

	public void addVector(Value point) {
		int[] coords = point.getCoordinates();
		int[] thisCoords = this.getCoordinates();
		for(int i = 0; i < this.getDimension(); i++){
			thisCoords[i] += coords[i];
		}
		if(point instanceof PartialCentroid){
			this.count += ((PartialCentroid) point).getCount();
		}
		else
			this.count++;
	}
	
}
