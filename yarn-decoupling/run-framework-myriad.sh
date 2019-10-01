MESOS_SOURCE_DIR=/opt/mesos
MESOS_BUILD_DIR=${MESOS_SOURCE_DIR}/build
PROTOBUF_JAR=${MESOS_BUILD_DIR}/src/java/target/protobuf-java-3.5.0.jar
JAVA_HOME=/usr/lib/jvm/java-1.8.0-openjdk-1.8.0.222.b10-0.fc30.x86_64/jre
JAVA=${JAVA-${JAVA_HOME}/bin/java}
MESOS_JAR=${MESOS_BUILD_DIR}/src/java/target/mesos-1.8.0.jar
EXAMPLES_JAR=/tmp/yarn-decoupling.jar

echo $JAVA

exec ${JAVA} -cp ${EXAMPLES_JAR} \
    MyriadDriverD \
    "${1}":5050 \
    "${2}"
    #zk://${1}:2181/mesos