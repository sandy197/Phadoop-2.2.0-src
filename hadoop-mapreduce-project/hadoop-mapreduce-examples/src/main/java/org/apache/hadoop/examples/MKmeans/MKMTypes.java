package org.apache.hadoop.examples.MKmeans;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.io.Writable;


public class MKMTypes {
	
	public static boolean DEBUG = true;
	public static int UNDEF_VAL = -1;
	
	public static enum VectorType{
		REGULAR(0), CENTROID(1), PARTIALCENTROID(2);
		
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
			case 2:
				return PARTIALCENTROID;
			default:
				return REGULAR;
			}
		}
	}
	
	public static class Values implements Writable{
		private int valCount;
		private List<Value> values;
		
		public Values(){
			valCount = 0;
			values = new ArrayList<Value>();
		}
		
		public Values(int count){
			this.valCount = count;
			values = new ArrayList<Value>();
		}
		
		public int getValCount() {
			return valCount;
		}
		
		public void setValCount(int valCount) {
			this.valCount = valCount;
		}
		
		public List<Value> getValues() {
			return values;
		}
		
		public void setValues(List<Value> values) {
			this.values = values;
		}
		
		@Override
		public void readFields(DataInput in) throws IOException {
			valCount = in.readInt();
			values = new ArrayList<Value>();
			for(int i = 0; i < valCount; i++){
				Value val = new Value();
				val.readFields(in);
				values.add(val);
			}
		}
		
		@Override
		public void write(DataOutput out) throws IOException {
			out.writeInt(valCount);
			if(values != null && !values.isEmpty()){
				for(Value value : values){
					value.write(out);
				}
			}
			if(DEBUG) System.out.println("ValCount : " + valCount + ", #values "+ values.size());
		}
	}
}
