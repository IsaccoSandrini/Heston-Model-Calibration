package it.univr.montecarlo;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationModel;
import net.finmath.montecarlo.assetderivativevaluation.products.AbstractAssetMonteCarloProduct;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;


public class LookbackCallFixedStrike extends AbstractAssetMonteCarloProduct {
	
	private double maturity, strike;
	private int underlyingIndex, numberOfTimeStepsDiscretized;
	
	public LookbackCallFixedStrike(double maturity, double strike) {
		this.maturity = maturity;
		this.strike = strike;
		this.underlyingIndex = 0;
		this.numberOfTimeStepsDiscretized = -1;
	}

	public LookbackCallFixedStrike(double maturity, double strike, int numberOfTimeSteps) {
		this.maturity = maturity;
		this.strike = strike;
		this.underlyingIndex = 0;
		this.numberOfTimeStepsDiscretized = numberOfTimeSteps;
	}
	
	public LookbackCallFixedStrike(int underlyingIndex, double maturity, double strike) {
		this.maturity = maturity;
		this.strike = strike;
		this.underlyingIndex = underlyingIndex;
		this.numberOfTimeStepsDiscretized = -1;
	}

	public LookbackCallFixedStrike(int underlyingIndex, double maturity, double strike, int numberOfTimeSteps) {
		this.maturity = maturity;
		this.strike = strike;
		this.underlyingIndex = underlyingIndex;
		this.numberOfTimeStepsDiscretized = numberOfTimeSteps;
	}

	@Override
	public RandomVariable getValue(double evaluationTime, AssetModelMonteCarloSimulationModel model)
			throws CalculationException {
		int numberOfTimeStepsUnderlying = model.getTimeDiscretization().getNumberOfTimeSteps();
		TimeDiscretization times;
		
		if(numberOfTimeStepsDiscretized < 0 || numberOfTimeStepsDiscretized > numberOfTimeStepsUnderlying) {
			times = model.getTimeDiscretization();
			double[] discretizedTimes = times.getAsDoubleArray();
		} 
		else {
			times = newDiscretization(numberOfTimeStepsDiscretized, model);
		}
		
		RandomVariable maximum = model.getRandomVariableForConstant(0.0);
		for(final double time: times) {
			maximum = maximum.floor(model.getAssetValue(time, underlyingIndex));
		}
		
		RandomVariable values = maximum.sub(strike).floor(0.0);
		
		final RandomVariable numeraireAtMaturity = model.getNumeraire(maturity);
		final RandomVariable monteCarloWeights = model.getMonteCarloWeights(maturity);
		
		values = values.div(numeraireAtMaturity).mult(monteCarloWeights);
		
		final RandomVariable numeraireAtEvalTime = model.getNumeraire(evaluationTime);
		final RandomVariable monteCarloWeightsAtEvalTime = model.getMonteCarloWeights(evaluationTime);
		
		values = values.mult(numeraireAtEvalTime).div(monteCarloWeightsAtEvalTime);		
		
		return values;
	}
	
	private TimeDiscretization newDiscretization (int numberOfTimeStepsDiscretized, AssetModelMonteCarloSimulationModel model) {
		TimeDiscretizationFromArray times;
		double[] timesAsDouble = new double[numberOfTimeStepsDiscretized+1];
		double[] allTimes = model.getTimeDiscretization().getAsDoubleArray();
		
		int numberOfAllSteps = allTimes.length;
		
		double fractionIndex = (double)numberOfAllSteps / ((double)numberOfTimeStepsDiscretized+1);
		
		for(int i=0; i<numberOfTimeStepsDiscretized; i++) {
			timesAsDouble[i] = allTimes[(int)Math.round(fractionIndex*i)];
		}
		
		timesAsDouble[numberOfTimeStepsDiscretized] = allTimes[numberOfAllSteps-1];
		
		times = new TimeDiscretizationFromArray(timesAsDouble);
		
 		return times;
	}
}
