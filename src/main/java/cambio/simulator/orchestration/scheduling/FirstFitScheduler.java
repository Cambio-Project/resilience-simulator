package cambio.simulator.orchestration.scheduling;

import cambio.simulator.entities.NamedEntity;
import cambio.simulator.orchestration.environment.*;
import cambio.simulator.orchestration.management.ManagementPlane;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;

public class FirstFitScheduler extends Scheduler {

    private static final FirstFitScheduler instance = new FirstFitScheduler();

    //private constructor to avoid client applications to use constructor
    private FirstFitScheduler() {
        this.rename("FirstFitScheduler");
    }

    public static FirstFitScheduler getInstance() {
        return instance;
    }

    @Override
    public void schedulePods() {
        if (podWaitingQueue.isEmpty()) {
            getModel().sendTraceNote(this.getQuotedName() + " 's Waiting Queue is empty.");
            return;
        }
        int i = 0;
        final int podWaitingQueueInitSize = podWaitingQueue.size();
        while (i < podWaitingQueueInitSize) {
            i++;
            schedulePod();
        }
    }

    public boolean schedulePod() {

        final Pod pod = getNextPodFromWaitingQueue();

        if (pod != null) {
            Node candidateNote = null;
            int cpuDemand = pod.getCPUDemand();
            for (Node node : cluster.getNodes()) {
                if (node.getReserved() + cpuDemand <= node.getTotalCPU()) {
                    candidateNote = node;
                    break;
                }
            }
            if (candidateNote != null) {
                candidateNote.addPod(pod);
                sendTraceNote(this.getQuotedName() + " has scheduled " + pod.getQuotedName() + " on node " + candidateNote);
                return true;
            } else {
                podWaitingQueue.add(pod);
                sendTraceNote(this.getQuotedName() + " was not able to schedule pod " + pod + ". Insufficient resources!");
                sendTraceNote(this.getQuotedName() + " has send " + pod + " back to the Pod Waiting Queue");
                return false;
            }
        }
        sendTraceNote(this.getQuotedName() + " has no pods left for scheduling");
        return false;
    }

    @Override
    public SchedulerType getSchedulerType() {
        return SchedulerType.FIRSTFIT;
    }


}