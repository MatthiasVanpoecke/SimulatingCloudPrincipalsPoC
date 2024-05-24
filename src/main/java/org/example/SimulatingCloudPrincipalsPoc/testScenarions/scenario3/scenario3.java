/*
 * CloudSim Plus: A modern, highly-extensible and easier-to-use Framework for
 * Modeling and Simulation of Cloud Computing Infrastructures and Services.
 * http://cloudsimplus.org
 *
 *     Copyright (C) 2015-2021 Universidade da Beira Interior (UBI, Portugal) and
 *     the Instituto Federal de Educação Ciência e Tecnologia do Tocantins (IFTO, Brazil).
 *
 *     This file is part of CloudSim Plus.
 *
 *     CloudSim Plus is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     CloudSim Plus is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with CloudSim Plus. If not, see <http://www.gnu.org/licenses/>.
 */
package org.example.SimulatingCloudPrincipalsPoc.testScenarions.scenario3;

import org.cloudsimplus.brokers.DatacenterBroker;
import org.cloudsimplus.brokers.DatacenterBrokerSimple;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.datacenters.Datacenter;
import org.cloudsimplus.datacenters.DatacenterSimple;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.hosts.HostSimple;
import org.cloudsimplus.resources.Pe;
import org.cloudsimplus.resources.PeSimple;
import org.cloudsimplus.utilizationmodels.UtilizationModelDynamic;
import org.cloudsimplus.vms.Vm;
import org.cloudsimplus.vms.VmSimple;

import java.util.ArrayList;
import java.util.List;

/**
 * Scenario 2 based on BasicFirstExample <a href="https://github.com/cloudsimplus/cloudsimplus-examples/blob/0ecb9a8716f413a16e8a6e406fd50b18878816d0/src/main/java/org/cloudsimplus/examples/BasicFirstExample.java">...</a>
 */
public class scenario3 {
    private static final int  HOSTS = 2;
    private static final int  HOST_PES = 9;
    private static final int  HOST_MIPS = 1000; // Milion Instructions per Second (MIPS)
    private static final int  HOST_RAM = 2048; //in Megabytes
    private static final long HOST_BW = 10_000; //in Megabits/s
    private static final long HOST_STORAGE = 1_000_000; //in Megabytes

    private static final int VMS = 4;
    private static final int VM_PES = 4;

    private static final int USERS = 3;
    //should be a factor of USERS
    private static final int CLOUDLETS = 6;
    private static final int CLOUDLET_PES = 2;
    private static final int CLOUDLET_LENGTH = 10_000; // Milion Instructions (MI)

    private final CloudSimPlus simulation;
    private final DatacenterBroker broker0;
    private List<Vm> vmList;
    private List<Cloudlet> cloudletList;
    private Datacenter datacenter0;

    public static void main(String[] args) {
        new scenario3();
    }

    private scenario3() {
        simulation = new CloudSimPlus();
        //maak een datacenter aan in deze functie worden ook de hosts aangemaakt
        datacenter0 = createDatacenter();
        datacenter0.setSchedulingInterval(10);

        //Creates a broker that is a software acting on behalf of a cloud customer to manage his/her VMs and Cloudlets
        //Maak een broker aan deze stel software voor die namens een cloudklant optreedt om zijn / haar VM's en Cloudlets te beheren
        broker0 = new DatacenterBrokerSimple(simulation);

        broker0.setVmDestructionDelay(10);

        //maak een lijst van VM's aan
        vmList = createVms();
        //maak een lijst van Cloudlets aan
        cloudletList = createCloudlets();
        //voeg de VM's en Cloudlets toe aan de broker
        broker0.submitVmList(vmList);
        broker0.submitCloudletList(cloudletList);

        //start de simulatie
        simulation.start();

        final var cloudletFinishedList = broker0.getCloudletFinishedList();
        new UserCloudletTableBuilder(cloudletFinishedList).build();
    }

    /**
     * Creates a Datacenter and its Hosts.
     */
    private Datacenter createDatacenter() {
        final var hostList = new ArrayList<Host>(HOSTS);
        for(int i = 0; i < HOSTS; i++) {
            final var host = createHost();
            hostList.add(host);
        }

        //Uses a VmAllocationPolicySimple by default to allocate VMs
        return new DatacenterSimple(simulation, hostList);
    }

    private Host createHost() {
        final var peList = new ArrayList<Pe>(HOST_PES);
        //List of Host's CPUs (Processing Elements, PEs)
        for (int i = 0; i < HOST_PES; i++) {
            //Uses a PeProvisionerSimple by default to provision PEs for VMs
            peList.add(new PeSimple(HOST_MIPS));
        }

        /*
        Uses ResourceProvisionerSimple by default for RAM and BW provisioning
        and VmSchedulerSpaceShared for VM scheduling.
        */
        return new HostSimple(HOST_RAM, HOST_BW, HOST_STORAGE, peList);
    }

    /**
     * Creates a list of VMs.
     */
    private List<Vm> createVms() {
        final var vmList = new ArrayList<Vm>(VMS);
        for (int i = 0; i < VMS; i++) {
            //Uses a CloudletSchedulerTimeShared by default to schedule Cloudlets
            final var vm = new VmSimple(HOST_MIPS, VM_PES);
            vm.setRam(512).setBw(1000).setSize(10_000);
            vmList.add(vm);
        }

        return vmList;
    }

    /**
     * Creates a list of Cloudlets.
     */
    private List<Cloudlet> createCloudlets() {
        final var cloudletList = new ArrayList<Cloudlet>(CLOUDLETS);

        //UtilizationModel defining the Cloudlets use only 50% of any resource all the time
        final var utilizationModel = new UtilizationModelDynamic(0.5);

        for (int x = 0; x < USERS; x++){
            for (int i = 0; i < CLOUDLETS/USERS; i++) {
                final var cloudlet = new UserCloudlet(CLOUDLET_LENGTH, CLOUDLET_PES, utilizationModel, String.format("User %d", x));
                cloudlet.setSizes(1024);
                cloudletList.add(cloudlet);
            }
        }

        return cloudletList;
    }
}
