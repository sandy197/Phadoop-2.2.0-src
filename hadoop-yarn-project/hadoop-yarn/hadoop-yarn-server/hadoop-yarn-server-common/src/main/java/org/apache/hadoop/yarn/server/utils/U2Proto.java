package org.apache.hadoop.yarn.server.utils;
//package org.apache.hadoop.yarn.ipc;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;


public class U2Proto {
	public static int BASE_PORT = 16000;
	public static int MAP_BASE_PORT = 17000;
	
	public static enum Command{
		U2_RUN_TASK,
		U2_IS_ACTIVE,
		U2_STOP_LISTENER
	}
	
	public static enum Status{
		U2_SUCCESS
	}
	
	public static Response getResponse(Request req, Socket socket) throws IOException, ClassNotFoundException{
		ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
		ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
		oos.writeObject(req);
		Response response = (Response)ois.readObject();
		oos.close();
		ois.close();
		return response;
	}

	public static boolean isTaskProcessListening(Socket socket) {
		Request req = new Request(Command.U2_IS_ACTIVE);
		Response resp;
		boolean isAlive = false;
		//send request over the socket
		try {
			resp = getResponse(req, socket);
			if(resp.getStatus() == Status.U2_SUCCESS){
				isAlive = true;
			}			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return isAlive;
	}
	
	public static class Request{
		private Command cmd;
		private String hostName;
		private int portNum;
		private String TaskAttemptId;
		private int jvmIdInt;
		
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
	
	public static class Response{
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

