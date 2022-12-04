package org.cloudbus.cloudsim.examples;

import java.util.List;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Vm;

/**
 * The GeneticAlgorithm class is our main abstraction for managing the
 * operations of the genetic algorithm. This class is meant to be
 * problem-specific, meaning that (for instance) the "calcFitness" method may
 * need to change from problem to problem.
 * 
 * This class concerns itself mostly with population-level operations, but also
 * problem-specific operations such as calculating fitness, testing for
 * termination criteria, and managing mutation and crossover operations (which
 * generally need to be problem-specific as well).
 * 
 * Generally, GeneticAlgorithm might be better suited as an abstract class or an
 * interface, rather than a concrete class as below. A GeneticAlgorithm
 * interface would require implementation of methods such as
 * "isTerminationConditionMet", "calcFitness", "mutatePopulation", etc, and a
 * concrete class would be defined to solve a particular problem domain. For
 * instance, the concrete class "TravelingSalesmanGeneticAlgorithm" would
 * implement the "GeneticAlgorithm" interface. This is not the approach we've
 * chosen, however, so that we can keep each chapter's examples as simple and
 * concrete as possible.
 * 
 * @author bkanber
 *
 */
public class GeneticAlgorithm {
	private int populationSize;
	public static List<Cloudlet> cloudletList;
	public static List<Vm> vmlist;

	/**
	 * Mutation rate is the fractional probability than an individual gene will
	 * mutate randomly in a given generation. The range is 0.0-1.0, but is
	 * generally small (on the order of 0.1 or less).
	 */
	private double mutationRate;

	/**
	 * Crossover rate is the fractional probability that two individuals will
	 * "mate" with each other, sharing genetic information, and creating
	 * offspring with traits of each of the parents. Like mutation rate the
	 * rance is 0.0-1.0 but small.
	 */
	private double crossoverRate;

	/**
	 * Elitism is the concept that the strongest members of the population
	 * should be preserved from generation to generation. If an individual is
	 * one of the elite, it will not be mutated or crossover.
	 */
	private int elitismCount;

	@SuppressWarnings("static-access")
	public GeneticAlgorithm(int populationSize, double mutationRate, double crossoverRate, int elitismCount, List<Cloudlet> cloudletList, List<Vm> vmlist) {
		this.populationSize = populationSize;
		this.mutationRate = mutationRate;
		this.crossoverRate = crossoverRate;
		this.elitismCount = elitismCount;
		this.cloudletList = cloudletList;
		this.vmlist = vmlist;
	}

	/**
	 * Initialize population
	 * 
	 * @param chromosomeLength
	 *            The length of the individuals chromosome
	 * @return population The initial population generated
	 */
	public Population initPopulation(int chromosomeLength, int dataCenterIterator) {
		// Initialize population
		Population population = new Population(this.populationSize, chromosomeLength, dataCenterIterator);
		return population;
	}
	
	/**
	 * Get Poisson Distribution for the chances of VM to fail
	 * 
	 * @param lambda
	 *            The fault rate at which a VM is going to fail
	 * @param x
	 *            x is going to 0, the number of VM we want to fail          
	 * @return the chances of 0 VM to fail
	 */
	static int factorial(int n)
    {    
        if (n == 0)    
            return 1;    
        else    
            return(n * factorial(n-1));    
    }    
	
	public static int getRandomPoisson(double lambda) 
    {
        double L = Math.exp(-lambda);
        double p = 1.0;
        int k = 0;

        do 
        {
            k++;
            p *= Math.random();
        } while (p > L);

        return k - 1;
    }
    
	public static double getPoisson(double lambda, int x, int n) 
    {
        double L = Math.exp(-lambda) * n;
        double p = Math.pow(lambda * n, x);
        int k = factorial(x);
        double result;
        
        result = L * p / k;

        return result;
    }

