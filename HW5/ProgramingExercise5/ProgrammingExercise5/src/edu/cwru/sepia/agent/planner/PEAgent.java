package edu.cwru.sepia.agent.planner;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionFeedback;
import edu.cwru.sepia.action.ActionResult;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.agent.planner.actions.StripsAction;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Template;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.util.Direction;

/**
 * This is an outline of the PEAgent. Implement the provided methods. You may
 * add your own methods and members.
 */
public class PEAgent extends Agent {

	// execution
	private long totalExecutionTime = 0;

	// The plan being executed
	private Stack<StripsAction> plan = null;

	// maps the real unit Ids to the plan's unit ids
	// when you're planning you won't know the true unit IDs that sepia assigns. So
	// you'll use placeholders (1, 2, 3).
	// this maps those placeholders to the actual unit IDs.
	private Map<Integer, Integer> peasantIdMap;
	private int townhallId;
	private int peasantTemplateId;

	public PEAgent(int playernum, Stack<StripsAction> plan) {
		super(playernum);
		peasantIdMap = new HashMap<Integer, Integer>();
		this.plan = plan;

	}

	public Stack<StripsAction> getPlan() {
		return this.plan;
	}

	@Override
	public Map<Integer, Action> initialStep(State.StateView stateView, History.HistoryView historyView) {
		// gets the townhall ID and the peasant ID
		for (int unitId : stateView.getUnitIds(playernum)) {
			Unit.UnitView unit = stateView.getUnit(unitId);
			String unitType = unit.getTemplateView().getName().toLowerCase();
			if (unitType.equals("townhall")) {
				townhallId = unitId;
			} else if (unitType.equals("peasant")) {
				peasantIdMap.put(unitId, unitId);
			}
		}

		// Gets the peasant template ID. This is used when building a new peasant with
		// the townhall
		for (Template.TemplateView templateView : stateView.getTemplates(playernum)) {
			if (templateView.getName().toLowerCase().equals("peasant")) {
				peasantTemplateId = templateView.getID();
				break;
			}
		}

		return middleStep(stateView, historyView);
	}

	int iterator = 0;

	/**
	 * This is where you will read the provided plan and execute it. If your plan is
	 * correct then when the plan is empty the scenario should end with a victory.
	 * If the scenario keeps running after you run out of actions to execute then
	 * either your plan is incorrect or your execution of the plan has a bug.
	 *
	 * You can create a SEPIA deposit action with the following method
	 * Action.createPrimitiveDeposit(int peasantId, Direction townhallDirection)
	 *
	 * You can create a SEPIA harvest action with the following method
	 * Action.createPrimitiveGather(int peasantId, Direction resourceDirection)
	 *
	 * You can create a SEPIA build action with the following method
	 * Action.createPrimitiveProduction(int townhallId, int peasantTemplateId)
	 *
	 * You can create a SEPIA move action with the following method
	 * Action.createCompoundMove(int peasantId, int x, int y)
	 *
	 * these actions are stored in a mapping between the peasant unit ID executing
	 * the action and the action you created.
	 *
	 * For the compound actions you will need to check their progress and wait until
	 * they are complete before issuing another action for that unit. If you issue
	 * an action before the compound action is complete then the peasant will stop
	 * what it was doing and begin executing the new action.
	 *
	 * To check an action's progress you can use the historyview object. Here is a
	 * short example. if (stateView.getTurnNumber() != 0) { Map<Integer,
	 * ActionResult> actionResults = historyView.getCommandFeedback(playernum,
	 * stateView.getTurnNumber() - 1); for (ActionResult result :
	 * actionResults.values()) { <stuff> } } Also remember to check your plan's
	 * preconditions before executing!
	 */
	@Override
	public Map<Integer, Action> middleStep(State.StateView stateView, History.HistoryView historyView) {
		long startTime = System.nanoTime();

		Map<Integer, Action> actionList = new HashMap<Integer, Action>();

		// checks to see if there are actions that have not been completed in the
		// history view
		if (stateView.getTurnNumber() != 0) {
			Map<Integer, ActionResult> previousActions = historyView.getCommandFeedback(playernum,
					stateView.getTurnNumber() - 1);
			for (ActionResult previousAction : previousActions.values()) {
				if (previousAction.getFeedback() == ActionFeedback.FAILED) {
					actionList.put(previousAction.getAction().getUnitId(), previousAction.getAction());
				}

				else if (previousAction.getFeedback() == ActionFeedback.INCOMPLETE) {
					totalExecutionTime += System.nanoTime() - startTime;
					return actionList;
				}
			}
		}

		// if all previous actions have been completed, execute the next action in the
		// plan
		if (actionList.isEmpty()) {
			StripsAction stripsAction = plan.pop();
			if (iterator >= 30) {
				// System.out.println("At iteration: " + iterator);
			}
			if (stripsAction.preconditionsMet(stripsAction.getParent())) {
				Map<Integer, Integer> idlePeasants = new HashMap<Integer, Integer>();
				for (Integer peasantId : peasantIdMap.keySet()) {
					idlePeasants.put(peasantId, peasantId);
				}

				List<Action> sepiaActions = createSepiaAction(stripsAction);
				for (Action action : sepiaActions) {
					actionList.put(action.getUnitId(), action);

					// remove active peasant
					idlePeasants.remove(action.getUnitId());
				}

				// call look ahead
				// lookAhead(stripsAction, actionList, idlePeasants);
			}
			iterator++;
		}

		totalExecutionTime += System.nanoTime() - startTime;
		return actionList;
	}

