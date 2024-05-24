package org.example.SimulatingCloudPrincipalsPoc.testScenarions.scenario4;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.cloudsimplus.autoscaling.HorizontalVmScaling;
import org.cloudsimplus.autoscaling.VmScalingAbstract;
import org.cloudsimplus.brokers.DatacenterBroker;
import org.cloudsimplus.listeners.VmHostEventInfo;
import org.cloudsimplus.vms.Vm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * <p>
 * A {@link HorizontalVmScaling} implementation that allows defining the condition
 * to identify an overloaded VM, based on any desired criteria, such as
 * current RAM, CPU and/or Bandwidth utilization.
 * A {@link DatacenterBroker} monitors the VMs that have
 * an HorizontalVmScaling object in order to create or destroy VMs on demand.
 * </p>
 *
 * <br>
 * <p>The overload condition has to be defined
 * by providing a {@link Predicate} using the {@link #setOverloadPredicate(Predicate)} method.
 * Check the {@link HorizontalVmScaling} documentation for details on how to enable
 * horizontal down scaling using the {@link DatacenterBroker}.
 * </p>
 *
 * @author Manoel Campos da Silva Filho
 * @since CloudSim Plus 1.0
 * @see HorizontalVmScaling
 */
@Accessors(chain = true)
public class HorizontalScalingWithMaxVms extends VmScalingAbstract implements HorizontalVmScaling {
    private static final Logger LOGGER = LoggerFactory.getLogger(org.cloudsimplus.autoscaling.HorizontalVmScalingSimple.class.getSimpleName());

    @Getter
    @Setter
    @NonNull
    private Supplier<Vm> vmSupplier;

    /**
     * The last number of cloudlet creation requests
     * received by the broker. This is not related to the VM,
     * but the overall Cloudlets creation requests.
     */
    private long cloudletCreationRequests;

    @Getter @Setter
    private Predicate<Vm> overloadPredicate;

    @Getter @Setter
    private int maxVms;

    public HorizontalScalingWithMaxVms() {
        super();
        this.overloadPredicate = FALSE_PREDICATE;
        this.vmSupplier = () -> Vm.NULL;
    }

    @Override
    protected boolean requestUpScaling(final double time) {
        if(!haveNewCloudletsArrived()){
            return false;
        }

        final double vmCpuUsagePercent = getVm().getCpuPercentUtilization() * 100;
        final Vm newVm = getVmSupplier().get();
        final String timeStr = "%.2f".formatted(time);
        LOGGER.info(
                "{}: {}{}: Requesting creation of {} to receive new Cloudlets in order to balance load of {}. Vm with id {} CPU usage is {}%",
                timeStr, getClass().getSimpleName(), getVm(), newVm, getVm(), getVm().getId(), vmCpuUsagePercent);
        getVm().getBroker().submitVm(newVm);

        cloudletCreationRequests = getVm().getBroker().getCloudletCreatedList().size();
        return true;
    }

    /**
     * {@return true or false} to indicate if new Cloudlets were submitted
     * to the broker since the last time this method was called.
     */
    private boolean haveNewCloudletsArrived(){
        return getVm().getBroker().getCloudletCreatedList().size() > cloudletCreationRequests;
    }

    @Override
    public final boolean requestUpScalingIfPredicateMatches(final VmHostEventInfo evt) {
        if (isTimeToCheckPredicate(evt.getTime())) {
            setLastProcessingTime(evt.getTime());
            boolean is_overloaded = overloadPredicate.test(getVm()) ;
            if (is_overloaded) {
                //check if the number of VMs is less than the maximum number of VMs allowed
                //stream filter out destroyed VMs
                if (getVm().getBroker().getVmCreatedList().stream().filter(vm -> !vm.isInMigration()).count() < maxVms) {
                    return requestUpScaling(evt.getTime());
                } else {
                    final String timeStr = "%.2f".formatted(evt.getTime());
                    LOGGER.info(
                            "{}: {}{}: Vm creation request ignored. Maximum number of VMs reached. {} VMs already created.",
                            timeStr, getClass().getSimpleName(), getVm(), maxVms);
                    return false;
                }
                //if the number of VMs is equal to the maximum number of VMs, then do not create a new VM
            }
            return false;
        }
        return false;
    }
}
