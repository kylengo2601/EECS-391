package edu.cwru.sepia.agent.planner;

//
// import from libraries
//

import java.util.ArrayList;
import java.util.List;

import edu.cwru.sepia.agent.planner.actions.StripsAction;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.ResourceNode.ResourceView;
import edu.cwru.sepia.environment.model.state.ResourceType;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import edu.cwru.sepia.environment.model.state.UnitTemplate;
import edu.cwru.sepia.util.Direction;

/**
 * This class is used to represent the state of the game after applying one of
 * the available actions. It will also track the A* specific information such as
 * the parent pointer and the cost and heuristic function. Remember that unlike
 * the path planning A* from the first assignment the cost of an action may be
 * more than 1. Specifically the cost of executing a compound action such as
 * move can be more than 1. You will need to account for this in your heuristic
 * and your cost function.
 *
 * The first instance is constructed from the StateView object (like in PA2).
 * Implement the methods provided and add any other methods and member variables
 * you need.
 *
 * Some useful API calls for the state view are
 *
 * state.getXExtent() and state.getYExtent() to get the map size
 *
 * I recommend storing the actions that generated the instance of the GameState
 * in this class using whatever class/structure you use to represent actions.
 */
public class GameState implements Comparable<GameState> {

	//
	// instance fields
	//

	// state
	State.StateView state;

	// player numbers
	private int playerNum;

	// goal amount
	private int requiredGold;
	private int requiredWood;

	// build peasant
	private boolean buildPeasants;

	// map dimensions
	private int xExtent;
	private int yExtent;

	// resource nodes
	private List<ResourceView> resourceNodes;

	// map layout
	private boolean[][] map;

	private int[][] goldMap;
	private int[][] woodMap;

	// units
	private List<UnitView> allUnits;
	private List<UnitView> playerUnits = new ArrayList<UnitView>();
	private UnitView townHall;

	//current amounts of deposited resources
	private int currentGold;
	private int currentWood;

	// cost
	private double cost;
	
	//heuristic
	private double heuristic;

	// parent
	private GameState parent;

	// list of Strips Actions
	private List<StripsAction> plan;

	//
	// accessor and setter methods
	//

	public UnitView getTownHall() {
		return townHall;
	}

	public void setTownHall(UnitView townHall) {
		this.townHall = townHall;
	}

	public State.StateView getState() {
		return state;
	}

	public int getPlayerNum() {
		return playerNum;
	}

	public int getRequiredGold() {
		return requiredGold;
	}

	public int getRequiredWood() {
		return requiredWood;
	}

	public boolean isBuildPeasants() {
		return buildPeasants;
	}

	public int getxExtent() {
		return xExtent;
	}

	public int getyExtent() {
		return yExtent;
	}

	public List<ResourceView> getResourceNodes() {
		return resourceNodes;
	}

	public boolean[][] getMap() {
		return map;
	}

	public List<UnitView> getAllUnits() {
		return allUnits;
	}

	public List<UnitView> getPlayerUnits() {
		return playerUnits;
	}

	public int getCurrentGold() {
		return currentGold;
	}

	public int getCurrentWood() {
		return currentWood;
	}

	public int[][] getGoldMap() {
		return this.goldMap;
	}

	public int[][] getWoodMap() {
		return this.woodMap;
	}

	public List<StripsAction> getPlan() {
		return this.plan;
	}

	public void setCost(double cost) {
		this.cost = cost;
	}

	public void setParent(GameState parent) {
		this.parent = parent;
	}

	public void setPlan(List<StripsAction> plan) {
		this.plan = plan;
	}

	//
	// constructor methods
	//