	private void lookAhead(StripsAction stripsAction, Map<Integer, Action> actionList,
			Map<Integer, Integer> idlePeasants) {
		List<Action> actions = stripsAction.createSEPIAaction();
		if (actions.get(0).getType() != ActionType.PRIMITIVEPRODUCE) {
			Stack<StripsAction> temp = new Stack<StripsAction>();

			StripsAction next = null;
			while (!plan.isEmpty() && !checkValidParallel((next = plan.pop()), idlePeasants))
				temp.push(next);

			if (!plan.isEmpty() && next != null) {
				List<Action> nextActions = next.createSEPIAaction();
				for (Action action : nextActions) {
					actionList.put(action.getUnitId(), action);
				}
			}

			while (!temp.isEmpty())
				plan.push(temp.pop());
		}
	}

	private boolean checkValidParallel(StripsAction stripsAction, Map<Integer, Integer> idlePeasants) {
		boolean isValid = true;

		if (stripsAction.preconditionsMet(stripsAction.getParent())) {
			List<Action> actions = stripsAction.createSEPIAaction();
			for (Action action : actions) {
				if (!idlePeasants.containsKey(action.getUnitId()))
					isValid = false;
			}
		}

		return isValid;
	}

	private Direction determineDirection(String direction) {
		Direction d = null;

		if (direction.equals("NORTH"))
			d = Direction.NORTH;
		else if (direction.equals("NORTHEAST"))
			d = Direction.NORTHEAST;
		else if (direction.equals("EAST"))
			d = Direction.EAST;
		else if (direction.equals("SOUTHEAST"))
			d = Direction.SOUTHEAST;
		else if (direction.equals("SOUTH"))
			d = Direction.SOUTH;
		else if (direction.equals("SOUTHWEST"))
			d = Direction.SOUTHWEST;
		else if (direction.equals("WEST"))
			d = Direction.WEST;
		else if (direction.equals("NORTHWEST"))
			d = Direction.NORTHWEST;

		return d;
	}

	/**
	 * Returns a SEPIA version of the specified Strips Action.
	 * 
	 * @param action
	 *            StripsAction
	 * @return SEPIA representation of same action
	 */
	private List<Action> createSepiaAction(StripsAction action) {
		return action.createSEPIAaction();
	}

	public long getTotalExecutionTime() {
		return totalExecutionTime;
	}

	@Override
	public void terminalStep(State.StateView stateView, History.HistoryView historyView) {
	}

	@Override
	public void savePlayerData(OutputStream outputStream) {

	}

	@Override
	public void loadPlayerData(InputStream inputStream) {

	}
}