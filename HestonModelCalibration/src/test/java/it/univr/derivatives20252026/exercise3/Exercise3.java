package it.univr.derivatives20252026.exercise3;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.Set;
import java.util.TreeMap;

import it.univr.derivatives.marketdataprovider.MarketDataProvider;
import net.finmath.exception.CalculationException;
import net.finmath.fouriermethod.calibration.BoundConstraint;
import net.finmath.fouriermethod.calibration.CalibratedModel;
import net.finmath.fouriermethod.calibration.ScalarParameterInformationImplementation;
import net.finmath.fouriermethod.calibration.CalibratedModel.OptimizationResult;
import net.finmath.fouriermethod.calibration.models.CalibratableHestonModel;
import net.finmath.fouriermethod.products.EuropeanOption;
import net.finmath.fouriermethod.products.smile.EuropeanOptionSmileByCarrMadan;
import net.finmath.functions.HestonModel;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.volatilities.OptionSurfaceData;
import net.finmath.modelling.descriptor.HestonModelDescriptor;
import net.finmath.optimizer.OptimizerFactory;
import net.finmath.optimizer.OptimizerFactoryLevenbergMarquardt;
import net.finmath.optimizer.SolverException;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.daycount.DayCountConvention;
import net.finmath.time.daycount.DayCountConvention_ACT_365;

public class Exercise3 {

