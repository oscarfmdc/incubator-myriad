import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.server.resourcemanager.RMContext;
import org.apache.mesos.MesosSchedulerDriver;
import org.apache.mesos.Protos;
import org.apache.mesos.Protos.CommandInfo;
import org.apache.mesos.Protos.ExecutorInfo;
import org.apache.mesos.Protos.FrameworkInfo;
import org.apache.mesos.Scheduler;
import org.apache.myriad.Main;
import org.apache.myriad.scheduler.yarn.MyriadFairScheduler;
import org.apache.myriad.scheduler.yarn.interceptor.CompositeInterceptor;

public class MainRunner {

    private static final String frameworkName = "mesos-framework-example";
    private static final String executorName = "executor-example-name";
    private static final String remoteExecutorPath = "/tmp/yarn-decoupling.jar";
    private static String command = "export JAVA_HOME=/usr && sudo -E /opt/hadoop/bin/yarn --config /opt/hadoop/etc/hadoop/ resourcemanager";

    private static FrameworkInfo getFrameworkInfo() {
        FrameworkInfo.Builder builder = FrameworkInfo.newBuilder();
        builder.setFailoverTimeout(43200000);
        builder.setUser("");
        builder.setName(frameworkName);
        return builder.build();
    }

    private static CommandInfo.URI getUri() {
        CommandInfo.URI.Builder uriBuilder = CommandInfo.URI.newBuilder();
        uriBuilder.setValue(remoteExecutorPath);
        uriBuilder.setExtract(false);
        return uriBuilder.build();
    }

    private static CommandInfo getCommandInfo() {
        CommandInfo.Builder cmdInfoBuilder = Protos.CommandInfo.newBuilder();
        cmdInfoBuilder.addUris(getUri());
        cmdInfoBuilder.setValue(command);
        cmdInfoBuilder.setShell(true);
        return cmdInfoBuilder.build();
    }

    private static ExecutorInfo getExecutorInfo() {
        ExecutorInfo.Builder builder = ExecutorInfo.newBuilder();
        builder.setExecutorId(Protos.ExecutorID.newBuilder().setValue(executorName));
        builder.setCommand(getCommandInfo());
        builder.setName(executorName);
//        builder.addResources(buildResource("cpus", 0.1))
//                .addResources(buildResource("mem", 128))
//                .addResources(buildResource("disk", 10000));

        return builder.build();
    }

    private static Protos.Resource buildResource(String name, double value) {
        return Protos.Resource.newBuilder()
                .setName(name)
                .setType(Protos.Value.Type.SCALAR)
                .setScalar(buildScalar(value)).build();
    }

    private static Protos.Value.Scalar.Builder buildScalar(double value) {
        return Protos.Value.Scalar.newBuilder().setValue(value);
    }

    private static void runFramework(String mesosMaster) {

        Scheduler scheduler = new ExampleScheduler(getExecutorInfo(), getCommandInfo());
        MesosSchedulerDriver driver = new MesosSchedulerDriver(scheduler, getFrameworkInfo(), mesosMaster);

        int status = driver.run() == Protos.Status.DRIVER_STOPPED ? 0 : 1;

        // Ensure that the driver process terminates.
        driver.stop();

        try {
            Main.initialize(new Configuration(), new MyriadFairScheduler(), null, new CompositeInterceptor());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // For this test to pass reliably on some platforms, this sleep is
        // required to ensure that the SchedulerDriver teardown is complete
        // before the JVM starts running native object destructors after
        // System.exit() is called. 500ms proved successful in test runs,
        // but on a heavily loaded machine it might not.
        // and its associated tasks via the Java API and wait until their
        // teardown is complete to exit.
        // TODO: Inspect the status of the driver
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            System.out.println("I was interrupted!");
            e.printStackTrace();
        }
        System.exit(status);
    }

    public static void main(String[] args) {
        runFramework(args[0]);
    }
}
