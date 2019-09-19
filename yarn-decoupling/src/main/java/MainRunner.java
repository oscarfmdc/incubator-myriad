import org.apache.mesos.MesosSchedulerDriver;
import org.apache.mesos.Protos;
import org.apache.mesos.Protos.CommandInfo;
import org.apache.mesos.Protos.FrameworkInfo;
import org.apache.mesos.Scheduler;

public class MainRunner {

    private static final String frameworkName = "myriad-yarn-decoupling";
    private static final String remoteExecutorPath = "/root/hadoop-2.7.7.tar";

    private static String commandRM = "ls -la ./root/hadoop-2.7.7/";
    private static String commandNM = "ls -la ./root/hadoop-2.7.7/";

//    private static String commandRM = "export JAVA_HOME=/usr && sudo -E ./root/hadoop-2.7.7/bin/yarn --config ./root/hadoop-2.7.7/etc/hadoop/ resourcemanager";
//    private static String commandNM = "export JAVA_HOME=/usr && sudo -E ./root/hadoop-2.7.7/bin/yarn --config ./root/hadoop-2.7.7/etc/hadoop/ nodemanager";

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
//        uriBuilder.setExecutable(true);
        uriBuilder.setExtract(false);
        return uriBuilder.build();
    }

    private static CommandInfo getCommandInfo(String command) {
        CommandInfo.Builder cmdInfoBuilder = Protos.CommandInfo.newBuilder();
        cmdInfoBuilder.addUris(getUri());
        cmdInfoBuilder.setValue(command);
        cmdInfoBuilder.setShell(true);
        return cmdInfoBuilder.build();
    }

    private static void runFramework(String mesosMaster) {

        Scheduler scheduler = new ExampleScheduler(commandRM, getCommandInfo(commandNM));
        MesosSchedulerDriver driver = new MesosSchedulerDriver(scheduler, getFrameworkInfo(), mesosMaster);

        int status = driver.run() == Protos.Status.DRIVER_STOPPED ? 0 : 1;

        // Ensure that the driver process terminates.
        driver.stop();

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
