/**
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package org.apache.hadoop.mapred;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.List;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSError;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocalDirAllocator;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.io.IOUtils;
import org.apache.hadoop.ipc.RPC;

//srkandul
import org.apache.hadoop.ipc.GenericMatrix;

import org.apache.hadoop.mapreduce.MRConfig;
import org.apache.hadoop.mapreduce.MRJobConfig;
import org.apache.hadoop.mapreduce.TaskType;
import org.apache.hadoop.mapreduce.filecache.DistributedCache;
import org.apache.hadoop.mapreduce.security.TokenCache;
import org.apache.hadoop.mapreduce.security.token.JobTokenIdentifier;
import org.apache.hadoop.mapreduce.security.token.JobTokenSecretManager;
import org.apache.hadoop.mapreduce.v2.util.MRApps;
import org.apache.hadoop.metrics2.lib.DefaultMetricsSystem;
import org.apache.hadoop.metrics2.source.JvmMetrics;
import org.apache.hadoop.net.NetUtils;
import org.apache.hadoop.security.Credentials;
import org.apache.hadoop.security.SecurityUtil;
import org.apache.hadoop.security.UserGroupInformation;
import org.apache.hadoop.security.token.Token;
import org.apache.hadoop.util.DiskChecker.DiskErrorException;
import org.apache.hadoop.util.StringUtils;
import org.apache.hadoop.yarn.YarnUncaughtExceptionHandler;
import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.api.ApplicationConstants.Environment;
import org.apache.hadoop.yarn.api.records.ApplicationAttemptId;
import org.apache.hadoop.yarn.util.ConverterUtils;
import org.apache.log4j.LogManager;
//import org.apache.hadoop.yarn.ipc.U2Proto;
import org.apache.hadoop.yarn.server.utils.U2Proto;

/**
 * The main() for MapReduce task processes.
 */
public class YarnChild {

  private static final Log LOG = LogFactory.getLog(YarnChild.class);
  
  private static final boolean DEBUG = true;
  //srkandul : for the sake of git
  public GenericMatrix matrixRead;
  //TODO:populate in job conf !
  private boolean stopChild = false;

  static volatile TaskAttemptID taskid = null;

//srkandul
public YarnChild(){
}

	  
  public boolean isStopChild() {
	return stopChild;
  }


