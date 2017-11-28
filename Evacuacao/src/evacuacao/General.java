package evacuacao;

import java.util.List;

import jade.core.AID;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
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
import sajas.core.behaviours.OneShotBehaviour;
import sajas.core.behaviours.TickerBehaviour;
import sajas.domain.DFService;

public class General extends Agent {
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	private double posx, posy;
	private int visionRadius, speakRadius;
	
	//exit
	private int exitx = -1,exity=-1;
	
	private double speed;
	
	private AID[] sellerAgents;
	
	public General(ContinuousSpace<Object> space, Grid<Object> grid, int x, int y) {
		this.space = space;
		this.grid = grid;
		this.posx =x;
		this.posy =y;
		this.visionRadius = 2; 
		this.speakRadius = 50;
	};
	
	protected void setup() {
		
		speed = 1;

		space.moveTo(this, posx, posy);
		grid.moveTo(this, (int) posx, (int) posy);

		addBehaviour(new SearchForExit(this,1));
		addBehaviour(new GeneralRandomMovement(this,1));
		addBehaviour(new MessageListener());
		addBehaviour(new GeneralMessages());
		
		// Register the book-selling service in the yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("ShareGeneralExit");
		sd.setName("JADE-ShareGeneralExit");
		dfd.addServices(sd);
		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}
		
		System.out.println("criei o general");
		
	}
	
	private class GeneralRandomMovement extends TickerBehaviour {
		public GeneralRandomMovement(Agent a, long period) {
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
	
	private class GeneralMessages extends Behaviour {

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
					
					addBehaviour(new ShareExit());
					
				} catch (NullPointerException e) {
					
				}
				
				msgtemp = MessageTemplate.MatchConversationId("share_general_exit");
				reply = myAgent.receive(msgtemp);

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
	
	private class ShareExit extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;

		@Override
		public void action() {
			// Update the list of Captains
			DFAgentDescription template = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			sd.setType("ShareGeneralExit");
			template.addServices(sd);
			try {
				DFAgentDescription[] result = DFService.search(myAgent, template);
				
				sellerAgents = new AID[result.length];
				for (int i = 0; i < result.length; ++i) {
					sellerAgents[i] = result[i].getName();
				}
			} catch (FIPAException fe) {
				fe.printStackTrace();
			}

			// Send the goal to all Captains
			ACLMessage inform = new ACLMessage(ACLMessage.INFORM);
			for (int i = 0; i < sellerAgents.length; ++i) {
				inform.addReceiver(sellerAgents[i]);
			}
			inform.setContent(exitx + "-" + exity);
			inform.setConversationId("share_general_exit");
			inform.setReplyWith("cfp" + System.currentTimeMillis());
			myAgent.send(inform);
			
		}
	}
	
	
}
