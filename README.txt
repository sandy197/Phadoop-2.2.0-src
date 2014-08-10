This project uses maven to build the framework and JDK 1.6 to compile the sources.

Untar the Phadoop-2.2.0-src.tar.gz file as follows:
tar xf Phadoop-2.2.0-src.tar.gz
The root directory of Phadoop source code is its home directory (PHADOOP_HOME)

To build run the following command from the PHADOOP_HOME dir:
mvn clean package -Pdist -DskipTests

Set the following environment variables:
export PHADOOP_HOME=<PhadoopHomeDir>

export HADOOP_COMMON_HOME=$PHADOOP_HOME/hadoop-common-project/hadoop-common/target/hadoop-common-2.2.0

export HADOOP_HDFS_HOME=$PHADOOP_HOME/hadoop-hdfs-project/hadoop-hdfs/target/hadoop-hdfs-2.2.0

export HADOOP_MAPRED_HOME=$PHADOOP_HOME/hadoop-mapreduce-project/target/hadoop-mapreduce-2.2.0/

export HADOOP_YARN_HOME=$PHADOOP_HOME/hadoop-yarn-project/target/hadoop-yarn-project-2.2.0/

export PATH=$HADOOP_COMMON_HOME/bin:$HADOOP_HDFS_HOME/bin:$PATH

export PATH=$PHADOOP_HOME/hadoop-yarn-project/target/hadoop-yarn-project-2.2.0/bin/:$PATH

export HADOOP_CONF_DIR=$PHADOOP_HOME/hadoop-yarn-project/target/hadoop-yarn-project-2.2.0/conf/

Start the HDFS daemons: Namenode, Datanode using
hdfs namenode -format
hdfs namenode
hdfs datanode

Start the YARN daemons : Resource manager, Node manager
yarn resourcemanager
yarn nodemanager

Download the libjars folder from /cvs/srkandul/Phadoop_libjars for running Phadoop workloads.

To run any workload run the following commands:
export PHADOOP_LIBS=<libjarsPath>
hadoop fs -mkdir /libraries
hadoop fs -copyFromLocal $PHADOOP_LIBS/libpapi.so /libraries/libpapi.so.1
hadoop fs -copyFromLocal $PHADOOP_LIBS/librapl.so /libraries/librapl.so.1

K-means:

hadoop org.apache.hadoop.examples.MKmeans.MKMDriver -libjars $PHADOOP_LIBS/threadpin-1.0-SNAPSHOT.jar -D mapred.child.env="java.library.path=." -files PHADOOP_LIBS=/libthreadpin.so 2100 40 3 2 20 8 30 16 2 0 0 1

USAGE: <COUNT> <K> <DIMENSION OF VECTORS> <SegmentsPerDimension> <MAXITERATIONS> <num of tasks> <convgDelta> <FirstTaskInputCount> <Diff/Ratio> <isLinear(0/1)> <isCalibration> <opt:powerCap> <isUniform>

<COUNT> : total number of vectors to classify (Not used currently but required)
<K> : Number of groups the input vectors are classified into
<DIMENSION OF VECTORS> : Dimensions of all the vectors handled
<SegmentsPerDimension> : Number of segments a dimension is divided into
<MAXITERATIONS> : Maximum number of iterations if not converged.
<num of tasks> : Number of tasks that need to execute in parallel
<convgDelta> : Convergence distance for k-means
<FirstTaskInputCount> : Number of vectors to be handled by the first task based on which the number of vectors for other tasks is determined
<Diff/Ratio> : Difference if the distribution is linear, ratio if the distribution is exponential
<isLinear(0/1)> : Is the distribution of vectors among parallel tasks linear or exponential
<isCalibration> : Is this a calibration run
<opt:powerCap> : Power cap setting used for calibration. Required if this is a calibration run
<isUniform> : Is the distribution of vectors among the concurrent tasks uniform

To run the sparse/blocked matrix multiplication workload run the following command, which will display the parameters that need to be passed:

hadoop org.apache.hadoop.examples.ParSpMM.SpMMMR.SpMMDriver -libjars $PHADOOP_LIBS/threadpin-1.0-SNAPSHOT.jar -D mapred.child.env="java.library.path=." -files $PHADOOP_LIBS/libthreadpin.so

Running these commands without any parameters will display the list of parameters needed for running the workload.
<I> <J> <K> <IB> <KB> <JB> <nzc> <nzr> <diff> <isAUniform> <reduceTaskCount> <isCalibrationRun> <powerCap(if isCalibration == 1)><isUniformLoad> <isSparseMM>

<I> : Number of rows of matrix A
<J> : Number of columns of matrix A, Number of rows of matrix B
<K> : Number of columns of matrix B
<IB>: Number of rows of block in A
<KB>: Number of columns/rows of block in A/B
<JB>: Number of columns of block in B
<nzc>: Number of non-zero cols in B
<nzr>: Number of non-zero rows in B
<diff>: Difference between the nzc,nzr of adjacent blocks of B
<isAUniform>: Is matrix A uniform wrt to density
<reduceTaskCount>: Number of reduce tasks per map-reduce job (iteration)
<isCalibrationRun> : Is this a calibration run
<powerCap(if isCalibration == 1)> : Power cap setting used for calibration. Required if this is a calibration run
<isUniformLoad> : Is the distribution of vectors among the concurrent tasks uniform
<isSparseMM> : Is this sparse matrix multiplication (1) or blocked matrix multiplication (0)