	/**
	 * Construct a GameState from a stateview object. This is used to construct the
	 * initial search node. All other nodes should be constructed from the another
	 * constructor you create or by factory functions that you create.
	 *
	 * @param state
	 *            The current stateview at the time the plan is being created
	 * @param playernum
	 *            The player number of agent that is planning
	 * @param requiredGold
	 *            The goal amount of gold (e.g. 200 for the small scenario)
	 * @param requiredWood
	 *            The goal amount of wood (e.g. 200 for the small scenario)
	 * @param buildPeasants
	 *            True if the BuildPeasant action should be considered
	 */
	public GameState(State.StateView state, int playernum, int requiredGold, int requiredWood, boolean buildPeasants) {

		// current state
		this.state = state;

		// get player number
		playerNum = playernum;

		// get goal
		this.requiredGold = requiredGold;
		this.requiredWood = requiredWood;

		// build peasant
		this.buildPeasants = buildPeasants;

		// get the map dimensions
		xExtent = state.getXExtent();
		yExtent = state.getYExtent();

		// get all resource nodes
		resourceNodes = state.getAllResourceNodes();

		// build empty map
		map = new boolean[xExtent][yExtent];
		goldMap = new int[xExtent][yExtent];
		woodMap = new int[xExtent][yExtent];
		for (int x = 0; x < xExtent; x++) {
			for (int y = 0; y < yExtent; y++) {
				map[x][y] = false;
				goldMap[x][y] = 0;
				woodMap[x][y] = 0;
			}
		}

		// identify resource positions
		for (ResourceView resourceNode : resourceNodes) {
			map[resourceNode.getXPosition()][resourceNode.getYPosition()] = true;
			if (resourceNode.getType() == ResourceNode.Type.GOLD_MINE) {
				goldMap[resourceNode.getXPosition()][resourceNode.getYPosition()] = resourceNode.getAmountRemaining();
			}
			if (resourceNode.getType() == ResourceNode.Type.TREE) {
				woodMap[resourceNode.getXPosition()][resourceNode.getYPosition()] = resourceNode.getAmountRemaining();
			}
		}

		// get all units and classify them
		allUnits = state.getAllUnits();
		for (UnitView unit : allUnits) {
			if (unit.getTemplateView().getPlayer() == playerNum) {
				if (unit.getTemplateView().getName().equals("Peasant")) {
					playerUnits.add(unit);
				} else if (unit.getTemplateView().getName().equals("TownHall")) {
					this.townHall = unit;
				}
			}
		}

		// identify townhall position
		map[townHall.getXPosition()][townHall.getYPosition()] = true;

		// get current resource amounts
		this.currentGold = state.getResourceAmount(playernum, ResourceType.GOLD);
		this.currentWood = state.getResourceAmount(playernum, ResourceType.WOOD);
		this.plan = new ArrayList<StripsAction>();
		this.cost = 0;
		this.heuristic = heuristic();
	}

	/**
	 * Creates a copy of the current GameState
	 * 
	 * @param parent
	 *            the state to make a copy of
	 */
	public GameState(GameState parent) {
		this.state = parent.state;
		this.playerNum = parent.playerNum;
		this.requiredGold = parent.requiredGold;
		this.requiredWood = parent.requiredWood;
		this.buildPeasants = parent.buildPeasants;

		this.currentGold = parent.currentGold;
		this.currentWood = parent.currentWood;

		this.xExtent = parent.xExtent;
		this.yExtent = parent.yExtent;

		this.resourceNodes = copyNodes(parent.resourceNodes);
		this.map = copyMapBool(parent.map);
		this.goldMap = copyMapInt(parent.goldMap);
		this.woodMap = copyMapInt(parent.woodMap);

		this.allUnits = copyUnits(parent.allUnits);
		this.playerUnits = copyUnits(parent.playerUnits);

		this.cost = getCost();
		this.parent = parent;

		this.plan = copy(parent.getPlan());
		this.townHall = parent.townHall;
		this.heuristic = heuristic();
	}

	//
	// mutator methods
	//

	/**
	 * Adds gold to current amount.
	 * 
	 * @param goldAdded
	 *            the amount to be added to the current gold
	 */
	public void addGold(int goldAdded) {
		currentGold += goldAdded;
	}

	/**
	 * Adds wood to the current amount.
	 * 
	 * @param woodAdded
	 *            the amount to be added to the current wood
	 */
	public void addWood(int woodAdded) {
		currentWood += woodAdded;
	}

	/**
	 * Adds cost to the current amount.
	 * 
	 * @param cost
	 *            the amount to be added to the current cost
	 */
	public void addCost(double cost) {
		this.cost += cost;
	}

