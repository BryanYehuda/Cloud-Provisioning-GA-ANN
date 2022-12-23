package org.cloudbus.cloudsim.examples;



import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintStream;
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
import org.encog.ml.data.MLData;
import org.encog.ml.data.MLDataPair;
import org.encog.ml.data.MLDataSet;
import org.encog.ml.data.basic.BasicMLData;
import org.encog.ml.data.basic.BasicMLDataSet;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.training.lma.LevenbergMarquardtTraining;
import org.encog.neural.networks.training.propagation.back.Backpropagation;
import org.encog.neural.networks.training.propagation.manhattan.ManhattanPropagation;
import org.encog.neural.networks.training.propagation.quick.QuickPropagation;
import org.encog.neural.networks.training.propagation.resilient.ResilientPropagation;
import org.encog.neural.networks.training.propagation.scg.ScaledConjugateGradient;
import org.encog.persist.EncogDirectoryPersistence;
import org.encog.util.arrayutil.NormalizationAction;
import org.encog.util.arrayutil.NormalizedField;

public class CloudSimulationANN {
	
	// Double Array to hold the raw length data
	public static double LENGTH_RAW_DATA[][];
		
	// Double Array to hold the raw target data
	public static double TARGET_RAW_DATA[][];	
		
	public static double[][] Reading2DArrayFromFileLength()
	{
		Scanner scannerLength;
		int rows = 163; // Number of rows to be scanned
		int columns = 9; // Number of columns to be scanned
		double [][] arrayLength = new double[rows][columns];
			
		try 
		{
			scannerLength = new Scanner(new BufferedReader(new FileReader(System.getProperty("user.dir")+ "/test/TestLength-SDSC.txt")));
			while(scannerLength.hasNextLine()) {
				for (int i=0; i<arrayLength.length; i++) {
				    String[] line = scannerLength.nextLine().trim().split(" "); // Splitting the dataset
				    for (int j=0; j<line.length; j++){
				        arrayLength[i][j] = Integer.parseInt(line[j]); // Parsing String to Integer and save to array
				    }
				}
			}
		} catch (FileNotFoundException e) 
		{
				e.printStackTrace();
		}
		return arrayLength;
	}
		
