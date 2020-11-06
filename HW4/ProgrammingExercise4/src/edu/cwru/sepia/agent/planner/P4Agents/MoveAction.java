package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.planner.actions.StripsAction;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import edu.cwru.sepia.environment.model.state.UnitTemplate;

public class MoveAction implements StripsAction {
	// instance fields
	UnitView peasant;
	Position newPosition;
	
	private String stripsAction;
	private Action sepiaAction;
	
	GameState parent;

	public MoveAction(UnitView peasant, Position newPosition, GameState parent) {
		this.peasant = peasant;
		this.newPosition = newPosition;
		this.parent = parent;
	}
	
	public GameState getParent() {
		return this.parent;
	}

	//checks to see if the action can be executed on the state
	@Override
	public boolean preconditionsMet(GameState state) {

		int newX = newPosition.x;
		int newY = newPosition.y;

		if (newX >= state.getxExtent() || newX < 0 || newY >= state.getyExtent() || newY < 0
				|| state.getMap()[newX][newY]) {
			return false;
		} else {
			for (UnitView unit : state.getPlayerUnits()) {
				if (newX == unit.getXPosition() && newY == unit.getYPosition()) {
					return false;
				}
			}
		}
		return true;

	}

	//applies the action and returns a new gameState
	@Override
	public GameState apply(GameState state) {
		GameState newGameState = new GameState(state);
		int newX = newPosition.x;
		int newY = newPosition.y;

		//find the unit that is moving, update the value, and put the new unit into the list
		UnitView changedUnit = state.findUnit(peasant.getID(), newGameState.getPlayerUnits());
		UnitTemplate newTemplate = new UnitTemplate(changedUnit.getID());
		newTemplate.setCanGather(true);
		Unit newUnit = new Unit(newTemplate, changedUnit.getID());
		newUnit.setxPosition(newX);
		newUnit.setyPosition(newY);
		if (peasant.getCargoAmount() > 0) {
			newUnit.setCargo(peasant.getCargoType(), peasant.getCargoAmount());
		}
		newGameState.getPlayerUnits().remove(changedUnit);
		newGameState.getPlayerUnits().add(new UnitView(newUnit));

		//create the STRIPS action
		stripsAction = "Move(" + changedUnit.getID() + "," + newX + "," + newY + ")";
		
		//create the SEPIA action
		sepiaAction = Action.createCompoundMove(changedUnit.getID(), newX, newY);
		
		// update cost 
		Position peasantPos = new Position(peasant.getXPosition(), peasant.getYPosition());
		double cost = peasantPos.chebyshevDistance(newPosition);
		newGameState.addCost(cost);
		newGameState.heuristic();

		// update plan
		newGameState.addPlan(this);

		return newGameState;
	}

	//converts strips action into sepia action
	@Override
	public Action createSEPIAaction() {
		return sepiaAction;
	}
	
	@Override
	public String toString() {
		return stripsAction;
	}

}