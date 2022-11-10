import java.io.FileNotFoundException;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.DoubleSummaryStatistics;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.stream.DoubleStream;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerSpaceShared;
import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicySimple;
import org.cloudbus.cloudsim.VmSchedulerSpaceShared;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

public class CloudSimulationExample {

	/** The cloudlet list. */
	private static List<Cloudlet> cloudletList;

	/** The vmlist. */
	private static List<Vm> vmlist;
	

	private static List<Vm> createVM(int userId, int vms) {

		//Creates a container to store VMs. 
		//This list is passed to the broker later
		LinkedList<Vm> list = new LinkedList<Vm>();

		//VM Parameters
		long size = 1000; //image size (MB)
		int[] ram = {512,1024,2048}; //vm memory (MB)
		int[] mips = {400,500,600}; //vm processing power
		long bw = 1000; //vm bandwith
		int pesNumber = 1; //number of cpus
		String vmm = "Xen"; //VMM name
		
		
		//create VMs
		Vm[] vm = new Vm[vms];

		for(int i=0;i<vms;i++){
			//for creating a VM with a time shared scheduling policy for cloudlets:
			vm[i] = new Vm(i, userId, mips[i%3], pesNumber, ram[i%3], bw, size, vmm, new CloudletSchedulerSpaceShared());
			list.add(vm[i]);
		}
		
	
		return list;
	}


