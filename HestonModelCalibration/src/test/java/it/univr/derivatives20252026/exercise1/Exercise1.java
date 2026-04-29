package it.univr.derivatives20252026.exercise1;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.DoubleUnaryOperator;

import org.junit.Assert;

import it.univr.derivatives.marketdataprovider.MarketDataProvider;
import it.univr.derivatives.utils.TimeSeries;
import net.finmath.fouriermethod.calibration.BoundConstraint;
import net.finmath.fouriermethod.calibration.CalibratedModel;
import net.finmath.fouriermethod.calibration.ScalarParameterInformationImplementation;
import net.finmath.fouriermethod.calibration.CalibratedModel.OptimizationResult;
import net.finmath.fouriermethod.calibration.models.CalibratableHestonModel;
import net.finmath.fouriermethod.products.smile.EuropeanOptionSmileByCarrMadan;
import net.finmath.functions.UsefulMethodsArrays;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.DiscountCurveFromForwardCurve;
import net.finmath.marketdata.model.curves.DiscountCurveInterpolation;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.marketdata.model.curves.CurveInterpolation.ExtrapolationMethod;
import net.finmath.marketdata.model.curves.CurveInterpolation.InterpolationEntity;
import net.finmath.marketdata.model.curves.CurveInterpolation.InterpolationMethod;
import net.finmath.marketdata.model.volatilities.OptionSurfaceData;
import net.finmath.modelling.descriptor.HestonModelDescriptor;
import net.finmath.optimizer.OptimizerFactory;
import net.finmath.optimizer.OptimizerFactoryLevenbergMarquardt;
import net.finmath.plots.*;
import net.finmath.time.businessdaycalendar.BusinessdayCalendar;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingTARGETHolidays;
import net.finmath.time.businessdaycalendar.BusinessdayCalendar.DateRollConvention;

/**
 * Exercise 1
 * 
 * Perform a daily recalibration of the Heston model and plot
 * 
 * A time series of the calibration error
 * 
 * A time series for each of the 5 parameters of the model
 * 
 * Discuss the stability of the estimates
 * 
 * Is the market practice of daily recalibration consistent with the assumptions of the model?
 */
public class Exercise1 {

