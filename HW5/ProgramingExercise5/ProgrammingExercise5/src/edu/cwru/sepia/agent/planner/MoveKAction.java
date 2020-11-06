package edu.cwru.sepia.agent.planner;

import java.util.ArrayList;
import java.util.List;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.planner.actions.StripsAction;

public class MoveKAction implements StripsAction {
	
	private Position originalPosition;
	private List<Peasant> peasants;
	private Position newPosition;

	private String stripsAction;
	private List<Action> sepiaActions;

	private GameState parent;
	private List<Position> availablePositions;

	public MoveKAction(List<Peasant> peasants, Position newPosition, GameState parent) {
		this.peasants = peasants;
		this.newPosition = newPosition;
		this.parent = parent;
		this.availablePositions = new ArrayList<Position>();
		this.sepiaActions = new ArrayList<Action>();
		stripsAction = "";
		this.originalPosition = peasants.get(0).getAdjacent();
	}

	@Override
	public boolean preconditionsMet(GameState state) {
		int newX = newPosition.x;
		int newY = newPosition.y;
		
		String toLocation = findLocationType(newPosition);
		
		if(toLocation.equals("Gold Mine")) {
			int resourceAmount = parent.getGoldMap()[newX][newY];
			
			if((int) ((resourceAmount - 1) / 100) + 1 < peasants.size()) {
				return false;
			}
			
			if((int) (((parent.getRequiredGold() - parent.getCurrentGold()) - 1) / 100) + 1 < peasants.size()) {
				return false;
			}
			
		}
		
		if(toLocation.equals("Tree")) {
			int resourceAmount = parent.getWoodMap()[newX][newY];
			
			if((int) ((resourceAmount - 1) / 100) + 1 < peasants.size()) {
				return false;
			}
			
			if((int) (((parent.getRequiredWood() - parent.getCurrentWood()) - 1) / 100) + 1 < peasants.size()) {
				return false;
			}
			
		}
		
		Position firstPosition = peasants.get(0).getAdjacent();
		for (Peasant peasant : peasants) {
			if (!peasant.getAdjacent().equals(firstPosition)) {
				return false;
			}
		}

		List<Position> totalPositions = findSurroundingPositions(newX, newY);
		for (Position position : totalPositions) {
			if (!state.getMap()[position.x][position.y] && !(position.x >= state.getxExtent() || position.x < 0
					|| position.y >= state.getyExtent() || position.y < 0)) {
				boolean collision = false;
				for(Peasant peasant : state.getPeasantUnits()) {
					if(peasant.getxPos() == position.x && peasant.getyPos() == position.y) {
						collision = true;
					}
				}
				if(!collision) {
					availablePositions.add(position);
				}
			}
			
		}

		if (availablePositions.size() > peasants.size()) {
			return true;
		} else {
			return false;
		}
	}

	private List<Position> findSurroundingPositions(int xPos, int yPos) {
		List<Position> positions = new ArrayList<>();

		for (int i = xPos - 1; i <= xPos + 1; i++) {
			for (int j = yPos - 1; j <= yPos + 1; j++) {
				positions.add(new Position(i, j));
			}
		}
		return positions;

	}

	@Override
	public GameState apply(GameState state) {
		GameState newGameState = new GameState(state);
		int newX = newPosition.x;
		int newY = newPosition.y;
		int worstChebyshev = 0;

		for (Peasant peasant : peasants) {
			// find the best available position that the peasant can go to near its
			// destination
			Position bestPosition = findBestPosition(peasant);
			int currentChebyshev = bestPosition.chebyshevDistance(new Position(peasant.getxPos(), peasant.getyPos()));
			if (currentChebyshev > worstChebyshev) {
				worstChebyshev = currentChebyshev;
			}

			Peasant newPeasant = newGameState.findPeasant(peasant.getID(), newGameState.getPeasantUnits());

			// update the peasant list in the new game state
			newPeasant.setxPos(bestPosition.x);
			newPeasant.setyPos(bestPosition.y);

			newPeasant.setAdjacent(newPosition);

			// remove the spot from the list of available positions
			availablePositions.remove(bestPosition);

			// create SEPIA action
			sepiaActions.add(Action.createCompoundMove(peasant.getID(), bestPosition.x, bestPosition.y));
		}

		// update costs
		newGameState.addCost(worstChebyshev);

		// create STRIPS action
		// MoveK(from, to)
		String fromLocation = findLocationType(originalPosition);
		String toLocation = findLocationType(newPosition);

		stripsAction = "Move" + peasants.size() + "(" + fromLocation + "," + toLocation + ")";

		// update the existing plan
		newGameState.heuristic();
		newGameState.addPlan(this);

		return newGameState;
	}

	private Position findBestPosition(Peasant peasant) {
		Position peasantPosition = new Position(peasant.getxPos(), peasant.getyPos());
		Position bestPosition = null;
		double bestChebyshev = 0.0;

		for (Position position : availablePositions) {
			if (bestPosition == null) {
				bestPosition = position;
				bestChebyshev = peasantPosition.chebyshevDistance(position);
			} else {
				int currentChebyshev = peasantPosition.chebyshevDistance(position);
				if (currentChebyshev < bestChebyshev) {
					bestPosition = position;
					bestChebyshev = currentChebyshev;
				}
			}
		}

		return bestPosition;
	}

	private String findLocationType(Position position) {
		String locationType = "";
		if (parent.getTownHall().getXPosition() == position.x
				&& parent.getTownHall().getYPosition() == position.y) {
			locationType = "Townhall";
		} else if (parent.getGoldMap()[position.x][position.y] != 0) {
			locationType = "Gold Mine";
		} else if (parent.getWoodMap()[position.x][position.y] != 0) {
			locationType = "Tree";
		}

		return locationType;
	}

	@Override
	public GameState getParent() {
		return this.parent;
	}

	@Override
	public List<Action> createSEPIAaction() {
		return sepiaActions;
	}
	
	@Override
	public String toString() {
		return stripsAction;
	}
}