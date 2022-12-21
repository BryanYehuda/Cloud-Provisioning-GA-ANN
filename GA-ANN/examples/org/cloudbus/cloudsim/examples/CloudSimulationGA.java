package org.cloudbus.cloudsim.examples;



import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.DoubleSummaryStatistics;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.DoubleStream;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.CloudletSchedulerSpaceShared;
import org.cloudbus.cloudsim.DatacenterBroker;
import org.cloudbus.cloudsim.DatacenterCharacteristics;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.UtilizationModel;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmSchedulerTimeShared;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.PowerDatacenter;
import org.cloudbus.cloudsim.power.PowerHostUtilizationHistory;
import org.cloudbus.cloudsim.power.PowerVmAllocationPolicySimple;
import org.cloudbus.cloudsim.power.models.PowerModelLinear;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

public class CloudSimulationGA {
	
	private static PowerDatacenter datacenter1, datacenter2, datacenter3, datacenter4, datacenter5, datacenter6;
	/** The cloudlet list. */
	private static List<Cloudlet> cloudletList;

	/** The vmlist. */
	private static List<Vm> vmlist;

	private static List<Vm> createVM(int userId, int vms) {

		//Creates a container to store VMs. 
		//This list is passed to the broker later
		LinkedList<Vm> list = new LinkedList<Vm>();

		//VM Parameters
		long size = 10000; //Image size (MB)
		int[] ram = {512,1024,2048}; //VM memory (MB)
		int[] mips = {400,500,600}; //VM processing power (MIPS)
		long bw = 1000; //VM bandwith
		int pesNumber = 1; //Number of cpus
		String vmm = "Xen"; //VMM name
		
		//create VMs
		Vm[] vm = new Vm[vms];

		for(int i=0;i<vms;i++){
			//For loop to create a VM with a time shared scheduling policy for cloudlets:
			vm[i] = new Vm(i, userId, mips[i%3], pesNumber, ram[i%3], bw, size, vmm, new CloudletSchedulerSpaceShared());
			list.add(vm[i]);
		}
		
		return list;
	}

