package it.univr.derivatives20252026.exercise2;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;

import it.univr.derivatives.marketdataprovider.MarketDataProvider;
import it.univr.derivatives20252026.exercise1.Exercise1;
import net.finmath.exception.CalculationException;
import net.finmath.fouriermethod.calibration.BoundConstraint;
import net.finmath.fouriermethod.calibration.CalibratedModel;
import net.finmath.fouriermethod.calibration.ScalarParameterInformationImplementation;
import net.finmath.fouriermethod.calibration.CalibratedModel.OptimizationResult;
import net.finmath.fouriermethod.calibration.models.CalibratableHestonModel;
import net.finmath.fouriermethod.models.CharacteristicFunctionModel;
import net.finmath.fouriermethod.models.HestonModel;
import net.finmath.fouriermethod.products.EuropeanOption;
import net.finmath.fouriermethod.products.smile.EuropeanOptionSmileByCarrMadan;
import net.finmath.integration.SimpsonRealIntegrator;
import net.finmath.integration.TrapezoidalRealIntegrator;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.DiscountCurveInterpolation;
import net.finmath.marketdata.model.curves.CurveInterpolation.ExtrapolationMethod;
import net.finmath.marketdata.model.curves.CurveInterpolation.InterpolationEntity;
import net.finmath.marketdata.model.curves.CurveInterpolation.InterpolationMethod;
import net.finmath.marketdata.model.volatilities.OptionSurfaceData;
import net.finmath.modelling.descriptor.HestonModelDescriptor;
import net.finmath.optimizer.OptimizerFactory;
import net.finmath.optimizer.OptimizerFactoryLevenbergMarquardt;
import net.finmath.optimizer.SolverException;

public class Exercise2 {

	public static void main(String[] args) throws SolverException, CalculationException {
		/*
		 * Application of the Carr Madan formula for the
		 * static replication of a Variance swap.
		 */		
		
		//Step 1 Create a Heston model
		LocalDate referenceDate = LocalDate.of(2010,  8, 2);
		double initialValue = 100; //MarketDataProvider.getDaxData().get(referenceDate);
		
		/*
		 * Get the calibrated parameters
		 */
		double riskFreeRate = 0.0;
		double volatility = Math.pow(0.1944600863909872, .5);
		double theta = 0.1114161009508082;
		double kappa = 0.9999999999999998;
		double xi = 0.660778103808106;
		double rho = -0.7280770384313626; 
		HestonModel modelHS = new HestonModel(initialValue, riskFreeRate, volatility, theta, kappa, xi, rho);		
		
		/*
		 * Use the closed-form formula for the price of the variance swap
		 */
		double maturity = 30.0/252;
		double fairVarSwapRate = 0.0;
		
		//kappa star formula (5) in project description
		fairVarSwapRate = (1.0 - Math.exp(-kappa*maturity))/(kappa*maturity) * (
				Math.pow(volatility,2) - theta) + theta;
		
		//print kappa star
		System.out.println("The fair Variance Swap Rate is " + fairVarSwapRate);
		
		//forward price of dax
		double daxForwardPrice = initialValue*Math.exp(riskFreeRate * maturity);
		
		//integrators with trapezoid method
		TrapezoidalRealIntegrator integratorCalls2 = 
				new TrapezoidalRealIntegrator(daxForwardPrice*1.0, 4*daxForwardPrice, 300);
		
		TrapezoidalRealIntegrator integratorPut2 = 
				new TrapezoidalRealIntegrator(daxForwardPrice*0.01, daxForwardPrice, 300);
		
		//operators for integrand functions
		DoubleUnaryOperator integrandCalls2 = x ->	{		//(x) -> C(x,T)/x^2
			try {
				return new net.finmath.fouriermethod.products.
						EuropeanOption(maturity, x)
						.getValue(modelHS)/(x*x);
			} catch (CalculationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return 0.0;
		};
		
		DoubleUnaryOperator putCallParityOnKSquared = x -> {	//(x) -> P(x,T)/x^2 throw Put-Call parity
			try {
				return (new net.finmath.fouriermethod.products.
						EuropeanOption(maturity, x)
						.getValue(modelHS) + x - initialValue)/(x*x);
			} catch (CalculationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return 0.0;
		};
		
		//VIX replica formula (6) in project description
		double VIXReplica = 2.0/maturity * (riskFreeRate*maturity - Math.log(daxForwardPrice/initialValue)
				+ integratorPut2.integrate(putCallParityOnKSquared) + integratorCalls2.integrate(integrandCalls2));
		
		System.out.println("Replication VIX formula: " + VIXReplica);	
	}
}


