	public static double[][] Reading2DArrayFromFileTarget()
	{
		Scanner scannerTarget;
		int rows = 163; // Number of rows to be scanned
		int columns = 9; // Number of columns to be scanned
		double [][] arrayTarget = new double[rows][columns];
			
		try 
		{
			scannerTarget = new Scanner(new BufferedReader(new FileReader(System.getProperty("user.dir")+ "/test/TestTarget-SDSC.txt")));
			while(scannerTarget.hasNextLine()) {
				for (int i=0; i<arrayTarget.length; i++) {
				    String[] line = scannerTarget.nextLine().trim().split(" "); // Splitting the dataset
				    for (int j=0; j<line.length; j++) {
				    	arrayTarget[i][j] = Integer.parseInt(line[j]); // Parsing String to Integer and save to array
				    }
				}
			}
		} catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		}
		return arrayTarget;
	}
	
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

	private static ArrayList<Double> getSeedValue(int cloudletcount){
		
		// Creating an arraylist to store Cloudlet Datasets
		ArrayList<Double> seed = new ArrayList<Double>();
		Log.printLine(System.getProperty("user.dir")+ "/dataset/SDSCDatasetANN.txt");
		
		try{
			// Opening and scanning the file
			File fobj = new File(System.getProperty("user.dir")+ "/dataset/SDSCDatasetANN.txt");
			java.util.Scanner readFile = new java.util.Scanner(fobj);
			
			while(readFile.hasNextLine() && cloudletcount>0)
			{
				// Adding the file to the arraylist
				seed.add(readFile.nextDouble());
				cloudletcount--;
			}
			readFile.close();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		return seed;
	}

	private static List<Cloudlet> createCloudlet(int userId, int cloudlets){
		
		ArrayList<Double> randomSeed = getSeedValue(cloudlets);
		
		// Creates a container to store Cloudlets
		LinkedList<Cloudlet> list = new LinkedList<Cloudlet>();

		//Cloudlet parameters
		long length = 0; // Cloudlet length (MI) - 0 for SDSC
		//long length = 1000; // Cloudlet length (MI) - 1000 for Random Dataset
		long fileSize = 300; // Cloudlet file size (MB)
		long outputSize = 300; // Cloudlet file size (MB)
		int pesNumber = 1; // Cloudlet CPU needed to process
		UtilizationModel utilizationModel = new UtilizationModelFull();

		Cloudlet[] cloudlet = new Cloudlet[cloudlets];

		for(int i=0;i<cloudlets;i++){
			long finalLen = length +  Double.valueOf(randomSeed.get(i)).longValue();
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
			int vmNumber = 54; // The number of VMs created
			int cloudletNumber = 1479; // The number of Tasks created

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
			
				
			//Sixth step: Use ANN
			BasicNetwork network = (BasicNetwork)EncogDirectoryPersistence.loadObject(new File("ANNscheduler-SDSC.EG"));
			LENGTH_RAW_DATA = Reading2DArrayFromFileLength();
			TARGET_RAW_DATA = Reading2DArrayFromFileTarget();
			
			// Creating a normalization rules
			//NormalizedField input = new NormalizedField(NormalizationAction.Normalize, null, 50000, 10000, 1, 0); //for Random Dataset
			NormalizedField input = new NormalizedField(NormalizationAction.Normalize, null, 8790000, 0, 1, 0); //for SDSC
			NormalizedField output = new NormalizedField(NormalizationAction.Normalize, null, 10, 0, 1, 0);
			
			// Doing normalization to the Input
			for (int m=0; m<LENGTH_RAW_DATA.length; m++) {
				for (int n=0; n<9; n++) {
					LENGTH_RAW_DATA[m][n] = input.normalize(LENGTH_RAW_DATA[m][n]);
				}
			}
			
			// Doing normalization to the Output
			for (int m=0; m<TARGET_RAW_DATA.length; m++) {
				for (int n=0; n<9; n++) {
					TARGET_RAW_DATA[m][n] = output.normalize(TARGET_RAW_DATA[m][n]);
				}
			}
			
			// Create data
			MLDataSet trainingSet = new BasicMLDataSet(LENGTH_RAW_DATA, TARGET_RAW_DATA);
			int iterator = 0; //Iterator for the Cloudlet IDs
			Long placeholderLong; //Placeholder to convert long to integer
			
			// Testing the ANN
			for(MLDataPair pair: trainingSet ) {
				final MLData outputData = network.compute(pair.getInput());
				System.out.println("");
				System.out.println("For Input:");
				for (int a=0 ; a<9; a++) {
					System.out.print(Math.round(input.deNormalize(pair.getInput().getData(a))) + " ");
				}
				System.out.println("");
				System.out.println("Actual Result:");
				for (int b=0 ; b<9; b++) {
					System.out.print(Math.round(output.deNormalize(outputData.getData(b))) + " ");
				}
				System.out.println("");
				System.out.println("Assignment:");
				for (int c=0 ; c<9; c++) {
					placeholderLong = new Long(Math.round(output.deNormalize(outputData.getData(c))));
					int VMidOutput = placeholderLong.intValue();
					System.out.print(cloudletList.get(iterator*9+c).getCloudletId() + " Assigned to ");
					System.out.print((VMidOutput + iterator*9)%54);
					broker.bindCloudletToVm(cloudletList.get(iterator*9+c).getCloudletId(), (VMidOutput + iterator*9)%54);
					System.out.println("");
				}
				System.out.println("");
				iterator++;
			}
			
			
			// Seventh step: Starts the simulation
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