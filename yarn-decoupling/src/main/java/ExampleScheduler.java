/*
 * The Scheduler Driver is responsible for managing the scheduler's
 * lifecycle (e.g., start, stop, or wait to finish) and interacting
 * with Mesos Master (e.g., launch tasks, kill tasks, etc.).
 * Note that this interface is usually not implemented by a framework
 * itself, but it describes the possible calls a framework scheduler
 * can make to interact with the Mesos Master. Please note that usage
 * of this interface requires an instantiated MesosSchedulerDiver.
 *
 * - Starts the scheduler driver. This needs to be called before any
 *   other driver calls are made.
 *
 *   virtual Status start();
 *
 * - Stops the scheduler driver. If the 'failover' flag is set to
 *  false then it is expected that this framework will never
 *  reconnect to Mesos. So Mesos will unregister the framework and
 *  shutdown all its tasks and executors. If 'failover' is true, all
 *  executors and tasks will remain running (for some framework
 *  specific failover timeout) allowing the scheduler to reconnect
 *  (possibly in the same process, or from a different process, for
 *  example, on a different machine).
 *
 *  virtual Status stop(bool failover = false);
 *
 * - Aborts the driver so that no more callbacks can be made to the
 *  scheduler. The semantics of abort and stop have deliberately been
 *  separated so that code can detect an aborted driver (i.e., via
 *  the return status of SchedulerDriver::join, see below), and
 *  instantiate and start another driver if desired (from within the
 *  same process). Note that 'stop()' is not automatically called
 *  inside 'abort()'.
 *
 *  virtual Status abort();
 *
 * - Waits for the driver to be stopped or aborted, possibly
 *  _blocking_ the current thread indefinitely. The return status of
 *  this function can be used to determine if the driver was aborted
 *  (see mesos.proto for a description of Status).
 *
 *  virtual Status join();
 *
 * - Starts and immediately joins (i.e., blocks on) the driver.
 *
 *  virtual Status run();
 *
 * - Requests resources from Mesos (see mesos.proto for a description
 *  of Request and how, for example, to request resources from
 *  specific slaves). Any resources available are offered to the
 *  framework via Scheduler::resourceOffers callback, asynchronously.
 *
 *  virtual Status requestResources(const std::vector<Request>& requests);
 *
 * - Launches the given set of tasks. Any remaining resources (i.e.,
 *  those that are not used by the launched tasks or their executors)
 *  will be considered declined. Note that this includes resources
 *  used by tasks that the framework attempted to launch but failed
 *  (with `TASK_ERROR`) due to a malformed task description. The
 *  specified filters are applied on all unused resources (see
 *  mesos.proto for a description of Filters). Available resources
 *  are aggregated when multiple offers are provided. Note that all
 *  offers must belong to the same slave. Invoking this function with
 *  an empty collection of tasks declines offers in their entirety
 *  (see Scheduler::declineOffer).
 *
 *  virtual Status launchTasks(
 *    const std::vector<OfferID>& offerIds,
 *    const std::vector<TaskInfo>& tasks,
 *    const Filters& filters = Filters());
 *
 * - Kills the specified task. Note that attempting to kill a task is
 *  currently not reliable. If, for example, a scheduler fails over
 *  while it was attempting to kill a task it will need to retry in
 *  the future. Likewise, if unregistered / disconnected, the request
 *  will be dropped (these semantics may be changed in the future).
 *
 *  virtual Status killTask(const TaskID& taskId);
 *
 * - Accepts the given offers and performs a sequence of operations on
 *  those accepted offers. See Offer.Operation in mesos.proto for the
 *  set of available operations. Any remaining resources (i.e., those
 *  that are not used by the launched tasks or their executors) will
 *  be considered declined. Note that this includes resources used by
 *  tasks that the framework attempted to launch but failed (with
 *  `TASK_ERROR`) due to a malformed task description. The specified
 *  filters are applied on all unused resources (see mesos.proto for
 *  a description of Filters). Available resources are aggregated
 *  when multiple offers are provided. Note that all offers must
 *  belong to the same slave.
 *
 *  virtual Status acceptOffers(
 *    const std::vector<OfferID>& offerIds,
 *    const std::vector<Offer::Operation>& operations,
 *    const Filters& filters = Filters());
 *
 * - Declines an offer in its entirety and applies the specified
 *   filters on the resources (see mesos.proto for a description of
 *   Filters). Note that this can be done at any time, it is not
 *   necessary to do this within the Scheduler::resourceOffers
 *   callback.
 *
 *  virtual Status declineOffer(
 *    const OfferID& offerId,
 *    const Filters& filters = Filters());
 *
 * - Removes all filters previously set by the framework (via
 *   launchTasks()). This enables the framework to receive offers from
 *   those filtered slaves.
 8
 *  virtual Status reviveOffers();
 *
 * - Inform Mesos master to stop sending offers to the framework. The
 *   scheduler should call reviveOffers() to resume getting offers.
 *
 *  virtual Status suppressOffers();
 *
 * - Acknowledges the status update. This should only be called
 *   once the status update is processed durably by the scheduler.
 *   Not that explicit acknowledgements must be requested via the
 *   constructor argument, otherwise a call to this method will
 *   cause the driver to crash.
 *
 *  virtual Status acknowledgeStatusUpdate(const TaskStatus& status);
 *
 * - Sends a message from the framework to one of its executors. These
 *   messages are best effort; do not expect a framework message to be
 *   retransmitted in any reliable fashion.
 *
 *  virtual Status sendFrameworkMessage(
 *    const ExecutorID& executorId,
 *    const SlaveID& slaveId,
 *    const std::string& data);
 *
 * - Allows the framework to query the status for non-terminal tasks.
 *   This causes the master to send back the latest task status for
 *   each task in 'statuses', if possible. Tasks that are no longer
 *   known will result in a TASK_LOST update. If statuses is empty,
 *   then the master will send the latest status for each task
 *   currently known.
 *
 *  virtual Status reconcileTasks(const std::vector<TaskStatus>& statuses);
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.mesos.Protos;
import org.apache.mesos.Scheduler;
import org.apache.mesos.SchedulerDriver;

public class ExampleScheduler implements Scheduler {

    private int taskId = 0;
    private int resourceManagers = 0;
    private int nodeManagers = 0;
    private String commandInfoRM;
    private Protos.CommandInfo commandInfoNM;
    private String hostnameRM = "";
    private static final String remoteExecutorPath = "/root/hadoop-2.7.7.tar";

    public ExampleScheduler(String commandRM, Protos.CommandInfo commandInfoNM) {
        this.commandInfoRM = commandRM;
        this.commandInfoNM = commandInfoNM;
    }

    private static Protos.CommandInfo.URI getUri() {
        Protos.CommandInfo.URI.Builder uriBuilder = Protos.CommandInfo.URI.newBuilder();
        uriBuilder.setValue(remoteExecutorPath);
//        uriBuilder.setExecutable(true);
        uriBuilder.setExtract(false);
        return uriBuilder.build();
    }

    private static Protos.CommandInfo getCommandInfo(String command) {
        Protos.CommandInfo.Builder cmdInfoBuilder = Protos.CommandInfo.newBuilder();
        cmdInfoBuilder.addUris(getUri());
        cmdInfoBuilder.setValue(command);
        cmdInfoBuilder.setShell(true);
        return cmdInfoBuilder.build();
    }

    /*
     * Invoked when the scheduler successfully registers with a Mesos
     * master. A unique ID (generated by the master) used for
     * distinguishing this framework from others and MasterInfo
     * with the ip and port of the current master are provided as arguments.
     */
    @Override
    public void registered(SchedulerDriver schedulerDriver, Protos.FrameworkID frameworkID,
                           Protos.MasterInfo masterInfo) {
        System.out.println("Registered! ID = " + frameworkID.getValue());
    }

    /*
     * Invoked when the scheduler re-registers with a newly elected Mesos master.
     * This is only called when the scheduler has previously been registered.
     * `MasterInfo` containing the updated information about the elected master
     * is provided as an argument.
     */
    @Override
    public void reregistered(SchedulerDriver schedulerDriver, Protos.MasterInfo masterInfo) {
        System.out.println("Re-registered");
    }

    @Override
    public void resourceOffers(SchedulerDriver schedulerDriver, List<Protos.Offer> offers) {
        for (Protos.Offer offer : offers) {

            hostnameRM = offer.getHostname();

            String yarnConfig = "echo \"<configuration><property><name>yarn.acl.enable</name><value>0</value></property>" +
                    "<property><name>yarn.resourcemanager.hostname</name><value>" + hostnameRM + "</value></property></configuration>\" " +
                    "> /opt/hadoop/etc/hadoop/yarn-site.xml && ";

            if (resourceManagers < 1) {
                System.out.println("Starting RM");
                Protos.TaskID taskId = buildNewTaskID();
                Protos.TaskInfo task = Protos.TaskInfo.newBuilder()
                        .setName("task " + taskId).setTaskId(taskId)
                        .setSlaveId(offer.getSlaveId())
                        .addResources(buildResource("cpus", 1))
                        .addResources(buildResource("mem", 2048))
                        .addResources(buildResource("disk", 10000))
                        .setCommand(Protos.CommandInfo.newBuilder(getCommandInfo(yarnConfig + commandInfoRM)))
//                        .setExecutor(Protos.ExecutorInfo.newBuilder(executorInfo))
                        .build();

                resourceManagers++;
                launchTask(schedulerDriver, offer, task);

            }
            else if (nodeManagers < 2 && !hostnameRM.isEmpty()) {
                System.out.println("Starting NM");
                Protos.TaskID taskId = buildNewTaskID();
                Protos.TaskInfo task = Protos.TaskInfo.newBuilder()
                        .setName("task " + taskId).setTaskId(taskId)
                        .setSlaveId(offer.getSlaveId())
                        .addResources(buildResource("cpus", 1))
                        .addResources(buildResource("mem", 1024))
                        .addResources(buildResource("disk", 5000))
                        .setCommand(Protos.CommandInfo.newBuilder(commandInfoNM))
                        .build();

                nodeManagers++;
                launchTask(schedulerDriver, offer, task);
            }
        }
    }

    private void launchTask(SchedulerDriver schedulerDriver, Protos.Offer offer, Protos.TaskInfo task) {
        Collection<Protos.TaskInfo> tasks = new ArrayList<>();
        Collection<Protos.OfferID> offerIDs = new ArrayList<>();
        tasks.add(task);
        offerIDs.add(offer.getId());
        schedulerDriver.launchTasks(offerIDs, tasks);
    }

    private Protos.TaskID buildNewTaskID() {
        return Protos.TaskID.newBuilder()
                .setValue(Integer.toString(taskId++))
                .build();
    }

    private Protos.Resource buildResource(String name, double value) {
        return Protos.Resource.newBuilder()
                .setName(name)
                .setType(Protos.Value.Type.SCALAR)
                .setScalar(buildScalar(value)).build();
    }

    private Protos.Value.Scalar.Builder buildScalar(double value) {
        return Protos.Value.Scalar.newBuilder().setValue(value);
    }

    /*
     * Invoked when an offer is no longer valid (e.g., the slave was
     * lost or another framework used resources in the offer). If for
     * whatever reason an offer is never rescinded (e.g., dropped
     * message, failing over framework, etc.), a framework that attempts
     * to launch tasks using an invalid offer will receive TASK_LOST
     * status updates for those tasks (see Scheduler::resourceOffers).
     */
    @Override
    public void offerRescinded(SchedulerDriver schedulerDriver, Protos.OfferID offerID) {
        System.out.println("This offer's been rescinded");
    }

    /*
     * Invoked when the status of a task has changed (e.g., a slave is
     * lost and so the task is lost, a task finishes and an executor
     * sends a status update saying so, etc). If implicit
     * acknowledgements are being used, then returning from this
     * callback _acknowledges_ receipt of this status update! If for
     * whatever reason the scheduler aborts during this callback (or
     * the process exits) another status update will be delivered (note,
     * however, that this is currently not true if the slave sending the
     * status update is lost/fails during that time). If explicit
     * acknowledgements are in use, the scheduler must acknowledge this
     * status on the driver.
     */
    @Override
    public void statusUpdate(SchedulerDriver schedulerDriver, Protos.TaskStatus taskStatus) {

//        System.out.println("Status update: task " + taskStatus.getTaskId().getValue() +
//                " is in state " + taskStatus.getState().getValueDescriptor().getName());

        if (taskStatus.getState() == Protos.TaskState.TASK_LOST ||
                taskStatus.getState() == Protos.TaskState.TASK_KILLED ||
                taskStatus.getState() == Protos.TaskState.TASK_FAILED) {

//            System.err.println("Aborting because task " + taskStatus.getTaskId().getValue() +
//                    " is in unexpected state " +
//                    taskStatus.getState().getValueDescriptor().getName() +
//                    " with reason '" +
//                    taskStatus.getReason().getValueDescriptor().getName() + "'" +
//                    " from source '" +
//                    taskStatus.getSource().getValueDescriptor().getName() + "'" +
//                    " with message '" + taskStatus.getMessage() + "'");

            System.out.println("Aborting... " + taskStatus.getState().toString() + " " + taskStatus.getReason().name());

            schedulerDriver.abort();
        }
    }

    /*
     * Invoked when an executor sends a message. These messages are best
     * effort; do not expect a framework message to be retransmitted in
     * any reliable fashion.
     */
    @Override
    public void frameworkMessage(SchedulerDriver schedulerDriver,
                                 Protos.ExecutorID executorID, Protos.SlaveID slaveID, byte[] bytes) {
        System.out.println("Received message (scheduler): " + new String(bytes)
                + " from " + executorID.getValue());
    }

    /*
     * Invoked when the scheduler becomes "disconnected" from the master
     * (e.g., the master fails and another is taking over).
     */
    @Override
    public void disconnected(SchedulerDriver schedulerDriver) {
        System.out.println("Scheduler disconnected from the master");
    }

    /*
     * Invoked when a slave has been determined unreachable (e.g.,
     * machine failure, network partition). Most frameworks will need to
     * reschedule any tasks launched on this slave on a new slave.
     *
     * NOTE: This callback is not reliably delivered. If a host or
     * network failure causes messages between the master and the
     * scheduler to be dropped, this callback may not be invoked.
     */
    @Override
    public void slaveLost(SchedulerDriver schedulerDriver, Protos.SlaveID slaveID) {
        System.out.println("Lost slave: " + slaveID);
    }

    /*
     * Invoked when an executor has exited/terminated. Note that any
     * tasks running will have TASK_LOST status updates automagically
     * generated.
     *
     * NOTE: This callback is not reliably delivered. If a host or
     * network failure causes messages between the master and the
     * scheduler to be dropped, this callback may not be invoked.
     */
    @Override
    public void executorLost(SchedulerDriver schedulerDriver,
                             Protos.ExecutorID executorID, Protos.SlaveID slaveID, int i) {
        System.out.println("Lost executor on slave " + slaveID);
    }

    /* Invoked when there is an unrecoverable error in the scheduler or
     * scheduler driver. The driver will be aborted BEFORE invoking this
     * callback.
     */
    @Override
    public void error(SchedulerDriver schedulerDriver, String s) {
        System.out.println("We've got errors: " + s);
    }
}
