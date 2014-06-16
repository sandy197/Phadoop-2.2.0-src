package org.apache.hadoop.mapreduce.v2.app;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.mapred.RAPLRecord;
import org.apache.hadoop.mapreduce.v2.app.MRAppMaster.Cluster;

public class TestTargetComputation {
	private int getMaxExecTimeIndx(Map<Integer, RAPLRecord> raplRecords, String hostName){
    	int maxTaskIdx = -1;
		long maxExecTime = Long.MIN_VALUE;
    	for(Integer i : raplRecords.keySet()){
			RAPLRecord rec = raplRecords.get(i);
			if(hostName != null && hostName.equals(rec.getHostname())
					&& rec.getExectime() > maxExecTime){
				maxTaskIdx = i;
				maxExecTime = rec.getExectime();
			}
			else if(hostName == null && rec.getExectime() > maxExecTime){
				maxTaskIdx = i;
				maxExecTime = rec.getExectime();
			}
		}
    	return maxTaskIdx;
    }
	
	void computeTargetExecTimes(Map<Integer, RAPLRecord> raplRecords) throws IOException {
		if(raplRecords != null){
			int taskCount  = raplRecords.size();
			Map<Integer, RAPLRecord> reasgndRecs = new HashMap<Integer, RAPLRecord>();
			// plain max of all task exectimes
			int maxTaskIdx = getMaxExecTimeIndx(raplRecords, null);
			//set this max time for all tasks which are not in the same package.
			//Not simple there is a problem with this implementation as well
			List<Cluster> clusters = new ArrayList<Cluster>();
			//cluster all the records
			for(Integer i : raplRecords.keySet()){
				RAPLRecord rec = raplRecords.get(i);
				Cluster cluster = getCluster(rec.getHostname(), rec.getPkg(), clusters);
				if(cluster == null){
					cluster = new Cluster(rec.getHostname(), rec.getPkg());
					clusters.add(cluster);
				}
				cluster.addIfBelongs(rec);
			}
			//set the target times for each cluster.
			long targetTime = raplRecords.get(maxTaskIdx).getExectime();
			RAPLRecord maxRec = raplRecords.get(maxTaskIdx);
			Cluster maxCluster = getCluster(maxRec.getHostname(), maxRec.getPkg(), clusters);
			for(Cluster cluster : clusters){
				cluster.setTargetTime(targetTime, maxCluster);
			}
			System.out.println("Done computing the target times for all the tasks");
		}
		else
			System.out.println("RAPL records supplied are null");
	}
	
	private boolean isClusterPresent(String hostName, short pkgId, List<Cluster> clusters){
    	for(Cluster cluster : clusters){
    		if(hostName.equals(cluster.hostName) && pkgId == cluster.pkgId)
    			return true;
    	}
    	return false;
    }
    
    private Cluster getCluster(String hostName, short pkgId, List<Cluster> clusters){
    	for(Cluster cluster : clusters){
    		if(hostName.equals(cluster.hostName) && pkgId == cluster.pkgId)
    			return cluster;
    	}
    	return null;
    }
    
    public static void main(String[] args){
    	TestTargetComputation test = new TestTargetComputation();
    	Map<Integer, RAPLRecord> raplRecords = new HashMap<Integer, RAPLRecord>();
    	RAPLRecord rec;
    	int key = 0;
    	long exectime = 0L;
    	for(int i = 0; i < 5; i++){
	    	rec = new RAPLRecord(++exectime);
	    	rec.setHostname("local");
	    	rec.setPkg((short)0);
	    	raplRecords.put(++key, rec);
	    	try {
				test.computeTargetExecTimes(raplRecords);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    	
    	for(Integer k : raplRecords.keySet()){
    		System.out.println(raplRecords.get(k));
    	}
    }
}
