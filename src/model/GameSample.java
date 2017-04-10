package model;
// The initial state is not saved since it is empty by default
public class GameSample {
	int timeStep;
	
	DefenderAction defAction; // current defender action
	AttackerAction attAction; // current attacker action         
	GameState gameState; // next game state
	DefenderObservation defObservation; // next defender observation
	
	public GameSample()
	{
		this(0);
	}
	public GameSample(int timeStep)
	{
		this.timeStep = timeStep;
		this.gameState = new GameState();
		this.defObservation = new DefenderObservation();
		this.defAction = new DefenderAction();
		this.attAction = new AttackerAction();
	}
	public GameSample(int timeStep, GameState gameState, DefenderObservation defObservation
			, DefenderAction defAction, AttackerAction attAction)
	{
		this.timeStep = timeStep;
		this.gameState = gameState;
		this.defObservation = defObservation;
		this.defAction = defAction;
		this.attAction = attAction;
	}
	public int getTimeStep()
	{
		return this.timeStep;
	}
	public GameState getGameState()
	{
		return gameState;
	}
	public DefenderObservation getDefObservation()
	{
		return this.defObservation;
	}
	public DefenderAction getDefAction()
	{
		return this.defAction;
	}
	public AttackerAction getAttAction()
	{
		return attAction;
	}
	public void clear()
	{
		if(!this.gameState.getEnabledNodeSet().isEmpty())
			this.gameState.clear();
		if(!this.defObservation.getAlertSet().isEmpty())
			this.defObservation.clear();
		if(!this.defAction.getAction().isEmpty())
			this.defAction.clear();
		if(!this.attAction.getAction().isEmpty())
			this.attAction.clear();
	}
	public void print() {
		// TODO Auto-generated method stub
		System.out.println("-------------------------------------TIME STEP " + this.timeStep + "-------------------------------------------");
		this.defAction.print();
		this.attAction.print();
		this.gameState.print();
		this.defObservation.print();
		System.out.println("--------------------------------------------------------------------------------------------");
	}
}
