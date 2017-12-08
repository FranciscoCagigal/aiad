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
import sajas.core.AID;
import repast.simphony.util.SimUtilities;
import sajas.core.Agent;
import sajas.core.behaviours.Behaviour;
import sajas.core.behaviours.CyclicBehaviour;
import sajas.core.behaviours.OneShotBehaviour;

public class Soldier extends Agent {
	
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	private double posX, posY;
	private int visionRadius, speakRadius;
	
	//exit
	private int exitX = -1, exitY = -1;
	
	//general goal
	private double generalPlaceX, generalPlaceY;
	private State stage = State.WAITING_FOR_START;
	private jade.core.AID myGeneral;
	
	private Boolean canDelete = false;
	
	private double speed;
	
	private int type_of_game;
	
	public Soldier(ContinuousSpace<Object> space, Grid<Object> grid, int x, int y, int vision_radius, int speak_radius, int type_of_game) {
		this.space = space;
		this.grid = grid;
		this.posX =x;
		this.posY =y;
		this.visionRadius = vision_radius;
		this.speakRadius = speak_radius;
		this.type_of_game = type_of_game;
	};
	
	protected void setup() {		
		speed = 1;

		space.moveTo(this, posX, posY);
		grid.moveTo(this, (int) posX, (int) posY);

		addBehaviour(new SearchForExit());
		addBehaviour(new SoldierMessages());
		addBehaviour(new MessageListener());
	
		if(type_of_game==0)
			addBehaviour(new SoldierRandomMovement());
		else if(type_of_game==1) 
			addBehaviour(new SoldierRandomCoordenatedMovement());
		else addBehaviour(new SoldierSuperCoordinatedRandomMovement());
	}
	
	private double moveRnd() {
		int direction = RandomHelper.nextIntFromTo (0, 7);
		double angle= direction*45*Math.PI/180;
		space.moveByVector(this , speed, angle , 0);
		NdPoint myPoint = space.getLocation(this);
		grid.moveTo(this , (int)myPoint.getX(), (int)myPoint.getY ());
		return angle;
	}
	
	private NdPoint moveToPlace(double x, double y) {
		NdPoint  otherPoint = new  NdPoint(x,y);
		NdPoint myPoint = space.getLocation(this);
		double distance = Math.sqrt(Math.pow(myPoint.getX()-x,2) + Math.pow(myPoint.getY()-y,2));
		double  angle = SpatialMath.calcAngleFor2DMovement(space ,myPoint , otherPoint );
		if (distance >speed)
			space.moveByVector(this , speed, angle , 0);
		else {
			space.moveByVector(this , distance, angle , 0);
		}
		myPoint = space.getLocation(this);
		grid.moveTo(this , (int)myPoint.getX(), (int)myPoint.getY ());
		
		return myPoint;
	}
	
	private NdPoint moveToAngle(double angle) {
		space.moveByVector(this , speed, angle , 0);	
		NdPoint myPoint = space.getLocation(this);
		grid.moveTo(this , (int)myPoint.getX(), (int)myPoint.getY ());	
		return myPoint;
	}
	