	public static void main(String[] args) throws Exception {
		/*
		 * Load Market Data
		 */
		TreeMap<LocalDate, OptionSurfaceData> marketData = MarketDataProvider.getVolatilityDataContainer();
		
		
		//all available dates
		Set<LocalDate> keys = marketData.keySet();		
			
		//time series for Heston parameters of calibrated model
		TimeSeries volatilityTimeSeries = new TimeSeries();
		TimeSeries thetaTimeSeries = new TimeSeries();
		TimeSeries kappaTimeSeries = new TimeSeries();
		TimeSeries xiTimeSeries = new TimeSeries();
		TimeSeries rhoTimeSeries = new TimeSeries();
		TimeSeries rmseTimeSeries = new TimeSeries();
		
		/*
		 * These are just initial guess to get the calibration started.
		 */
		final double volatility = 0.0423;
		final double theta = 0.0818;
		final double kappa = 0.8455;
		final double xi = 0.4639;
		final double rho = -0.4;

		double[] initialParameters = new double[] {volatility, theta, kappa, xi, rho};
		//initial conditions
		final double[] parameterStep = new double[] { 0.01,0.01,0.01,0.01,0.01} /* parameterStep */;
		
		/*
		 * decide when to stop with the experiment
		 */
		
		//Set an initial date and control it is a business day, else rolls to next day
		LocalDate initialDate = LocalDate.of(2010, 7, 31);
		
		while(!keys.contains(initialDate)) {
			//if initalDate isn't in DAX dates, roll on
			initialDate = initialDate.plusDays(1);
		}
		
		LocalDate limit = LocalDate.of(2010, 8, 31);
		
		/*
		 * main loop
		 */
		
		//information on parameters boundaries
		final ScalarParameterInformationImplementation volatilityInformation = new ScalarParameterInformationImplementation(true, new BoundConstraint(0.01,1.0));
		final ScalarParameterInformationImplementation thetaInformation = new ScalarParameterInformationImplementation(true, new BoundConstraint(0.01,0.2));
		final ScalarParameterInformationImplementation kappaInformation = new ScalarParameterInformationImplementation(true, new BoundConstraint(0.01,1.0));
		final ScalarParameterInformationImplementation xiInformation = new ScalarParameterInformationImplementation(true, new BoundConstraint(0.01,1.0));
		final ScalarParameterInformationImplementation rhoInformation = new ScalarParameterInformationImplementation(true, new BoundConstraint(-1.0,1.0));
		
		//declaration of objects for calibration
		CalibratableHestonModel model;
		final OptimizerFactory optimizerFactory = new OptimizerFactoryLevenbergMarquardt(300 /* maxIterations */, 2 /* maxThreads */);
		EuropeanOptionSmileByCarrMadan pricer; //Carr Madan smile
		double maturity;
		CalibratedModel problem;
		
		OptimizationResult result;
		//HestonModelDescriptor hestonDescriptor;
		
		HestonModelDescriptor hestonModelDescriptor;
		//double initialPrice = MarketDataProvider.getDaxData().get(initialDate);
		

		DecimalFormat formatterPrice = new DecimalFormat("0000.#####");
		DecimalFormat formatterParameters = new DecimalFormat("0.#####");
		
		for(LocalDate today : keys) {
			if(today.isBefore(initialDate)) {}   							//skip in we are before than starting date
			else if(today.isBefore(limit)) {    							//do all computations until we reach limit day
				
				System.out.println("Calibration of: " + today);
				
				OptionSurfaceData todaysMarket = marketData.get(today);  	//get today's surface
				maturity = todaysMarket.getMaturities()[0];					//get a maturity to construct the smile object
				double priceToday = MarketDataProvider.getDaxData().get(today);				//get today's price of DAX
				
				//initialize the descriptor
				hestonModelDescriptor = new HestonModelDescriptor(today,					
																	priceToday,
																	todaysMarket.getDiscountCurve(),
																	todaysMarket.getDiscountCurve(),																
																	initialParameters[0],	//volatility
																	initialParameters[1],	//theta
																	initialParameters[2],	//kappa
																	initialParameters[3],	//xi
																	initialParameters[4]);	//rho
				
				//initialize the calibratable model
				model = new CalibratableHestonModel(hestonModelDescriptor,volatilityInformation,
						thetaInformation,kappaInformation,xiInformation,rhoInformation,false);
				
				//initialize Carr Madan smile and the calibrated model
				pricer = new EuropeanOptionSmileByCarrMadan(maturity, todaysMarket.getSmile(maturity).getStrikes());
				problem = new CalibratedModel(todaysMarket, model, optimizerFactory, pricer, initialParameters, parameterStep);
				
				//obtain calibration
				result = problem.getCalibration();

				//print number of iterations and rmsqe
				System.out.println("The solver required " + result.getIterations() + " iterations.");
				System.out.println("RMSQE " +result.getRootMeanSquaredError());
				
				hestonModelDescriptor = (HestonModelDescriptor) result.getModel().getModelDescriptor();
				
				//print optimal parameters values
				System.out.println("Volatility: " + hestonModelDescriptor.getVolatility());
				System.out.println("Theta: " + hestonModelDescriptor.getTheta());
				System.out.println("Kappa: " + hestonModelDescriptor.getKappa());
				System.out.println("Xi: " + hestonModelDescriptor.getXi());
				System.out.println("Rho: " + hestonModelDescriptor.getRho());
				System.out.println();

//				final ArrayList<String> errorsOverview = result.getCalibrationOutput();

//				for(final String myString : errorsOverview) {
//					System.out.println(myString);
//				}

				Assert.assertTrue(result.getRootMeanSquaredError() < 1.0);
				
				//add today's parameters to time series of each parameters
				volatilityTimeSeries.add(today, hestonModelDescriptor.getVolatility());
				thetaTimeSeries.add(today, hestonModelDescriptor.getTheta());
				kappaTimeSeries.add(today, hestonModelDescriptor.getKappa());
				xiTimeSeries.add(today, hestonModelDescriptor.getXi());
				rhoTimeSeries.add(today, hestonModelDescriptor.getRho());
				rmseTimeSeries.add(today, result.getRootMeanSquaredError());
				
				//update initial parameters for tomorrow's calibration with optimal parameters of today
				initialParameters[0] = Math.pow(volatilityTimeSeries.get(today),.5);	//squared root of variance
				initialParameters[1] = thetaTimeSeries.get(today);
				initialParameters[2] = kappaTimeSeries.get(today);
				initialParameters[3] = xiTimeSeries.get(today);
				initialParameters[4] = rhoTimeSeries.get(today);
			}
			
			//if we are after the limit day, break all
			else if(today.isAfter(limit)) break;						
		}
		
		//plots of parameters' time series
		volatilityTimeSeries.plot("Volatility");
		thetaTimeSeries.plot("Theta");
		kappaTimeSeries.plot("Kappa");
		xiTimeSeries.plot("Xi");
		rhoTimeSeries.plot("Rho");
		rmseTimeSeries.plot("RMSE");
		
		System.out.println("Total RMSE in time interval: " + rmseTimeSeries.getMean()*rmseTimeSeries.size());
	}
	
}
