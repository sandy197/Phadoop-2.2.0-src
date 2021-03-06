run() {
                        echo "\$ ${@}"
                        "${@}"
                        res=$?
                        if [ $res != 0 ]; then
                          echo
                          echo "Failed!"
                          echo
                          exit $res
                        fi
                      }

                      ROOT=`cd ../..;pwd`
                      echo
                      echo "Current directory `pwd`"
                      echo
                      run rm -rf hadoop-2.2.0
                      run mkdir hadoop-2.2.0
                      run cd hadoop-2.2.0
                      run cp -r $ROOT/hadoop-common-project/hadoop-common/target/hadoop-common-2.2.0/* .
                      run cp -r $ROOT/hadoop-common-project/hadoop-nfs/target/hadoop-nfs-2.2.0/* .
                      run cp -r $ROOT/hadoop-hdfs-project/hadoop-hdfs/target/hadoop-hdfs-2.2.0/* .
                      run cp -r $ROOT/hadoop-hdfs-project/hadoop-hdfs-httpfs/target/hadoop-hdfs-httpfs-2.2.0/* .
                      run cp -r $ROOT/hadoop-hdfs-project/hadoop-hdfs-nfs/target/hadoop-hdfs-nfs-2.2.0/* .
                      run cp -r $ROOT/hadoop-yarn-project/target/hadoop-yarn-project-2.2.0/* .
                      run cp -r $ROOT/hadoop-mapreduce-project/target/hadoop-mapreduce-2.2.0/* .
                      run cp -r $ROOT/hadoop-tools/hadoop-tools-dist/target/hadoop-tools-dist-2.2.0/* .
                      echo
                      echo "Hadoop dist layout available at: /home/srkandul/scndYr/hadoop/hadoop-2.2.0-src/hadoop-dist/target/hadoop-2.2.0"
                      echo