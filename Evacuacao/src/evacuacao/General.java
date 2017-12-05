package evacuacao;

import java.util.ArrayList;
import java.util.List;

import sajas.core.AID;
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
import sajas.core.behaviours.SimpleBehaviour;
import sajas.core.behaviours.TickerBehaviour;
import sajas.domain.DFService;

public class General extends Agent {
	private ContinuousSpace<Object> space;
	private Grid<Object> grid;
	private double posx, posy, investigateX, investigateY ;
	private int visionRadius, speakRadius;
	private List<AID> soldiers;
	private double mapx, mapy;
	
	private State stage=State.MOVING;
	
	//exit
	private int exitx = -1,exity=-1;
	
	private double speed;
	
	private int type_of_game;
	
	private AID[] sellerAgents;
	
	public General(ContinuousSpace<Object> space, Grid<Object> grid, int x, int y) {
		this.space = space;
		this.grid = grid;
		this.posx =x;
		this.posy =y;
		this.visionRadius = 2; 
		this.speakRadius = 1;
		type_of_game = 0;
	};
	
	public General(ContinuousSpace<Object> space, Grid<Object> grid, int x, int y, double investigateX, double investigateY, List<AID> list, double mapx, double mapy, int vision_radius, int speak_radius) {
		this.space = space;
		this.grid = grid;
		this.posx =x;
		this.posy =y;
		this.visionRadius = vision_radius; 
		this.speakRadius = speak_radius;
		this.investigateX=investigateX;
		this.investigateY=investigateY;
		this.soldiers=list;
		this.mapx=mapx;
		this.mapy=mapy;
		type_of_game=2;
	};

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
		if (distance >1)
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
	
	protected void setup() {
		
		speed = 1;

		space.moveTo(this, posx, posy);
		grid.moveTo(this, (int) posx, (int) posy);

		addBehaviour(new SearchForExit());
		addBehaviour(new MessageListener());
		addBehaviour(new GeneralMessages());
		
		if(type_of_game==0)
			addBehaviour(new GeneralRandomMovement());
		else {
			addBehaviour(new TellSoldiersArea());
			addBehaviour(new GeneralMovementToArea());
		}
		
		
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
	
	
	
	private class GeneralMovementToArea extends Behaviour {

		private static final long serialVersionUID = 1L;

		@Override
		public void action() {
			switch(stage) {
			case MOVING:
				moveToPlace(investigateX,investigateY);
				break;
			case FOUND_EXIT:
				moveToPlace(exitx,exity);
				break;
			}
		}

		@Override
		public boolean done() {
			// TODO Auto-generated method stub
			return false;
		}
	}
	
	private class GeneralRandomMovement extends Behaviour {

		private static final long serialVersionUID = 1L;

		@Override
		public void action() {
			//System.out.println("vou mover o agente " + myAgent.getName());
			
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
					exitx=goal.getX();
					exity=goal.getY();
					stage=State.FOUND_EXIT;
					addBehaviour(new ShareExit());
					transmitNewsToNearbySoldiers(exitx + "-" + exity,"inform_exit",false);
				}
			}
		}

		@Override
		public boolean done() {
			// TODO Auto-generated method stub
			return false;
		}

	}
	
	private void transmitNewsToNearbySoldiers(String content, String id,Boolean restriction) {
		GridPoint pt = grid.getLocation(this);
		GridCellNgh<Soldier> nghCreator = new GridCellNgh<Soldier>(grid, pt, Soldier.class, speakRadius, speakRadius);
		List<GridCell<Soldier>> gridCells = nghCreator.getNeighborhood(true);
		ACLMessage message_inform = new ACLMessage(ACLMessage.INFORM);
		int counter=0;
		for (GridCell<Soldier> cell : gridCells) {
			if (cell.size() > 0) {
				
				for (Object  obj : grid.getObjectsAt(cell.getPoint().getX(), cell.getPoint().getY ())) {
					if (obj  instanceof  Soldier) {
						if(restriction && soldiers.contains(((Soldier) obj).getAID())) {
							message_inform.addReceiver(((Soldier) obj).getAID());
						}
						else if(!restriction) message_inform.addReceiver(((Soldier) obj).getAID());
						counter++;
						
					}
				}		
			}
		}
		System.out.println("counter "+ counter);
		message_inform.setContent(content);
		message_inform.setConversationId(id);
		message_inform.setReplyWith(id + " " + System.currentTimeMillis());
		this.send(message_inform);
	}
	
	private class TellSoldiersArea extends OneShotBehaviour{

		private static final long serialVersionUID = 1L;

		@Override
		public void action() {
			double distance;
			if(2*visionRadius<=speakRadius)
				distance=2*visionRadius;
			else distance = speakRadius;
			String content=myAgent.getAID().toString();
			for(int i=0;i<soldiers.size();i++) {
				double realDistance = (investigateY+distance*(i+1))%mapy;
				content+="/-/"+soldiers.get(i).toString()+"/-/"+investigateX + "/-/" + realDistance;
			}	
			transmitNewsToNearbySoldiers(content,"follow_me",true);
		}		
	}
	
	private class GeneralMessages extends Behaviour {

		private static final long serialVersionUID = 1L;
		
		@Override
		public void action() {
			GridPoint pt = grid.getLocation(myAgent);
			if(exitx!=-1) {
				System.out.println("vou avisar os agentes " + myAgent.getAID());
				transmitNewsToNearbySoldiers(exitx + "-" + exity,"inform_exit",false);
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
		private List<String> arrived =new ArrayList<String>();
		
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
					stage=State.FOUND_EXIT;
					transmitNewsToNearbySoldiers(message,"inform_exit",false);
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
					stage=State.FOUND_EXIT;
					System.out.println("vou transmitir " + myAgent.getAID());
					transmitNewsToNearbySoldiers(message,"inform_exit",false);
				} catch (NullPointerException e) {
					
				}
				
				while(true) {
					msgtemp = MessageTemplate.MatchConversationId("arrived");
					reply = myAgent.receive(msgtemp);

					try {
						String message = reply.getContent();
						//System.out.println(myAgent.getName());
						if(!arrived.contains(message)) {
							for(int i=0;i<soldiers.size();i++) {
								
								if(soldiers.get(i).toString().equals(message)) {
									arrived.add(message);
									if(arrived.size()==soldiers.size()) {
										
										investigateX+=speed;
										investigateX = investigateX%mapy;
										arrived.clear();
										addBehaviour(new TellSoldiersArea());
									}
										
									break;
								}
							}
							
						}
					} catch (NullPointerException e) {
						break;
					}
				}
				
				
				
				
			}
			
		}
	}
	
	private class ShareExit extends OneShotBehaviour {
		private static final long serialVersionUID = 1L;

		@Override
		public void action() {
			
			DFAgentDescription template = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			sd.setType("ShareGeneralExit");
			template.addServices(sd);
			try {
				DFAgentDescription[] result = DFService.search(myAgent, template);
				
				sellerAgents = new AID[result.length];
				for (int i = 0; i < result.length; ++i) {
					sellerAgents[i] = (AID) result[i].getName();
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
