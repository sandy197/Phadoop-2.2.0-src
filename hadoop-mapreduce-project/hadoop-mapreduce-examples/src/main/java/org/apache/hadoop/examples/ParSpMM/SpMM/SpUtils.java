package org.apache.hadoop.examples.ParSpMM.SpMM;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import org.ncsu.sys.SpMM.HeapEntry;
import org.ncsu.sys.SpMM.HeapEntryComp;
import org.ncsu.sys.SpMM.StackEntry;
import org.ncsu.sys.SpMMMR.SpMMTypes.Pair;

public class SpUtils {
	
	public static int SpColByCol(SpDCSC A, SpDCSC B, int nA, List<StackEntry> multStack) {
		int cnz = 0;
		int cnzmax = A.nz + B.nz;
		
		float cf = (float)(nA + 1)/A.nzc;
		int csize = ceil(cf);
		
		//TODO:comeback:replace nA by A.n
		int[] aux = A.constructAux(nA);
		
		for(int i = 0; i < B.nzc; i++){
			int prevcnz = cnz;
			int nnzcol = B.cp.get(i+1) - B.cp.get(i);
			PriorityQueue<HeapEntry> wset = new PriorityQueue<HeapEntry>(nnzcol, new HeapEntryComp());
			
			int[] colnums = new int[nnzcol];
			
			List<Pair> colinds = new ArrayList<Pair>(nnzcol);
			copy(B.ir, B.cp.get(i), B.cp.get(i+1), colnums);
			
			A.fillColInds(colnums, colnums.length, colinds, aux, csize);
			int maxnnz = 0;
			
			for(int j = 0; j < colnums.length; j++){
				if(colinds.size() >= j && colinds.get(j).first != colinds.get(j).second){
					wset.add(new HeapEntry(A.ir.get(colinds.get(j).first), j, A.numArray.get(colinds.get(j).first)));
					maxnnz += colinds.get(j).second - colinds.get(j).first;
				}
			}
			
			//TODO:comeback:do we need this check
			if(cnz + maxnnz > cnzmax){
				//multstack is already a dynamic array
			}
			
			while(wset.size() > 0){
				HeapEntry entry = wset.poll();
				if(entry != null){
					int locb = entry.runr;
					int mrhs = entry.num * B.numArray.get(B.cp.get(i) + locb);
					//TODO:comeback:check about the returnedSAID
					if(cnz != prevcnz && multStack.size() >= cnz-1 
							&& multStack.get(cnz -1) != null && multStack.get(cnz -1).key.second == entry.key){
						multStack.get(cnz -1).value += mrhs; 
					}
					else{
//						StackEntry sentry = multStack.get(cnz);
//						sentry.value = mrhs;
//						sentry.key = new Pair(B.jc.get(i), entry.key);
						StackEntry sentry = new StackEntry(mrhs, new Pair(entry.key, B.jc.get(i)));
						multStack.add(sentry);
					}
					
					if((++(colinds.get(locb).first)) != colinds.get(locb).second){
						HeapEntry nentry = new HeapEntry(A.ir.get(colinds.get(locb).first), 
															entry.runr, A.numArray.get(colinds.get(locb).first));
						wset.add(nentry);
					}
				}
			}	
		}
		return cnz;
	}
	
	private static void copy(List<Integer> ir, int start, int end, int[] colnums) {
		for(int k = start; k < end; k++){
			colnums[k - start] = ir.get(k);
		}
	}

	public static int ceil(float cf) {
		return (int) Math.ceil(cf);
	}
	
	public static int floor(float cf){
		return (int) Math.floor(cf);
	}

}
