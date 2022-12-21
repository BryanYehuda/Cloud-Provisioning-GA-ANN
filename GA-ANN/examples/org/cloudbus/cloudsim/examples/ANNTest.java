package org.cloudbus.cloudsim.examples;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Scanner;

import org.encog.Encog;
import org.encog.engine.network.activation.ActivationReLU;
import org.encog.engine.network.activation.ActivationSigmoid;
import org.encog.ml.data.MLData;
import org.encog.ml.data.MLDataPair;
import org.encog.ml.data.MLDataSet;
import org.encog.ml.data.basic.BasicMLDataSet;
import org.encog.neural.networks.BasicNetwork;
import org.encog.neural.networks.layers.BasicLayer;
import org.encog.neural.networks.training.propagation.back.Backpropagation;
import org.encog.neural.networks.training.propagation.manhattan.ManhattanPropagation;
import org.encog.neural.networks.training.propagation.quick.QuickPropagation;
import org.encog.persist.EncogDirectoryPersistence;
import org.encog.util.arrayutil.NormalizationAction;
import org.encog.util.arrayutil.NormalizedField;

/**
 * XOR: This example is essentially the "Hello World" of neural network
 * programming.  This example shows how to construct an Encog neural
 * network to predict the output from the XOR operator.  This example
 * uses backpropagation to train the neural network.
 * 
 * This example attempts to use a minimum of Encog features to create and
 * train the neural network.  This allows you to see exactly what is going
 * on.  For a more advanced example, that uses Encog factories, refer to
 * the XORFactory example.
 * 
 */
public class ANNTest {
	
	// Double Array to hold the raw length data
	public static double LENGTH_RAW_DATA[][];
	
	// Double Array to hold the raw target data
	public static double TARGET_RAW_DATA[][];	
	
	public static double[][] Reading2DArrayFromFileLength()
	{
		Scanner scannerLength;
		int rows = 89; // Number of rows to be scanned
		int columns = 9; // Number of columns to be scanned
		double [][] arrayLength = new double[rows][columns];
		
		try 
		{
			scannerLength = new Scanner(new BufferedReader(new FileReader(System.getProperty("user.dir")+ "/train/DatasetLength-1000.txt")));
			while(scannerLength.hasNextLine()) {
			    for (int i=0; i<arrayLength.length; i++) {
			       String[] line = scannerLength.nextLine().trim().split(" "); // Splitting the dataset
			          for (int j=0; j<line.length; j++) {
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
		int rows = 89; // Number of rows to be scanned
		int columns = 9; // Number of columns to be scanned
		double [][] arrayTarget = new double[rows][columns];
		
		try 
		{
			scannerTarget = new Scanner(new BufferedReader(new FileReader(System.getProperty("user.dir")+ "/train/DatasetTarget-1000.txt")));
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
	
	/**
	 * The main method.
	 * @param args No arguments are used.
	 */
	public static void main(final String args[]) {
		
		// Saving the data scanned into the double arrays
		LENGTH_RAW_DATA = Reading2DArrayFromFileLength();
		TARGET_RAW_DATA = Reading2DArrayFromFileTarget();
		
		// Create a neural network
		BasicNetwork network = new BasicNetwork();
		network.addLayer(new BasicLayer(null,true,9)); // 9 input nodes
		network.addLayer(new BasicLayer(new ActivationReLU(),true,18)); // 18 hidden nodes
		network.addLayer(new BasicLayer(new ActivationSigmoid(),false,9)); // 9 output nodes
		network.getStructure().finalizeStructure();
		network.reset();
		
		// Creating a normalization rules
		NormalizedField input = new NormalizedField(NormalizationAction.Normalize, null, 50000, 10000, 1, 0);
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
		
		// Create training data
		MLDataSet trainingSet = new BasicMLDataSet(LENGTH_RAW_DATA, TARGET_RAW_DATA);
		
		// Train the neural network
		final ManhattanPropagation train = new ManhattanPropagation(network, trainingSet, 0.00001);
		int epoch = 1;
		
		do {
			train.iteration();
			System.out.println("Epoch #" + epoch + " Error:" + train.getError());
			epoch++;
		} while(epoch<100000 && train.getError()>0.12); // Epoch until 100000 or error below 14% (Best Fit for ANN)
		train.finishTraining();
		
		// Test the neural network
		System.out.println("Neural Network Results:");
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
			System.out.println("Ideal Result:");
			for (int c=0 ; c<9; c++) {
				System.out.print(Math.round(output.deNormalize(pair.getIdeal().getData(c))) + " ");
			}
			System.out.println("");
			System.out.println("");
		}
		
		// Saving the neural network
		EncogDirectoryPersistence.saveObject(new File("ANNscheduler.EG"), network);
		Encog.getInstance().shutdown();
	}
}