package com.zacleung.invsim.policy;

import invsim3.Simulator;

public abstract class DistrictPolicy extends Policy {
	abstract public int[] computeRetailerShipments(Simulator simulator)
			throws Exception;

	abstract public int[] computeDistrictShipments(Simulator simulator)
			throws Exception;
}
