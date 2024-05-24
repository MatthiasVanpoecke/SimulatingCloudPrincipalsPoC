package org.example.SimulatingCloudPrincipalsPoc.testScenarions.scenario4;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.cloudsimplus.brokers.DatacenterBroker;
import org.cloudsimplus.brokers.DatacenterBrokerSimple;
import org.cloudsimplus.builders.tables.CloudletsTableBuilder;
import org.cloudsimplus.builders.tables.MarkdownTable;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.cloudlets.CloudletSimple;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.datacenters.Datacenter;
import org.cloudsimplus.datacenters.DatacenterCharacteristicsSimple;
import org.cloudsimplus.datacenters.DatacenterSimple;
import org.cloudsimplus.distributions.ContinuousDistribution;
import org.cloudsimplus.distributions.UniformDistr;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.hosts.HostSimple;
import org.cloudsimplus.listeners.EventInfo;
import org.cloudsimplus.provisioners.ResourceProvisionerSimple;
import org.cloudsimplus.resources.Pe;
import org.cloudsimplus.resources.PeSimple;
import org.cloudsimplus.schedulers.cloudlet.CloudletSchedulerTimeShared;
import org.cloudsimplus.schedulers.vm.VmSchedulerTimeShared;
import org.cloudsimplus.utilizationmodels.UtilizationModelDynamic;
import org.cloudsimplus.utilizationmodels.UtilizationModelFull;
import org.cloudsimplus.vms.Vm;
import org.cloudsimplus.vms.VmCost;
import org.cloudsimplus.vms.VmSimple;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static java.util.Comparator.comparingDouble;



public class HorizontalScaling {
    private final CloudSimPlus simulation;
    private final Datacenter dc0;
    private final DatacenterBroker broker0;
    private final List<Host> hostList;
    private final List<Vm> vmList;
    private final List<Cloudlet> cloudletList;

    private final SimulationConfig config;
    private final ContinuousDistribution rand;

    private int createdCloudlets;
    private int createdVms;


    public static final SimulationConfig defaultConfig = SimulationConfig.builder()
            .schedulingInterval(5)
            .cloudletsCreationInterval(10)
            .hosts(50)
            .hostPes(32)
            .initialVms(1)
            .initialCloudlets(6)
            .maxVms(5)
            .vmDestructionDelay(10.0)
            .vmOverloadCpuPercentage(0.7)
            .cloudletLengths(new long[]{1000, 2000, 3000})
            .build();

    public static void main(String[] args) {

        var simulation = new HorizontalScaling(defaultConfig);
        simulation.simulate();

    }

    public HorizontalScaling(SimulationConfig config) {
        this.config = config;
        rand = new UniformDistr(0, config.getCloudletLengths().length, 1);
        hostList = new ArrayList<>(config.getHosts());
        vmList = new ArrayList<>(config.getInitialVms());
        cloudletList = new ArrayList<>(config.getInitialCloudlets());

        simulation = new CloudSimPlus(config.getVmOverloadCpuPercentage());
        //simulation.terminateAt(300);
        simulation.addOnClockTickListener(this::createNewCloudlets);

        dc0 = createDatacenter();
        broker0 = new DatacenterBrokerSimple(simulation);
        broker0.setVmDestructionDelay(config.getVmDestructionDelay());

        vmList.addAll(createListOfScalableVms(config.getInitialVms()));

        createCloudletList();
        broker0.submitVmList(vmList);
        broker0.submitCloudletList(cloudletList);
    }

    public void simulate() {
        simulate(System.out);
    }

    public void simulate (PrintStream out) {
        simulation.start();
        getSimulationResults(out);
        printTotalVmsCost(out);
        printSimulationDuration(out);
    }

    private void getSimulationResults(PrintStream out) {
        final var cloudletFinishedList = broker0.getCloudletFinishedList();
        final Comparator<Cloudlet> sortByVmId = comparingDouble(c -> c.getVm().getId());
        final Comparator<Cloudlet> sortByStartTime = comparingDouble(Cloudlet::getStartTime);
        cloudletFinishedList.sort(sortByVmId.thenComparing(sortByStartTime));
        MarkdownTable table = new MarkdownTable();
        table.setPrintStream(out);
        new CloudletsTableBuilder(cloudletFinishedList,table).build();
        out.print("\n\n");
    }

    private void createCloudletList() {
        for (int i = 0; i < config.getInitialCloudlets(); i++) {
            cloudletList.add(createCloudlet());
        }
    }

