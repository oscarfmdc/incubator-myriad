# Apache Mesos Framework Example

This is an example for a simple Apache Mesos Framework

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes.

### Prerequisites

This code is ready to run in the vagrant environment setup that can be found here:
https://github.com/apache/incubator-myriad/tree/master/vagrant/libvirt/mesos

### Installing

```

./gradlew clean :yarn-decoupling:build -x checkstyleMain -x findbugsMain 

sshpass -p 'vagrant' scp yarn-decoupling/run-framework-myriad.sh vagrant@192.168.121.84:/home/vagrant

sshpass -p 'vagrant' scp yarn-decoupling/run-executor-myriad.cmd vagrant@192.168.121.84:/home/vagrant

for i in 84 131 167 150 208; do sshpass -p 'vagrant' scp yarn-decoupling/build/libs/yarn-decoupling.jar vagrant@192.168.121.$i:/tmp; done

vagrant ssh mesos-m1

sh run-framework-myriad.sh mesos-m1 run-executor-myriad.cmd
```

```

./gradlew clean :yarn-decoupling:build -x checkstyleMain -x findbugsMain 

scp yarn-decoupling/run-framework-myriad.sh root@fedora-mesos-m0.node.keedio.cloud:/root
scp yarn-decoupling/build/libs/yarn-decoupling.jar root@fedora-mesos-m0.node.keedio.cloud:/tmp
scp yarn-decoupling/src/main/resources/myriad.properties root@fedora-mesos-m0.node.keedio.cloud:/root

ssh fedora-mesos-m0.node.keedio.cloud

sh run-framework-myriad.sh fedora-mesos-m0.node.keedio.cloud myriad.properties
```

### Testing

```
export JAVA_HOME=/usr

./hdfs namenode -format
./hdfs namenode
./hdfs datanode

./yarn jar ../../hadoop-mapreduce-examples-2.7.7.jar pi 16 1000
```

Apache Mesos UI: http://100.0.10.101:5050
Yarn UI: http://100.0.10.10x:8088

## Built With

* [Gradle](https://gradle.org/) - Dependency Management

## Acknowledgments

* https://github.com/opencredo/mesos_framework_demo