	/**
	 * Calculate fitness for an individual.
	 * 
	 * In this case, the fitness score is very simple: it's the number of ones
	 * in the chromosome. Don't forget that this method, and this whole
	 * GeneticAlgorithm class, is meant to solve the problem in the "AllOnesGA"
	 * class and example. For different problems, you'll need to create a
	 * different version of this method to appropriately calculate the fitness
	 * of an individual.
	 * 
	 * @param individual
	 *            the individual to evaluate
	 * @return double The fitness value for individual
	 */
	public double calcFitness(Individual individual, int dataCenterIterator, int cloudletIteration) {

		double totalExecutionTime = 0;
		double mips = 0;
		double failureRate = 0.04847468455;
		int iterator=0;
		dataCenterIterator = dataCenterIterator-1;
		
		for (int i=0 + dataCenterIterator*9 + cloudletIteration*54; i<9 + dataCenterIterator*9 + cloudletIteration*54; i++)
		{
			int gene = individual.getGene(iterator);
			if (gene%9 == 0 || gene%9 == 1 || gene%9 == 2) 
			{
				mips = 400;
			}else if (gene%9 == 3 || gene%9 == 4 || gene%9 == 5) 
			{
				mips = 500;
			}else if (gene%9 == 6 || gene%9 == 7 || gene%9 == 8)
			{
				mips = 600;
			}else break;
			
			//Log.printLine("Gene " + gene);
			totalExecutionTime = totalExecutionTime + cloudletList.get(i).getCloudletLength() / mips;
			iterator++;
		}
		
		int random = getRandomPoisson(failureRate);
        double poisson=(getPoisson(failureRate, random, 9));

		// Calculate fitness
		double fitness = 0.95 * (1/totalExecutionTime) + 0.05 * (1/poisson);
		//Log.printLine("Fitness " + fitness);

		// Store fitness
		individual.setFitness(fitness);

		return fitness;
	}

	/**
	 * Evaluate the whole population
	 * 
	 * Essentially, loop over the individuals in the population, calculate the
	 * fitness for each, and then calculate the entire population's fitness. The
	 * population's fitness may or may not be important, but what is important
	 * here is making sure that each individual gets evaluated.
	 * 
	 * @param population
	 *            the population to evaluate
	 */
	public void evalPopulation(Population population, int dataCenterIterator, int cloudletIteration) {
		
		// Loop over population evaluating individuals and suming population fitness
		double populationFitness=0;

		for (Individual individual : population.getIndividuals()) {

			double individualFitness = calcFitness(individual, dataCenterIterator, cloudletIteration); 
			individual.setFitness(individualFitness);
			populationFitness+=individualFitness;
		
		}
		population.setPopulationFitness(populationFitness);

	}

	/**
	 * Check if population has met termination condition
	 * 
	 * For this simple problem, we know what a perfect solution looks like, so
	 * we can simply stop evolving once we've reached a fitness of one.
	 * 
	 * @param population
	 * @return boolean True if termination condition met, otherwise, false
	 */
	
	/**
	 * Select parent for crossover
	 * 
	 * @param population
	 *            The population to select parent from
	 * @return The individual selected as a parent
	 */
	public Individual selectParent(Population population) {
		// Get individuals
		Individual individuals[] = population.getIndividuals();
		
		// Spin roulette wheel
		double populationFitness = population.getPopulationFitness();
		double rouletteWheelPosition = Math.random() * populationFitness;

		// Find parent
		double spinWheel = 0;
		for (Individual individual : individuals) {
			spinWheel += individual.getFitness();
			if (spinWheel >= rouletteWheelPosition) {
				return individual;
			}
		}
		return individuals[population.size() - 1];
	}