  public void setStopChild(boolean stopChild) {
	  this.stopChild = stopChild;
  }
/*
  //srkandul
  public void yarnChildMain(String host, int port, String taskAttemptId, int jvmIdInt) throws Throwable {
	    Thread.setDefaultUncaughtExceptionHandler(new YarnUncaughtExceptionHandler());
	    LOG.debug("Child starting");

	    final JobConf job = new JobConf();
	    job.setMatrix(matrixRead);
	    // Initing with our JobConf allows us to avoid loading confs twice
	    Limits.init(job);
	    job.addResource(MRJobConfig.JOB_CONF_FILE);
	    UserGroupInformation.setConfiguration(job);

	    //String host = args[0];
	    //int port = Integer.parseInt(args[1]);
	    final InetSocketAddress address =
	        NetUtils.createSocketAddrForHost(host, port);
	    //final TaskAttemptID firstTaskid = TaskAttemptID.forName(args[2]);
	    final TaskAttemptID firstTaskid = TaskAttemptID.forName(taskAttemptId);
	    //int jvmIdInt = Integer.parseInt(args[3]);
	    JVMId jvmId = new JVMId(firstTaskid.getJobID(),
	        firstTaskid.getTaskType() == TaskType.MAP, jvmIdInt);
	    
	    //TODO:check if we really need this. 
	    // A map task from the 2nd iteration can stop the child form the previous reduce run.
	    // We cannot launch a separate process for the MAP task as we can not differentiate it. 
	    if(firstTaskid.getTaskType() == TaskType.MAP){
	    	this.setStopChild(true);
	    }
	    // initialize metrics
	    DefaultMetricsSystem.initialize(
	        StringUtils.camelize(firstTaskid.getTaskType().name()) +"Task");

	    // Security framework already loaded the tokens into current ugi
	    Credentials credentials =
	        UserGroupInformation.getCurrentUser().getCredentials();
	    LOG.info("Executing with tokens:");
	    for (Token<?> token: credentials.getAllTokens()) {
	      LOG.info(token);
	    }

	    // Create TaskUmbilicalProtocol as actual task owner.
	    UserGroupInformation taskOwner =
	      UserGroupInformation.createRemoteUser(firstTaskid.getJobID().toString());
	    Token<JobTokenIdentifier> jt = TokenCache.getJobToken(credentials);
	    SecurityUtil.setTokenService(jt, address);
	    taskOwner.addToken(jt);
	    final TaskUmbilicalProtocol umbilical =
	      taskOwner.doAs(new PrivilegedExceptionAction<TaskUmbilicalProtocol>() {
	      @Override
	      public TaskUmbilicalProtocol run() throws Exception {
	        return (TaskUmbilicalProtocol)RPC.getProxy(TaskUmbilicalProtocol.class,
	            TaskUmbilicalProtocol.versionID, address, job);
	      }
	    });
	//srkandul
	    umbilical.getClass().getName();
	    if(umbilical instanceof  TaskAttemptContextImpl){
	    	
	    }
	    
	    // report non-pid to application master
	    JvmContext context = new JvmContext(jvmId, "-1000");
	    LOG.debug("PID: " + System.getenv().get("JVM_PID"));
	    Task task = null;
	    UserGroupInformation childUGI = null;

	    try {
	      int idleLoopCount = 0;
	      JvmTask myTask = null;;
	      // poll for new task
	      for (int idle = 0; null == myTask; ++idle) {
	        long sleepTimeMilliSecs = Math.min(idle * 500, 1500);
	        LOG.info("Sleeping for " + sleepTimeMilliSecs
	            + "ms before retrying again. Got null now.");
	        MILLISECONDS.sleep(sleepTimeMilliSecs);
	        myTask = umbilical.getTask(context);
	      }
	      if (myTask.shouldDie()) {
	        return;
	      }

	      task = myTask.getTask();
	      YarnChild.taskid = task.getTaskID();

	      // Create the job-conf and set credentials
	      configureTask(job, task, credentials, jt);

	      // log the system properties
	      String systemPropsToLog = MRApps.getSystemPropertiesToLog(job);
	      if (systemPropsToLog != null) {
	        LOG.info(systemPropsToLog);
	      }

	      // Initiate Java VM metrics
	      JvmMetrics.initSingleton(jvmId.toString(), job.getSessionId());
	      childUGI = UserGroupInformation.createRemoteUser(System
	          .getenv(ApplicationConstants.Environment.USER.toString()));
	      // Add tokens to new user so that it may execute its task correctly.
	      childUGI.addCredentials(credentials);

	      // set job classloader if configured before invoking the task
	      MRApps.setJobClassLoader(job);

	      // Create a final reference to the task for the doAs block
	      final Task taskFinal = task;
	      childUGI.doAs(new PrivilegedExceptionAction<Object>() {
	        @Override
	        public Object run() throws Exception {
	          // use job-specified working directory
	          FileSystem.get(job).setWorkingDirectory(job.getWorkingDirectory());
	          taskFinal.run(job, umbilical); // run the task
	          return null;
	        }
	      });
	      //TODO : get the matrix here from the job.
	      this.matrixRead = job.getMatrix();
	    } catch (FSError e) {
	      LOG.fatal("FSError from child", e);
	      umbilical.fsError(taskid, e.getMessage());
	    } catch (Exception exception) {
	      LOG.warn("Exception running child : "
	          + StringUtils.stringifyException(exception));
	      try {
	        if (task != null) {
	          // do cleanup for the task
	          if (childUGI == null) { // no need to job into doAs block
	            task.taskCleanup(umbilical);
	          } else {
	            final Task taskFinal = task;
	            childUGI.doAs(new PrivilegedExceptionAction<Object>() {
	              @Override
	              public Object run() throws Exception {
	                taskFinal.taskCleanup(umbilical);
	                return null;
	              }
	            });
	          }
	        }
	      } catch (Exception e) {
	        LOG.info("Exception cleaning up: " + StringUtils.stringifyException(e));
	      }
	      // Report back any failures, for diagnostic purposes
	      if (taskid != null) {
	        umbilical.fatalError(taskid, StringUtils.stringifyException(exception));
	      }
	    } catch (Throwable throwable) {
	      LOG.fatal("Error running child : "
	    	        + StringUtils.stringifyException(throwable));
	      if (taskid != null) {
	        Throwable tCause = throwable.getCause();
	        String cause = tCause == null
	                                 ? throwable.getMessage()
	                                 : StringUtils.stringifyException(tCause);
	        umbilical.fatalError(taskid, cause);
	      }
	    } finally {
	      RPC.stopProxy(umbilical);
	      DefaultMetricsSystem.shutdown();
	      // Shutting down log4j of the child-vm...
	      // This assumes that on return from Task.run()
	      // there is no more logging done.
	      LogManager.shutdown();
	    }
	  }
*/
  
  
  /**
   * This has to loop around waiting for data from ContainerLaunch.
   * It's a server waiting for commands. Commands contain the args from the original implementation.
   * 
   * 
	 * @param args port on which the server listens.
	 * @throws Throwable
	 * @author sandeep
	 */
  public static void main(String[] args){
	  boolean isFirst = true;
	  ServerSocket ssocket;
	  if(args.length < 4){
		  LOG.error("Insufficient args provided:");
	  }
	  YarnChild yc = new YarnChild();
	  int listeningPort = U2Proto.BASE_PORT + Integer.parseInt(args[3]);
	  LOG.info("Yarn process launched, listening on port:" + listeningPort);
	  //Create listening socket using the listening port
	  try{
		  ssocket = new ServerSocket(listeningPort);
	  }
	  catch(IOException ex){
		  LOG.fatal("Unable to listen on the port" + ex.getStackTrace());
		  return;
	  }
	  try{
		  while(!yc.isStopChild()){
			  if(isFirst){
				  yc.yarnChildMain(args[0], Integer.parseInt(args[1]), args[2], Integer.parseInt(args[3]));
				  LOG.info("**returned from the yarnchildmain invocation");
				  isFirst = false;
				  continue;
			  }
			  if (DEBUG) LOG.info(listeningPort + ":Yarnchild started listening");
			//listen for communication from ContainerLaunch only after the initial task execution
			  Socket csocket = ssocket.accept();
			  if(DEBUG) LOG.info("Connection accepted by the child");
			  ObjectInputStream ois = new ObjectInputStream(csocket.getInputStream());
			  ObjectOutputStream oos = new ObjectOutputStream(csocket.getOutputStream());
			  //TODO : create U2ProtoRequest/U2ProtoResponse in YARN project to avoid cyclic dependency.
			  U2Proto.Request request = (U2Proto.Request)ois.readObject();
			  //TODO: put all the below code as registered handler
			  // handle the request
			  if(request.getCmd() == U2Proto.Command.U2_RUN_TASK){
				  
				  String hostAM = request.getHostName();
				  int portAM = request.getPortNum();
				  String taskAttemptId = request.getTaskAttemptId();
				  int jvmIdInt = request.getJvmIdInt();
				  yc.yarnChildMain(hostAM, portAM, taskAttemptId, jvmIdInt);
				  //TODO:see if containerLaunch needs an ACK response
			  }
			  else if(request.getCmd() == U2Proto.Command.U2_STOP_LISTENER){
				  yc.setStopChild(true);
			  }
			  else if(request.getCmd() == U2Proto.Command.U2_IS_ACTIVE){
				  //TODO : send a response saying that I am alive and accepting requests
				  oos.writeObject(new U2Proto.Response(U2Proto.Status.U2_SUCCESS));
			  }
			  csocket.close();
		  }
		  if (DEBUG) LOG.info("YarnChild stopped !! No listener now on :" + listeningPort );
	  }
	  catch(IOException e){
		e.printStackTrace();
	  } catch (ClassNotFoundException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	  } catch (Throwable e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	  }
	  finally{
		  try {
			ssocket.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	  }
  }

public void yarnChildMain(String host, int port, String taskAttemptId, int jvmIdInt) throws Throwable {
//  public static void main1(String[] args) throws Throwable {
    Thread.setDefaultUncaughtExceptionHandler(new YarnUncaughtExceptionHandler());
    LOG.debug("Child starting");

    final JobConf defaultConf = new JobConf();
    defaultConf.addResource(MRJobConfig.JOB_CONF_FILE);
    UserGroupInformation.setConfiguration(defaultConf);

//    String host = args[0];
//    int port = Integer.parseInt(args[1]);
    final InetSocketAddress address =
        NetUtils.createSocketAddrForHost(host, port);
    final TaskAttemptID firstTaskid = TaskAttemptID.forName(taskAttemptId);
//    int jvmIdInt = Integer.parseInt(args[3]);
    JVMId jvmId = new JVMId(firstTaskid.getJobID(),
        firstTaskid.getTaskType() == TaskType.MAP, jvmIdInt);
    
  //TODO:check if we really need this. 
    // A map task from the 2nd iteration can stop the child form the previous reduce run.
    // We cannot launch a separate process for the MAP task as we can not differentiate it. 
    if(firstTaskid.getTaskType() == TaskType.MAP){
    	LOG.info("Map task is set to be stopped");
    	this.setStopChild(true);
    }

    // initialize metrics
    DefaultMetricsSystem.initialize(
        StringUtils.camelize(firstTaskid.getTaskType().name()) +"Task");

    // Security framework already loaded the tokens into current ugi
    Credentials credentials =
        UserGroupInformation.getCurrentUser().getCredentials();
    LOG.info("Executing with tokens:");
    for (Token<?> token: credentials.getAllTokens()) {
      LOG.info(token);
    }

    // Create TaskUmbilicalProtocol as actual task owner.
    UserGroupInformation taskOwner =
      UserGroupInformation.createRemoteUser(firstTaskid.getJobID().toString());
    Token<JobTokenIdentifier> jt = TokenCache.getJobToken(credentials);
    SecurityUtil.setTokenService(jt, address);
    taskOwner.addToken(jt);
    final TaskUmbilicalProtocol umbilical =
      taskOwner.doAs(new PrivilegedExceptionAction<TaskUmbilicalProtocol>() {
      @Override
      public TaskUmbilicalProtocol run() throws Exception {
        return (TaskUmbilicalProtocol)RPC.getProxy(TaskUmbilicalProtocol.class,
            TaskUmbilicalProtocol.versionID, address, defaultConf);
      }
    });

	//srkandul
	System.out.println(umbilical.getClass().getName());
	if(umbilical instanceof  TaskAttemptContextImpl){	
		System.out.println("************* Type is TaskAttemptContextImpl ****************");
	}

    // report non-pid to application master
    JvmContext context = new JvmContext(jvmId, "-1000");
    LOG.debug("PID: " + System.getenv().get("JVM_PID"));
    Task task = null;
    UserGroupInformation childUGI = null;

    try {
      int idleLoopCount = 0;
      JvmTask myTask = null;;
      // poll for new task
      for (int idle = 0; null == myTask; ++idle) {
        long sleepTimeMilliSecs = Math.min(idle * 500, 1500);
        LOG.info("Sleeping for " + sleepTimeMilliSecs
            + "ms before retrying again. Got null now.");
        MILLISECONDS.sleep(sleepTimeMilliSecs);
        myTask = umbilical.getTask(context);
      }
      if (myTask.shouldDie()) {
    	  LOG.info("**Die signal for mytask");
        return;
      }

      task = myTask.getTask();
      YarnChild.taskid = task.getTaskID();

      // Create the job-conf and set credentials
      final JobConf job = configureTask(task, credentials, jt);

      // Initiate Java VM metrics
      JvmMetrics.initSingleton(jvmId.toString(), job.getSessionId());
      childUGI = UserGroupInformation.createRemoteUser(System
          .getenv(ApplicationConstants.Environment.USER.toString()));
      // Add tokens to new user so that it may execute its task correctly.
      childUGI.addCredentials(credentials);

      // set job classloader if configured before invoking the task
      MRApps.setJobClassLoader(job);

      // Create a final reference to the task for the doAs block
      final Task taskFinal = task;
      childUGI.doAs(new PrivilegedExceptionAction<Object>() {
        @Override
        public Object run() throws Exception {
          // use job-specified working directory
          FileSystem.get(job).setWorkingDirectory(job.getWorkingDirectory());
          taskFinal.run(job, umbilical); // run the task
          return null;
        }
      });
    } catch (FSError e) {
      LOG.fatal("FSError from child", e);
      umbilical.fsError(taskid, e.getMessage());
    } catch (Exception exception) {
      LOG.warn("Exception running child : "
          + StringUtils.stringifyException(exception));
      try {
        if (task != null) {
          // do cleanup for the task
          if (childUGI == null) { // no need to job into doAs block
            task.taskCleanup(umbilical);
          } else {
            final Task taskFinal = task;
            childUGI.doAs(new PrivilegedExceptionAction<Object>() {
              @Override
              public Object run() throws Exception {
                taskFinal.taskCleanup(umbilical);
                return null;
              }
            });
          }
        }
      } catch (Exception e) {
        LOG.info("Exception cleaning up: " + StringUtils.stringifyException(e));
      }
      // Report back any failures, for diagnostic purposes
      if (taskid != null) {
        umbilical.fatalError(taskid, StringUtils.stringifyException(exception));
      }
    } catch (Throwable throwable) {
      LOG.fatal("Error running child : "
    	        + StringUtils.stringifyException(throwable));
      if (taskid != null) {
        Throwable tCause = throwable.getCause();
        String cause = tCause == null
                                 ? throwable.getMessage()
                                 : StringUtils.stringifyException(tCause);
        umbilical.fatalError(taskid, cause);
      }
    } finally {
      RPC.stopProxy(umbilical);
      DefaultMetricsSystem.shutdown();
      // Shutting down log4j of the child-vm...
      // This assumes that on return from Task.run()
      // there is no more logging done.
      LogManager.shutdown();
    }
  }

  public static void main1(String[] args) throws Throwable {
    Thread.setDefaultUncaughtExceptionHandler(new YarnUncaughtExceptionHandler());
    LOG.debug("Child starting");

    final JobConf defaultConf = new JobConf();
    defaultConf.addResource(MRJobConfig.JOB_CONF_FILE);
    UserGroupInformation.setConfiguration(defaultConf);

    String host = args[0];
    int port = Integer.parseInt(args[1]);
    final InetSocketAddress address =
        NetUtils.createSocketAddrForHost(host, port);
    final TaskAttemptID firstTaskid = TaskAttemptID.forName(args[2]);
    int jvmIdInt = Integer.parseInt(args[3]);
    JVMId jvmId = new JVMId(firstTaskid.getJobID(),
        firstTaskid.getTaskType() == TaskType.MAP, jvmIdInt);

    // initialize metrics
    DefaultMetricsSystem.initialize(
        StringUtils.camelize(firstTaskid.getTaskType().name()) +"Task");

    // Security framework already loaded the tokens into current ugi
    Credentials credentials =
        UserGroupInformation.getCurrentUser().getCredentials();
    LOG.info("Executing with tokens:");
    for (Token<?> token: credentials.getAllTokens()) {
      LOG.info(token);
    }

    // Create TaskUmbilicalProtocol as actual task owner.
    UserGroupInformation taskOwner =
      UserGroupInformation.createRemoteUser(firstTaskid.getJobID().toString());
    Token<JobTokenIdentifier> jt = TokenCache.getJobToken(credentials);
    SecurityUtil.setTokenService(jt, address);
    taskOwner.addToken(jt);
    final TaskUmbilicalProtocol umbilical =
      taskOwner.doAs(new PrivilegedExceptionAction<TaskUmbilicalProtocol>() {
      @Override
      public TaskUmbilicalProtocol run() throws Exception {
        return (TaskUmbilicalProtocol)RPC.getProxy(TaskUmbilicalProtocol.class,
            TaskUmbilicalProtocol.versionID, address, defaultConf);
      }
    });

	//srkandul
	System.out.println(umbilical.getClass().getName());
	if(umbilical instanceof  TaskAttemptContextImpl){	
		System.out.println("************* Type is TaskAttemptContextImpl ****************");
	}

    // report non-pid to application master
    JvmContext context = new JvmContext(jvmId, "-1000");
    LOG.debug("PID: " + System.getenv().get("JVM_PID"));
    Task task = null;
    UserGroupInformation childUGI = null;

    try {
      int idleLoopCount = 0;
      JvmTask myTask = null;;
      // poll for new task
      for (int idle = 0; null == myTask; ++idle) {
        long sleepTimeMilliSecs = Math.min(idle * 500, 1500);
        LOG.info("Sleeping for " + sleepTimeMilliSecs
            + "ms before retrying again. Got null now.");
        MILLISECONDS.sleep(sleepTimeMilliSecs);
        myTask = umbilical.getTask(context);
      }
      if (myTask.shouldDie()) {
        return;
      }

      task = myTask.getTask();
      YarnChild.taskid = task.getTaskID();

      // Create the job-conf and set credentials
      final JobConf job = configureTask(task, credentials, jt);

      // Initiate Java VM metrics
      JvmMetrics.initSingleton(jvmId.toString(), job.getSessionId());
      childUGI = UserGroupInformation.createRemoteUser(System
          .getenv(ApplicationConstants.Environment.USER.toString()));
      // Add tokens to new user so that it may execute its task correctly.
      childUGI.addCredentials(credentials);

      // set job classloader if configured before invoking the task
      MRApps.setJobClassLoader(job);

      // Create a final reference to the task for the doAs block
      final Task taskFinal = task;
      childUGI.doAs(new PrivilegedExceptionAction<Object>() {
        @Override
        public Object run() throws Exception {
          // use job-specified working directory
          FileSystem.get(job).setWorkingDirectory(job.getWorkingDirectory());
          taskFinal.run(job, umbilical); // run the task
          return null;
        }
      });
    } catch (FSError e) {
      LOG.fatal("FSError from child", e);
      umbilical.fsError(taskid, e.getMessage());
    } catch (Exception exception) {
      LOG.warn("Exception running child : "
          + StringUtils.stringifyException(exception));
      try {
        if (task != null) {
          // do cleanup for the task
          if (childUGI == null) { // no need to job into doAs block
            task.taskCleanup(umbilical);
          } else {
            final Task taskFinal = task;
            childUGI.doAs(new PrivilegedExceptionAction<Object>() {
              @Override
              public Object run() throws Exception {
                taskFinal.taskCleanup(umbilical);
                return null;
              }
            });
          }
        }
      } catch (Exception e) {
        LOG.info("Exception cleaning up: " + StringUtils.stringifyException(e));
      }
      // Report back any failures, for diagnostic purposes
      if (taskid != null) {
        umbilical.fatalError(taskid, StringUtils.stringifyException(exception));
      }
    } catch (Throwable throwable) {
      LOG.fatal("Error running child : "
    	        + StringUtils.stringifyException(throwable));
      if (taskid != null) {
        Throwable tCause = throwable.getCause();
        String cause = tCause == null
                                 ? throwable.getMessage()
                                 : StringUtils.stringifyException(tCause);
        umbilical.fatalError(taskid, cause);
      }
    } finally {
      RPC.stopProxy(umbilical);
      DefaultMetricsSystem.shutdown();
      // Shutting down log4j of the child-vm...
      // This assumes that on return from Task.run()
      // there is no more logging done.
      LogManager.shutdown();
    }
  }

  /**
   * Configure mapred-local dirs. This config is used by the task for finding
   * out an output directory.
   * @throws IOException 
   */
  private static void configureLocalDirs(Task task, JobConf job) throws IOException {
    String[] localSysDirs = StringUtils.getTrimmedStrings(
        System.getenv(Environment.LOCAL_DIRS.name()));
    job.setStrings(MRConfig.LOCAL_DIR, localSysDirs);
    LOG.info(MRConfig.LOCAL_DIR + " for child: " + job.get(MRConfig.LOCAL_DIR));
    LocalDirAllocator lDirAlloc = new LocalDirAllocator(MRConfig.LOCAL_DIR);
    Path workDir = null;
    // First, try to find the JOB_LOCAL_DIR on this host.
    try {
      workDir = lDirAlloc.getLocalPathToRead("work", job);
    } catch (DiskErrorException e) {
      // DiskErrorException means dir not found. If not found, it will
      // be created below.
    }
    if (workDir == null) {
      // JOB_LOCAL_DIR doesn't exist on this host -- Create it.
      workDir = lDirAlloc.getLocalPathForWrite("work", job);
      FileSystem lfs = FileSystem.getLocal(job).getRaw();
      boolean madeDir = false;
      try {
        madeDir = lfs.mkdirs(workDir);
      } catch (FileAlreadyExistsException e) {
        // Since all tasks will be running in their own JVM, the race condition
        // exists where multiple tasks could be trying to create this directory
        // at the same time. If this task loses the race, it's okay because
        // the directory already exists.
        madeDir = true;
        workDir = lDirAlloc.getLocalPathToRead("work", job);
      }
      if (!madeDir) {
          throw new IOException("Mkdirs failed to create "
              + workDir.toString());
      }
    }
    job.set(MRJobConfig.JOB_LOCAL_DIR,workDir.toString());
  }

  private static JobConf configureTask(Task task, Credentials credentials,
      Token<JobTokenIdentifier> jt) throws IOException {
    final JobConf job = new JobConf(MRJobConfig.JOB_CONF_FILE);
    job.setCredentials(credentials);

    ApplicationAttemptId appAttemptId =
        ConverterUtils.toContainerId(
            System.getenv(Environment.CONTAINER_ID.name()))
            .getApplicationAttemptId();
    LOG.debug("APPLICATION_ATTEMPT_ID: " + appAttemptId);
    // Set it in conf, so as to be able to be used the the OutputCommitter.
    job.setInt(MRJobConfig.APPLICATION_ATTEMPT_ID,
        appAttemptId.getAttemptId());

    // set tcp nodelay
    job.setBoolean("ipc.client.tcpnodelay", true);
    job.setClass(MRConfig.TASK_LOCAL_OUTPUT_CLASS,
        YarnOutputFiles.class, MapOutputFile.class);
    // set the jobToken and shuffle secrets into task
    task.setJobTokenSecret(
        JobTokenSecretManager.createSecretKey(jt.getPassword()));
    byte[] shuffleSecret = TokenCache.getShuffleSecretKey(credentials);
    if (shuffleSecret == null) {
      LOG.warn("Shuffle secret missing from task credentials."
          + " Using job token secret as shuffle secret.");
      shuffleSecret = jt.getPassword();
    }
    task.setShuffleSecret(
        JobTokenSecretManager.createSecretKey(shuffleSecret));

    // setup the child's MRConfig.LOCAL_DIR.
    configureLocalDirs(task, job);

    // setup the child's attempt directories
    // Do the task-type specific localization
    task.localizeConfiguration(job);

    // Set up the DistributedCache related configs
    setupDistributedCacheConfig(job);

    // Overwrite the localized task jobconf which is linked to in the current
    // work-dir.
    Path localTaskFile = new Path(MRJobConfig.JOB_CONF_FILE);
    writeLocalJobFile(localTaskFile, job);
    task.setJobFile(localTaskFile.toString());
    task.setConf(job);
    return job;
  }

  /**
   * Set up the DistributedCache related configs to make
   * {@link DistributedCache#getLocalCacheFiles(Configuration)}
   * and
   * {@link DistributedCache#getLocalCacheArchives(Configuration)}
   * working.
   * @param job
   * @throws IOException
   */
  private static void setupDistributedCacheConfig(final JobConf job)
      throws IOException {

    String localWorkDir = System.getenv("PWD");
    //        ^ ^ all symlinks are created in the current work-dir

    // Update the configuration object with localized archives.
    URI[] cacheArchives = DistributedCache.getCacheArchives(job);
    if (cacheArchives != null) {
      List<String> localArchives = new ArrayList<String>();
      for (int i = 0; i < cacheArchives.length; ++i) {
        URI u = cacheArchives[i];
        Path p = new Path(u);
        Path name =
            new Path((null == u.getFragment()) ? p.getName()
                : u.getFragment());
        String linkName = name.toUri().getPath();
        localArchives.add(new Path(localWorkDir, linkName).toUri().getPath());
      }
      if (!localArchives.isEmpty()) {
        job.set(MRJobConfig.CACHE_LOCALARCHIVES, StringUtils
            .arrayToString(localArchives.toArray(new String[localArchives
                .size()])));
      }
    }

    // Update the configuration object with localized files.
    URI[] cacheFiles = DistributedCache.getCacheFiles(job);
    if (cacheFiles != null) {
      List<String> localFiles = new ArrayList<String>();
      for (int i = 0; i < cacheFiles.length; ++i) {
        URI u = cacheFiles[i];
        Path p = new Path(u);
        Path name =
            new Path((null == u.getFragment()) ? p.getName()
                : u.getFragment());
        String linkName = name.toUri().getPath();
        localFiles.add(new Path(localWorkDir, linkName).toUri().getPath());
      }
      if (!localFiles.isEmpty()) {
        job.set(MRJobConfig.CACHE_LOCALFILES,
            StringUtils.arrayToString(localFiles
                .toArray(new String[localFiles.size()])));
      }
    }
  }

  private static final FsPermission urw_gr =
    FsPermission.createImmutable((short) 0640);

  /**
   * Write the task specific job-configuration file.
   * @throws IOException
   */
  private static void writeLocalJobFile(Path jobFile, JobConf conf)
      throws IOException {
    FileSystem localFs = FileSystem.getLocal(conf);
    localFs.delete(jobFile);
    OutputStream out = null;
    try {
      out = FileSystem.create(localFs, jobFile, urw_gr);
      conf.writeXml(out);
    } finally {
      IOUtils.cleanup(LOG, out);
    }
  }

}