    private void createNewCloudlets(final EventInfo info) {
        final long time = (long) info.getTime();
        if (time % config.getCloudletsCreationInterval() == 0 && time <= 50) {
            final int cloudletsNumber = 4;
            System.out.printf("\t#Creating %d Cloudlets at time %d.%n", cloudletsNumber, time);
            final List<Cloudlet> newCloudlets = new ArrayList<>(cloudletsNumber);
            for (int i = 0; i < cloudletsNumber; i++) {
                final var cloudlet = createCloudlet();
                cloudletList.add(cloudlet);
                newCloudlets.add(cloudlet);
            }

            broker0.submitCloudletList(newCloudlets);
        }
    }

    private Datacenter createDatacenter() {
        for (int i = 0; i < config.getHosts(); i++) {
            hostList.add(createHost());
        }

        return new DatacenterSimple(simulation, hostList)
                .setSchedulingInterval(config.getSchedulingInterval())
                .setCharacteristics(new DatacenterCharacteristicsSimple(1, 0, 0));
    }

    private Host createHost() {
        final var peList = new ArrayList<Pe>(config.getHostPes());
        for (int i = 0; i < config.getHostPes(); i++) {
            peList.add(new PeSimple(1000));
        }

        final long ram = 2048;
        final long storage = 1000000;
        final long bw = 10000;
        return new HostSimple(ram, bw, storage, peList)
                .setRamProvisioner(new ResourceProvisionerSimple())
                .setBwProvisioner(new ResourceProvisionerSimple())
                .setVmScheduler(new VmSchedulerTimeShared());
    }

    private List<Vm> createListOfScalableVms(final int vmsNumber) {
        final var newVmList = new ArrayList<Vm>(vmsNumber);
        for (int i = 0; i < vmsNumber; i++) {
            final Vm vm = createVm();
            createHorizontalVmScaling(vm);
            newVmList.add(vm);
        }

        return newVmList;
    }

    private void createHorizontalVmScaling(final Vm vm) {
        final var horizontalScaling = new HorizontalScalingWithMaxVms()
                .setMaxVms(config.getMaxVms())
                .setOverloadPredicate(this::isVmOverloaded)
                .setVmSupplier(this::createVm);
        vm.setHorizontalScaling(horizontalScaling);
    }

    private boolean isVmOverloaded(final Vm vm) {
        return vm.getCpuPercentUtilization() > config.getVmOverloadCpuPercentage();
    }

    private Vm createVm() {
        final int id = createdVms++;
        return new VmSimple(id, 1000, 2)
                .setRam(512).setBw(1000).setSize(10000)
                .setCloudletScheduler(new CloudletSchedulerTimeShared());
    }

    private Cloudlet createCloudlet() {
        final int id = createdCloudlets++;
        final var utilizadionModelDynamic = new UtilizationModelDynamic(0.1);

        final long length = config.getCloudletLengths()[(int) rand.sample()];
        return new CloudletSimple(id, length, 2)
                .setFileSize(1024)
                .setOutputSize(1024)
                .setUtilizationModelBw(utilizadionModelDynamic)
                .setUtilizationModelRam(utilizadionModelDynamic)
                .setUtilizationModelCpu(new UtilizationModelFull());
    }

    private void printTotalVmsCost(PrintStream out) {
        out.println();
        double totalCost = 0.0;
        int totalNonIdleVms = 0;
        double processingTotalCost = 0, memoryTotaCost = 0, storageTotalCost = 0, bwTotalCost = 0;
        for (final Vm vm : broker0.getVmCreatedList()) {
            final var cost = new VmCost(vm);
            processingTotalCost += cost.getProcessingCost();
            memoryTotaCost += cost.getMemoryCost();
            storageTotalCost += cost.getStorageCost();
            bwTotalCost += cost.getBwCost();

            totalCost += cost.getTotalCost();
            totalNonIdleVms += vm.getTotalExecutionTime() > 0 ? 1 : 0;
            out.println(cost);
            out.print("\n");
        }

        out.printf(
                "Total cost ($) for %3d created VMs from %3d in DC %d: %8.2f$ %13.2f$ %17.2f$ %12.2f$ %15.2f$%n",
                totalNonIdleVms, broker0.getVmsNumber(), dc0.getId(),
                processingTotalCost, memoryTotaCost, storageTotalCost, bwTotalCost, totalCost);
    }

    private void printSimulationDuration(PrintStream out) {
        out.printf("%nSimulation duration: %.2f seconds%n", simulation.clock());
    }
}


@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
class SimulationConfig {
    private int schedulingInterval;
    private int cloudletsCreationInterval;
    private int hosts;
    private int hostPes;
    private int initialVms;
    private int initialCloudlets;
    private int maxVms;
    private double vmDestructionDelay;
    private double vmOverloadCpuPercentage;
    private long[] cloudletLengths;
}