	/**
	 * Apply crossover to population
	 * 
	 * Crossover, more colloquially considered "mating", takes the population
	 * and blends individuals to create new offspring. It is hoped that when two
	 * individuals crossover that their offspring will have the strongest
	 * qualities of each of the parents. Of course, it's possible that an
	 * offspring will end up with the weakest qualities of each parent.
	 * 
	 * This method considers both the GeneticAlgorithm instance's crossoverRate
	 * and the elitismCount.
	 * 
	 * The type of crossover we perform depends on the problem domain. We don't
	 * want to create invalid solutions with crossover, so this method will need
	 * to be changed for different types of problems.
	 * 
	 * This particular crossover method selects random genes from each parent.
	 * 
	 * @param population
	 *            The population to apply crossover to
	 * @return The new population
	 */
	public Population crossoverPopulation(Population population, int dataCenterIterator) {
		// Create new population
		Population newPopulation = new Population(population.size());

		// Loop over current population by fitness
		for (int populationIndex = 0; populationIndex < population.size(); populationIndex++) {
			Individual parent1 = population.getFittest(populationIndex);

			// Apply crossover to this individual?
			if (this.crossoverRate > Math.random()&& populationIndex > this.elitismCount ) {
				// Initialize offspring
				Individual offspring = new Individual(parent1.getChromosomeLength(), dataCenterIterator);
				
				// Find second parent
				Individual parent2 = selectParent(population);

				// Loop over genome
				for (int geneIndex = 0; geneIndex < parent1.getChromosomeLength(); geneIndex++) {
					// Use half of parent1's genes and half of parent2's genes
					if (0.5 > Math.random()) {
						offspring.setGene(geneIndex, parent1.getGene(geneIndex));
					} else {
						offspring.setGene(geneIndex, parent2.getGene(geneIndex));
					}
				}

				// Add offspring to new population
				newPopulation.setIndividual(populationIndex, offspring);
			} else {
				// Add individual to new population without applying crossover
				newPopulation.setIndividual(populationIndex, parent1);
			}
		}

		return newPopulation;
	}