	public void addPlan(StripsAction action) {
		plan.add(action);
	}

	
	/**
	 * 
	 * @param original list of resource nodes
	 * @return a deep copied list of resource nodes
	 */
	private List<ResourceView> copyNodes(List<ResourceView> original) {
		List<ResourceView> copy = new ArrayList<ResourceView>();

		for (ResourceView resource : original) {
			ResourceView rv = new ResourceView(new ResourceNode(resource.getType(), resource.getXPosition(),
					resource.getYPosition(), resource.getAmountRemaining(), resource.getID()));
			copy.add(rv);
		}
		return copy;
	}


	/**
	 * copies a list of unitViews
	 * @param original list of unitViews
	 * @return deep copied list of unitViews
	 */
	private List<UnitView> copyUnits(List<UnitView> original) {
		List<UnitView> copy = new ArrayList<UnitView>();
		for (UnitView uv : original) {
			Unit unit = new Unit(new UnitTemplate(uv.getID()), uv.getID());
			unit.setCargo(uv.getCargoType(), uv.getCargoAmount());
			unit.setxPosition(uv.getXPosition());
			unit.setyPosition(uv.getYPosition());
			copy.add(new UnitView(unit));
		}
		return copy;
	}

	/**
	 * copies a list of elements to a new list
	 * @param original the list to be copied
	 * @return the copied list
	 */
	private <T> List<T> copy(List<T> original) {
		List<T> copiedList = new ArrayList<T>();
		for (T element : original) {
			copiedList.add(element);
		}

		return copiedList;
	}


	/**
	 * copies an array of booleans
	 * @param original the original array
	 * @return the copied array
	 */
	private boolean[][] copyMapBool(boolean[][] original) {
		boolean[][] copy = new boolean[original.length][original[0].length];
		for (int i = 0; i < original.length; i++) {
			for (int j = 0; j < original[0].length; j++) {
				copy[i][j] = original[i][j];
			}
		}
		return copy;
	}


	/**
	 * copies an array of integers
	 * @param original the original array
	 * @return the copied array
	 */
	private int[][] copyMapInt(int[][] original) {
		int[][] copy = new int[original.length][original[0].length];
		for (int i = 0; i < original.length; i++) {
			for (int j = 0; j < original[0].length; j++) {
				copy[i][j] = original[i][j];
			}
		}
		return copy;
	}