	private boolean checkForCapitans(int radius) {
		GridPoint pt = grid.getLocation(this);
		GridCellNgh<General> nghCreatorGeneral = new GridCellNgh<General>(grid, pt, General.class, radius, radius);
		List<GridCell<General>> gridCellsGeneral = nghCreatorGeneral.getNeighborhood(true);
		for (GridCell<General> cell : gridCellsGeneral) {
			if (cell.size() > 0) {
				
				for (Object  obj : grid.getObjectsAt(cell.getPoint().getX(), cell.getPoint().getY ())) {
					if (obj  instanceof  General) {
						return true;
					}
				}
				
				break;
			}
		}
		return false;		
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
					if (Geometry.doLinesIntersect(pt1.getX(), pt1.getY(), pt2.getX(), pt2.getY(),
							(double)wall.getX1(), (double)wall.getY1(),
							(double)wall.getX2(), (double)wall.getY2() )){
						return false;											
					}
				}
			}
		}
		return true;
	}
	
	private boolean checkForSpecificCapitan(int radius, jade.core.AID myGeneral) {
		GridPoint pt = grid.getLocation(this);
		GridCellNgh<General> nghCreatorGeneral = new GridCellNgh<General>(grid, pt, General.class, radius, radius);
		List<GridCell<General>> gridCellsGeneral = nghCreatorGeneral.getNeighborhood(true);

		for (GridCell<General> cell : gridCellsGeneral) {
			if (cell.size() > 0) {
				
				for (Object  obj : grid.getObjectsAt(cell.getPoint().getX(), cell.getPoint().getY ())) {
					
					if (obj  instanceof  General && ((General) obj).getAID().toString().equals(myGeneral.toString())) {
						return true;
					}
				}
			}
		}
		return false;
	}
	
	private class SoldierSuperCoordinatedRandomMovement extends Behaviour {

		private static final long serialVersionUID = 1L;
		@Override
		public void action() {
			NdPoint  myPoint;
			switch(stage) {
			case MOVING:
				myPoint = moveToPlace(generalPlaceX,generalPlaceY);
				if(myPoint.getX()==generalPlaceX && myPoint.getY()==generalPlaceY) {
					stage=State.FINISHED_MOVING;
					if(checkForSpecificCapitan(speakRadius,myGeneral)) {
						myAgent.addBehaviour(new WarnGeneralArrival());
					}
						
					else {
						transmitNewsToNearbySoldiers(myAgent.getAID().toString()+"/-/"+myGeneral.toString(),"transmit_arrival");
					}
				}
				break;
			case FOUND_EXIT:
				moveToPlace(exitX,exitY);
				break;
			}
			
		}

		@Override
		public boolean done() {
			// TODO Auto-generated method stub
			return false;
		}
	}
	
	
	
	private class SoldierRandomMovement extends Behaviour {

		private static final long serialVersionUID = 1L;
		@Override

		public void action() {
			if(exitX==-1) {
				
				while(true){
					NdPoint lastPoint = space.getLocation(myAgent);
					double angle = moveRnd();
					NdPoint myPoint = space.getLocation(myAgent);
					if (canMove(grid,lastPoint,  myPoint)){
						grid.moveTo(myAgent, (int)myPoint.getX(), (int)myPoint.getY());
						break;
					}else {
						moveToAngle(angle+Math.PI);
					}
				}
			} else {
				moveToPlace(exitX,exitY);
			}
		}

		@Override
		public boolean done() {
			// TODO Auto-generated method stub
			return false;
		}
	}
	
	private void transmitNewsToNearbySoldiers(String content, String id) {
		GridPoint pt = grid.getLocation(this);
		GridCellNgh<Soldier> nghCreator = new GridCellNgh<Soldier>(grid, pt, Soldier.class, speakRadius, speakRadius);
		List<GridCell<Soldier>> gridCells = nghCreator.getNeighborhood(true);
		ACLMessage message_inform = new ACLMessage(ACLMessage.INFORM);

		for (GridCell<Soldier> cell : gridCells) {
			if (cell.size() > 0) {
				
				for (Object  obj : grid.getObjectsAt(cell.getPoint().getX(), cell.getPoint().getY ())) {
					if (obj  instanceof  Soldier) {
						message_inform.addReceiver(((Soldier) obj).getAID());
					}
				}		
			}
		}
		message_inform.setContent(content);
		message_inform.setConversationId(id);
		message_inform.setReplyWith(id + " " + System.currentTimeMillis());
		this.send(message_inform);
	}
	
	private class WarnGeneralArrival extends OneShotBehaviour{

		private static final long serialVersionUID = 1L;

		@Override
		public void action() {
			ACLMessage message_inform = new ACLMessage(ACLMessage.INFORM);
			
			message_inform.addReceiver(myGeneral);
			message_inform.setContent(myAgent.getAID().toString());
			message_inform.setConversationId("arrived");
			message_inform.setReplyWith("arrived " + System.currentTimeMillis());
			myAgent.send(message_inform);
			
		}		
	}
	
	private class SoldierRandomCoordenatedMovement extends Behaviour {


		private static final long serialVersionUID = 1L;
		@Override
		public void action() {
			NdPoint  myPoint;

			if(exitX==-1) {
				
				while(true) {
					
					GridPoint pt = grid.getLocation(myAgent);
					
					GridCellNgh<General> nghCreatorGeneral = new GridCellNgh<General>(grid, pt, General.class, speakRadius-7, speakRadius-7);
					List<GridCell<General>> gridCellsGeneral = nghCreatorGeneral.getNeighborhood(true);
					
					int counter =0;
					myPoint = space.getLocation(this);
					for (GridCell<General> cell : gridCellsGeneral) {
						if (cell.size() > 0) {
							
							for (Object  obj : grid.getObjectsAt(cell.getPoint().getX(), cell.getPoint().getY ())) {
								if (obj  instanceof  General) {
									counter++;
								}
							}
							
							break;
						}
					}
					
					if(counter==0) {
						GridCellNgh<General> nghCreatorGeneral2 = new GridCellNgh<General>(grid, pt, General.class, speakRadius, speakRadius);
						List<GridCell<General>> gridCellsGeneral2 = nghCreatorGeneral2.getNeighborhood(true);
						
						myPoint = space.getLocation(myAgent);
						for (GridCell<General> cell : gridCellsGeneral2) {
							if (cell.size() > 0) {
								
								for (Object  obj : grid.getObjectsAt(cell.getPoint().getX(), cell.getPoint().getY ())) {
									if (obj  instanceof  General) {
										double  angle = SpatialMath.calcAngleFor2DMovement(space , myPoint ,new NdPoint(cell.getPoint().getX(), cell.getPoint().getY ()) );
										space.moveByVector(myAgent , speed, angle , 0);
										myPoint = space.getLocation(myAgent);
										grid.moveTo(myAgent , (int)myPoint.getX(), (int)myPoint.getY ());
										break;
									}
								}
								
								break;
							}
						}
						break;
					}
					
					int direction = RandomHelper.nextIntFromTo (0, 7);
					double angle= direction*45*Math.PI/180;
					space.moveByVector(myAgent , speed, angle , 0);
					myPoint = space.getLocation(myAgent);
					grid.moveTo(myAgent , (int)myPoint.getX(), (int)myPoint.getY ());
					
					pt = grid.getLocation(myAgent);
					
					GridCellNgh<General> nghCreatorGeneral1 = new GridCellNgh<General>(grid, pt, General.class, speakRadius-7, speakRadius-7);
					List<GridCell<General>> gridCellsGeneral1 = nghCreatorGeneral1.getNeighborhood(true);
					
					counter =0;
					
					for (GridCell<General> cell : gridCellsGeneral1) {
						if (cell.size() > 0) {
							
							for (Object  obj : grid.getObjectsAt(cell.getPoint().getX(), cell.getPoint().getY ())) {
								if (obj  instanceof  General) {
									counter++;
								}
							}		
							
							break;
						}
					}
					if(counter>0)
						break;
					else {
						space.moveByVector(myAgent , speed, angle + Math.PI , 0);
						myPoint = space.getLocation(myAgent);
						grid.moveTo(myAgent , (int)myPoint.getX(), (int)myPoint.getY ());
					}
				}
				
				
			}else {
				NdPoint  otherPoint = new  NdPoint(exitX,exitY);
				}
		}

		@Override
		public boolean done() {
			// TODO Auto-generated method stub
			return false;
		}
	}
	
	private class SearchForExit extends Behaviour {


		private static final long serialVersionUID = 1L;

		@Override
		public void action() {
			GridPoint pt = grid.getLocation(myAgent);
			
			GridCellNgh<Exit> nghCreator = new GridCellNgh<Exit>(grid, pt, Exit.class, visionRadius, visionRadius);
			List<GridCell<Exit>> gridCells = nghCreator.getNeighborhood(true);
			GridPoint goal = null;
			for (GridCell<Exit> cell : gridCells) {
				if (cell.size() > 0) {
					goal = cell.getPoint();
					exitX=goal.getX();
					exitY=goal.getY();
					stage=State.FOUND_EXIT;
					myAgent.removeBehaviour(this);
					System.out.println("encontrei saída");
				}
			}
		}
		@Override
		public boolean done() {
			return false;
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
		private List<String> arrivals = new ArrayList<String>();

		@Override
		public void action() {
			
			MessageTemplate msgtemp = MessageTemplate.MatchConversationId("inform_exit");
			ACLMessage reply = myAgent.receive(msgtemp);
			
			try {
				String message = reply.getContent();
				String[] coords = message.split("-");
				exitX = Integer.parseInt(coords[0]);
				exitY = Integer.parseInt(coords[1]);
				stage=State.FOUND_EXIT;
				transmitNewsToNearbySoldiers(message,"inform_exit");
			} catch (NullPointerException e) {
			}
			
			if(exitX==-1) {
				while(true) {
					msgtemp = MessageTemplate.MatchConversationId("follow_me");
					reply = myAgent.receive(msgtemp);

					try {					
						if (myGeneral==null) {
							myGeneral = reply.getSender();
						}
						String message = reply.getContent();
						String[] coords = message.split("/-/");
						if(myGeneral.toString().equals(coords[0])&& !reply.getSender().toString().equals(myAgent.getAID().toString()) && stage!=State.MOVING) {
							for(int i=1;i<coords.length;i+=3) {
								if(coords[i].equals(myAgent.getAID().toString())) {
									if(generalPlaceX != Double.parseDouble(coords[i+1]) || generalPlaceY != Double.parseDouble(coords[i+2])) {
										generalPlaceX = Double.parseDouble(coords[i+1]);
										generalPlaceY = Double.parseDouble(coords[i+2]);
										stage=State.MOVING;
										canDelete=true;
										transmitNewsToNearbySoldiers(message,"follow_me");
									}
									
								}
							}
							
						}
						
					} catch (NullPointerException e) {
						break;
					}
				}
				
				if(canDelete) {
					canDelete=false;arrivals.clear();
				}
				
				
				if(stage==State.FINISHED_MOVING) {
					while(true) {
						msgtemp = MessageTemplate.MatchConversationId("transmit_arrival");
						reply = myAgent.receive(msgtemp);
						
						try {
							String message = reply.getContent();
							String[] messages = message.split("/-/");
							if(!arrivals.contains(message) && !messages[0].equals(myAgent.getAID().toString())) {
								arrivals.add(message);
								if(myGeneral.toString().equals(messages[1]) && checkForSpecificCapitan(speakRadius,myGeneral)) {
									ACLMessage message_inform = new ACLMessage(ACLMessage.INFORM);						
									message_inform.addReceiver(myGeneral);
									message_inform.setContent(messages[0]);
									message_inform.setConversationId("arrived");
									message_inform.setReplyWith("arrived " + System.currentTimeMillis());
									myAgent.send(message_inform);
								}else {
									transmitNewsToNearbySoldiers(message,"transmit_arrival");
								}
							}
							
						} catch (NullPointerException e) {
							break;
						}
					}
				}
				
	
			}
			
		}
	}	
}
