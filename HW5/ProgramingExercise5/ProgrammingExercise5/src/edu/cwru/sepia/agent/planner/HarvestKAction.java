package edu.cwru.sepia.agent.planner;

import java.util.ArrayList;
import java.util.List;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.planner.actions.StripsAction;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.ResourceNode.ResourceView;
import edu.cwru.sepia.util.Direction;

public class HarvestKAction implements StripsAction {

	//
	// instance fields
	//

	private GameState parent;

	private int k;

	private List<Peasant> peasant;
	private List<Position> newPosition;

	private Position resourcePosition;

	private String stripsAction;
	private List<Action> sepiaAction = new ArrayList<Action>();

	//
	// constructor methods
	//

	// creates a HarvestGoldAction by instantiating the following fields
	public HarvestKAction(List<Peasant> peasant, Position resourcePosition, GameState parent) {
		this.peasant = peasant;
		this.resourcePosition = resourcePosition;
		this.parent = parent;

		// get number of parallel moves
		this.k = peasant.size();
		
		this.newPosition = new ArrayList<Position>();
		// get peasant location
		for (Peasant p : peasant) {
			newPosition.add(new Position(p.getxPos(), p.getyPos()));
		}
	}

	//
	// inherited methods
	//

	/**
	 * Check if peasants are adjacent to the resource node
	 * 
	 * @return true if peasant is adjacent, otherwise false
	 */
	@Override
	public boolean preconditionsMet(GameState state) {
		boolean preconditionMet = true;

		try {
			// if there is no resource at the position
			if ((int)((state.getGoldMap()[resourcePosition.x][resourcePosition.y] - 1) / 100) + 1 < peasant.size()
					&& (int) (state.getWoodMap()[resourcePosition.x][resourcePosition.y] - 1) % 100 + 1 < peasant.size())
				preconditionMet = false;

			for (int i = 0; i < k; i++) {
				// if the peasant is holding something or is not adjacent to the resource
				if (peasant.get(i).getHoldingAmount() != 0 || !isAdjacent(newPosition.get(i)))
					preconditionMet = false;
			}
		} catch (Exception e) {
			return false;
		}

		return preconditionMet;
	}

	@Override
	public GameState apply(GameState state) {
		// create the new GameState
		GameState newGameState = new GameState(state);

		// create the STRIPS action
		// HarvestK(k, peasantID, resourceDirection)
		stripsAction = "Harvest" + k + "(";

		for (int i = 0; i < k; i++) {
			Peasant newPeasant = newGameState.findPeasant(peasant.get(i).getID(), newGameState.getPeasantUnits());

			// update the resource node
			int newX = resourcePosition.x;
			int newY = resourcePosition.y;

			int resourceRemaining = Math.max(newGameState.getGoldMap()[newX][newY],
					newGameState.getWoodMap()[newX][newY]);

			ResourceView changedResource = state.findResource(newX, newY, state.getResourceNodes());
			ResourceNode newResource = null;

			// if there is < 100 remaining at the resource node
			if (resourceRemaining <= 100) {
				if (changedResource.getType() == ResourceNode.Type.GOLD_MINE) {
					// update resource maps in state
					newGameState.getMap()[newX][newY] = false;
					newGameState.getGoldMap()[newX][newY] = -1;

					// update peasant
					newPeasant.setHoldingAmount(resourceRemaining);
					newPeasant.setHoldingGold(true);

					// create new resource node
					resourceRemaining = 0;
					newResource = new ResourceNode(ResourceNode.Type.GOLD_MINE, newX, newY, resourceRemaining,
							changedResource.getID());
				} else if (changedResource.getType() == ResourceNode.Type.TREE) {
					// update resource maps in state
					newGameState.getMap()[newX][newY] = false;
					newGameState.getWoodMap()[newX][newY] = -1;

					// update peasant
					newPeasant.setHoldingAmount(resourceRemaining);
					newPeasant.setHoldingWood(true);

					// create new resource node
					resourceRemaining = 0;
					newResource = new ResourceNode(ResourceNode.Type.TREE, newX, newY, resourceRemaining,
							changedResource.getID());
				}
			}
			// if there is >= 100 remaining at the resource node
			else {
				if (changedResource.getType() == ResourceNode.Type.GOLD_MINE) {
					// update resource maps in state
					newGameState.getGoldMap()[newX][newY] -= 100;

					// update peasant
					newPeasant.setHoldingAmount(100);
					newPeasant.setHoldingGold(true);

					// create new resource node
					resourceRemaining -= 100;
					newResource = new ResourceNode(ResourceNode.Type.GOLD_MINE, newX, newY, resourceRemaining,
							changedResource.getID());
				} else if (changedResource.getType() == ResourceNode.Type.TREE) {
					// update resource maps in state
					newGameState.getWoodMap()[newX][newY] -= 100;

					// update peasant
					newPeasant.setHoldingAmount(100);
					newPeasant.setHoldingWood(true);

					// create new resource node
					resourceRemaining -= 100;
					newResource = new ResourceNode(ResourceNode.Type.TREE, newX, newY, resourceRemaining,
							changedResource.getID());
				}
			}

			newGameState.getResourceNodes().remove(changedResource);
			newGameState.getResourceNodes().add(new ResourceView(newResource));
			
			if(i > 0) {
				stripsAction = stripsAction + ",";
			}
			
			stripsAction = stripsAction + newPeasant.getID() + ","
					+ determineDirection(newPeasant.getxPos(), newPeasant.getyPos(), newX, newY);

			// create the SEPIA action
			// Action.createPrimitiveGather(int peasantId, Direction resourceDirection)
			sepiaAction.add(Action.createPrimitiveGather(newPeasant.getID(),
					determineDirection(newPeasant.getxPos(), newPeasant.getyPos(), newX, newY)));
		}

		stripsAction = stripsAction + ")";

		// update the cost and heuristic
		double cost = 1;
		newGameState.addCost(cost);
		newGameState.heuristic();

		// update the plan
		newGameState.addPlan(this);

		return newGameState;
	}

	@Override
	public GameState getParent() {
		return this.parent;
	}

	@Override
	public String toString() {
		return stripsAction;
	}

	@Override
	public List<Action> createSEPIAaction() {
		return sepiaAction;

	}

	//
	// helper methods
	//

	// determines if the input position is adjacent to the resource
	private boolean isAdjacent(Position newPosition) {
		// if the position is out of bounds
		if (newPosition.x < 0 || newPosition.x >= parent.getxExtent() || newPosition.y < 0
				|| newPosition.y >= parent.getyExtent())
			return false;
		// if the position is on a resource or townhall
		else if (parent.getMap()[newPosition.x][newPosition.y])
			return false;
		// if the position is not adjacent to the resource
		else if (Math.abs(newPosition.x - resourcePosition.x) > 1 || Math.abs(newPosition.y - resourcePosition.y) > 1)
			return false;
		// otherwise
		else
			return true;
	}

	// determines the direction that the peasant must take to harvest
	private Direction determineDirection(int peasantX, int peasantY, int resourceX, int resourceY) {
		int x = resourceX - peasantX;
		int y = resourceY - peasantY;

		for (Direction d : Direction.values()) {
			// if the direction matches
			if (x == d.xComponent() && y == d.yComponent())
				return d;
		}

		// otherwise
		return null;
	}
}