package org.apache.hadoop.examples.Kmeans;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.WritableComparable;

public class KMTypes {
	
	public static enum VectorType{
		REGULAR(0), CENTROID(1);
		
		private int typeVal;
		private VectorType(int typeVal){
			this.typeVal = typeVal;
		}
		
		public int getTypeVal(){
			return this.typeVal;
		}

		public static VectorType getType(int readInt) {
			switch(readInt){
			case 0:
				return REGULAR;
			case 1:
				return CENTROID;
			default:
				return REGULAR;
			}
		}
	}
	
	public static class Value implements WritableComparable{
		
		private int dimension;
		private int[] coordinates;
		private int count;
		private int centroidIdx;
		
		public Value(){
			this.count = 0;
			centroidIdx = -1;
		}
		
		public Value(int dimension){
			this.dimension = dimension; 
			this.coordinates = new int[dimension];
			for(int i = 0; i < dimension; i++)
				this.coordinates[i] = 0;
			this.count = 0;
			//unassigned to any cluster
			centroidIdx = -1;
			
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
		public int getCount() {
			return count;
		}
		public void setCount(int count) {
			this.count = count;
		}
		@Override
		public void readFields(DataInput in) throws IOException {
			dimension = in.readInt();
			coordinates = new int[dimension];
			for(int i = 0; i < dimension; i++){
				coordinates[i] = in.readInt();
			}
			count = in.readInt();
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
			out.writeInt(count);
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

		public void addVector(int[] vector) {
			for(int i = 0; i < this.dimension; i++){
				this.coordinates[i] += vector[i];
			}
			this.count++;
		}
		
		public void addVector(Value vector){
			int[] coords = vector.getCoordinates();
			for(int i = 0; i < this.dimension; i++){
				this.coordinates[i] += coords[i];
			}
			this.count += vector.getCount();
		}
		
		public String toString(){
			StringBuilder sb = new StringBuilder();
			sb.append( "[" + this.getDimension() + "," + this.getCentroidIdx() + "," + this.getCount() + "(");
			for(int coord : this.getCoordinates()){
				sb.append(coord + ",");
			}
			sb.append(")] ");
			return sb.toString();
		}
		
	}
	
	
	public static class Key implements WritableComparable{

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
}
