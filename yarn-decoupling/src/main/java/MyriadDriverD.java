import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.mesos.MesosSchedulerDriver;
import org.apache.mesos.Protos;
import org.apache.mesos.Protos.FrameworkInfo;
import org.apache.mesos.Scheduler;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Properties;

public class MyriadDriverD {

    private static final String frameworkName = "apache-myriad";
    private static int frameworkFailoverTimeout;
    private static String driverHostname;

    private static FrameworkInfo getFrameworkInfo() {
        FrameworkInfo.Builder builder = FrameworkInfo.newBuilder();
        builder.setFailoverTimeout(frameworkFailoverTimeout);
        builder.setUser("");
        builder.setName(frameworkName);
        return builder.build();
    }

    public static class MyThread extends Thread {

        public void run(){
            try {
                HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
                server.createContext("/", new HHandler());
                System.out.println("setting up server");
                server.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    static class HHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {

            OutputStream os = t.getResponseBody();

            try {
                File newFile = new File("/root/hadoop-2.7.7.tar");

                t.sendResponseHeaders(200, 0);
                FileInputStream fs = new FileInputStream(newFile);
                final byte[] buffer = new byte[0x10000];
                int count;
                while ((count = fs.read(buffer)) >= 0) {
                    os.write(buffer,0,count);
                }
                fs.close();
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            os.close();
        }
    }

    private static void runFramework(String mesosMaster, String configPath) {

        try {
            driverHostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        MyThread myThread = new MyThread();
        myThread.start();

        Properties prop = null;

        try {
            InputStream input = new FileInputStream(configPath);
            prop = new Properties();
            prop.load(input);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        frameworkFailoverTimeout = Integer.parseInt((String) prop.getOrDefault("framework_failover_timeout", 43200000));

        String remoteExecutorPath = "http://" + driverHostname + ":8000/hadoop-2.7.7.tar";

        String commandNM = "export JAVA_HOME=/usr && sudo -E ./hadoop-2.7.7/bin/yarn --config ./hadoop-2.7.7/etc/hadoop/ nodemanager";
        String commandRM = "export JAVA_HOME=/usr && sudo -E ./hadoop-2.7.7/bin/yarn --config ./hadoop-2.7.7/etc/hadoop/ resourcemanager";
        Scheduler scheduler = new MyriadSchedulerD(commandRM, commandNM, remoteExecutorPath, prop);
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
        runFramework(args[0], args[1]);
    }
}
