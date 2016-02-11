package com.zacleung.invsim.policy;

import invsim3.Simulator;

public abstract class XDockPolicy extends Policy {
	public abstract int[] computeShipments(Simulator simulator) throws Exception;
}