	public static void main(String[] args) throws SolverException, CalculationException {
		
		/*
		 * Load Market Data
		 */
		//loading market data
		TreeMap<LocalDate, OptionSurfaceData> marketData = MarketDataProvider.getVolatilityDataContainer();
		
		//all available dates
		Set<LocalDate> keys = marketData.keySet();
		
		//setting a reference date to start hedging
		LocalDate referenceDate = LocalDate.of(2006, 1, 2);
		
		//get initial value at reference date
		double initialValue = MarketDataProvider.getDaxData().get(referenceDate);
		
		//initial guess of parameters
		double riskFreeRate;
		double volatility = Math.pow(0.1944600863909872, .5);
		double theta = 0.1114161009508082;
		double kappa = 0.9999999999999998;
		double xi = 0.660778103808106;
		double rho = -0.7280770384313626; 

		//get discounted curve from market surface
		DiscountCurve discountedCurve = marketData.get(referenceDate).getDiscountCurve();
		
		//descriptor of heston model for calibration
		HestonModelDescriptor hestonModelDescriptor = new HestonModelDescriptor(referenceDate, initialValue,
				discountedCurve, discountedCurve, volatility, theta, kappa, xi, rho);

		
		final ScalarParameterInformationImplementation volatilityInformation = new ScalarParameterInformationImplementation(true, new BoundConstraint(0.01,1.0));
		final ScalarParameterInformationImplementation thetaInformation = new ScalarParameterInformationImplementation(true, new BoundConstraint(0.01,0.2));
		final ScalarParameterInformationImplementation kappaInformation = new ScalarParameterInformationImplementation(true, new BoundConstraint(0.01,1.0));
		final ScalarParameterInformationImplementation xiInformation = new ScalarParameterInformationImplementation(true, new BoundConstraint(0.01,1.0));
		final ScalarParameterInformationImplementation rhoInformation = new ScalarParameterInformationImplementation(true, new BoundConstraint(-1.0,1.0));

		final CalibratableHestonModel model = new CalibratableHestonModel(hestonModelDescriptor,volatilityInformation,thetaInformation,kappaInformation,xiInformation,rhoInformation,false);

		final OptimizerFactory optimizerFactory = new OptimizerFactoryLevenbergMarquardt(300 /* maxIterations */, 2 /* maxThreads */);

		final double[] initialParameters = new double[] {volatility,
				theta, kappa, xi, rho} /* initialParameters */;
		final double[] parameterStep = new double[] { 0.01,0.01,0.01,0.01,0.01} /* parameterStep */;

		/*
		 * Maturity and strikes are here immaterial and only meant to generate the first instance of the class.
		 */
		
		double maturityAsDouble = marketData.get(referenceDate).getMaturities()[0];

		int months = 2;
		double optionMaturity = ((double)months)*1.0/12.0;
		riskFreeRate = - Math.log(discountedCurve.getDiscountFactor(optionMaturity))/optionMaturity;
	
		
		EuropeanOptionSmileByCarrMadan pricer = new EuropeanOptionSmileByCarrMadan(maturityAsDouble,
				marketData.get(referenceDate).getSmile(maturityAsDouble).getStrikes());

		CalibratedModel problem = new CalibratedModel(marketData.get(referenceDate), model, optimizerFactory, pricer,initialParameters,parameterStep);

		OptimizationResult result = problem.getCalibration();
		
		//Step 1 Create a Heston model
		hestonModelDescriptor = (HestonModelDescriptor) result.getModel().getModelDescriptor();
		
		volatility = hestonModelDescriptor.getVolatility();
		theta = hestonModelDescriptor.getTheta();
		kappa = hestonModelDescriptor.getKappa();
		xi = hestonModelDescriptor.getXi();
		rho = hestonModelDescriptor.getRho(); 
		
		net.finmath.fouriermethod.models.HestonModel modelHS = new net.finmath.fouriermethod.models.HestonModel(initialValue, riskFreeRate, volatility, theta, kappa, xi, rho);		
		
		//Step 2 We sell today one option with maturity 2 month
		
		//option strike as 95% of underlying at reference date
		double optionStrike = 0.95 * initialValue;
		
		//selling price of option
		double sellingPrice = new net.finmath.fouriermethod.products.EuropeanOption(optionMaturity, optionStrike).getValue(modelHS);
				
		System.out.println("We sell an option with maturity " + months  + " months, strike  " + optionStrike +" and price "+ sellingPrice);

		System.out.println();
		
		//This is when we stop the hedging portfolio
		LocalDate maturity = referenceDate.plusMonths(months);		
		DayCountConvention daycountConvention = new DayCountConvention_ACT_365();
		
		/*
		 * Step 3: delta Hedge
		 */
		
		//just initialize your holdings.
		double bankAccountAtTimeIndex = 1.0;
		double assetValueAtTimeIndex = initialValue;
		
		//initial positions
		double amountOfBankAccount = sellingPrice/bankAccountAtTimeIndex;	
		double amountOfUnderlyingAsset = 0.0;
		
		/*
		 * main loop
		 */
		LocalDate previousDate = referenceDate;
		for(LocalDate currentDay : keys) {
			if(currentDay.isBefore(referenceDate)) {}
			else if(currentDay.plusDays(1).isAfter(maturity)) {
				break;
			}
			else {
			
				//Calculate the Day Count Fraction
				double dayCountFraction = daycountConvention.getDaycountFraction(previousDate, currentDay);
				//Time To maturity for the delta calculation
				final double timeToMaturity = daycountConvention.getDaycountFraction(currentDay, maturity);
				
				//Get the current value of the underlying from the data
				assetValueAtTimeIndex = MarketDataProvider.getDaxData().get(currentDay);
				//Update the bank account to include the latest data
				bankAccountAtTimeIndex = bankAccountAtTimeIndex*Math.exp(riskFreeRate*dayCountFraction);
				
				//Calculate the delta with respect to Heston model of the underlying
				final double delta = HestonModel.hestonOptionDelta(100, riskFreeRate, 0.0, volatility, theta, kappa, xi, rho, timeToMaturity, optionStrike*100/assetValueAtTimeIndex);
				//the new number of stocks and the change of delta
				final double newNumberOfStocks	= delta;
				final double stockToBuy = newNumberOfStocks - amountOfUnderlyingAsset;
				amountOfUnderlyingAsset = delta;
				//the change in the bank account
				final double bankAccountAssetsToSell = stockToBuy * assetValueAtTimeIndex / bankAccountAtTimeIndex;
				amountOfBankAccount -= bankAccountAssetsToSell;
				//change of date
				previousDate = currentDay;
				//printing dynamic of delta hedging
				System.out.println("\nCurrent Day: " + currentDay +
						"\n timeToMaturity: " + timeToMaturity +
						"\n delta: " + delta +
						"\n underlying value today: " + assetValueAtTimeIndex +
						"\n bank account value today: " + bankAccountAtTimeIndex +
						"\n amountOfBankAccount: " + amountOfBankAccount +
						"\n amountOfUnderlyingAsset: " + amountOfUnderlyingAsset +
						"\n portfolio value: " + (assetValueAtTimeIndex*amountOfUnderlyingAsset + bankAccountAtTimeIndex*amountOfBankAccount));
			
			}
		}
		
		//get underlying value at maturity
		double underlyingAtMaturity = MarketDataProvider.getDaxData().get(previousDate);
		//compute the portfolio value at the end of the hedging strategy
		double portfolioValue = underlyingAtMaturity*amountOfUnderlyingAsset + bankAccountAtTimeIndex*amountOfBankAccount;
		//compute the payoff at maturity
		double payoffAtMaturity = Math.max(underlyingAtMaturity - optionStrike, 0.0);
		
		System.out.println();
		//print results
		System.out.println("Underlying at maturity " + underlyingAtMaturity);
		System.out.println("Strike of the option " + optionStrike);
		System.out.println("The final value of the portfolio is " + portfolioValue);
		System.out.println("The payoff of the option is " + payoffAtMaturity);
		System.out.println("The hedging error is  " + (portfolioValue - payoffAtMaturity));
		
	}

}
