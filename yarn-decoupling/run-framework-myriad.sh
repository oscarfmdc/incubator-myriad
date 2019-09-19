MESOS_SOURCE_DIR=/opt/mesos
MESOS_BUILD_DIR=${MESOS_SOURCE_DIR}/build
PROTOBUF_JAR=${MESOS_BUILD_DIR}/src/java/target/protobuf-java-3.5.0.jar
JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk-1.8.0.212.b04-0.el7_6.x86_64/
JAVA=${JAVA-${JAVA_HOME}/bin/java}
MESOS_JAR=${MESOS_BUILD_DIR}/src/java/target/mesos-1.8.0.jar
EXAMPLES_JAR=/tmp/yarn-decoupling.jar

echo $JAVA

exec ${JAVA} -cp ${PROTOBUF_JAR}:${MESOS_JAR}:${EXAMPLES_JAR} \
    -Djava.library.path=${MESOS_BUILD_DIR}/src/.libs \
    MainRunner \
    zk://${1}:2181/mesos $2