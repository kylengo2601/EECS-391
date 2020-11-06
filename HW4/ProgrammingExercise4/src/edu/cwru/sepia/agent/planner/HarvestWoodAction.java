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

public class HarvestWoodAction implements StripsAction {
	// instance fields
	UnitView peasant;
	Position newPosition;
	
	private String stripsAction;
	private Action sepiaAction;
	
	GameState parent;
	
	//creates a HarvestWoodAction by instantiating the following fields
	public HarvestWoodAction(UnitView peasant, Position newPosition, GameState parent) {
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
			if (state.getWoodMap()[newX][newY] > 0 && peasant.getCargoAmount() == 0) {
				return true;
			}
			return false;
		}
	}

	
	//applies the action and returns a new gameState
	@Override
	public GameState apply(GameState state) {
		GameState newGameState = new GameState(state);
		int newX = newPosition.x;
		int newY = newPosition.y;

		int woodRemaining = newGameState.getWoodMap()[newX][newY];

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
		if (woodRemaining < 100) {
			newGameState.getWoodMap()[newX][newY] = 0;
			newUnit.setCargo(ResourceType.WOOD, woodRemaining);
			newResource = new ResourceNode(ResourceNode.Type.TREE, newX, newY, 0, changedResource.getID());
		} 
		else {
			newGameState.getWoodMap()[newX][newY] -= 100;
			newUnit.setCargo(ResourceType.WOOD, 100);
			woodRemaining = woodRemaining - 100;
			newResource = new ResourceNode(ResourceNode.Type.TREE, newX, newY, woodRemaining, changedResource.getID());
		}
		newGameState.getPlayerUnits().remove(changedUnit);
		newGameState.getPlayerUnits().add(new UnitView(newUnit));

		newGameState.getResourceNodes().remove(changedResource);
		newGameState.getResourceNodes().add(new ResourceView(newResource));

		// create the STRIPS action
		stripsAction = "HarvestWood(" + changedUnit.getID() + "," + determineDirection(newUnit.getxPosition(),
				newUnit.getyPosition(), newX, newY) + ")";

		// create the SEPIA action
		sepiaAction = Action.createPrimitiveGather(changedUnit.getID(), determineDirection(newUnit.getxPosition(),
				newUnit.getyPosition(), newX, newY));

		
		
		// update cost
		double cost = 1;
		newGameState.addCost(cost);
		newGameState.heuristic();

		// update plan
		newGameState.addPlan(this);

		return newGameState;
	}
	
	//determines the direction that the peasant must take to harvest
	private Direction determineDirection(int peasantX, int peasantY, int treeX, int treeY) {
		int x = treeX - peasantX;
		int y = treeY - peasantY;
		
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