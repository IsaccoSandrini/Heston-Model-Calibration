package it.univr.montecarlo;


import java.util.Arrays;
import java.util.function.DoubleUnaryOperator;

import it.univr.analyticprices.AnalyticPrices;
//import it.univr.montecarlo.finmathlibraryproducts.AssetModelMonteCarloSimulationModel;
//import it.univr.montecarlo.finmathlibraryproducts.MonteCarloAssetModel;
//import it.univr.usefulmethodsarrays.UsefulMethodsArrays;
import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.BrownianMotionFromMersenneRandomNumbers;
import net.finmath.montecarlo.assetderivativevaluation.*;
import net.finmath.montecarlo.assetderivativevaluation.models.BachelierModel;
import net.finmath.montecarlo.assetderivativevaluation.products.AbstractAssetMonteCarloProduct;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;
import net.finmath.interpolation.*;

public class Tests {

	public static void main(String[] args) throws CalculationException {
		//model parameters
		double spotPrice = 100.0;
		double riskFreeRate = 0.1;
		double volatility = 0.3;

		//option parameters
		double maturity = 1.0;		
		double strike = 100.0;
		int numberOfFixingsForDiscretelyMonitoredLookbacks = 100;


		//time discretization parameters
		int numberOfTimeSteps = 1000;
		double initialTime = 0.0;
		double timeStep = maturity / numberOfTimeSteps;
		TimeDiscretization times = new TimeDiscretizationFromArray(initialTime, numberOfTimeSteps, timeStep);

		// Replace the call to the constructor with no arguments with the one you wrote.
		// Here you have to do it in such a way that the options are (approximations of) the continuously monitored ones 
		AbstractAssetMonteCarloProduct continuouslyMonitoredCallFixedStrike = new LookbackCallFixedStrike(maturity, strike);
		AbstractAssetMonteCarloProduct continuouslyMonitoredPutFixedStrike = new LookbackPutFixedStrike(maturity, strike);
		AbstractAssetMonteCarloProduct continuouslyMonitoredCallFloatingStrike = new LookbackCallFloatingStrike(maturity);
		AbstractAssetMonteCarloProduct continuouslyMonitoredPutFloatingStrike = new LookbackPutFloatingStrike(maturity);

		// Replace the call to the constructor with no arguments with the one you wrote.
		// Here you have to do it in such a way that the options are the discretely monitored ones 
		AbstractAssetMonteCarloProduct discretelyMonitoredCallFixedStrike = new LookbackCallFixedStrike(maturity, strike, numberOfFixingsForDiscretelyMonitoredLookbacks);
		AbstractAssetMonteCarloProduct discretelyMonitoredPutFixedStrike = new LookbackPutFixedStrike(maturity, strike, numberOfFixingsForDiscretelyMonitoredLookbacks);
		AbstractAssetMonteCarloProduct discretelyMonitoredCallFloatingStrike = new LookbackCallFloatingStrike(maturity, numberOfFixingsForDiscretelyMonitoredLookbacks);
		AbstractAssetMonteCarloProduct discretelyMonitoredPutFloatingStrike = new LookbackPutFloatingStrike(maturity, numberOfFixingsForDiscretelyMonitoredLookbacks);
		
		//simulation parameters
		int numberOfPaths = 10000;
		int seed = 1897;
		
		//simulation construction
		BrownianMotion ourDriver = new BrownianMotionFromMersenneRandomNumbers(times, 1, numberOfPaths, seed);
		MonteCarloBlackScholesModel blackScholesProcess = new MonteCarloBlackScholesModel(spotPrice, riskFreeRate, volatility, ourDriver);

		//Monte-Carlo prices for continuously monitored options
		double mCPriceContinuouslyMonitoredCallFixed = continuouslyMonitoredCallFixedStrike.getValue(blackScholesProcess);
		double mCPriceContinuouslyMonitoredPutFixed = continuouslyMonitoredPutFixedStrike.getValue(blackScholesProcess);
		double mCPriceContinuouslyMonitoredCallFloating = continuouslyMonitoredCallFloatingStrike.getValue(blackScholesProcess);
		double mCPriceContinuouslyMonitoredPutFloating = continuouslyMonitoredPutFloatingStrike.getValue(blackScholesProcess);

		//Monte-Carlo prices for discretely monitored options
		double mCPriceDiscretelyMonitoredCallFixed = discretelyMonitoredCallFixedStrike.getValue(blackScholesProcess);
		double mCPriceDiscretelyMonitoredPutFixed = discretelyMonitoredPutFixedStrike.getValue(blackScholesProcess);
		double mCPriceDiscretelyMonitoredCallFloating = discretelyMonitoredCallFloatingStrike.getValue(blackScholesProcess);
		double mCPriceDiscretelyMonitoredPutFloating = discretelyMonitoredPutFloatingStrike.getValue(blackScholesProcess);

		
		//Analytic prices for continuously monitored options
		double analyticPriceContinuouslyMonitoredCallFloating = AnalyticPrices.continuouslyMonitoredLookbackCallFloatingStrike(spotPrice, riskFreeRate, volatility, maturity);
		double analyticPriceContinuouslyMonitoredPutFloating = AnalyticPrices.continuouslyMonitoredLookbackPutFloatingStrike(spotPrice, riskFreeRate, volatility, maturity);
		double analyticPriceContinuouslyMonitoredCallFixed = AnalyticPrices.continuouslyMonitoredLookbackCallFixedStrike(spotPrice, riskFreeRate, volatility, maturity, strike);
		double analyticPriceContinuouslyMonitoredPutFixed = AnalyticPrices.continuouslyMonitoredLookbackPutFixedStrike(spotPrice, riskFreeRate, volatility, maturity, strike);
		
		//Analytic prices for discretely monitored options
		double approximatedBroadiePriceDiscretelyMonitoredCallFloating = AnalyticPrices.discretelyMonitoredLookbackCallFloatingStrike(spotPrice, riskFreeRate, volatility, maturity, numberOfFixingsForDiscretelyMonitoredLookbacks);
		double approximatedBroadiePriceDiscretelyMonitoredPutFloating = AnalyticPrices.discretelyMonitoredLookbackPutFloatingStrike(spotPrice, riskFreeRate, volatility, maturity, numberOfFixingsForDiscretelyMonitoredLookbacks);
		double approximatedBroadiePriceDiscretelyMonitoredCallFixed = AnalyticPrices.discretelyMonitoredLookbackCallFixedStrike(spotPrice, riskFreeRate, volatility, maturity, strike, numberOfFixingsForDiscretelyMonitoredLookbacks);
		double approximatedBroadiePriceDiscretelyMonitoredPutFixed = AnalyticPrices.discretelyMonitoredLookbackPutFixedStrike(spotPrice, riskFreeRate, volatility, maturity, strike, numberOfFixingsForDiscretelyMonitoredLookbacks);

		
		System.out.println("MC price of continuously monitored call with fixed strike: " + mCPriceContinuouslyMonitoredCallFixed);
		System.out.println("Analytic price of continuously monitored call with fixed strike: " + analyticPriceContinuouslyMonitoredCallFixed);
		System.out.println("MC price of discretely monitored call with fixed strike: " + mCPriceDiscretelyMonitoredCallFixed);
		System.out.println("Approximated Broadie price of discretely monitored call with fixed strike: " + approximatedBroadiePriceDiscretelyMonitoredCallFixed);
		
		System.out.println();
		
		System.out.println("MC price of continuously monitored put with fixed strike: " + mCPriceContinuouslyMonitoredPutFixed);
		System.out.println("Analytic price of continuously monitored put with fixed strike: " + analyticPriceContinuouslyMonitoredPutFixed);
		System.out.println("MC price of discretely monitored put with fixed strike: " + mCPriceDiscretelyMonitoredPutFixed);
		System.out.println("Approximated Broadie price of discretely monitored put with fixed strike: " + approximatedBroadiePriceDiscretelyMonitoredPutFixed);

		System.out.println();
		
		System.out.println("MC price of continuously monitored call with floating strike: " + mCPriceContinuouslyMonitoredCallFloating);
		System.out.println("Analytic price of continuously monitored call with floating strike: " + analyticPriceContinuouslyMonitoredCallFloating);
		System.out.println("MC price of discretely monitored call with floating strike: " + mCPriceDiscretelyMonitoredCallFloating);
		System.out.println("Approximated Broadie price of discretely monitored call with floating strike: " + approximatedBroadiePriceDiscretelyMonitoredCallFloating);

		System.out.println();
		
		System.out.println("MC price of continuously monitored put with floating strike: " + mCPriceContinuouslyMonitoredPutFloating);
		System.out.println("Analytic price of continuously monitored put with floating strike: " + analyticPriceContinuouslyMonitoredPutFloating);
		System.out.println("MC price of discretely monitored put with floating strike: " + mCPriceDiscretelyMonitoredPutFloating);
		System.out.println("Approximated Broadie price of discretely monitored put with floating strike: " + approximatedBroadiePriceDiscretelyMonitoredPutFloating);
		
		//confronto con aumento numero di punti di dicretizzazione dell'intervallo temporale
		
		int[] numberOfTimeStepsArray = {10, 20, 50, 100, 200, 500, 1000, 2500};
		System.out.println("\nNumber of times \tMonteCarlo  \t\tApproximationed Broadie");
		for(int i: numberOfTimeStepsArray) {
			discretelyMonitoredPutFloatingStrike = new LookbackPutFloatingStrike(maturity, i);
			mCPriceDiscretelyMonitoredPutFloating = discretelyMonitoredPutFloatingStrike.getValue(blackScholesProcess);
			approximatedBroadiePriceDiscretelyMonitoredPutFloating = AnalyticPrices.discretelyMonitoredLookbackPutFloatingStrike(spotPrice, riskFreeRate, volatility, maturity, i);
			
			System.out.println(i + "\t\t\t" + mCPriceDiscretelyMonitoredPutFloating + "\t" + approximatedBroadiePriceDiscretelyMonitoredPutFloating);
		}
		
		//Prova usando modello di Bachelier per il sottostante,
		// utilizzando come volatilità volatility*spotPrice
		
		BachelierModel bachelierModelForMC = new BachelierModel(spotPrice, riskFreeRate, spotPrice*volatility);
		AssetModelMonteCarloSimulationModel ourSimulation = new MonteCarloAssetModel(bachelierModelForMC, ourDriver);	
		discretelyMonitoredCallFixedStrike = new LookbackCallFixedStrike(maturity, strike, 500);
		mCPriceContinuouslyMonitoredCallFixed = continuouslyMonitoredCallFixedStrike.getValue(ourSimulation);
		mCPriceDiscretelyMonitoredCallFixed = discretelyMonitoredCallFixedStrike.getValue(ourSimulation);
		
		System.out.println("\nMC price of continuously monitored call with fixed strike on BachelierModel: " + mCPriceContinuouslyMonitoredCallFixed);
		System.out.println("MC price of discretely monitored call with fixed strike on BachelierModel: " + mCPriceDiscretelyMonitoredCallFixed);
		
	}
}
