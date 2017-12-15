package evacuacao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import evacuacao.Soldier.AskForHelp;
import sajas.core.AID;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
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
import sajas.proto.ContractNetInitiator;
import sajas.proto.ContractNetResponder;

public class General extends MovableAgent {

	private double posx, posy, investigateX, investigateY ;
	private List<AID> soldiers;
	private double mapx, mapy;
	private int direction = 0;
	
	
	//exit
	private int exitx = -1, exity = -1;
	
	private static double speed=1;
	
	private int type_of_game;
	
	private AID[] sellerAgents;
	private Boolean hasTransmitedPosition = false;
	
	private Boolean greedy;
	
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
	
	public General(ContinuousSpace<Object> space, Grid<Object> grid, int x, int y, double investigateX, double investigateY, List<AID> list, double mapx, double mapy, int vision_radius, int speak_radius, int type_of_game) {
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
		this.type_of_game=type_of_game;
		if(type_of_game==2)
			greedy=false;
		else if(type_of_game==3)
			greedy=true;
		stage=State.MOVING;
		
	};
	
	protected void setup() {
		space.moveTo(this, posx, posy);
		grid.moveTo(this, (int) posx, (int) posy);

		addBehaviour(new ShareMapGeneral());
		addBehaviour(new SearchForExit());
		
		addBehaviour(new MessageListener());
		addBehaviour(new GeneralMessages());
		
		if(type_of_game==0)
			addBehaviour(new GeneralRandomMovement());
		else {
			addBehaviour(new TellSoldiersArea());
			addBehaviour(new GeneralMovementToArea());
		}
		
		addBehaviour(new AnswerCallBehaviour());
		
		id=this.getLocalName().replaceAll("\\D+","");
		
		// Register the book-selling service in the yellow pages
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("ShareGeneralExit");
		sd.setName("JADE-ShareGeneralExit");
		dfd.addServices(sd);
		sd = new ServiceDescription();
		sd.setType("ShareMapGeneral");
		sd.setName("JADE-ShareMapGeneral");
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
			NdPoint myPoint;
			switch(stage) {
			case WAITING_FOR_ANSWER:
				if(wallToBeAbolished.wasDestroyed())
					stage=State.MOVING;
				break;
			case HELPING:
				myPoint = moveToPlace(helpX,helpY,greedy);
				if(myPoint!=null &&  myPoint.getX()==helpX && myPoint.getY()==helpY) {
					abolishWall();
					stage=State.MOVING;
				}
				break;
			case MOVING:
				if(hasTransmitedPosition){
					myPoint = moveToPlace(investigateX,investigateY,true);
					if(myPoint!=null && myPoint.getX()==investigateX && myPoint.getY()==investigateY) {
						stage=State.FINISHED_MOVING;				
					}
				}
				break;
			case FOUND_EXIT:
				myPoint = moveToPlace(exitx,exity,greedy);
				if(myPoint.getX()==exitx && myPoint.getY()==exity) {
					stage = State.IS_IN_EXIT;
					System.out.println("oi " + RunEnvironment.getInstance().getCurrentSchedule().getTickCount());
					
					myAgent.removeBehaviour(this);				
				}
				break;
			case ASKING_FOR_HELP:
				ACLMessage msg = new ACLMessage(ACLMessage.CFP);
				msg.setOntology("cryForHelp");
				msg.setPerformative(ACLMessage.CFP);
				msg.setConversationId("request_general"+id);
                msg.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
                msg.setContent(space.getLocation(myAgent).getX() + "-" + space.getLocation(myAgent).getY());
                Date date = new Date();
				date.setSeconds(date.getSeconds()+2);
				msg.setReplyByDate(date);
				
                GridPoint pt = grid.getLocation(myAgent);
				
				GridCellNgh<General> nghCreatorGeneral = new GridCellNgh<General>(grid, pt, General.class, speakRadius, speakRadius);
				List<GridCell<General>> gridCellsGeneral = nghCreatorGeneral.getNeighborhood(true);
				int counter=0;
				for (GridCell<General> cell : gridCellsGeneral) {
					if (cell.size() > 0) {
						
						for (Object  obj : grid.getObjectsAt(cell.getPoint().getX(), cell.getPoint().getY ())) {
							if (obj  instanceof  General && !((General) obj).getAID().equals(myAgent.getAID())) {
								msg.addReceiver(((General) obj).getAID());
								counter++;
							}
						}
					}
				}
					
				GridCellNgh<Soldier> nghCreatorGeneral1 = new GridCellNgh<Soldier>(grid, pt, Soldier.class, speakRadius, speakRadius);
				List<GridCell<Soldier>> gridCellsGeneral1 = nghCreatorGeneral1.getNeighborhood(true);
					
					
				for (GridCell<Soldier> cell1 : gridCellsGeneral1) {
					if (cell1.size() > 0) {
						
						for (Object  obj : grid.getObjectsAt(cell1.getPoint().getX(), cell1.getPoint().getY ())) {
							if (obj  instanceof  Soldier) {
								msg.addReceiver(((Soldier) obj).getAID());
								counter++;
							}
						}
					}
				}

				System.out.println("help me " + myAgent.getLocalName());
                
                if(counter>0) {
                	stage=State.WAITING_FOR_ANSWER;
                	addBehaviour(new AskForHelp(msg));
                }
                else stage=State.MOVING;
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
			
			if(exitx==-1) {
				moveRandom();
			} else {
				NdPoint myPoint = moveToPlace(exitx,exity,true);
				if(myPoint.getX()==exitx && myPoint.getY()==exity) {
					stage = State.IS_IN_EXIT;
					System.out.println(RunEnvironment.getInstance().getCurrentSchedule().getTickCount());
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
					//stage=State.FOUND_EXIT;
					myAgent.removeBehaviour(this);
					
					addBehaviour(new ShareExit());
					transmitNewsToNearbySoldiers(myAgent.getAID()+"/"+0+"/-/"+Arrays.deepToString(myMap),"shareMap",false);
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
	
	private int transmitNewsToNearbySoldiers(String content, String id,Boolean restriction) {
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
		message_inform.setContent(content);
		message_inform.setConversationId(id);
		message_inform.setReplyWith(id + " " + System.currentTimeMillis());
		this.send(message_inform);
		return counter;
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
			hasTransmitedPosition=true;
		}		
	}
	
	private class GeneralMessages extends Behaviour {

		private static final long serialVersionUID = 1L;
		
		@Override
		public void action() {
			if(exitx!=-1) {
				if(transmitNewsToNearbySoldiers(exitx + "-" + exity,"inform_exit",false)>0)
					stage=State.FOUND_EXIT;
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
		private List<String> maps = new ArrayList<String>();
		
		@Override
		public void action() {
			
			while(true) {
				MessageTemplate msgtemp = MessageTemplate.MatchConversationId("ShareMapGeneral");
				ACLMessage reply = myAgent.receive(msgtemp);
				try {	
					String message = reply.getContent();
					String[] coords = message.split("/-/");
					if(!maps.contains(coords[0])) {
						maps.add(coords[0]);
						String[] map = coords[1].replaceAll("\\[", "").replaceAll("\\]", "").replaceAll("\\s", "").split(",");
						buildMapFromString(map);	
					}
					
				} catch (NullPointerException e) {
					break;
				}
			}
			
			if(exitx==-1) {
				MessageTemplate msgtemp = MessageTemplate.MatchConversationId("inform_exit");
				ACLMessage reply = myAgent.receive(msgtemp);

				try {
					String message = reply.getContent();
					String[] coords = message.split("-");
					exitx = Integer.parseInt(coords[0]);
					exity = Integer.parseInt(coords[1]);
					//stage=State.FOUND_EXIT;
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
					//stage=State.FOUND_EXIT;
					transmitNewsToNearbySoldiers(message,"inform_exit",false);
				} catch (NullPointerException e) {
					
				}
				
				while(true) {
					msgtemp = MessageTemplate.MatchConversationId("shareMap");
					reply = myAgent.receive(msgtemp);
					try {	
						String message = reply.getContent();
						String[] coords = message.split("/-/");
						if(!maps.contains(coords[0])) {
							maps.add(coords[0]);
							String[] map = coords[1].replaceAll("\\[", "").replaceAll("\\]", "").replaceAll("\\s", "").split(",");
							buildMapFromString(map);	
						}
						
					} catch (NullPointerException e) {
						break;
					}
				}
				
				
				
				if(stage==State.FINISHED_MOVING) {
					
					while(true) {
						msgtemp = MessageTemplate.MatchConversationId("arrived");
						reply = myAgent.receive(msgtemp);

						try {
							String message = reply.getContent();
							String[] coords = message.split("/-/");
							if(!arrived.contains(message)) {
								hasTransmitedPosition=false;
								for(int i=0;i<soldiers.size();i++) {
									
									if(soldiers.get(i).toString().equals(coords[0])) {
										
										arrived.add(message);
										if(arrived.size()%soldiers.size()==0) {
											stage=State.MOVING;	
											//arrived.clear();
											if(((int)(investigateX+speed+visionRadius)<=mapx && direction==0) || (direction==1 && (int)(investigateX-speed-visionRadius)>=0)) {
												
											}else {
												if(direction==1)
													direction=0;
												else direction=1;
												investigateY+=1+soldiers.size();
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
	
	private class ShareMapGeneral extends Behaviour {
		private static final long serialVersionUID = 1L;
		private int counter=1;
		@Override
		public void action() {
			
			updateMap();
			
			DFAgentDescription template = new DFAgentDescription();
			ServiceDescription sd = new ServiceDescription();
			sd.setType("ShareMapGeneral");
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
			inform.setContent(myAgent.getAID()+"/"+counter+"/-/"+Arrays.deepToString(myMap));
			inform.setConversationId("ShareMapGeneral");
			inform.setReplyWith("cfp" + System.currentTimeMillis());
			myAgent.send(inform);
			transmitNewsToNearbySoldiers(myAgent.getAID()+"/"+counter+"/-/"+Arrays.deepToString(myMap),"shareMap",false);
			counter++;
			
		}
		@Override
		public boolean done() {
			// TODO Auto-generated method stub
			return false;
		}
	}
	
	public class AskForHelp extends ContractNetInitiator {

		private static final long serialVersionUID = 1L;

		AskForHelp(ACLMessage msg) {
            super(General.this, msg);
        }

        @Override
        public void handleAllResponses(Vector proposes, Vector responses) {
            double minCost = Double.MAX_VALUE;
            System.out.println("entrei aqui " + proposes.size() + " " + responses.size());
        	
            ACLMessage minCostProposal = null;
            for (Object proposeObj : proposes) {
            	if(proposeObj!=null) {
            		ACLMessage propose = (ACLMessage) proposeObj;
                    double cost = Double.parseDouble(propose.getContent());
                    if (cost < minCost) {
                        minCost = cost;
                        if (minCostProposal != null) {
                            ACLMessage response = minCostProposal.createReply();
                            response.setPerformative(ACLMessage.REJECT_PROPOSAL);
                            responses.add(response);
                        }

                        minCostProposal = propose;
                    } else {
                        ACLMessage response = propose.createReply();
                        response.setPerformative(ACLMessage.REJECT_PROPOSAL);
                        responses.add(response);
                    }
            	} 
            }

            if (minCostProposal != null) {
                ACLMessage selectedMessage = minCostProposal.createReply();
                selectedMessage.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                selectedMessage.setContent(space.getLocation(myAgent).getX() + "-" + space.getLocation(myAgent).getY());
                helper=(sajas.core.AID) selectedMessage.getSender();
                
                GridPoint pt = grid.getLocation(myAgent);
				
				GridCellNgh<General> nghCreatorGeneral = new GridCellNgh<General>(grid, pt, General.class, speakRadius, speakRadius);
				List<GridCell<General>> gridCellsGeneral = nghCreatorGeneral.getNeighborhood(true);
				
				for (GridCell<General> cell : gridCellsGeneral) {
					if (cell.size() > 0) {
						
						for (Object  obj : grid.getObjectsAt(cell.getPoint().getX(), cell.getPoint().getY ())) {
							if (obj  instanceof  General && !((General) obj).getAID().equals(myAgent.getAID())) {
								((General) obj).setBerlimWall(wallToBeAbolished);
								break;
							}
						}
					}
				}
					
				GridCellNgh<Soldier> nghCreatorGeneral1 = new GridCellNgh<Soldier>(grid, pt, Soldier.class, speakRadius, speakRadius);
				List<GridCell<Soldier>> gridCellsGeneral1 = nghCreatorGeneral1.getNeighborhood(true);
					
					
				for (GridCell<Soldier> cell1 : gridCellsGeneral1) {
					if (cell1.size() > 0) {
						
						for (Object  obj : grid.getObjectsAt(cell1.getPoint().getX(), cell1.getPoint().getY ())) {
							if (obj  instanceof  Soldier) {
								((Soldier) obj).setBerlimWall(wallToBeAbolished);
								break;
							}
						}
					}
				}
                responses.add(selectedMessage);
            }
        }

    }
	
	private class AnswerCallBehaviour extends ContractNetResponder {

        AnswerCallBehaviour() {
        	super(General.this, MessageTemplate.and(
                    MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET),  MessageTemplate.MatchPerformative(ACLMessage.CFP)));
        	}

        @Override
        public ACLMessage handleCfp(ACLMessage message) {
        	
        	if(stage==State.WAITING_FOR_DECISION || stage==State.HELPING)
        		return null;
        	
            ACLMessage response = message.createReply();
            response.setPerformative(ACLMessage.PROPOSE);
            System.out.println("recebi esta mensagem " + message.getContent() + " " + myAgent.getAID()+ " " + message.getConversationId() + " " + id);
            String[] coordinates = message.getContent().split("-");
            double cost = Math.abs(Double.parseDouble(coordinates[0])-space.getLocation(myAgent).getX())+ Math.abs(Double.parseDouble(coordinates[1])-space.getLocation(myAgent).getY());
            System.out.println("mandei cost " + myAgent.getAID());
            response.setContent("" + cost);
            
            stage=State.WAITING_FOR_DECISION;
            ForceReturnToState();
            return response;
        }

        @Override
        public ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept) {
           	System.out.println("recebi confirmacao " + myAgent.getAID());
            ACLMessage response = accept.createReply();
            String[] coordinates = accept.getContent().split("-");
            helpX=Double.parseDouble(coordinates[0]);
            helpY=Double.parseDouble(coordinates[1]);
            stage=State.HELPING;
            response.setPerformative(ACLMessage.INFORM);
            return response;
        }
        
        public void handleRejectProposal(ACLMessage cfp, ACLMessage propose, ACLMessage reject) {
        	 System.out.println("recebi reject " + myAgent.getAID());
        	stage=State.MOVING;
        }
    }
	
}