	/**
	 * Unlike in the first A* assignment there are many possible goal states. As
	 * long as the wood and gold requirements are met the peasants can be at any
	 * location and the capacities of the resource locations can be anything. Use
	 * this function to check if the goal conditions are met and return true if they
	 * are.
	 *
	 * @return true if the goal conditions are met in this instance of game state.
	 */
	public boolean isGoal() {
		if (this.getCurrentGold() >= this.requiredGold && this.getCurrentWood() >= this.requiredWood) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * The branching factor of this search graph are much higher than the planning.
	 * Generate all of the possible successor states and their associated actions in
	 * this method.
	 *
	 * @return A list of the possible successor states and their associated actions
	 */
	public List<GameState> generateChildren() {
		List<GameState> children = new ArrayList<>();
		
		// creates children based on the position of each peasant
		for (UnitView unit : playerUnits) {
			for (Direction d : Direction.values()) {

				int newX = unit.getXPosition() + d.xComponent();
				int newY = unit.getYPosition() + d.yComponent();
				Position newPosition = new Position(newX, newY);
				
				//checks if the harvestGold action can be performed. If so, it is added as a child
				HarvestGoldAction harvestGold = new HarvestGoldAction(unit, newPosition, this);

				if (harvestGold.preconditionsMet(this)) {
					children.add(harvestGold.apply(this));
					continue;
				}

				//checks if the harvestWood action can be performed. If so, it is added as a child
				HarvestWoodAction harvestWood = new HarvestWoodAction(unit, newPosition, this);

				if (harvestWood.preconditionsMet(this)) {
					children.add(harvestWood.apply(this));
					continue;
				}

				Position townHallPosition = new Position(townHall.getXPosition(), townHall.getYPosition());

				// checks if the peasant can deposit gold at this state. If so, add it as a child
				DepositGoldAction depositGold = new DepositGoldAction(unit, newPosition, townHallPosition, this);

				if (depositGold.preconditionsMet(this)) {
					children.add(depositGold.apply(this));
					continue;
				}

				// checks if the peasant can deposit wood at this state. If so, add it as a child
				DepositWoodAction depositWood = new DepositWoodAction(unit, newPosition, townHallPosition, this);

				if (depositWood.preconditionsMet(this)) {
					children.add(depositWood.apply(this));
					continue;
				}

			}
			
			// find the best gold mine action
			Position goldMine = findBestResource(new Position(unit.getXPosition(), unit.getYPosition()), ResourceNode.Type.GOLD_MINE);

			for (Direction d : Direction.values()) {
				int newX = goldMine.x + d.xComponent();
				int newY = goldMine.y + d.yComponent();
				MoveAction move = new MoveAction(unit, new Position(newX, newY), this);
				if (move.preconditionsMet(this)) {
					children.add(move.apply(this));
					continue;
				}
			}
			
			// find the best tree action
			Position tree = findBestResource(new Position(unit.getXPosition(), unit.getYPosition()), ResourceNode.Type.TREE);

			for (Direction d : Direction.values()) {
				int newX = tree.x + d.xComponent();
				int newY = tree.y + d.yComponent();
				MoveAction move = new MoveAction(unit, new Position(newX, newY), this);
				if (move.preconditionsMet(this)) {
					children.add(move.apply(this));
					continue;
				}
			}
			
			
			// move to townhall action
			for (Direction d : Direction.values()) {
				int newX = townHall.getXPosition() + d.xComponent();
				int newY = townHall.getYPosition() + d.yComponent();
				MoveAction move = new MoveAction(unit, new Position(newX, newY), this);
				if (move.preconditionsMet(this)) {
					children.add(move.apply(this));
					continue;
				}
			}

		}

		return children;
	}
	
	/**
	 * given a position and the resource we are trying to get, this determines the position of the best resourceNode to go to
	 * @param currentPosition
	 * @param type the resource we are trying to get
	 * @return the best resourceNode to go to
	 */
	private Position findBestResource(Position currentPosition, ResourceNode.Type type) {
		Position bestNode = null;
		int bestNodeAmount = 0;
		int bestChebyshev = 0;
		int required;
		int current;
		int[][] map;
		if(type == ResourceNode.Type.GOLD_MINE) {
			required = requiredGold;
			current = currentGold;
			map = goldMap;
		}
		else {
			required = requiredWood;
			current = currentWood;
			map = woodMap;
		}
		
		// checks through the specified map and compares the distances and the amounts remaining to what is needed for the pesant		
		for(int i = 0; i < map.length; i ++) {
			for(int j = 0; j < map[i].length; j ++) {
				if(map[i][j] > 0) {
					int nodeChebyshev = currentPosition.chebyshevDistance(new Position(i, j));;
					if(bestNode == null) {
						bestNode = new Position(i, j);
						bestChebyshev = nodeChebyshev;
						bestNodeAmount = map[i][j];
					}
					else {
						//if the current node is closer than the best, update if the current has enough resources
						if(nodeChebyshev <= bestChebyshev) {
							if(map[i][j] >= bestNodeAmount || map[i][j] >= 100) {
								bestNode = new Position(i, j);
								bestChebyshev = nodeChebyshev;
								bestNodeAmount = map[i][j];
							}
							else if(map[i][j] >= required - current) {
								bestNode = new Position(i, j);
								bestChebyshev = nodeChebyshev;
								bestNodeAmount = map[i][j];
							}
						}
						//if the current node is further than the best, only update if the best doesn't have enough and the current does
						else {
							if(bestNodeAmount >= 100) {
								continue;
							}
							if(map[i][j] >= bestNodeAmount) {
								if(map[i][j] >= 100) {
									if(bestNodeAmount < required - current) {
										bestNode = new Position(i, j);
										bestChebyshev = nodeChebyshev;
										bestNodeAmount = map[i][j];
									}
								}
							}
						}
					}
				}
			}
		}
		
		return bestNode;
		
	}

	/**
	 * finds a unitView from a list of unitVIews by ID
	 * @param unitId the ID that we are looking for
	 * @param units the list of units to search
	 * @return the desired unit or null if that unitId doesn't exit
	 */
	public UnitView findUnit(int unitId, List<UnitView> units) {
		for (UnitView unit : units) {
			if (unit.getID() == unitId) {
				return unit;
			}
		}
		return null;
	}

	/**
	 * finds a resourceView from a list of resourceViews by position
	 * @param x the x position of the desired resourceView
	 * @param y the x position of the desired resourceView
	 * @param resources the list of resourceViews
	 * @return the desired resourceView or null if it doesn't exist
	 */
	public ResourceView findResource(int x, int y, List<ResourceView> resources) {
		for (ResourceView resource : resources) {
			if (resource.getXPosition() == x && resource.getYPosition() == y) {
				return resource;
			}
		}
		return null;
	}

	/**
	 * Write your heuristic function here. Remember this must be admissible for the
	 * properties of A* to hold. If you can come up with an easy way of computing a
	 * consistent heuristic that is even better, but not strictly necessary.
	 *
	 * Add a description here in your submission explaining your heuristic.
	 *
	 * @return The value estimated remaining cost to reach a goal state from this
	 *         state.
	 */
	public double heuristic() {
		double initialGoldAmount = requiredGold - currentGold;
		double initialWoodAmount = requiredWood - currentWood;
		
		double hvalue = initialGoldAmount + initialWoodAmount;
		
		UnitView peasant = playerUnits.get(0);
		double peasantAmount = peasant.getCargoAmount();
		
		//peasant is holding something
		if(peasantAmount > 0) {
			// the peasant is at a spot where it can deposit
			if(canDeposit(peasant)) {
				hvalue -= peasantAmount*0.75;
			}
			// the peasant just harvested something but is not by the townhall
			else {
				hvalue -= peasantAmount * 0.5;
			}
		}
		
		//peasant is holding anything
		else {
			// move to a place next to a resource
			double amountToHarvest = canHarvest(peasant);
			if(amountToHarvest > 0) {
				hvalue -= amountToHarvest * 0.25;
			}
		}
		this.heuristic = hvalue;
		return hvalue;
	}
	
	/**
	 * determines whether the peasant can deposit based on location
	 * @param peasant
	 * @return true or false, depending on if the peasant can deposit based on location
	 */
	public boolean canDeposit(UnitView peasant) {
		if(Math.abs(peasant.getXPosition() - townHall.getXPosition()) <= 1 && Math.abs(peasant.getYPosition() - townHall.getYPosition()) <= 1) {
			return true;
		}
		return false;
	}
	
	/**
	 * determines whether the peasant can harvest based on location
	 * @param peasant 
	 * @return true or false, depending on if the peasant can harvest based on location
	 */
	public double canHarvest(UnitView peasant) {
		
		for(Direction d : Direction.values()) {
			int newX = peasant.getXPosition() + d.xComponent();
			int newY = peasant.getYPosition() + d.yComponent();
			int goldAmount = goldMap[newX][newY];
			int woodAmount = woodMap[newX][newY];
			if(goldAmount > 0) {
				if (requiredGold - currentGold > 0) {
					if (goldAmount > 100) {
						return 100;
					}
					return goldMap[newX][newY];
				}
			}
			if(woodAmount > 0) {
				if (requiredWood - currentWood > 0) {
					if (woodAmount > 100) {
						return 100;
					}
					return woodMap[newX][newY];
				}
			}
		}
		return 0.0;
	}

	/**
	 * Write the function that computes the current cost to get to this node. This
	 * is combined with your heuristic to determine which actions/states are better
	 * to explore.
	 *
	 * @return The current cost to reach this goal
	 */
	public double getCost() {
		//This method is very brief since other methods like addCost() update the cost when creating a new gameState
		return this.cost;
	}

	/**
	 * This is necessary to use your state in the Java priority queue. See the
	 * official priority queue and Comparable interface documentation to learn how
	 * this function should work.
	 *
	 * @param o
	 *            The other game state to compare
	 * @return 1 if this state costs more than the other, 0 if equal, -1 otherwise
	 */
	@Override
	public int compareTo(GameState o) {
		double thisAStarValue = this.getCost() + this.heuristic();
		double oAStarValue = o.getCost() + o.heuristic();

		if (thisAStarValue > oAStarValue) {
			return 1;
		} else if (thisAStarValue == oAStarValue) {
			return 0;
		} else {
			return -1;
		}
	}

	/**
	 * This will be necessary to use the GameState as a key in a Set or Map.
	 *
	 * @param o
	 *            The game state to compare
	 * @return True if this state equals the other state, false otherwise.
	 */
	@Override
	public boolean equals(Object o) {
		if (o instanceof GameState) {
			if (this.currentGold == ((GameState) o).getCurrentGold()
					&& this.currentWood == ((GameState) o).getCurrentWood()
					&& this.allUnits.equals(((GameState) o).getAllUnits())
					&& this.heuristic() == ((GameState) o).heuristic()
					&& resourceNodesEquals(((GameState) o).getResourceNodes())) {
				return true;
			}
		}

		return false;
	}
 
	/**
	 * helper method to compare each resource node in the list using deep equals
	 * @param oResourceList the list to compare
	 * @return true if the two nodes are equal, false if otherwise
	 */
	private boolean resourceNodesEquals(List<ResourceNode.ResourceView> oResourceList) {
		boolean isEqual = true;
		List<ResourceView> resourceList = this.getResourceNodes();
		for (ResourceView node : resourceList) {
			ResourceView oNode = findResourceNode(node.getID(), oResourceList);
			if (oNode == null || !resourceViewEquals(node, oNode)) {
				isEqual = false;
			}
		}
		return isEqual;
	}

	/**
	 * finds a resourceNode from a list based on ID
	 * @param resourceId the ID to check
	 * @param resourceList the list to check from
	 * @return the resourceNode that has the given ID
	 */
	private ResourceView findResourceNode(int resourceId, List<ResourceNode.ResourceView> resourceList) {
		for (ResourceView node : resourceList) {
			if (node.getID() == resourceId) {
				return node;
			}
		}
		return null;
	}

	/**
	 * checks if two resourceViews are equal based on amount and position
	 * @param view1
	 * @param view2
	 * @return true if they are equal, false if otherwise
	 */
	private boolean resourceViewEquals(ResourceView view1, ResourceView view2) {
		if (view1.getAmountRemaining() == view2.getAmountRemaining() && view1.getXPosition() == view2.getXPosition()
				&& view1.getYPosition() == view2.getYPosition()) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * This is necessary to use the GameState as a key in a HashSet or HashMap.
	 * Remember that if two objects are equal they should hash to the same value.
	 *
	 * @return An integer hashcode that is equal for equal states.
	 */
	@Override
	public int hashCode() {
		// TODO: Implement me!
		int result = 17;
		result = 31 * result + currentWood;
		result = 31* result + currentGold;
		int x = playerUnits.get(0).getXPosition();
		int y = playerUnits.get(0).getYPosition();
		result = 31 * result + x;
		result = 31 * result + y;
		int holding = playerUnits.get(0).getCargoAmount();
		result = 31 * result + holding;
		return result;
	}

	/* helper methods to check if a move is legal */
	private String legalMove(int x, int y) {
		if (x >= xExtent || x < 0 || y >= yExtent || y < 0) {
			return "Out of Bounds";
		} else if (map[x][y]) {
			if (goldMap[x][y] > 0) {
				return "Gold Mine";
			} else if (woodMap[x][y] > 0) {
				return "Tree";
			} else if (x == townHall.getXPosition() && y == townHall.getYPosition()) {
				return "TownHall";
			}
		} else {
			for (UnitView unit : playerUnits) {
				if (x == unit.getXPosition() && y == unit.getYPosition()) {
					return "Hit Player";
				}
			}
		}
		return "Valid Move";
	}

}