package evacuacao;

import java.util.List;

import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.SpatialMath;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import sajas.core.Agent;
import sajas.core.behaviours.Behaviour;
import sajas.core.behaviours.CyclicBehaviour;
import sajas.core.behaviours.TickerBehaviour;

public class Soldier extends Agent {
	
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	private double posx, posy;
	private int visionRadius, speakRadius;
	
	//exit
	private int exitx = -1,exity=-1;
	
	private double speed;
	
	public Soldier(ContinuousSpace<Object> space, Grid<Object> grid, int x, int y) {
		this.space = space;
		this.grid = grid;
		this.posx =x;
		this.posy =y;
		this.visionRadius = 2; 
		this.speakRadius = 10;
	};
	
	protected void setup() {
		
		speed = 1;

		space.moveTo(this, posx, posy);
		grid.moveTo(this, (int) posx, (int) posy);

		addBehaviour(new SearchForExit(this,1));
		addBehaviour(new SoldierRandomMovement(this,1));
		addBehaviour(new MessageListener());
		addBehaviour(new SoldierMessages());
		
	}
	
	private class SoldierRandomMovement extends TickerBehaviour {
		public SoldierRandomMovement(Agent a, long period) {
			super(a, period);
		}

		private static final long serialVersionUID = 1L;

		@Override
		protected void onTick() {
			NdPoint  myPoint;
			
			if(exitx==-1) {
				int direction = RandomHelper.nextIntFromTo (0, 7);
				double angle= direction*45*Math.PI/180;
				space.moveByVector(myAgent , speed, angle , 0);
				myPoint = space.getLocation(myAgent);
				grid.moveTo(myAgent , (int)myPoint.getX(), (int)myPoint.getY ());
			}else {
				NdPoint  otherPoint = new  NdPoint(exitx,exity);
				
				myPoint = space.getLocation(myAgent);
				double distance = Math.sqrt(Math.pow(myPoint.getX()-exitx,2) + Math.pow(myPoint.getY()-exity,2));
				double  angle = SpatialMath.calcAngleFor2DMovement(space ,myPoint , otherPoint );
				if (distance >1)
					space.moveByVector(myAgent , speed, angle , 0);
				else space.moveByVector(myAgent , distance, angle , 0);
				myPoint = space.getLocation(myAgent);
				grid.moveTo(myAgent , (int)myPoint.getX(), (int)myPoint.getY ());
			}
			
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
					exitx=goal.getX();
					exity=goal.getY();
				}
			}
			
		}

	}
	
	private class SoldierMessages extends Behaviour {

		private static final long serialVersionUID = 1L;
		
		@Override
		public void action() {
			if(exitx!=-1) {
				GridPoint pt = grid.getLocation(myAgent);

				GridCellNgh<Soldier> nghCreator = new GridCellNgh<Soldier>(grid, pt, Soldier.class, speakRadius, speakRadius);
				List<GridCell<Soldier>> gridCells = nghCreator.getNeighborhood(false);
				ACLMessage message_inform = new ACLMessage(ACLMessage.INFORM);
				
				for (GridCell<Soldier> cell : gridCells) {
					if (cell.size() > 0) {
						
						for (Object  obj : grid.getObjectsAt(cell.getPoint().getX(), cell.getPoint().getY ())) {
							if (obj  instanceof  Soldier) {
								message_inform.addReceiver(((Soldier) obj).getAID());
								System.out.println("adicionei");
							}
						}		

						message_inform.setContent(exitx + "-" + exity);
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
			
			if(exitx==-1) {
				MessageTemplate msgtemp = MessageTemplate.MatchConversationId("inform_exit");
				ACLMessage reply = myAgent.receive(msgtemp);

				try {
					String message = reply.getContent();
					String[] coords = message.split("-");
					exitx = Integer.parseInt(coords[0]);
					exity = Integer.parseInt(coords[1]);
				} catch (NullPointerException e) {
					
				}
			}
			
		}
	}
	
}
