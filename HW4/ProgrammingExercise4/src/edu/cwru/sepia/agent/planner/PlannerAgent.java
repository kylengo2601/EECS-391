package edu.cwru.sepia.agent.planner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Stack;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.agent.planner.actions.StripsAction;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.State;

/**
 * Created by Devin on 3/15/15.
 */
public class PlannerAgent extends Agent {

	final int requiredWood;
	final int requiredGold;
	final boolean buildPeasants;
	
	private int planSize;

	// Your PEAgent implementation. This prevents you from having to parse the text
	// file representation of your plan.
	PEAgent peAgent;

	public PlannerAgent(int playernum, String[] params) {
		super(playernum);

		if (params.length < 3) {
			System.err.println(
					"You must specify the required wood and gold amounts and whether peasants should be built");
		}

		requiredWood = Integer.parseInt(params[0]);
		requiredGold = Integer.parseInt(params[1]);
		buildPeasants = Boolean.parseBoolean(params[2]);

		System.out.println("required wood: " + requiredWood + " required gold: " + requiredGold + " build Peasants: "
				+ buildPeasants);
	}

	@Override
	public Map<Integer, Action> initialStep(State.StateView stateView, History.HistoryView historyView) {

		Stack<StripsAction> plan = AstarSearch(
				new GameState(stateView, playernum, requiredGold, requiredWood, buildPeasants));

		if (plan == null) {
			System.err.println("No plan was found");
			System.exit(1);
			return null;
		}
		planSize = plan.size();

		// write the plan to a text file
		savePlan(plan);

		// Instantiates the PEAgent with the specified plan.
		peAgent = new PEAgent(playernum, plan);

		return peAgent.initialStep(stateView, historyView);
	}

	@Override
	public Map<Integer, Action> middleStep(State.StateView stateView, History.HistoryView historyView) {
		if (peAgent == null) {
			System.err.println("Planning failed. No PEAgent initialized.");
			return null;
		}
		
		//execute the peAgent's middleStep until there are no actions left in the plan
		if(peAgent.getPlan().size() > 0) {
			return peAgent.middleStep(stateView, historyView);
		}
		else {
			return new HashMap<>();
		}
	}

	@Override
	public void terminalStep(State.StateView stateView, History.HistoryView historyView) {
		System.out.println("Success");
		System.out.println("Number of steps: " + planSize);
	}

	@Override
	public void savePlayerData(OutputStream outputStream) {

	}

	@Override
	public void loadPlayerData(InputStream inputStream) {

	}

	/**
	 * Perform an A* search of the game graph. This should return your plan as a
	 * stack of actions. This is essentially the same as your first assignment. The
	 * implementations should be very similar. The difference being that your nodes
	 * are now GameState objects not MapLocation objects.
	 *
	 * @param startState
	 *            The state which is being planned from
	 * @return The plan or null if no plan is found.
	 */
	private Stack<StripsAction> AstarSearch(GameState startState) {

		// create an empty stack containing the plan
		Stack<StripsAction> plan = new Stack<>();

		// initialize open list
		PriorityQueue<GameState> openList = new PriorityQueue<>(100);
		Hashtable<GameState, Double> openListHash = new Hashtable<>();

		// initialize closed list
		Hashtable<GameState, Double> closedList = new Hashtable<>();

		// put starting node in open list
		openList.add(startState);
		openListHash.put(startState, 0.0);

		// while the open list is not empty
		while (openList.size() > 0) {
			// find the node with the lowest A* value
			GameState q = openList.poll();
			openListHash.remove(q);
			
			// generate all the successors
			List<GameState> children = q.generateChildren();

			// for each children
			for (GameState child : children) {
				
				// if the child is a goal state
				if (child.isGoal()) {
					List<StripsAction> childPlan = child.getPlan();
					for (int i = childPlan.size() - 1; i >= 0; i--) {
						plan.push(childPlan.get(i));
					}

					return plan;
				}
				// if the child already exists in the open list
				else if (openListHash.containsKey(child)) {
					GameState openListVersion = null;

					// find the open list's version
					for (GameState key : openListHash.keySet()) {
						if (key.equals(child)) {
							openListVersion = key;
						}
					}

					// if the open list's version has a higher A* value
					if (openListVersion.compareTo(child) > 0) {
						// if the child already exists in the closed list
						if (closedList.containsKey(child)) {
							GameState closedListVersion = null;

							// find the closed list's version
							for (GameState key : closedList.keySet()) {
								if (key.equals(child)) {
									closedListVersion = key;
								}
							}

							// if the closed list's version has a higher A* value
							if (closedListVersion.compareTo(child) > 0) {
								// delete the open list's version
								openList.remove(openListVersion);
								openListHash.remove(openListVersion);

								// add child to open list
								openList.add(child);
								openListHash.put(child, 0.0);
							}
						}
					}
				}
				// if the open list does not contain the child
				else {
					openList.add(child);
					openListHash.put(child, 0.0);
				}
			}

			// put q into the closed list
			closedList.put(q, 0.0);
		}

		return null;
	}

	/**
	 * This has been provided for you. Each strips action is converted to a string
	 * with the toString method. This means each class implementing the StripsAction
	 * interface should override toString. Your strips actions should have a form
	 * matching your included Strips definition writeup. That is <action
	 * name>(<param1>, ...). So for instance the move action might have the form of
	 * Move(peasantID, X, Y) and when grounded and written to the file Move(1, 10,
	 * 15).
	 *
	 * @param plan
	 *            Stack of Strips Actions that are written to the text file.
	 */
	private void savePlan(Stack<StripsAction> plan) {
		if (plan == null) {
			System.err.println("Cannot save null plan");
			return;
		}

		File outputDir = new File("saves");
		outputDir.mkdirs();

		File outputFile = new File(outputDir, "plan.txt");

		PrintWriter outputWriter = null;
		try {
			outputFile.createNewFile();

			outputWriter = new PrintWriter(outputFile.getAbsolutePath());

			Stack<StripsAction> tempPlan = (Stack<StripsAction>) plan.clone();
			while (!tempPlan.isEmpty()) {
				outputWriter.println(tempPlan.pop().toString());
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (outputWriter != null)
				outputWriter.close();
		}
	}
}