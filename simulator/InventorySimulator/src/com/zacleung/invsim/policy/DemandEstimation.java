package com.zacleung.invsim.policy;

import com.google.common.base.Objects;

import invsim3.Facility;
import invsim3.Forecast;
import invsim3.Retailer;

public class DemandEstimation {
	public static enum Type {
		PAST_CONSUMPTION, PAST_DEMAND, FUTURE,
		LAST_YEAR_CONSUMPTION, LAST_YEAR_DEMAND,
	};

	public final Type type;
	public final int numberOfPeriods;

	public DemandEstimation(Type type, int numberOfPeriods) {
		this.type = type;
		this.numberOfPeriods = numberOfPeriods;
	}
	
	public double getPerPeriodDemand(Facility facility) {		
		int t = facility.getCurrentTimePeriod();
		int Y = facility.getNumberOfPeriodsInYear();
		Retailer retailer;
		
		double sum = 0;
		switch(type) {
		
		case PAST_CONSUMPTION:
			for (int i = 0; i < numberOfPeriods; ++i) {
				sum += facility.getIssues(t - 1 - i);
			}
			break;
			
		case PAST_DEMAND:
			retailer = (Retailer) facility;
			for (int i = 0; i < numberOfPeriods; ++i) {
				sum += retailer.getDemand(t - 1 - i);
			}
			break;
			
		
		case FUTURE:
			retailer = (Retailer) facility;
			Forecast f = retailer.demand.getForecast(t, numberOfPeriods);
			for (int i = 0; i < numberOfPeriods; ++i) {
				sum += f.mean[i];
			}
			break;
		
		case LAST_YEAR_CONSUMPTION:
			for (int i = 0; i < numberOfPeriods; ++i) {
				sum += facility.getIssues(t + i - Y);
			}
			break;
			
		case LAST_YEAR_DEMAND:
			retailer = (Retailer) facility;
			for (int i = 0; i < numberOfPeriods; ++i) {
				sum += retailer.getDemand(t + i - Y);
			}
			break;
		}
	
		
		//System.out.printf("getAverageConsumptionPerPeriod = %.1f%n",
		//		sum / numberOfPeriods);
		return sum / numberOfPeriods;
	}
	
	@Override
	public String toString() {
		return Objects.toStringHelper(this.getClass())
				.add("type", type)
				.add("number of periods", numberOfPeriods)
				.toString();
	}
	

}
