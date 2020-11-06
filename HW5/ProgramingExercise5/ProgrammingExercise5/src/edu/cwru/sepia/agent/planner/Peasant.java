package edu.cwru.sepia.agent.planner;

public class Peasant {
	private boolean holdingGold;
	private boolean holdingWood;
	private int holdingAmount = 0;
	private int xPos;
	private int yPos;
	private int id;
	private Position adjacent;

	public int getHoldingAmount() {
		return holdingAmount;
	}
	
	public void clearHoldingAmount() {
		this.holdingWood = false;
		this.holdingGold = false;
		this.holdingAmount = 0;
	}

	public void setHoldingAmount(int holdingAmount) {
		this.holdingAmount = holdingAmount;
	}

	public int getxPos() {
		return xPos;
	}

	public void setxPos(int xPos) {
		this.xPos = xPos;
	}

	public int getyPos() {
		return yPos;
	}

	public void setyPos(int yPos) {
		this.yPos = yPos;
	}
	
	public boolean isHoldingGold() {
		return holdingGold;
	}

	public void setHoldingGold(boolean holdingGold) {
		this.holdingGold = holdingGold;
	}

	public boolean isHoldingWood() {
		return holdingWood;
	}

	public void setHoldingWood(boolean holdingWood) {
		this.holdingWood = holdingWood;
	}
	
	public int getID() {
		return this.id;
	}
	
	public void setID(int id) {
		this.id = id;
	}
	
	public Position getAdjacent() {
		return this.adjacent;
	}
	
	public void setAdjacent(Position adjacent) {
		this.adjacent = adjacent;
	}

	public Peasant(int id, int xPos, int yPos, boolean holdingGold, boolean holdingWood, int holdingAmount, Position adjacent) {
		this.id = id;
		this.xPos = xPos;
		this.yPos = yPos;
		this.holdingGold = holdingGold;
		this.holdingWood = holdingWood;
		this.holdingAmount = holdingAmount;
		this.adjacent = adjacent;
	}
	
	
}