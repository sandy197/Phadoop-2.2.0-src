package org.apache.hadoop.yarn.server.utils;
//package org.apache.hadoop.yarn.ipc;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Map;


public class U2Proto {
	public static int BASE_PORT = 16000;
	public static int MAP_BASE_PORT = 17000;
	
	public static enum Command implements Serializable{
		U2_RUN_TASK,
		U2_IS_ACTIVE,
		U2_STOP_LISTENER
	}
	
	public static enum Status implements Serializable{
		U2_SUCCESS,
		U2_FAILURE
	}
	
	public static Response getResponse(Request req, Socket socket){
		Response response =  null;
		try{
			ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
			ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
			oos.writeObject(req);
			response = (Response)ois.readObject();
			oos.close();
			ois.close();
		} catch(IOException iex){
			iex.printStackTrace();			
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return response;
	}

	public static boolean isTaskProcessListening(int port) {
		boolean retVal = false;
		Socket socket = null;
		Request req = new Request(Command.U2_IS_ACTIVE);
		Response resp;
		
		
		try {
             socket = new Socket(InetAddress.getByName(null), port);
             resp = getResponse(req, socket);
             if(resp.getStatus() == Status.U2_SUCCESS){
            	 retVal = true;
            	 System.out.println(port + "**Process is listening, port unavailable");
             }
        } catch (Exception ex) {
        	ex.printStackTrace();
        	System.out.println(port + "**No listener Process running, port available");
            retVal =  false;
        }
		finally{
			closeSilently(socket);
		}
		return retVal;
    }
	
	private static void closeSilently(Socket socket) {
		try{
			if(socket != null) socket.close();
		} catch(IOException ex){
			//nothing
		}
		
	}

	public static class Request implements Serializable{
		private Command cmd;
		private String hostName;
		private int portNum;
		private String TaskAttemptId;
		private int jvmIdInt;
		private Map<String, String> environment;
		
		public Map<String, String> getEnvironment() {
			return environment;
		}

		public void setEnvironment(Map<String, String> environment) {
			this.environment = environment;
		}

		public Request(Command cmd){
			this.cmd = cmd;
		}

		public String getHostName() {
			return hostName;
		}

		public void setHostName(String hostName) {
			this.hostName = hostName;
		}

		public int getPortNum() {
			return portNum;
		}

		public void setPortNum(int portNum) {
			this.portNum = portNum;
		}

		public String getTaskAttemptId() {
			return TaskAttemptId;
		}

		public void setTaskAttemptId(String taskAttemptId) {
			TaskAttemptId = taskAttemptId;
		}

		public int getJvmIdInt() {
			return jvmIdInt;
		}

		public void setJvmIdInt(int jvmIdInt) {
			this.jvmIdInt = jvmIdInt;
		}

		public Command getCmd() {
			return cmd;
		}

		public void setCmd(Command cmd) {
			this.cmd = cmd;
		}
		
	}
	
	public static class Response implements Serializable{
		private Status status;
		private String message;
		
		public Response(Status status){
			this.status = status;
		}

		public Status getStatus() {
			return status;
		}

		public void setStatus(Status status) {
			this.status = status;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}
		
		
	}
	
	
	
	

}

