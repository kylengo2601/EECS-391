package edu.cwru.sepia.agent.planner;

import java.util.ArrayList;
import java.util.List;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.planner.actions.StripsAction;
import edu.cwru.sepia.util.Direction;

public class DepositKAction implements StripsAction {

	//
	// instance fields
	//

	private GameState parent;

	private int k;

	private List<Peasant> peasant = null;
	private List<Position> newPosition = null;

	private Position townHallPosition = null;

	private String stripsAction;
	private List<Action> sepiaAction = new ArrayList<Action>();

	//
	// constructor methods
	//

	public DepositKAction(List<Peasant> peasant, Position townHallPosition, GameState parent) {
		this.peasant = peasant;
		this.townHallPosition = townHallPosition;
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
	 * Check if peasants are adjacent to the townhall
	 * 
	 * @return true if peasant is adjacent, otherwise false
	 */
	@Override
	public boolean preconditionsMet(GameState state) {
		boolean preconditionMet = true;

		for (int i = 0; i < k; i++) {
			// if the peasant is not holding something or is not adjacent to the townhall
			if (peasant.get(i).getHoldingAmount() <= 0 || !isAdjacent(newPosition.get(i)))
				preconditionMet = false;
		}

		return preconditionMet;
	}

	@Override
	public GameState apply(GameState state) {
		// create the new GameState
		GameState newGameState = new GameState(state);

		// create the STRIPS action
		// DepositK(k, peasantID, townHallDirection)
		stripsAction = "Deposit" + k + "(";

		for (int i = 0; i < k; i++) {
			Peasant newPeasant = newGameState.findPeasant(peasant.get(i).getID(), newGameState.getPeasantUnits());

			// update the gold or wood amount
			int cargoAmount = newPeasant.getHoldingAmount();
			if (newPeasant.isHoldingGold())
				newGameState.addGold(cargoAmount);
			else if (newPeasant.isHoldingWood())
				newGameState.addWood(cargoAmount);

			// update the peasant
			newPeasant.clearHoldingAmount();
			
			if(i > 0) {
				stripsAction = stripsAction + ",";
			}
			stripsAction = stripsAction + newPeasant.getID() + "," + determineDirection(newPeasant.getxPos(),
					newPeasant.getyPos(), townHallPosition.x, townHallPosition.y);

			// create the SEPIA action
			// Action.createPrimitiveDeposit(int peasantId, Direction townhallDirection)
			sepiaAction.add(Action.createPrimitiveDeposit(newPeasant.getID(), determineDirection(newPeasant.getxPos(),
					newPeasant.getyPos(), townHallPosition.x, townHallPosition.y)));
		}

		stripsAction = stripsAction + ")";

		// update the cost
		// double cost = newUnit.getTemplate().getDurationDeposit();
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

	// creates the SEPIA Action from the strips action
	@Override
	public List<Action> createSEPIAaction() {
		return sepiaAction;
	}

	//
	// helper methods
	//

	// determines if the input position is adjacent to the townhall
	private boolean isAdjacent(Position newPosition) {
		// if the position is out of bounds
		if (newPosition.x < 0 || newPosition.x >= parent.getxExtent() || newPosition.y < 0
				|| newPosition.y >= parent.getyExtent())
			return false;
		// if the position is on a resource or townhall
		else if (parent.getMap()[newPosition.x][newPosition.y])
			return false;
		// if the position is not adjacent to the townhall
		else if (Math.abs(newPosition.x - townHallPosition.x) > 1 || Math.abs(newPosition.y - townHallPosition.y) > 1)
			return false;
		// otherwise
		else
			return true;
	}

	// determine the direction that the peasant must take to deposit
	private Direction determineDirection(int peasantX, int peasantY, int townhallX, int townhallY) {
		int x = townhallX - peasantX;
		int y = townhallY - peasantY;

		for (Direction d : Direction.values()) {
			if (x == d.xComponent() && y == d.yComponent()) {
				return d;
			}
		}
		return null;
	}
}