package agent;

import graph.Node;

import java.util.ArrayList;
import java.util.List;

import model.DefenderAction;
import model.DefenderBelief;
import model.DefenderObservation;
import model.DependencyGraph;

import org.apache.commons.math3.distribution.AbstractIntegerDistribution;
import org.apache.commons.math3.distribution.UniformIntegerDistribution;
import org.apache.commons.math3.random.RandomGenerator;


public class MinCutDefender extends Defender{
	int maxNumRes;
	int minNumRes;
	double numResRatio;
	public MinCutDefender(double maxNumRes, double minNumRes, double numResRatio)
	{
		super(DEFENDER_TYPE.MINCUT);
		this.maxNumRes = (int)maxNumRes;
		this.minNumRes = (int)minNumRes;
		this.numResRatio = numResRatio;
	}
	public static DefenderAction sampleAction(List<Node> dCandidateNodeList, int numNodetoProtect,
			AbstractIntegerDistribution rnd)
	{
		DefenderAction action = new DefenderAction();
		
		boolean[] isChosen = new boolean[dCandidateNodeList.size()];
		for(int i = 0; i < dCandidateNodeList.size(); i++)
			isChosen[i] = false;
		int count = 0;
		while(count < numNodetoProtect)
		{
			int idx = rnd.sample();
			if(!isChosen[idx])
			{
				action.addNodetoProtect(dCandidateNodeList.get(idx));
				isChosen[idx] = true;
				count++;
			}
				
		}
		return action;
	}
	@Override
	public DefenderAction sampleAction(DependencyGraph depGraph,
			int curTimeStep, int numTimeStep, DefenderBelief dBelief, RandomGenerator rng) {
		// TODO Auto-generated method stub
		List<Node> dCandidateNodeList = new ArrayList<Node>(depGraph.getMinCut());
		int numNodetoProtect = 0;
		if(dCandidateNodeList.size() < this.minNumRes)
			numNodetoProtect = dCandidateNodeList.size();
		else 
		{
			numNodetoProtect = Math.max(this.minNumRes, (int)(this.numResRatio * dCandidateNodeList.size()));
			numNodetoProtect = Math.min(this.maxNumRes, numNodetoProtect);
		}
		if(dCandidateNodeList.size() == 0)
			return new DefenderAction();
		// Sample nodes
		UniformIntegerDistribution rnd = new UniformIntegerDistribution(rng, 0, dCandidateNodeList.size() - 1);
		return sampleAction(dCandidateNodeList, numNodetoProtect, rnd);	
	}
	@Override
	public DefenderBelief updateBelief(DependencyGraph depGraph,
			DefenderBelief currentBelief, DefenderAction dAction,
			DefenderObservation dObservation, int curTimeStep, int numTimeStep,
			RandomGenerator rng)
	{
		return null;
	}
}
