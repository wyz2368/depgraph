package model;

// The initial state is not saved since it is empty by default

public final class GameSample {
	private final int timeStep;
	
	private final DefenderAction defAction; // current defender action
	private final AttackerAction attAction; // current attacker action         
	private final GameState gameState; // next game state
	private final DefenderObservation defObservation; // next defender observation
	
	public GameSample() {
		this(0);
	}
	
	public GameSample(final int aTimeStep) {
		this.timeStep = aTimeStep;
		this.gameState = new GameState();
		this.defObservation = new DefenderObservation();
		this.defAction = new DefenderAction();
		this.attAction = new AttackerAction();
	}
	
	public GameSample(final int aTimeStep, final GameState aGameState, final DefenderObservation aDefObservation
		, final DefenderAction aDefAction, final AttackerAction aAttAction) {
		this.timeStep = aTimeStep;
		this.gameState = aGameState;
		this.defObservation = aDefObservation;
		this.defAction = aDefAction;
		this.attAction = aAttAction;
	}
	
	public int getTimeStep() {
		return this.timeStep;
	}
	
	public GameState getGameState() {
		return this.gameState;
	}
	
	public DefenderObservation getDefObservation() {
		return this.defObservation;
	}
	
	public DefenderAction getDefAction() {
		return this.defAction;
	}
	
	public AttackerAction getAttAction() {
		return this.attAction;
	}
	
	public void clear() {
		if (!this.gameState.getEnabledNodeSet().isEmpty()) {
			this.gameState.clear();
		}
		if (!this.defObservation.getAlertSet().isEmpty()) {
			this.defObservation.clear();
		}
		if (!this.defAction.getAction().isEmpty()) {
			this.defAction.clear();
		}
		if (!this.attAction.getAction().isEmpty()) {
			this.attAction.clear();
		}
	}
	
	public void print() {
		System.out.println(
			"-------------------------------------TIME STEP " + this.timeStep + "-------------------------------------------");
		this.defAction.print();
		this.attAction.print();
		this.gameState.print();
		this.defObservation.print();
		System.out.println("--------------------------------------------------------------------------------------------");
	}
}
