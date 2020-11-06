package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.planner.actions.StripsAction;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.ResourceNode.ResourceView;
import edu.cwru.sepia.environment.model.state.ResourceType;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import edu.cwru.sepia.util.Direction;
import edu.cwru.sepia.environment.model.state.UnitTemplate;

public class HarvestGoldAction implements StripsAction {
	// instance fields
	UnitView peasant;
	Position newPosition;

	private String stripsAction;
	private Action sepiaAction;
	
	GameState parent;
	
	//creates a HarvestGoldAction by instantiating the following fields
	public HarvestGoldAction(UnitView peasant, Position newPosition, GameState parent) {
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

		if (newX >= state.getxExtent() || newX < 0 || newY >= state.getyExtent() || newY < 0) {
			return false;
		}

		else {
			if (state.getGoldMap()[newX][newY] > 0 && peasant.getCargoAmount() == 0) {
				return true;
			}
			return false;
		}
	}

	@Override
	public GameState apply(GameState state) {
		GameState newGameState = new GameState(state);
		int newX = newPosition.x;
		int newY = newPosition.y;

		int goldRemaining = newGameState.getGoldMap()[newX][newY];

		//find the unit that is harvesting, update the value, and put the new unit into the list
		UnitView changedUnit = state.findUnit(peasant.getID(), newGameState.getPlayerUnits());
		UnitTemplate newTemplate = new UnitTemplate(changedUnit.getID());
		newTemplate.setCanGather(true);
		Unit newUnit = new Unit(newTemplate, changedUnit.getID());
		newUnit.setxPosition(peasant.getXPosition());
		newUnit.setyPosition(peasant.getYPosition());

		ResourceView changedResource = state.findResource(newX, newY, state.getResourceNodes());
		ResourceNode newResource;

		//if there is < 100 at a node, it should become 0, otherwise only decrease 100
		if (goldRemaining < 100) {
			newGameState.getGoldMap()[newX][newY] = 0;
			newUnit.setCargo(ResourceType.GOLD, goldRemaining);
			goldRemaining = 0;
			newResource = new ResourceNode(ResourceNode.Type.GOLD_MINE, newX, newY, goldRemaining, changedResource.getID());
		} else {
			newGameState.getGoldMap()[newX][newY] -= 100;
			newUnit.setCargo(ResourceType.GOLD, 100);
			goldRemaining -= 100;
			newResource = new ResourceNode(ResourceNode.Type.GOLD_MINE, newX, newY, goldRemaining,
					changedResource.getID());
		}
		newGameState.getPlayerUnits().remove(changedUnit);
		newGameState.getPlayerUnits().add(new UnitView(newUnit));

		newGameState.getResourceNodes().remove(changedResource);
		newGameState.getResourceNodes().add(new ResourceView(newResource));

		// create the STRIPS action
		stripsAction = "HarvestGold(" + changedUnit.getID() + "," + determineDirection(newUnit.getxPosition(),
				newUnit.getyPosition(), newX, newY) + ")";

		// create the SEPIA action
		sepiaAction = Action.createPrimitiveGather(changedUnit.getID(), determineDirection(newUnit.getxPosition(),
				newUnit.getyPosition(), newX, newY));
		
		
		// update the cost
		double cost = 1;
		newGameState.addCost(cost);
		newGameState.heuristic();

		// update the plan
		newGameState.addPlan(this);

		return newGameState;
	}
	
	//determines the direction that the peasant must take to harvest
	private Direction determineDirection(int peasantX, int peasantY, int goldMineX, int goldMineY) {
		int x = goldMineX - peasantX;
		int y = goldMineY - peasantY;
		
		for (Direction d : Direction.values()) {
			if(x == d.xComponent() && y == d.yComponent()) {
				return d;
			}
		}
		return null;
	}


	@Override
	public Action createSEPIAaction() {
		return sepiaAction;
	}
	
	@Override
	public String toString() {
		return stripsAction;
	}

}
