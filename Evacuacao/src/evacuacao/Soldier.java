package evacuacao;

import java.util.ArrayList;
import java.util.List;

import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.Direction;
import repast.simphony.space.SpatialMath;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.SimUtilities;
import sajas.core.Agent;
import sajas.core.behaviours.Behaviour;
import sajas.core.behaviours.CyclicBehaviour;
import sajas.core.behaviours.TickerBehaviour;

public class Soldier extends Agent {
	
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	private double posX, posY;
	private int visionRadius, speakRadius;
	
	//exit
	private int exitX = -1, exitY = -1;
	
	private double speed;
	
	public Soldier(ContinuousSpace<Object> space, Grid<Object> grid, int x, int y) {
		this.space = space;
		this.grid = grid;
		this.posX = x;
		this.posY = y;
		this.visionRadius = 2; 
		this.speakRadius = 10;
	};
	
	protected void setup() {		
		speed = 1;

		space.moveTo(this, posX, posY);
		grid.moveTo(this, (int) posX, (int) posY);

		addBehaviour(new SearchForExit(this,1));
		addBehaviour(new SoldierRandomMovement(this,1));
		addBehaviour(new MessageListener());
		addBehaviour(new SoldierMessages());		
	}
	
	public static boolean canMove (Grid<Object> grid, NdPoint pt1, NdPoint pt2) {
		GridPoint pt = new GridPoint( (int) pt1.getX(), (int) pt1.getY());
		GridCellNgh<WallChunk> nghCreator = new GridCellNgh<WallChunk>(grid, pt, WallChunk.class, 1, 1);
		List<GridCell<WallChunk>> gridCells = nghCreator.getNeighborhood(true);
		List<Wall> walls = new ArrayList<Wall>();
		for (GridCell<WallChunk> cell : gridCells ) {
			if (cell.size() > 0) {
				for (WallChunk wc : cell.items() ) {
					Wall wall = wc.getWall();
					if (walls.contains(wall))
						continue;
					walls.add(wall);
					//System.out.println(wall.getX1() + "," + wall.getY1() + "," + wall.getX2() + "," + wall.getY2());
					if (Geometry.doLinesIntersect(pt1.getX(), pt1.getY(), pt2.getX(), pt2.getY(),
							(double)wall.getX1(), (double)wall.getY1(),
							(double)wall.getX2(), (double)wall.getY2() )){
						//System.out.println(pt1.getX() + "," +  pt2.getY() + "," + pt2.getX()+ "," + pt1.getY() +
						//		" intersects " + wall.getX1() + "," + wall.getY1() + "," + wall.getX2() + "," + wall.getY2());
						return false;											
					}
				}
			}
		}
		return true;
	}
	
	private class SoldierRandomMovement extends TickerBehaviour {
		public SoldierRandomMovement(Agent a, long period) {
			super(a, period);
		}

		private static final long serialVersionUID = 1L;

		@Override
		protected void onTick() {
			NdPoint myPoint;
			
			if (exitX == -1) {
				myPoint = space.getLocation(myAgent);
				while(true){
					int direction = RandomHelper.nextIntFromTo(0, 7);
					double angle = direction * 45 * Math.PI/180;				
					NdPoint destPoint = new NdPoint(myPoint.getX() + speed * Math.cos(angle), myPoint.getY() + speed * Math.sin(angle));					
					if (canMove(grid, myPoint, destPoint)){
						space.moveByVector(myAgent, speed, angle, 0);
						break;
					}
				}
			} else {
				NdPoint otherPoint = new NdPoint(exitX, exitY);				
				myPoint = space.getLocation(myAgent);
				double distance = Math.sqrt(Math.pow(myPoint.getX()-exitX, 2) + Math.pow(myPoint.getY()-exitY, 2));
				double angle = SpatialMath.calcAngleFor2DMovement(space, myPoint, otherPoint);
				if (distance > 1)
					space.moveByVector(myAgent, speed, angle , 0);
				else space.moveByVector(myAgent, distance, angle, 0);
			}
			myPoint = space.getLocation(myAgent);
			grid.moveTo(myAgent, (int)myPoint.getX(), (int)myPoint.getY());
		}
	}
	
