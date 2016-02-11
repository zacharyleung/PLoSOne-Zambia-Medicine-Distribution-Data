package invsim3;

public abstract class SimulatorEntity {
	Simulator simulator = null;
	
	public void setSimulator(Simulator simulator) {
		this.simulator = simulator;
	}

	/**
	 * Get the current time of the simulator
	 * @return
	 */
	public int getCurrentTimePeriod() {
		return simulator.getCurrentTimePeriod();
	}
	
	public int getNumberOfPeriodsInYear() {
		return simulator.getNumberOfPeriodsInYear();
	}
}
