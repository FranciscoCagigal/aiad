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

public class General extends MovableAgent {

	private double posx, posy, investigateX, investigateY ;
	private List<AID> soldiers;
	private double mapx, mapy;
	private int direction = 0;
	
	private State stage=State.MOVING;
	
	//exit
	private int exitx = -1, exity = -1;
	
	private static double speed=1;
	
	private int type_of_game;
	
	private AID[] sellerAgents;
	private Boolean hasTransmitedPosition = false;
	
	public General(ContinuousSpace<Object> space, Grid<Object> grid, int x, int y, int vision_radius, int speak_radius) {
		super(space,grid,speed,vision_radius,speak_radius);
		this.space = space;
		this.grid = grid;
		this.posx =x;
		this.posy =y;
		this.visionRadius = vision_radius; 
		this.speakRadius = speak_radius;
		type_of_game = 0;
	};
	
	public General(ContinuousSpace<Object> space, Grid<Object> grid, int x, int y, double investigateX, double investigateY, List<AID> list, double mapx, double mapy, int vision_radius, int speak_radius) {
		super(space,grid,speed,vision_radius,speak_radius);
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
	
	protected void setup() {
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
	}
	
	
	
	private class GeneralMovementToArea extends Behaviour {

		private static final long serialVersionUID = 1L;

		@Override
		public void action() {
			switch(stage) {
			case MOVING:
				if(hasTransmitedPosition){
					moveToPlace(investigateX,investigateY);
				}
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
			System.out.println("tou a chamar o random do general");
			if(exitx==-1) {
				moveRnd();
			} else {
				NdPoint myPoint = moveToPlace(exitx,exity);
				if(myPoint.getX()==exitx && myPoint.getY()==exity) {
					stage = State.IS_IN_EXIT;
					myAgent.removeBehaviour(this);
				}
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
					myAgent.removeBehaviour(this);
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
		System.out.println("minha posicao "+ pt);
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
				System.out.println("nova posicao "+ investigateY);
				double realDistance = (investigateY+distance*(i+1))%mapy;
				content+="/-/"+soldiers.get(i).toString()+"/-/"+investigateX + "/-/" + realDistance;
			}	
			transmitNewsToNearbySoldiers(content,"follow_me",true);
			hasTransmitedPosition=true;
		}		
	}
	
	private class GeneralMessages extends Behaviour {

		private static final long serialVersionUID = 1L;
		
		@Override
		public void action() {
			if(exitx!=-1) {
				transmitNewsToNearbySoldiers(exitx + "-" + exity,"inform_exit",false);
				if(stage == State.IS_IN_EXIT)
					myAgent.removeBehaviour(this);
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
					transmitNewsToNearbySoldiers(message,"inform_exit",false);
				} catch (NullPointerException e) {
					
				}
				
				while(true) {
					msgtemp = MessageTemplate.MatchConversationId("arrived");
					reply = myAgent.receive(msgtemp);

					try {
						String message = reply.getContent();
						System.out.println("recebi arrived");
						if(!arrived.contains(message)) {
							hasTransmitedPosition=false;
							for(int i=0;i<soldiers.size();i++) {
								
								if(soldiers.get(i).toString().equals(message)) {
									arrived.add(message);
									if(arrived.size()==soldiers.size()) {
										arrived.clear();
										if(((int)(investigateX+speed+visionRadius)<=mapx && direction==0) || (direction==1 && (int)(investigateX-speed-visionRadius)>=0)) {
											
										}else {
											if(direction==1)
												direction=0;
											else direction=1;
											investigateY+=1;
										}
										if(direction==0)
											investigateX+=speed;
										else investigateX-=speed;
										
										
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
