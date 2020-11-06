package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.planner.actions.StripsAction;
import edu.cwru.sepia.environment.model.state.ResourceType;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import edu.cwru.sepia.util.Direction;
import edu.cwru.sepia.environment.model.state.UnitTemplate;

public class DepositWoodAction implements StripsAction {
	//instance fields
	Position townHallPos = null;
	Position newPos = null;
	UnitView peasant = null;
	
	private String stripsAction;
	private Action sepiaAction;
	
	GameState parent;

	//creates a DepositWoodAction by setting the given fields
	public DepositWoodAction(UnitView peasant, Position newPos, Position townHallPos, GameState parent) {
		this.peasant = peasant;
		this.newPos = newPos;
		this.townHallPos = townHallPos;
		this.parent = parent;
	}
	
	//getter method
	public GameState getParent() {
		return this.parent;
	}

	// checks if the given action can be performed on the state
	@Override
	public boolean preconditionsMet(GameState state) {
		// check if peasant new Position is at townHallPosition
		if (hasWood() && newPos.equals(townHallPos)) {
			return true;
		}
		return false;
	}

	// checks if the peasant holding wood
	public boolean hasWood() {
		if (peasant.getCargoAmount() > 0 && peasant.getCargoType() == ResourceType.WOOD) {
			return true;
		}
		return false;
	}

	// returns a new game state that is the result of applying the action
	@Override
	public GameState apply(GameState state) {
		
		//create the new gameState
		GameState newGameState = new GameState(state);
		
		//find the unit that is harvesting, update the value, and put the new unit into the list
		UnitView changedUnit = newGameState.findUnit(peasant.getID(), newGameState.getPlayerUnits());
		Unit newUnit = new Unit(new UnitTemplate(changedUnit.getID()), changedUnit.getID());
		newUnit.setxPosition(peasant.getXPosition());
		newUnit.setyPosition(peasant.getYPosition());
		newUnit.clearCargo();
		newGameState.getPlayerUnits().remove(changedUnit);
		newGameState.getPlayerUnits().add(new UnitView(newUnit));

		newGameState.addWood(peasant.getCargoAmount());

		// create the STRIPS action
		// DepositWood(peasantID, townHallDirection)
		stripsAction = "DepositWood(" + changedUnit.getID() + "," + determineDirection(newUnit.getxPosition(),
				newUnit.getyPosition(), townHallPos.x, townHallPos.y) + ")";

		// create the SEPIA action
		// Action.createPrimitiveDeposit(int peasantId, Direction townhallDirection)
		sepiaAction = Action.createPrimitiveDeposit(changedUnit.getID(), determineDirection(newUnit.getxPosition(),
				newUnit.getyPosition(), townHallPos.x, townHallPos.y));

		// update the cost
		//double cost = newUnit.getTemplate().getDurationDeposit();
		double cost = 1;
		newGameState.addCost(cost);
		newGameState.heuristic();

		// update the plan
		newGameState.addPlan(this);

		return newGameState;

	}
	
	//determine which direction the peasant must move to deposit something to the townhall
	private Direction determineDirection(int peasantX, int peasantY, int townhallX, int townhallY) {
		int x = townhallX - peasantX;
		int y = townhallY - peasantY;
		
		for (Direction d : Direction.values()) {
			if(x == d.xComponent() && y == d.yComponent()) {
				return d;
			}
		}
		return null;
	}

	//creates the SEPIA Action from the strips action
	@Override
	public Action createSEPIAaction() {
		return sepiaAction;
	}
	
	@Override
	public String toString() {
		return stripsAction;
	}

}