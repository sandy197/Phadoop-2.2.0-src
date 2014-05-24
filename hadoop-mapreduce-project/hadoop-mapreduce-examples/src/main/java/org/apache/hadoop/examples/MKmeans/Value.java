package org.apache.hadoop.examples.MKmeans;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.WritableComparable;

public class Value implements WritableComparable {
//	private VectorType type; 
//	public VectorType getType() {
//		return type;
//	}
//
//	public void setType(VectorType type) {
//		this.type = type;
//	}

	private int dimension;
	private int[] coordinates;
	private int centroidIdx;
	
	public Value(){
		
	}
	
	public Value(int dimension){
		this.dimension = dimension; 
		this.coordinates = new int[dimension];
		for(int i = 0; i < dimension; i++)
			this.coordinates[i] = 0;
		this.centroidIdx = MKMTypes.UNDEF_VAL;
	}
	
	public Value(int dimension, int centroidIdx){
		this(dimension);
		this.centroidIdx = centroidIdx;
	}
	
	public int getCentroidIdx() {
		return centroidIdx;
	}

	public void setCentroidIdx(int centroidIdx) {
		this.centroidIdx = centroidIdx;
	}
	
	public int getDimension() {
		return dimension;
	}

	public void setDimension(int dimension) {
		this.dimension = dimension;
	}

	public int[] getCoordinates() {
		return coordinates;
	}

	public void setCoordinates(int[] coordinates) {
		this.coordinates = coordinates;
	}
	
	public void copy(Value val){
		this.dimension = val.dimension;
		this.coordinates = new int[dimension];
		int[] valCoords = val.getCoordinates();
		for(int i = 0; i < dimension; i++)
			this.coordinates[i] = valCoords[i];
		this.centroidIdx = val.getCentroidIdx();
	}

	@Override
	public void readFields(DataInput in) throws IOException {
		dimension = in.readInt();
		if(coordinates == null)
			coordinates = new int[dimension];
		for(int i = 0; i < dimension; i++){
			coordinates[i] = in.readInt();
		}
		centroidIdx = in.readInt();
	}

	@Override
	public void write(DataOutput out) throws IOException {
		out.writeInt(dimension);
		if(coordinates != null){
			for(int i = 0; i < dimension; i++){
				out.writeInt(coordinates[i]);
			}
		}
		out.writeInt(centroidIdx);
	}

	@Override
	public int compareTo(Object o) {
		Value val = (Value)o;
		int[] oCoords = val.getCoordinates();
		for(int i =0; i < dimension; i++){
			if(this.coordinates[i] < oCoords[i]){
				return -1;
			}
			else if(this.coordinates[i] > oCoords[i]){
				return 1;
			}
		}
		return 0;
	}
	
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append( "[" + this.getDimension() + "," + this.getCentroidIdx() + "(");
		for(int coord : this.getCoordinates()){
			sb.append(coord + ",");
		}
		sb.append(")] ");
		return sb.toString();
	}
}
