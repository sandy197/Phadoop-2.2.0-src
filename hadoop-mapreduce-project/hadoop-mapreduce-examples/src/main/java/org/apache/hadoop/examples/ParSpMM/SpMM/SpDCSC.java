package org.apache.hadoop.examples.ParSpMM.SpMM;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import java.util.Comparator;

import org.apache.commons.collections.ResettableIterator;
import org.apache.hadoop.examples.ParSpMM.SpMMMR.SpMMTypes.Pair;
import org.apache.hadoop.examples.ParSpMM.SpMMMR.SpMMTypes.Value;


public class SpDCSC extends SpMat {
	
	public static boolean DEBUG = true;
	
	public List<Integer> cp;
	public List<Integer> jc;
	public List<Integer> ir;
	public List<Integer> numArray;
	
	public int nz;
	public int nzc;
	
	
//	public SpDCSC(){
//		super(1, 2);
//	}
	/**
	 * TODO:Add params based on the arguments used to create this object
	 */
	public SpDCSC(Iterable<Value> values, int m, int n){
		super(m,n);		
		
		//init
		cp = new ArrayList<Integer>();
		jc = new ArrayList<Integer>();
		ir = new ArrayList<Integer>();
		numArray = new ArrayList<Integer>();
		
		int nzcColCount = 0;
		int nzCount = 0;
		Iterator<Value> itr1 = values.iterator();
		Value val = (Value)itr1.next();
		int rowIdx = val.index1;
		int colIdx = val.index2;
		cp.add(nzCount++);
		jc.add(colIdx);
		nzcColCount++;
		numArray.add(val.v);
		ir.add(rowIdx);
		
		//fill arrays
		if(DEBUG) System.out.println("**Order for construction");
		while(itr1.hasNext()){
			Value value = (Value)itr1.next();
			if(DEBUG) System.out.println(value);
			rowIdx = value.index1;
			colIdx = value.index2;
			numArray.add(value.v);
			ir.add(rowIdx);
			
			if(colIdx != jc.get(nzcColCount-1)){
				cp.add(nzCount);
				jc.add(colIdx);
				nzcColCount++;
			}
			nzCount++;
			
		}
		cp.add(nzCount);
		this.nz = nzCount;
		this.nzc = nzcColCount;
	}
	
	public void fillColInds(int[] colNums, 
			int nind, List<Pair> colInds, int[] aux, int csize){
		boolean found;
		for(int j = 0; j < nind; j++){
			int pos = auxIndex(colNums[j], aux, csize);
			if(pos >= 0){
//				colInds.get(j).first = this.cp.get(pos);
//				colInds.get(j).second = this.cp.get(pos+1);
				colInds.add(new Pair(this.cp.get(pos), this.cp.get(pos+1)));
			}
			else{
//				colInds.get(j).first = 0;
//				colInds.get(j).second = 0;
				colInds.add(new Pair(0,0));
			}
		}
	}
	
	private int auxIndex(int i, int[] aux, int csize) {
		int base = SpUtils.floor((float)i/csize);
		int start = aux[base];
		int end = aux[base+1];
		//TODO:check again
		for(int j = start; j < end; j++){
			if(i == jc.get(j))
				return j;
		}
		return -1;
	}

	public void resize(int newNZC, int newNZ){
		
	}

	@Override
	public List<StackEntry> SpMatMultiply(SpMat B) {
		int mDim = this.m;
		int nDim = B.n;
		//TODO: check for zero A/B
		
		//TODO:comeback:stackEntry:Tuples_AnXBn
		List<StackEntry> multStack = new ArrayList<StackEntry>();
		int cnz = SpUtils.SpColByCol((SpDCSC)this, (SpDCSC)B, this.n, multStack);
		
		printList(multStack);
		//TODO : take context, block indices and sizes as input to the method and use it to writeback the result.
		return multStack;
	}

	
	private void printList(List<StackEntry> multStack) {
		for(StackEntry se : multStack){
			System.out.println("[" + se.key.first +"," + se.key.second +"]->" + se.value );
		}
		
	}

	public int[] constructAux(int ndim){
		float cf = (float)(ndim + 1) / this.nzc;
		int colChunks = SpUtils.ceil((float)(ndim + 1)/SpUtils.ceil(cf));
		
		int[] aux = new int[colChunks + 1];
		
		int chunksize = SpUtils.ceil(cf);
		int reg = 0;
		int curchunk = 0;
		aux[curchunk++] = 0;
		for(int i = 0; i < this.nzc; i++){
			if(this.jc.get(i) >= curchunk * chunksize){
				while(this.jc.get(i) >= curchunk * chunksize){
					aux[curchunk++] = reg;
				}
			}
			reg = i + 1;
		}
		while(curchunk <= colChunks){
			aux[curchunk++] = reg;
		}
		return aux;
	}
	
	public String toString(){
		StringBuilder sb = new StringBuilder("--------------XXXX----------------");
		sb.append("\n").append("cp: ");
		for(Integer cpe : cp){
			sb.append(cpe +",");
		}
		sb.append("\n").append("jc :");
		for(Integer jce : jc){
			sb.append(jce + ",");
		}
		sb.append("\n").append("ir :");
		for(int i = 0; i < ir.size(); i++){
			sb.append(ir.get(i)+",");
		}
		sb.append("\n").append("numx :");
		for(int i = 0; i < numArray.size(); i++){
			sb.append(numArray.get(i)+",");
		}
		sb.append("\n").append("--------------XXXX----------------");
		return sb.toString();
	}
	

	@Override
	public SpMat SpMatMerge(SpMat B) {
		return null;
	}

	public Set<Integer> getnzcIndices(){
		Set<Integer> nzcSet = new HashSet<Integer>();
		nzcSet.addAll(jc);
		return nzcSet;
	}
	
	public Set<Integer> getnzrIndices(){
		Set<Integer> nzrSet = new HashSet<Integer>();
		nzrSet.addAll(ir);
		return nzrSet;
	}
	
	
}