	/**
	 * Apply mutation to population
	 * 
	 * Mutation affects individuals rather than the population. We look at each
	 * individual in the population, and if they're lucky enough (or unlucky, as
	 * it were), apply some randomness to their chromosome. Like crossover, the
	 * type of mutation applied depends on the specific problem we're solving.
	 * In this case, we simply randomly flip 0s to 1s and vice versa.
	 * 
	 * This method will consider the GeneticAlgorithm instance's mutationRate
	 * and elitismCount
	 * 
	 * @param population
	 *            The population to apply mutation to
	 * @return The mutated population
	 */
	public Population mutatePopulation(Population population, int dataCenterIterator) {
		// Initialize new population
		Population newPopulation = new Population(this.populationSize);
		dataCenterIterator = dataCenterIterator - 1;

		// Loop over current population by fitness
		for (int populationIndex = 0; populationIndex < population.size(); populationIndex++) {
			Individual individual = population.getFittest(populationIndex);

			// Loop over individual's genes
			for (int geneIndex = 0; geneIndex < individual.getChromosomeLength(); geneIndex++) {
				// Skip mutation if this is an elite individual
				if (populationIndex > this.elitismCount) {
					// Does this gene need mutation?
					if (this.mutationRate > Math.random()) {
						// Get new gene
						int newGene=0 + 9 * dataCenterIterator;
						if (individual.getGene(geneIndex)%9 == 0) {
							double r=Math.random();
						    if(r<0.125)
						    {
						    	newGene=1 + 9 * dataCenterIterator;
						    }
						    else if(r>0.125 && r<0.250)
						    {
						    	newGene=2 + 9 * dataCenterIterator;
						    }
						    else if(r>0.250 && r<0.375)
						    {
						    	newGene=3 + 9 * dataCenterIterator;
						    }
						    else if(r>0.375 && r<0.5)
						    {
						    	newGene=4 + 9 * dataCenterIterator;
						    }
						    else if(r>0.5 && r<0.625)
						    {
						    	newGene=5 + 9 * dataCenterIterator;
						    }
						    else if(r>0.625 && r<0.75)
						    {
						    	newGene=6 + 9 * dataCenterIterator;
						    }
						    else if(r>0.75 && r<0.875)
						    {
						    	newGene=7 + 9 * dataCenterIterator;
						    }
					    	else
						    {
						    	newGene=8 + 9 * dataCenterIterator;
						    }
						}
						else if (individual.getGene(geneIndex)%9 == 1) {
							double r=Math.random();
						    if(r<0.125)
						    {
						    	newGene=0 + 9 * dataCenterIterator;
						    }
						    else if(r>0.125 && r<0.250)
						    {
						    	newGene=2 + 9 * dataCenterIterator;
						    }
						    else if(r>0.250 && r<0.375)
						    {
						    	newGene=3 + 9 * dataCenterIterator;
						    }
						    else if(r>0.375 && r<0.5)
						    {
						    	newGene=4 + 9 * dataCenterIterator;
						    }
						    else if(r>0.5 && r<0.625)
						    {
						    	newGene=5 + 9 * dataCenterIterator;
						    }
						    else if(r>0.625 && r<0.75)
						    {
						    	newGene=6 + 9 * dataCenterIterator;
						    }
						    else if(r>0.75 && r<0.875)
						    {
						    	newGene=7 + 9 * dataCenterIterator;
						    }
					    	else
						    {
						    	newGene=8 + 9 * dataCenterIterator;
						    }
						}
						else if (individual.getGene(geneIndex)%9 == 2) {
							double r=Math.random();
						    if(r<0.125)
						    {
						    	newGene=0 + 9 * dataCenterIterator;
						    }
						    else if(r>0.125 && r<0.250)
						    {
						    	newGene=1 + 9 * dataCenterIterator;
						    }
						    else if(r>0.250 && r<0.375)
						    {
						    	newGene=3 + 9 * dataCenterIterator;
						    }
						    else if(r>0.375 && r<0.5)
						    {
						    	newGene=4 + 9 * dataCenterIterator;
						    }
						    else if(r>0.5 && r<0.625)
						    {
						    	newGene=5 + 9 * dataCenterIterator;
						    }
						    else if(r>0.625 && r<0.75)
						    {
						    	newGene=6 + 9 * dataCenterIterator;
						    }
						    else if(r>0.75 && r<0.875)
						    {
						    	newGene=7 + 9 * dataCenterIterator;
						    }
					    	else
						    {
						    	newGene=8 + 9 * dataCenterIterator;
						    }
						}
						else if (individual.getGene(geneIndex)%9 == 3) {
							double r=Math.random();
						    if(r<0.125)
						    {
						    	newGene=0 + 9 * dataCenterIterator;
						    }
						    else if(r>0.125 && r<0.250)
						    {
						    	newGene=1 + 9 * dataCenterIterator;
						    }
						    else if(r>0.250 && r<0.375)
						    {
						    	newGene=2 + 9 * dataCenterIterator;
						    }
						    else if(r>0.375 && r<0.5)
						    {
						    	newGene=4 + 9 * dataCenterIterator;
						    }
						    else if(r>0.5 && r<0.625)
						    {
						    	newGene=5 + 9 * dataCenterIterator;
						    }
						    else if(r>0.625 && r<0.75)
						    {
						    	newGene=6 + 9 * dataCenterIterator;
						    }
						    else if(r>0.75 && r<0.875)
						    {
						    	newGene=7 + 9 * dataCenterIterator;
						    }
					    	else
						    {
						    	newGene=8 + 9 * dataCenterIterator;
						    }
						}
						else if (individual.getGene(geneIndex)%9 == 4) {
							double r=Math.random();
						    if(r<0.125)
						    {
						    	newGene=0 + 9 * dataCenterIterator;
						    }
						    else if(r>0.125 && r<0.250)
						    {
						    	newGene=1 + 9 * dataCenterIterator;
						    }
						    else if(r>0.250 && r<0.375)
						    {
						    	newGene=2 + 9 * dataCenterIterator;
						    }
						    else if(r>0.375 && r<0.5)
						    {
						    	newGene=3 + 9 * dataCenterIterator;
						    }
						    else if(r>0.5 && r<0.625)
						    {
						    	newGene=5 + 9 * dataCenterIterator; 
						    }
						    else if(r>0.625 && r<0.75)
						    {
						    	newGene=6 + 9 * dataCenterIterator;
						    }
						    else if(r>0.75 && r<0.875)
						    {
						    	newGene=7 + 9 * dataCenterIterator;
						    }
					    	else
						    {
						    	newGene=8 + 9 * dataCenterIterator;
						    }
						}
						else if (individual.getGene(geneIndex)%9 == 5) {
							double r=Math.random();
						    if(r<0.125)
						    {
						    	newGene=0 + 9 * dataCenterIterator;
						    }
						    else if(r>0.125 && r<0.250)
						    {
						    	newGene=1 + 9 * dataCenterIterator;
						    }
						    else if(r>0.250 && r<0.375)
						    {
						    	newGene=2 + 9 * dataCenterIterator;
						    }
						    else if(r>0.375 && r<0.5)
						    {
						    	newGene=3 + 9 * dataCenterIterator;
						    }
						    else if(r>0.5 && r<0.625)
						    {
						    	newGene=4 + 9 * dataCenterIterator;
						    }
						    else if(r>0.625 && r<0.75)
						    {
						    	newGene=6 + 9 * dataCenterIterator;
						    }
						    else if(r>0.75 && r<0.875)
						    {
						    	newGene=7 + 9 * dataCenterIterator;
						    }
					    	else
						    {
						    	newGene=8 + 9 * dataCenterIterator;
						    }
						}
						else if (individual.getGene(geneIndex)%9 == 6) {
							double r=Math.random();
						    if(r<0.125)
						    {
						    	newGene=0 + 9 * dataCenterIterator;
						    }
						    else if(r>0.125 && r<0.250)
						    {
						    	newGene=1 + 9 * dataCenterIterator;
						    }
						    else if(r>0.250 && r<0.375)
						    {
						    	newGene=2 + 9 * dataCenterIterator;
						    }
						    else if(r>0.375 && r<0.5)
						    {
						    	newGene=3 + 9 * dataCenterIterator;
						    }
						    else if(r>0.5 && r<0.625)
						    {
						    	newGene=4 + 9 * dataCenterIterator;
						    }
						    else if(r>0.625 && r<0.75)
						    {
						    	newGene=5 + 9 * dataCenterIterator;
						    }
						    else if(r>0.75 && r<0.875)
						    {
						    	newGene=7 + 9 * dataCenterIterator;
						    }
					    	else
						    {
						    	newGene=8 + 9 * dataCenterIterator;
						    }
						}
						else if (individual.getGene(geneIndex)%9 == 7) {
							double r=Math.random();
						    if(r<0.125)
						    {
						    	newGene=0 + 9 * dataCenterIterator;
						    }
						    else if(r>0.125 && r<0.250)
						    {
						    	newGene=1 + 9 * dataCenterIterator;
						    }
						    else if(r>0.250 && r<0.375)
						    {
						    	newGene=2 + 9 * dataCenterIterator;
						    }
						    else if(r>0.375 && r<0.5)
						    {
						    	newGene=3 + 9 * dataCenterIterator;
						    }
						    else if(r>0.5 && r<0.625)
						    {
						    	newGene=4 + 9 * dataCenterIterator;
						    }
						    else if(r>0.625 && r<0.75)
						    {
						    	newGene=5 + 9 * dataCenterIterator;
						    }
						    else if(r>0.75 && r<0.875)
						    {
						    	newGene=6 + 9 * dataCenterIterator;
						    }
					    	else
						    {
						    	newGene=8 + 9 * dataCenterIterator;
						    }
						}
						else if (individual.getGene(geneIndex)%9 == 8) {
							double r=Math.random();
						    if(r<0.125)
						    {
						    	newGene=0 + 9 * dataCenterIterator;
						    }
						    else if(r>0.125 && r<0.250)
						    {
						    	newGene=1 + 9 * dataCenterIterator;
						    }
						    else if(r>0.250 && r<0.375)
						    {
						    	newGene=2 + 9 * dataCenterIterator;
						    }
						    else if(r>0.375 && r<0.5)
						    {
						    	newGene=3 + 9 * dataCenterIterator;
						    }
						    else if(r>0.5 && r<0.625)
						    {
						    	newGene=4 + 9 * dataCenterIterator;
						    }
						    else if(r>0.625 && r<0.75)
						    {
						    	newGene=5 + 9 * dataCenterIterator;
						    }
						    else if(r>0.75 && r<0.875)
						    {
						    	newGene=6 + 9 * dataCenterIterator;
						    }
					    	else
						    {
						    	newGene=7 + 9 * dataCenterIterator;
						    }
						}
						// Mutate gene
						individual.setGene(geneIndex, newGene);
					}
				}
			}

			// Add individual to population
			newPopulation.setIndividual(populationIndex, individual);
		}

		// Return mutated population
		return newPopulation;
	}

}