	private static List<Cloudlet> createCloudlet(int userId, int cloudlets){
		// Creates a container to store Cloudlets
		LinkedList<Cloudlet> list = new LinkedList<Cloudlet>();

		//cloudlet parameters
		long length = 20000;
		long fileSize = 300;
		long outputSize = 300;
		int pesNumber = 1;
		UtilizationModel utilizationModel = new UtilizationModelFull();

		Cloudlet[] cloudlet = new Cloudlet[cloudlets];

		for(int i=0;i<cloudlets;i++){
			Random r = new Random();
			cloudlet[i] = new Cloudlet(i, (length + r.nextInt(20000)), pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
			
			// setting the owner of these Cloudlets
			cloudlet[i].setUserId(userId);
			list.add(cloudlet[i]);
		}

		return list;
	}

	/**
	 * Creates main() to run this example
	 */
	public static void main(String[] args) {
		Log.printLine("Starting Cloud Simulation Example...");

		try {
			// First step: Initialize the CloudSim package. It should be called
			// before creating any entities.
			int num_user = 1;   // number of grid users
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false;  // mean trace events
			int hostId=0;

			// Initialize the CloudSim library
			CloudSim.init(num_user, calendar, trace_flag);
			
			//Second step: Create Datacenters
			//Datacenters are the resource providers in CloudSim. We need at list one of them to run a CloudSim simulation
			@SuppressWarnings("unused")
			Datacenter datacenter1 = createDatacenter("Datacenter_1", hostId);
			hostId = 3;
			@SuppressWarnings("unused")
			Datacenter datacenter2 = createDatacenter("Datacenter_2", hostId);
			hostId = 6;
			@SuppressWarnings("unused")
			Datacenter datacenter3 = createDatacenter("Datacenter_3", hostId);
			hostId = 9;
			@SuppressWarnings("unused")
			Datacenter datacenter4 = createDatacenter("Datacenter_4", hostId);
			hostId = 12;
			@SuppressWarnings("unused")
			Datacenter datacenter5 = createDatacenter("Datacenter_5", hostId);
			hostId = 15;
			@SuppressWarnings("unused")
			Datacenter datacenter6 = createDatacenter("Datacenter_6", hostId);

			//Third step: Create Broker
			DatacenterBroker broker = createBroker();
			int brokerId = broker.getId();

			//Fourth step: Create VMs and Cloudlets and send them to broker
			vmlist = createVM(brokerId,54); //creating vms
			cloudletList = createCloudlet(brokerId,100); // creating cloudlets

			broker.submitVmList(vmlist);
			broker.submitCloudletList(cloudletList);

			// Fifth step: Starts the simulation
			CloudSim.startSimulation();

			// Final step: Print results when simulation is over
			List<Cloudlet> newList = broker.getCloudletReceivedList();
			

			CloudSim.stopSimulation();

			printCloudletList(newList);

			Log.printLine("Cloud Simulation Example finished!");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			Log.printLine("The simulation has been terminated due to an unexpected error");
		}
	}

	private static Datacenter createDatacenter(String name, int hostId){

		// Here are the steps needed to create a PowerDatacenter:
		// 1. We need to create a list to store one or more
		//    Machines
		List<Host> hostList = new ArrayList<Host>();

		// 2. A Machine contains one or more PEs or CPUs/Cores. Therefore, should
		//    create a list to store these PEs before creating
		//    a Machine.
		List<Pe> peList1 = new ArrayList<Pe>();
		List<Pe> peList2 = new ArrayList<Pe>();
		List<Pe> peList3 = new ArrayList<Pe>();

		int mipsUnused = 300;
		int mips1 = 400; //Must be bigger than the VMs
		int mips2 = 500; //Must be bigger than the VMs
		int mips3 = 600; //Must be bigger than the VMs

		// 3. Create PEs and add these into the list.
		//for a quad-core machine, a list of 4 PEs is required:
		peList1.add(new Pe(0, new PeProvisionerSimple(mips1))); // need to store Pe id and MIPS Rating, Must be bigger than the VMs
		peList1.add(new Pe(1, new PeProvisionerSimple(mips1)));
		peList1.add(new Pe(2, new PeProvisionerSimple(mips1)));
		peList1.add(new Pe(3, new PeProvisionerSimple(mipsUnused)));
		peList2.add(new Pe(4, new PeProvisionerSimple(mips2)));
		peList2.add(new Pe(5, new PeProvisionerSimple(mips2)));
		peList2.add(new Pe(6, new PeProvisionerSimple(mips2)));
		peList2.add(new Pe(7, new PeProvisionerSimple(mipsUnused)));
		peList3.add(new Pe(8, new PeProvisionerSimple(mips3)));
		peList3.add(new Pe(9, new PeProvisionerSimple(mips3)));
		peList3.add(new Pe(10, new PeProvisionerSimple(mips3)));
		peList3.add(new Pe(11, new PeProvisionerSimple(mipsUnused)));

		//4. Create Hosts with its id and list of PEs and add them to the list of machines
		int ram = 128000 ; //host memory (MB), Must be bigger than the VMs
		long storage = 1000000; //host storage (MB), Must be bigger than the VMs
		int bw = 10000; //host bandwith

		hostList.add(
    			new Host(
    				hostId,
    				new RamProvisionerSimple(ram),
    				new BwProvisionerSimple(bw),
    				storage,
    				peList1,
    				new VmSchedulerSpaceShared(peList1)
    			)
    		); // This is our first machine

		hostId++;

		hostList.add(
    			new Host(
    				hostId,
    				new RamProvisionerSimple(ram),
    				new BwProvisionerSimple(bw),
    				storage,
    				peList2,
    				new VmSchedulerSpaceShared(peList2)
    			)
    		); // Second machine
		
		hostId++;
		
		hostList.add(
    			new Host(
    				hostId,
    				new RamProvisionerSimple(ram),
    				new BwProvisionerSimple(bw),
    				storage,
    				peList3,
    				new VmSchedulerSpaceShared(peList3)
    			)
    		);// Third machine


		// 5. Create a DatacenterCharacteristics object that stores the
		//    properties of a data center: architecture, OS, list of
		//    Machines, allocation policy: time- or space-shared, time zone
		//    and its price (G$/Pe time unit).
		String arch = "x86";      		// system architecture
		String os = "Linux";          	// operating system
		String vmm = "Xen";				// name
		double time_zone = 10.0;        // time zone this resource located
		double cost = 3.0;              // the cost of using processing in this resource
		double costPerMem = 0.05;		// the cost of using memory in this resource
		double costPerStorage = 0.1;	// the cost of using storage in this resource
		double costPerBw = 0.1;			// the cost of using bw in this resource
		LinkedList<Storage> storageList = new LinkedList<Storage>();

		DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage, costPerBw);


		// 6. Finally, we need to create a PowerDatacenter object.
		Datacenter datacenter = null;
		try {
			datacenter = new Datacenter(name, characteristics, new VmAllocationPolicySimple(hostList), storageList, 0);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return datacenter;
	}

	private static DatacenterBroker createBroker(){

		DatacenterBroker broker = null;
		try {
			broker = new DatacenterBroker("Broker");
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return broker;
	}

	/**
	 * Prints the Cloudlet objects
	 * @param list  list of Cloudlets
	 * @throws FileNotFoundException 
	 */
	private static void printCloudletList(List<Cloudlet> list) throws FileNotFoundException {
		
		
		int size = list.size();
		Cloudlet cloudlet = null;

		String indent = "    ";
		Log.printLine();
	    Log.printLine("========== OUTPUT ==========");
	    Log.printLine("Cloudlet ID" + indent + "STATUS" + indent +
	             "Data center ID" + indent + "VM ID" + indent + "Time" 
	    		 + indent + "Start Time" + indent + "Finish Time"+ indent + "Waiting Time");
	     
	    double waitTimeSum = 0.0;
	    double CPUTimeSum  = 0.0;
	    int totalValues    = 0;
	    DecimalFormat dft  = new DecimalFormat("###.##");
	     
	    double response_time[] = new double[size];
	     
	    for (int i = 0; i < size; i++) {
	    	cloudlet = list.get(i);
	        Log.print(cloudlet.getCloudletId() + indent + indent);
	        
	        if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS){
	        	Log.print("SUCCESS");
	            CPUTimeSum  = CPUTimeSum  + cloudlet.getActualCPUTime();
	            waitTimeSum = waitTimeSum + cloudlet.getWaitingTime();
	            Log.printLine(indent + indent + indent + cloudlet.getResourceId() + indent + indent + indent + cloudlet.getVmId() +
	                 indent + indent + dft.format(cloudlet.getActualCPUTime()) + indent + indent + dft.format(cloudlet.getExecStartTime())+
	                 indent + indent + dft.format(cloudlet.getFinishTime())+ indent + indent + indent + dft.format(cloudlet.getWaitingTime()));
	            totalValues++; 
	         	 
	         	response_time[i] = cloudlet.getActualCPUTime();
	        }
	    }
	    DoubleSummaryStatistics stats = DoubleStream.of(response_time).summaryStatistics();
	     
	    // Show the parameters of the questions 2 and 3
	    Log.printLine();
	    System.out.println("min = " + stats.getMin());
	    System.out.println("Response_Time: " + CPUTimeSum/totalValues);
	     	    
	     
	    Log.printLine();
	    Log.printLine("TotalCPUTime : "             + CPUTimeSum);
	    Log.printLine("TotalWaitTime : "            + waitTimeSum);
	    Log.printLine("TotalCloudletsFinished : "   + totalValues);
	    Log.printLine();
	    Log.printLine();

	     
	    //Average Cloudlets Finished 
	    Log.printLine("AverageCloudletsFinished : " + (CPUTimeSum/ totalValues));

		//Average Start Time
		double totalStartTime =0.0;
		for (int i = 0; i < size; i++) {
			totalStartTime = cloudletList.get(i).getExecStartTime();	
		}
		double avgStartTime = totalStartTime/size;
		System.out.println("Average StartTime: " + avgStartTime );
		
		//Average Execution Time
		double ExecTime =0.0;
		for (int i = 0; i < size; i++) {
			ExecTime = cloudletList.get(i).getActualCPUTime();	
		}
		double avgExecTime = ExecTime/size;
		System.out.println("Average Execution Time: " + avgExecTime );
		
		//Average Finish Time
		double totalTime =0.0;
		for (int i = 0; i < size; i++) {
			totalTime = cloudletList.get(i).getFinishTime();	
		}
		double avgTAT = totalTime/size;
		System.out.println("Average FinishTime: " + avgTAT );
		
		//Average Waiting Time
		double avgWT = cloudlet.getWaitingTime()/size;
		System.out.println("Average Waiting time: " + avgWT);
		
		Log.printLine();
	    Log.printLine();
		
		//Throughput
		double maxFT =0.0;
		for (int i = 0; i < size; i++) {
			double currentFT = cloudletList.get(i).getFinishTime();	
			if (currentFT > maxFT) {
				maxFT = currentFT;
			}	
		}
		double throughput = size/maxFT;
		System.out.println("Throughput: " + throughput );
		
		//Makespan
		double makespan =0.0;
		double makespan_total = makespan + cloudlet.getActualCPUTime();
		System.out.println("Makespan: " + makespan_total);
		
		//Imbalance Degree
	    double degree_of_imbalance = (stats.getMax() - stats.getMin())/(CPUTimeSum/ totalValues);
	    System.out.println("Imbalance Degree: " + degree_of_imbalance);
	    
	    //Scheduling Length
	    double scheduling_length = waitTimeSum + makespan_total;
	    Log.printLine("Total Scheduling Length: " + scheduling_length);
	    
	    //Resource Utilization
	    double resource_utilization = totalTime / (makespan_total * size);
	    Log.printLine("Resouce Utilization: " + resource_utilization);
	}
	
	
	
}