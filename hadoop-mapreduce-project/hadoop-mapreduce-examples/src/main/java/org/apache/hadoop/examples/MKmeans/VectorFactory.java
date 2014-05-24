package org.apache.hadoop.examples.MKmeans;

import org.apache.hadoop.examples.MKmeans.MKMTypes.VectorType;

import com.sun.org.apache.xerces.internal.impl.xpath.regex.RegularExpression;

public class VectorFactory {
	public static Value getInstance(VectorType type){
		Value value = null;
		switch(type){
		case CENTROID:
		case REGULAR:
			value = new Value();
			break;
		case PARTIALCENTROID:
			value = new PartialCentroid();
			break;
		default:
			System.out.println("ERROR: undefined type");
			break;
		}
		return value;
	}

	public static Value getInstance(VectorType type, int dimension) {
		Value value = null;
			switch(type){
			case CENTROID:
			case REGULAR:
				value = new Value(dimension);
				break;
			case PARTIALCENTROID:
				value = new PartialCentroid(dimension);
				break;
			default:
				System.out.println("ERROR: undefined type");
				break;
			}
			return value;
	}
}
