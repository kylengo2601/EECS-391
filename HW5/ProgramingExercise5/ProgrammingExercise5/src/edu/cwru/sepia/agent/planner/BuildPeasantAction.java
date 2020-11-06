package edu.cwru.sepia.agent.planner;

import java.util.ArrayList;
import java.util.List;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.planner.actions.StripsAction;
import edu.cwru.sepia.environment.model.state.Template.TemplateView;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import edu.cwru.sepia.environment.model.state.UnitTemplate;
import edu.cwru.sepia.util.Direction;

public class BuildPeasantAction implements StripsAction {

	//
	// instance fields
	//

	private GameState parent;

	private int currentGold;
	private int currentFood;

	private int peasantGoldCost;
	private int peasantFoodCost;
	private int peasantLimit;

	private Peasant peasant;
	private int newX;
	private int newY;

	private String stripsAction;
	private List<Action> sepiaAction = new ArrayList<Action>();

	//
	// constructor methods
	//

	public BuildPeasantAction(GameState parent) {
		this.parent = parent;
	}

	//
	// inherited methods
	//

	// checks to see if the action can be executed on the state
	@Override
	public boolean preconditionsMet(GameState state) {
		// get current amount of resources
		currentGold = state.getCurrentGold();
		currentFood = state.getCurrentFood();

		// get resource cost of building peasant
		peasantGoldCost = state.getState().getTemplate(state.getPlayerNum(), "Peasant").getGoldCost();
		peasantFoodCost = state.getState().getTemplate(state.getPlayerNum(), "Peasant").getFoodCost();
		peasantLimit = state.getState().getSupplyCap(state.getPlayerNum());

		// check if the resources are enough to build a peasant
		if (state.isBuildPeasants() && currentGold >= peasantGoldCost && currentFood < peasantLimit) {
			// check if there are empty positions around townhall to place peasant
			int townHallXPos = state.getTownHall().getXPosition();
			int townHallYPos = state.getTownHall().getYPosition();

			for (Direction d : Direction.values()) {
				newX = townHallXPos + d.xComponent();
				newY = townHallYPos + d.yComponent();
				if (legalPosition(state, newX, newY)) {
					return true;
				}
			}
		}

		return false;
	}

	// applies the action and returns a new gameState
	@Override
	public GameState apply(GameState state) {
		GameState newGameState = new GameState(state);

		// get townHall position
		Position townHallPosition = new Position(state.getTownHall().getXPosition(),
				state.getTownHall().getYPosition());

		// create new peasant
		TemplateView peasantTemplate = newGameState.getState().getTemplate(newGameState.getPlayerNum(), "Peasant");
		int peasantTemplateID = peasantTemplate.getID();
		Unit peasantUnit = new Unit(new UnitTemplate(peasantTemplateID), peasantTemplateID);
		UnitView peasantUnitView = new UnitView(peasantUnit);
		
		//peasant = new Peasant(10, newX, newY, false, false, 0, townHallPosition);
		
		
		if (newGameState.getPeasantUnits().size() == 1) {
			peasant = new Peasant(10, newX, newY, false, false, 0, townHallPosition);
		} else if (newGameState.getPeasantUnits().size() == 2) {
			peasant = new Peasant(11, newX, newY, false, false, 0, townHallPosition);
		}

		newGameState.getPeasantUnits().add(peasant);
		newGameState.getPlayerUnits().add(peasantUnitView);
		newGameState.getAllUnits().add(peasantUnitView);

		// decrement current gold after building peasant
		newGameState.addGold(-(peasantGoldCost));

		// increment current food after building peasant
		newGameState.addFood(peasantFoodCost);

		// set STRIPS command
		// Action.createPrimitiveProduction(int townhallId, int peasantTemplateId)
		stripsAction = "BuildPeasant(" + state.getTownHall().getID() + "," + peasantTemplateID + ")";
		sepiaAction.add(Action.createPrimitiveProduction(state.getTownHall().getID(), peasantTemplateID));

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

	// check if the position is legal to place peasant
	private boolean legalPosition(GameState state, int x, int y) {
		// if the position exceeds the border
		if (x < 0 || x >= state.getxExtent() || y < 0 || y >= state.getyExtent())
			return false;
		// if there is a townhall or resource on the position
		else if (state.getMap()[x][y])
			return false;
		// if there is a unit on the position
		else {
			for (UnitView unit : state.getPlayerUnits()) {
				if (x == unit.getXPosition() && y == unit.getYPosition())
					return false;
			}
		}

		// otherwise
		return true;
	}
}