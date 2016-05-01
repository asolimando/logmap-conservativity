/*******************************************************************************
 * Copyright 2016 by the Department of Computer Science (University of Genova and University of Oxford)
 * 
 *    This file is part of LogMapC an extension of LogMap matcher for conservativity principle.
 * 
 *    LogMapC is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 * 
 *    LogMapC is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 * 
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with LogMapC.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package scc.algoSolver;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import javax.swing.SwingUtilities;

import org.uncommons.maths.binary.BitString;
import org.uncommons.maths.number.ConstantGenerator;
import org.uncommons.maths.random.MersenneTwisterRNG;
import org.uncommons.maths.random.Probability;
import org.uncommons.watchmaker.framework.CachingFitnessEvaluator;
import org.uncommons.watchmaker.framework.CandidateFactory;
import org.uncommons.watchmaker.framework.EvolutionEngine;
import org.uncommons.watchmaker.framework.EvolutionObserver;
import org.uncommons.watchmaker.framework.EvolutionaryOperator;
import org.uncommons.watchmaker.framework.FitnessEvaluator;
import org.uncommons.watchmaker.framework.GenerationalEvolutionEngine;
import org.uncommons.watchmaker.framework.PopulationData;
import org.uncommons.watchmaker.framework.SelectionStrategy;
import org.uncommons.watchmaker.framework.TerminationCondition;
import org.uncommons.watchmaker.framework.operators.BitStringCrossover;
import org.uncommons.watchmaker.framework.operators.BitStringMutation;
import org.uncommons.watchmaker.framework.operators.EvolutionPipeline;
import org.uncommons.watchmaker.framework.selection.TournamentSelection;
import org.uncommons.watchmaker.framework.termination.ElapsedTime;
import org.uncommons.watchmaker.framework.termination.GenerationCount;
import org.uncommons.watchmaker.framework.termination.Stagnation;
import org.uncommons.watchmaker.framework.termination.TargetFitness;

import util.FileUtil;
import util.Params;
import util.Util;
import scc.evol.BitStringCycleFactory;
import scc.evol.BitStringToDiagnosis;
import scc.evol.DiagnosisFitnessEvaluator;
import scc.graphDataStructure.Diagnosis;
import scc.graphDataStructure.LightAdjacencyList;
import scc.graphDataStructure.LightCycles;
import scc.graphDataStructure.LightEdge;
import scc.graphDataStructure.LightSCC;

public class SGASolver extends ProblemSolver {

	private static Random rnd = null;

	static {
		//		try {
		//rnd = new MersenneTwisterRNG(new RandomDotOrgSeedGenerator());
		//rnd = new MersenneTwisterRNG(new SecureRandomSeedGenerator());
		//rnd = new MersenneTwisterRNG(new DevRandomSeedGenerator());
		rnd = new MersenneTwisterRNG();
		//		} catch (SeedException e) {
		//			e.printStackTrace();
		//		}
	}

	private int generationCount = 100, populationCount = 100, stagnation = 25;
	protected int mutationProbIncreaseTime = 12;
	protected double mutationProbIncrease = 0.01;
	private boolean useAverageFitnessStagnation = false, fixMutation = false;
	private double mutationProb = 0.1; // 0.02
	private double maxMutationProb = 0.3;
	public BitStringMutation mutationOperator = null;
	private double target, costRemoved;

	List<Integer> population = new LinkedList<>();
	public Map<LightEdge,Integer> mappingIndex = new HashMap<>();
	public Map<Integer, LightEdge> indexMapping = new HashMap<>();
	public int [] removedIndexes;
	CandidateFactory<BitString> factory = null;
	EvolutionEngine<BitString> engine = null;
	FitnessEvaluator<BitString> delegate = 
			new DiagnosisFitnessEvaluator(indexMapping);
	CachingFitnessEvaluator<BitString> eval = 
			new CachingFitnessEvaluator<>(delegate);
			SelectionStrategy<Object> selectionStrategy = 
					new TournamentSelection(new Probability(0.9d));

			public SGASolver(LightAdjacencyList adj, LightCycles cycles, 
					LightSCC scc, double target, Set<LightEdge> removedMappings, 
					boolean fixMutation) {
				this(adj,cycles,scc,removedMappings);
				this.target = target;
				this.fixMutation = fixMutation;
			}

			public SGASolver(LightAdjacencyList adj, LightCycles cycles, LightSCC scc, 
					Set<LightEdge> removedMappings) {
				super(adj, cycles, scc);

				Set<LightEdge> mappings = scc.extractMappings(adj, true);
				generationCount = Math.max(100, mappings.size() * 10);
				populationCount = Math.max(100, mappings.size() * 10);
				Iterator<LightEdge> itr = mappings.iterator();
				LightEdge e = null;
				int c = 0;
				while(itr.hasNext()){
					e = itr.next();
					mappingIndex.put(e, c);
					indexMapping.put(c, e);
					++c;
				}

				removedIndexes = new int[removedMappings.size()];
				c = 0;
				for (LightEdge rm : removedMappings){
					removedIndexes[c++] = mappingIndex.get(rm);
					costRemoved += rm.confidence;
				}

				List<Diagnosis> hDiags = new ArrayList<>(2);
				hDiags.add(adj.computeDiagnosisOnCycles(
						cycles, scc, true));
				hDiags.add(adj.computeDiagnosisOnCycles(
						cycles, scc, false));
				
				factory = new BitStringCycleFactory(mappingIndex.size(),
						cycles,mappingIndex,hDiags);
				List<EvolutionaryOperator<BitString>> operators = 
						new LinkedList<EvolutionaryOperator<BitString>>();
				operators.add(new BitStringCrossover());
				operators.add(mutationOperator = new BitStringMutation(
						new Probability(mutationProb)));
				operators.add(new BitStringToDiagnosis(this));

				EvolutionaryOperator<BitString> pipeline = 
						new EvolutionPipeline<>(operators);

						engine = new GenerationalEvolutionEngine<BitString>(
								factory, pipeline, eval, selectionStrategy, rnd);
						engine.addEvolutionObserver(new PopulationUpdater(this));
			}

			@Override
			public Diagnosis computeDiagnosis() 
					throws InterruptedException, TimeoutException {
				long startTime = Util.getMSec();

				TerminationCondition[] terminationConditions = 
						new TerminationCondition[4];
				terminationConditions[0] = new GenerationCount(generationCount);
				double minCost = (target == -1 ? Double.MAX_VALUE : target);
				if(target == -1){
					for (LightEdge m : mappingIndex.keySet())
						if(m.confidence < minCost)
							minCost = m.confidence;
					minCost += costRemoved;
				}
				stagnation = generationCount/4;

				terminationConditions[1] = new TargetFitness(minCost,false);
				terminationConditions[2] = new Stagnation(stagnation, false, 
						useAverageFitnessStagnation);
				terminationConditions[3] = new ElapsedTime(Params.SGATimeout*1000);

				if(Params.verbosity > 0)
					FileUtil.writeLogAndConsole("SGA params: population = " + populationCount 
							+ ", generations = " + generationCount 
							+ ", timeout = " + Params.SGATimeout 
							+ ", minCost = " + minCost 
							+ ", stagnation = " + stagnation);

				Diagnosis d = new Diagnosis(engine.evolve(populationCount,
						new Double(populationCount*0.1).intValue(), 
						terminationConditions), indexMapping);
				d.setTime(Util.getDiffmsec(startTime));
				if(Params.verbosity > 0)
					FileUtil.writeLogAndConsole("Terminating condition: " 
							+ engine.getSatisfiedTerminationConditions());
				if(Params.verbosity > 0)
					FileUtil.writeLogAndConsole("SGA diagnosis in " + d.getTime() 
							+ " ms");
				return d;
			}

			private class PopulationUpdater implements EvolutionObserver<BitString>
			{
				private double bestFit = Double.MAX_VALUE;
				private int firstGenerationNum = 0;
				private SGASolver sga;

				public PopulationUpdater(SGASolver sga){
					this.sga = sga;
				}

				public void populationUpdate(
						final PopulationData<? extends BitString> data)
				{
					SwingUtilities.invokeLater(new Runnable()
					{
						public void run()
						{
							try {
								//if(data.getGenerationNumber() == generationCount-1)
								if(Params.verbosity > 0)
									FileUtil.writeLogAndConsole("Best " + data.getGenerationNumber() 
											+ ": " + data.getBestCandidate() + " -> " 
											+ data.getBestCandidateFitness());

								if(bestFit > data.getBestCandidateFitness())
								{
									bestFit = data.getBestCandidateFitness();
									firstGenerationNum = data.getGenerationNumber();
								}
								else if(!fixMutation && bestFit == data.getBestCandidateFitness()) {
									if(data.getGenerationNumber() - firstGenerationNum 
											> sga.mutationProbIncreaseTime){
										// BEGIN reflection code
										Class rightJavaClass = sga.mutationOperator.getClass();

										Field[] fs = rightJavaClass.getDeclaredFields();								
										try {
											Field operands = Class.forName(
													rightJavaClass.getName()
													).getDeclaredField(fs[0].getName());
											operands.setAccessible(true);
											sga.mutationProb = Math.min(maxMutationProb, 
													sga.mutationProb+sga.mutationProbIncrease);
											operands.set(sga.mutationOperator,
													new ConstantGenerator<Probability>(
															new Probability(sga.mutationProb)
															)
													);
											if(Params.verbosity > 1)
												FileUtil.writeLogAndConsole(
														"MutationProb = " 
												+ sga.mutationProb);
											operands.setAccessible(false);

										} catch (IllegalArgumentException | IllegalAccessException 
												| NoSuchFieldException | SecurityException 
												| ClassNotFoundException e) {
											FileUtil.writeErrorLogAndConsole("Exception in reflection code (SGA)");
											e.printStackTrace();
										}
										// END reflection code
									}
								}
							}
							catch(NullPointerException e){
								FileUtil.writeErrorLogAndConsole("Exception in evolve method");
								e.printStackTrace();
							}
						}
					});
				}
			}

}