	private static ArrayList<Integer> getSeedValue(int cloudletcount){
		
		// Creating an arraylist to store Cloudlet Datasets
		ArrayList<Integer> seed = new ArrayList<Integer>();
		Log.printLine(System.getProperty("user.dir")+ "/RandomDataset.txt");
		
		try{
			// Opening and scanning the file
			File fobj = new File(System.getProperty("user.dir")+ "/RandomDataset.txt");
			java.util.Scanner readFile = new java.util.Scanner(fobj);
			
			while(readFile.hasNextLine() && cloudletcount>0)
			{
				// Adding the file to the arraylist
				seed.add(readFile.nextInt());
				cloudletcount--;
			}
			readFile.close();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		return seed;
	}

	private static List<Cloudlet> createCloudlet(int userId, int cloudlets){
		
		ArrayList<Integer> randomSeed = getSeedValue(cloudlets);
		
		// Creates a container to store Cloudlets
		LinkedList<Cloudlet> list = new LinkedList<Cloudlet>();

		//Cloudlet parameters
		long length = 1000; // Cloudlet length (MI)
		long fileSize = 300; // Cloudlet file size (MB)
		long outputSize = 300; // Cloudlet file size (MB)
		int pesNumber = 1; // Cloudlet CPU needed to process
		UtilizationModel utilizationModel = new UtilizationModelFull();

		Cloudlet[] cloudlet = new Cloudlet[cloudlets];

		for(int i=0;i<cloudlets;i++){
			long finalLen = length + randomSeed.get(i);
			// Creating the cloudlet with all the parameter listed
			cloudlet[i] = new Cloudlet(i, finalLen, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
			
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
			int num_user = 1;   // Number of grid users
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false;  // Mean trace events
			int hostId=0; // Starting host ID
			BufferedWriter outputWriter = null;
			outputWriter = new BufferedWriter(new FileWriter("filename.txt")); //Save output to text file
			int vmNumber = 54; // The number of VMs created
			int cloudletNumber = 1000; // The number of Tasks created

			// Initialize the CloudSim library
			CloudSim.init(num_user, calendar, trace_flag);
			
			
			//Second step: Create Datacenters
			//Datacenters are the resource providers in CloudSim. We need at least one of them to run a CloudSim simulation
			datacenter1 = createDatacenter("DataCenter_1", hostId);
			hostId = 3;
			datacenter2 = createDatacenter("DataCenter_2", hostId);
			hostId = 6;
			datacenter3 = createDatacenter("DataCenter_3", hostId);
			hostId = 9;
			datacenter4 = createDatacenter("DataCenter_4", hostId);
			hostId = 12;
			datacenter5 = createDatacenter("DataCenter_5", hostId);
			hostId = 15;
			datacenter6 = createDatacenter("DataCenter_6", hostId);
			
			
			//Third step: Create Broker
			DatacenterBroker broker = createBroker();
			int brokerId = broker.getId();

			
			//Fourth step: Create VMs and Cloudlets 
			vmlist = createVM(brokerId,vmNumber); //Creating vms
			cloudletList = createCloudlet(brokerId,cloudletNumber); // Creating cloudlets
			
			
			//Fifth step: Send VMs and Cloudlets to broker 	
			broker.submitVmList(vmlist);
			broker.submitCloudletList(cloudletList);
			
			
			//Sixth step: Use Genetic Algorithm
			int chromosomeLength = 9; //number of genes inside a chromosome
			int cloudletLoopingNumber = cloudletNumber/vmNumber - 1; //number of iteration needed to process the dataset
			
			for (int cloudletIterator=0; cloudletIterator<=cloudletLoopingNumber; cloudletIterator++)
			{
				System.out.println("Cloudlet Iteration Number " + cloudletIterator);
				for (int dataCenterIterator = 1; dataCenterIterator <= 6; dataCenterIterator++)
				{
					// Initialize Genetic Algorithm
					GeneticAlgorithm ga = new GeneticAlgorithm(20, 0.3, 0.95, 2, cloudletList, vmlist);
					
					// Initialize population
					System.out.println("Datacenter " + dataCenterIterator + " Population Initialization");
					Population population = ga.initPopulation(chromosomeLength, dataCenterIterator);
					
					// Evaluate population
					ga.evalPopulation(population, dataCenterIterator, cloudletIterator);
					
					// Genetic Algorithm Iteration
					int iteration = 1;
					while (iteration <= 15) 
					{	
						// get fittest individual from population in every iteration
						Individual fit = population.getFittest(0);
						
						System.out.print("Fittest: ");
						for(int j=0;j<9;j++) {
							System.out.print(fit.chromosome[j] + " ");
						}
						System.out.println("  fitness => " + fit.getFitness());

						// Apply crossover
						population = ga.crossoverPopulation(population, dataCenterIterator);

						// Apply mutation
						population = ga.mutatePopulation(population, dataCenterIterator);

						// Evaluate population
						ga.evalPopulation(population, dataCenterIterator, cloudletIterator);

						// Increment the current generation
						iteration++;
					}
					// Get the fittest individual from Genetic Algorithm
					System.out.println("Best solution of GA: " + population.getFittest(0) + " For Datacenter-" + dataCenterIterator);
					System.out.println("Highest Fitness Achieved: " + population.getFittest(0).getFitness());
					
					// Assign Cloudlet to their respective VMs according to the fittest individual's chromosome
					//outputWriter.write("{");
					for (int assigner=0+(dataCenterIterator-1)*9 + cloudletIterator*54; assigner<9+(dataCenterIterator-1)*9 + cloudletIterator*54; assigner++)
					{
						broker.bindCloudletToVm(assigner, population.getFittest(0).getGene(assigner%9));
						outputWriter.write(Long.toString(cloudletList.get(assigner).getCloudletId())); // Print Cloudlet Length
						//outputWriter.write(Long.toString(population.getFittest(0).getGene(assigner%9)%9)); // Print Assigned VM ID %
						outputWriter.write(" ");
						//if (assigner%9<8)
						//{
						//	outputWriter.write(",");
						//}
					}	
					outputWriter.newLine();
					//outputWriter.write("}");
				}
			}
			
			
			// Seventh step: Starts the simulation
			CloudSim.startSimulation();

			outputWriter.flush();
			outputWriter.close();
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
	
	

	private static PowerDatacenter createDatacenter(String name, int hostId){

		// Here are the steps needed to create a PowerDatacenter:
		// 1. We need to create a list to store one or more machines
		List<PowerHost> hostList = new ArrayList<PowerHost>();

		
		// 2. A Machine contains one or more PEs or CPUs/Cores. Therefore, should
		// create a list to store these PEs before creating a Machine.
		List<Pe> peList1 = new ArrayList<Pe>();
		List<Pe> peList2 = new ArrayList<Pe>();
		List<Pe> peList3 = new ArrayList<Pe>();

		int mipsunused= 300; // Unused core, only 3 cores will be able to process Cloudlets for this simulation
		int mips1 = 400; // The MIPS Must be bigger than the VMs
		int mips2 = 500;
		int mips3 = 600;
		

		// 3. Create PEs and add these into the list.
		//for a quad-core machine, a list of 4 PEs is required:
		peList1.add(new Pe(0, new PeProvisionerSimple(mips1))); // need to store Pe id and MIPS Rating, Must be bigger than the VMs
		peList1.add(new Pe(1, new PeProvisionerSimple(mips1)));
		peList1.add(new Pe(2, new PeProvisionerSimple(mips1)));
		peList1.add(new Pe(3, new PeProvisionerSimple(mipsunused)));
		peList2.add(new Pe(4, new PeProvisionerSimple(mips2)));
		peList2.add(new Pe(5, new PeProvisionerSimple(mips2)));
		peList2.add(new Pe(6, new PeProvisionerSimple(mips2)));
		peList2.add(new Pe(7, new PeProvisionerSimple(mipsunused)));
		peList3.add(new Pe(8, new PeProvisionerSimple(mips3)));
		peList3.add(new Pe(9, new PeProvisionerSimple(mips3)));
		peList3.add(new Pe(10, new PeProvisionerSimple(mips3)));
		peList3.add(new Pe(11, new PeProvisionerSimple(mipsunused)));
	

		//4. Create Hosts with its id and list of PEs and add them to the list of machines
		int ram = 128000 ; //Host memory (MB), Must be bigger than the VMs
		long storage = 1000000; //Host storage (MB)
		int bw = 10000; //Host bandwith
		int maxpower = 117; // Host Max Power
		int staticPowerPercentage = 50; // Host Static Power Percentage

		hostList.add(
				new PowerHostUtilizationHistory(
						hostId, new RamProvisionerSimple(ram), 
						new BwProvisionerSimple(bw),
						storage, 
						peList1, 
						new VmSchedulerTimeShared(peList1),
						new PowerModelLinear(maxpower, staticPowerPercentage)));
		hostId++;
		
		hostList.add(
				new PowerHostUtilizationHistory(
						hostId, new RamProvisionerSimple(ram), 
						new BwProvisionerSimple(bw),
						storage, 
						peList2, 
						new VmSchedulerTimeShared(peList2),
						new PowerModelLinear(maxpower, staticPowerPercentage)));
		hostId++;
		
		hostList.add(
				new PowerHostUtilizationHistory(
						hostId, new RamProvisionerSimple(ram), 
						new BwProvisionerSimple(bw),
						storage, 
						peList3, 
						new VmSchedulerTimeShared(peList3),
						new PowerModelLinear(maxpower, staticPowerPercentage)));


		// 5. Create a DatacenterCharacteristics object that stores the
		// properties of a data center: architecture, OS, list of
		// Machines, allocation policy: time- or space-shared, time zone
		// and its price (G$/Pe time unit).
		String arch = "x86";      		// System architecture
		String os = "Linux";          	// Operating system
		String vmm = "Xen";				// Name
		double time_zone = 10.0;        // Time zone this resource located
		double cost = 3.0;              // The cost of using processing in this resource
		double costPerMem = 0.05;		// The cost of using memory in this resource
		double costPerStorage = 0.1;	// The cost of using storage in this resource
		double costPerBw = 0.1;			// The cost of using bw in this resource
		LinkedList<Storage> storageList = new LinkedList<Storage>();

		DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                arch, os, vmm, hostList, time_zone, cost, costPerMem, costPerStorage, costPerBw);


		// 6. Finally, we need to create a PowerDatacenter object.
		PowerDatacenter datacenter = null;
		try {
			datacenter = new PowerDatacenter(name, characteristics, new PowerVmAllocationPolicySimple(hostList), storageList, 9); // 15 --> is the cloud scheduling interval
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
		
		// Initializing the printed output to zero
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
	    
	    // Printing all the status of the Cloudlets
	    for (int i = 0; i < size; i++) {
	    	cloudlet = list.get(i);
	        Log.print(cloudlet.getCloudletId() + indent + indent);
	        
	        if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS){
	        	Log.print("SUCCESS");
	            CPUTimeSum  = CPUTimeSum  + cloudlet.getActualCPUTime();
	            waitTimeSum = waitTimeSum + cloudlet.getWaitingTime();
	            Log.printLine(indent + indent + indent + (cloudlet.getResourceId()-1) + indent + indent + indent + cloudlet.getVmId() +
	                 indent + indent + dft.format(cloudlet.getActualCPUTime()) + indent + indent + dft.format(cloudlet.getExecStartTime())+
	                 indent + indent + dft.format(cloudlet.getFinishTime())+ indent + indent + indent + dft.format(cloudlet.getWaitingTime()));
	            totalValues++; 
	         	 
	         	response_time[i] = cloudlet.getActualCPUTime();
	        }
	    }
	    DoubleSummaryStatistics stats = DoubleStream.of(response_time).summaryStatistics();
	     
	    // Show the parameters and print them out
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
		double makespan_total = makespan + cloudlet.getFinishTime();
		System.out.println("Makespan: " + makespan_total);
		
		//Imbalance Degree
	    double degree_of_imbalance = (stats.getMax() - stats.getMin())/(CPUTimeSum/ totalValues);
	    System.out.println("Imbalance Degree: " + degree_of_imbalance);
	    
	    //Scheduling Length
	    double scheduling_length = waitTimeSum + makespan_total;
	    Log.printLine("Total Scheduling Length: " + scheduling_length);
	    
	    //CPU Resource Utilization
	    double resource_utilization = (CPUTimeSum / (makespan_total * 54)) * 100;
	    Log.printLine("Resouce Utilization: " + resource_utilization);
	 
	    //Energy Consumption
	    Log.printLine(String.format("Total Energy Consumption: %.2f kWh",
				(datacenter1.getPower() + datacenter2.getPower()+ datacenter3.getPower()+ datacenter4.getPower()+ datacenter5.getPower()+ datacenter6.getPower())/ (3600*1000)));
	}
	
	
	
}