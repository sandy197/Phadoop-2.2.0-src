package org.apache.hadoop.examples.ParSpMM.SpMMMR;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;

public class SpMMTypes {
	public static class Pair{
		public int first;
		public int second;
		
		public Pair(int first, int second){
			this.first = first;
			this.second = second;
		}
	}
	public static class IndexPair implements WritableComparable {
		public int index1;
		public int index2;
		
		public IndexPair(){
			
		}
		
		public IndexPair(int index1, int index2){
			this.index1 = index1;
			this.index2 = index2;
		}
		
		public void write (DataOutput out)
			throws IOException
		{
			out.writeInt(index1);
			out.writeInt(index2);
		}
		public void readFields (DataInput in)
			throws IOException
		{
			index1 = in.readInt();
			index2 = in.readInt();
		}
		public int compareTo (Object other) {
			IndexPair o = (IndexPair)other;
			if (this.index1 < o.index1) {
				return -1;
			} else if (this.index1 > o.index1) {
				return +1;
			}
			if (this.index2 < o.index2) {
				return -1;
			} else if (this.index2 > o.index2) {
				return +1;
			}
			return 0;
		}
		
		public int hashCode () {
			return index1 << 16 + index2;
		}
	}
	
	public static class Key implements WritableComparable {
		public int index1;
		public int index2;
		public int index3;
		public byte m;
		public void write (DataOutput out)
			throws IOException
		{
			out.writeInt(index1);
			out.writeInt(index2);
			out.writeInt(index3);
			out.writeByte(m);
		}
		public void readFields (DataInput in)
			throws IOException
		{
			index1 = in.readInt();
			index2 = in.readInt();
			index3 = in.readInt();
			m = in.readByte();
		}
		public int compareTo (Object other) {
			Key o = (Key)other;
			if (this.index1 < o.index1) {
				return -1;
			} else if (this.index1 > o.index1) {
				return +1;
			}
			if (this.index2 < o.index2) {
				return -1;
			} else if (this.index2 > o.index2) {
				return +1;
			}
			if (this.index3 < o.index3) {
				return -1;
			} else if (this.index3 > o.index3) {
				return +1;
			}
			if (this.m < o.m) {
				return -1;
			} else if (this.m > o.m) {
				return +1;
			}
			return 0;
		}
	}
	
	public static class Value implements WritableComparable {
		public int index1;
		public int index2;
		public int v;
		
		public Value(){
			
		}
		
		public Value(int index1, int index2, int v){
			this.index1 = index1;
			this.index2 = index2;
			this.v = v;
		}
		
		public void write (DataOutput out)
			throws IOException
		{
			out.writeInt(index1);
			out.writeInt(index2);
			out.writeInt(v);
		}
		public void readFields (DataInput in)
			throws IOException
		{
			index1 = in.readInt();
			index2 = in.readInt();
			v = in.readInt();
		}
		@Override
		public int compareTo(Object o) {
			Value other = (Value)o;
			if(this.index1 < other.index1){
				return -1;
			}
			else if(this.index1 > other.index1){
				return 1;
			}
			if(this.index2 < other.index2){
				return -1;
			}
			else if(this.index2 > other.index2){
				return 1;
			}
			return 0;
		}

		public void set(int value) {
			index1 = -1;
			index2 = -1;
			v = value;
			
		}
	}
}