	private class SearchForExit extends TickerBehaviour {
		public SearchForExit(Agent a, long period) {
			super(a, period);
		}

		private static final long serialVersionUID = 1L;

		@Override
		protected void onTick() {
			GridPoint pt = grid.getLocation(myAgent);

			GridCellNgh<Exit> nghCreator = new GridCellNgh<Exit>(grid, pt, Exit.class, visionRadius, visionRadius);
			List<GridCell<Exit>> gridCells = nghCreator.getNeighborhood(true);

			GridPoint goal = null;
			for (GridCell<Exit> cell : gridCells) {
				if (cell.size() > 0) {
					goal = cell.getPoint();
					exitX=goal.getX();
					exitY=goal.getY();
				}
			}			
		}
	}
	
	private class SoldierMessages extends Behaviour {

		private static final long serialVersionUID = 1L;
		
		@Override
		public void action() {
			if(exitX!=-1) {
				GridPoint pt = grid.getLocation(myAgent);

				GridCellNgh<Soldier> nghCreator = new GridCellNgh<Soldier>(grid, pt, Soldier.class, speakRadius, speakRadius);
				List<GridCell<Soldier>> gridCells = nghCreator.getNeighborhood(false);
				ACLMessage message_inform = new ACLMessage(ACLMessage.INFORM);
				
				for (GridCell<Soldier> cell : gridCells) {
					if (cell.size() > 0) {
						
						for (Object  obj : grid.getObjectsAt(cell.getPoint().getX(), cell.getPoint().getY ())) {
							if (obj  instanceof  Soldier) {
								message_inform.addReceiver(((Soldier) obj).getAID());
								//System.out.println("adicionei");
							}
						}		

						message_inform.setContent(exitX + "-" + exitY);
						message_inform.setConversationId("inform_exit");
						message_inform.setReplyWith("inform_exit " + System.currentTimeMillis());
						myAgent.send(message_inform);
						
						break;
					}
				}
				
				GridCellNgh<General> nghCreatorGen = new GridCellNgh<General>(grid, pt, General.class, speakRadius, speakRadius);
				List<GridCell<General>> gridCellsGen = nghCreatorGen.getNeighborhood(false);
				message_inform = new ACLMessage(ACLMessage.INFORM);
				
				for (GridCell<General> cell : gridCellsGen) {
					if (cell.size() > 0) {
						
						for (Object  obj : grid.getObjectsAt(cell.getPoint().getX(), cell.getPoint().getY ())) {
							if (obj  instanceof  General) {
								message_inform.addReceiver(((General) obj).getAID());
								//System.out.println("adicionei");
							}
						}		

						message_inform.setContent(exitX + "-" + exitY);
						message_inform.setConversationId("inform_exit");
						message_inform.setReplyWith("inform_exit " + System.currentTimeMillis());
						myAgent.send(message_inform);
						
						break;
					}
				}				
			}			
		}

		@Override
		public boolean done() {
			// TODO Auto-generated method stub
			return false;
		}
		
	}
	
	private class MessageListener extends CyclicBehaviour {
		private static final long serialVersionUID = 1L;

		@Override
		public void action() {
			
			if(exitX==-1) {
				//System.out.println("entrei");
				MessageTemplate msgtemp = MessageTemplate.MatchConversationId("inform_exit");
				ACLMessage reply = myAgent.receive(msgtemp);
				
				try {
					String message = reply.getContent();
					String[] coords = message.split("-");
					exitX = Integer.parseInt(coords[0]);
					exitY = Integer.parseInt(coords[1]);
				} catch (NullPointerException e) {
					//System.out.println("lol ");
				}
			}
			
		}
	}	
}
