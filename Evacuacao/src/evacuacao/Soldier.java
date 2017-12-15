package evacuacao;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import evacuacao.General.AskForHelp;
import jade.core.AID;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.Direction;
import repast.simphony.space.SpatialMath;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridPoint;
import sajas.core.behaviours.Behaviour;
import sajas.core.behaviours.CyclicBehaviour;
import sajas.core.behaviours.OneShotBehaviour;
import sajas.proto.ContractNetInitiator;
import sajas.proto.ContractNetResponder;

public class Soldier extends MovableAgent {
	
	private double posX, posY;
	
	//exit
	private int exitX = -1, exitY = -1;
	
	//general goal
	private double generalPlaceX, generalPlaceY;
	private jade.core.AID myGeneral;
	
	private Boolean canDelete = false;
	
	private static double speed=1;
	
	private int type_of_game;
	private int MAPX = 50, MAPY=50;
	
	private Boolean greedy;
	
	
	public Soldier(ContinuousSpace<Object> space, Grid<Object> grid, double x, double y, int vision_radius, int speak_radius, int type_of_game) {
		super(space,grid,speed,vision_radius,speak_radius);
		this.space = space;
		this.grid = grid;
		this.posX =x;
		this.posY =y;
		this.visionRadius = vision_radius;
		this.speakRadius = speak_radius;
		this.type_of_game = type_of_game;
		if(type_of_game==2)
			greedy=false;
		else if(type_of_game==3)
			greedy=true;
		

		stage = State.WAITING_FOR_START;
		
		if(type_of_game<2)
			stage = State.MOVING;
		
	};
	
	protected void setup() {

		space.moveTo(this, posX, posY);
		grid.moveTo(this, (int) posX, (int) posY);
		updateMap();
		addBehaviour(new ShareMap());
		addBehaviour(new SearchForExit());
		addBehaviour(new SoldierMessages());
		addBehaviour(new MessageListener());
		addBehaviour(new AnswerCallBehaviour());
		
		if(type_of_game==0)
			addBehaviour(new SoldierRandomMovement());
		else if(type_of_game==1) 
			addBehaviour(new SoldierRandomCoordenatedMovement());
		else addBehaviour(new SoldierSuperCoordinatedRandomMovement());
		
		id=this.getLocalName().replaceAll("\\D+","");
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
		@SuppressWarnings("deprecation")
		@Override
		public void action() {
			NdPoint  myPoint;
			switch(stage) {
			case HELPING:
				myPoint = moveToPlace(helpX,helpY,greedy);
				
				if(myPoint!=null &&  myPoint.getX()==helpX && myPoint.getY()==helpY) {
					abolishWall();
					stage=State.MOVING;
				}
				break;
			case FINISHED_MOVING:
				if(checkForSpecificCapitan(speakRadius,myGeneral)) {
					myAgent.addBehaviour(new WarnGeneralArrival());
				}
					
				else {
					transmitNewsToNearbySoldiers(myAgent.getAID().toString()+"/-/"+generalPlaceX+"/-/"+generalPlaceY+"/-/"+myGeneral.toString(),"transmit_arrival");
				}
				break;
			case MOVING:
				myPoint = moveToPlace(generalPlaceX,generalPlaceY,true);
				if(myPoint!=null &&  myPoint.getX()==generalPlaceX && myPoint.getY()==generalPlaceY) {
					stage=State.FINISHED_MOVING;
					if(checkForSpecificCapitan(speakRadius,myGeneral)) {
						myAgent.addBehaviour(new WarnGeneralArrival());
					}
						
					else {
						transmitNewsToNearbySoldiers(myAgent.getAID().toString()+"/-/"+generalPlaceX+"/-/"+generalPlaceY+"/-/"+myGeneral.toString(),"transmit_arrival");
					}
				}
				break;
			case FOUND_EXIT:
				
				myPoint = moveToPlace(exitX,exitY,greedy);
				if(myPoint.getX()==exitX && myPoint.getY()==exitY) {
					stage = State.IS_IN_EXIT;
					System.out.println("oi " + RunEnvironment.getInstance().getCurrentSchedule().getTickCount());
					myAgent.removeBehaviour(this);
				}
				break;
			case WAITING_FOR_ANSWER:
				if(wallToBeAbolished.wasDestroyed())
					stage=State.MOVING;
				break;
			
			case ASKING_FOR_HELP:
				ACLMessage msg = new ACLMessage(ACLMessage.CFP);
                msg.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
                msg.setContent(space.getLocation(myAgent).getX() + "-" + space.getLocation(myAgent).getY());
                msg.setOntology("cryForHelp");
                msg.setPerformative(ACLMessage.CFP);
				msg.setConversationId("request_soldier"+id);
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
							if (obj  instanceof  General) {
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
							if (obj  instanceof  Soldier && !((Soldier) obj).getAID().equals(myAgent.getAID())) {
								msg.addReceiver(((Soldier) obj).getAID());
								counter++;
							}
						}
					}
				}
                
                String str = myAgent.getLocalName().replaceAll("\\D+","");
                System.out.println("help me " + myAgent.getLocalName());
                addBehaviour(new AskForHelp(msg));
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
	
	
	
	private class SoldierRandomMovement extends Behaviour {

		private static final long serialVersionUID = 1L;
		@Override

		public void action() {
			NdPoint myPoint;
			switch(stage) {
			case HELPING:
				System.out.println("tenho de mover " + helpX + " " + helpY);
				myPoint = moveToPlace(helpX,helpY,true);
				
				if(myPoint!=null &&  myPoint.getX()==helpX && myPoint.getY()==helpY) {
					abolishWall();
					System.out.println("demolir parede");
					stage=State.MOVING;
				}
				break;
			case MOVING:
				moveRandom();
				break;
			case FOUND_EXIT:
				
				myPoint = moveToPlace(exitX,exitY,true);
				if(myPoint.getX()==exitX && myPoint.getY()==exitY) {
					stage = State.IS_IN_EXIT;
					System.out.println("oi " + RunEnvironment.getInstance().getCurrentSchedule().getTickCount());
					myAgent.removeBehaviour(this);
				}
				break;
			
			case WAITING_FOR_ANSWER:
				if(wallToBeAbolished.wasDestroyed())
					stage=State.MOVING;
				break;
			
			case ASKING_FOR_HELP:
				ACLMessage msg = new ACLMessage(ACLMessage.CFP);
                msg.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
                DecimalFormat format = new DecimalFormat("#0.000");
                
                msg.setContent(Math.round(space.getLocation(myAgent).getX() * 1000d) / 1000d + "-" + Math.round(space.getLocation(myAgent).getY() * 1000d) / 1000d);
                msg.setOntology("cryForHelp");
                msg.setPerformative(ACLMessage.CFP);
				msg.setConversationId("request_soldier"+id);
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
							if (obj  instanceof  General) {
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
							if (obj  instanceof  Soldier && !((Soldier) obj).getAID().equals(myAgent.getAID())) {
								msg.addReceiver(((Soldier) obj).getAID());
								counter++;
							}
						}
					}
				}
                
                String str = myAgent.getLocalName().replaceAll("\\D+","");
                System.out.println("help me " + myAgent.getLocalName());
                addBehaviour(new AskForHelp(msg));
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
	
	private class ShareMap extends Behaviour {

		private static final long serialVersionUID = 1L;
		
		private boolean[][] auxMyMap;
		private int counter=1;
		
		@Override
		public void action() {	
			if(stage == State.IS_IN_EXIT) {
				myAgent.removeBehaviour(this);
			}
			if(auxMyMap==null || compareArrays(auxMyMap,myMap)) {
				transmitNewsToNearbySoldiers(myAgent.getAID()+"/"+counter+"/-/"+Arrays.deepToString(myMap),"shareMap");
				transmitNewsToNearbyGeneral(myAgent.getAID()+"/"+counter+"/-/"+Arrays.deepToString(myMap),"shareMap");
				counter++;
			}
			
		}

		@Override
		public boolean done() {
			// TODO Auto-generated method stub
			return false;
		}
	}
	
	public static Boolean compareArrays(boolean[][] array1, boolean[][] array2) {
	    for (int i = 0; i < array1.length; i++) {

	        for (int a = 0; a < array1[i].length; a++) {

	            if (array2[i][a] != array1[i][a]) {
		            return false;
	            }
	        }
	    }
	    return true;
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
			if(stage == State.MOVING){
				myAgent.removeBehaviour(this);
				return;
			}
			ACLMessage message_inform = new ACLMessage(ACLMessage.INFORM);
			
			message_inform.addReceiver(myGeneral);
			message_inform.setContent(myAgent.getAID().toString()+"/-/"+generalPlaceX+"/-/"+generalPlaceY);
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
					//stage=State.FOUND_EXIT;
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
				List<GridCell<Soldier>> gridCells = nghCreator.getNeighborhood(true);
				ACLMessage message_inform = new ACLMessage(ACLMessage.INFORM);
				int counter=0;
				for (GridCell<Soldier> cell : gridCells) {
					if (cell.size() > 0) {
						
						for (Object  obj : grid.getObjectsAt(cell.getPoint().getX(), cell.getPoint().getY ())) {
							if (obj  instanceof  Soldier) {
								message_inform.addReceiver(((Soldier) obj).getAID());
								counter++;
							}
						}		

						message_inform.setContent(exitX + "-" + exitY);
						message_inform.setConversationId("inform_exit");
						message_inform.setReplyWith("inform_exit " + System.currentTimeMillis());
						myAgent.send(message_inform);
					}
				}
				
				GridCellNgh<General> nghCreatorGen = new GridCellNgh<General>(grid, pt, General.class, speakRadius, speakRadius);
				List<GridCell<General>> gridCellsGen = nghCreatorGen.getNeighborhood(true);
				message_inform = new ACLMessage(ACLMessage.INFORM);
				for (GridCell<General> cell : gridCellsGen) {
					if (cell.size() > 0) {
						
						for (Object  obj : grid.getObjectsAt(cell.getPoint().getX(), cell.getPoint().getY ())) {
							if (obj  instanceof  General) {
								message_inform.addReceiver(((General) obj).getAID());
								counter++;
							}
						}		

						message_inform.setContent(exitX + "-" + exitY);
						message_inform.setConversationId("inform_exit");
						message_inform.setReplyWith("inform_exit " + System.currentTimeMillis());
						myAgent.send(message_inform);
					}
				}
				
				if(counter>0) {
					stage=State.FOUND_EXIT;
				}
				
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
		private List<String> arrivals = new ArrayList<String>();
		private List<String> maps = new ArrayList<String>();

		@Override
		public void action() {
			if(stage == State.IS_IN_EXIT)
				myAgent.removeBehaviour(this);
			
			while(true) {
				MessageTemplate msgtemp = MessageTemplate.MatchConversationId("shareMap");
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
			
			MessageTemplate msgtemp = MessageTemplate.MatchConversationId("inform_exit");
			ACLMessage reply = myAgent.receive(msgtemp);
			
			try {
				String message = reply.getContent();
				String[] coords = message.split("-");
				exitX = Integer.parseInt(coords[0]);
				exitY = Integer.parseInt(coords[1]);
				//stage=State.FOUND_EXIT;
				transmitNewsToNearbySoldiers(myAgent.getAID()+"/"+0+"/-/"+Arrays.deepToString(myMap),"shareMap");
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
							if(!messages[0].equals(myAgent.getAID().toString()) && !reply.getSender().toString().equals(myAgent.getAID().toString())) {
								arrivals.add(message);
								if(myGeneral.toString().equals(messages[3]) && checkForSpecificCapitan(speakRadius,myGeneral)) {
									ACLMessage message_inform = new ACLMessage(ACLMessage.INFORM);						
									message_inform.addReceiver(myGeneral);
									message_inform.setContent(messages[0]+"/-/"+messages[1]+"/-/"+messages[2]);
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
	
	public class AskForHelp extends ContractNetInitiator {
		private static final long serialVersionUID = 1L;

		AskForHelp(ACLMessage msg) {
            super(Soldier.this, msg);
            System.out.println("pedi ajuda ");
        }

        @Override
        public void handleAllResponses(Vector proposes, Vector responses) {
            
        	System.out.println("entrei aqui " + proposes.size() + " " + responses.size());
        	
        	double minCost = Integer.MAX_VALUE;
            ACLMessage minCostProposal = null;

            for (Object proposeObj : proposes) {
            	
            	if(proposeObj!=null) {
	                ACLMessage propose = (ACLMessage) proposeObj;
	                System.out.println("o custo recebido é de " + propose.getContent());
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
                helper=(sajas.core.AID) selectedMessage.getSender();
                selectedMessage.setContent(Math.round(space.getLocation(myAgent).getX() * 1000d) / 1000d + "-" + Math.round(space.getLocation(myAgent).getY() * 1000d) / 1000d);
                
                GridPoint pt = grid.getLocation(myAgent);
				
				GridCellNgh<General> nghCreatorGeneral = new GridCellNgh<General>(grid, pt, General.class, speakRadius, speakRadius);
				List<GridCell<General>> gridCellsGeneral = nghCreatorGeneral.getNeighborhood(true);
				
				for (GridCell<General> cell : gridCellsGeneral) {
					if (cell.size() > 0) {
						
						for (Object  obj : grid.getObjectsAt(cell.getPoint().getX(), cell.getPoint().getY ())) {
							if (obj  instanceof  General && !((General) obj).getAID().equals(myAgent.getAID())) {
								System.out.println("demole isto " + wallToBeAbolished);
								System.out.println("demolidor " + ((General) obj).getAID());
								((General) obj).setBerlimWall(wallToBeAbolished);
							}
						}
					}
				}
					
				GridCellNgh<Soldier> nghCreatorGeneral1 = new GridCellNgh<Soldier>(grid, pt, Soldier.class, speakRadius, speakRadius);
				List<GridCell<Soldier>> gridCellsGeneral1 = nghCreatorGeneral1.getNeighborhood(true);
					
					
				for (GridCell<Soldier> cell1 : gridCellsGeneral1) {
					if (cell1.size() > 0) {
						
						for (Object  obj : grid.getObjectsAt(cell1.getPoint().getX(), cell1.getPoint().getY ())) {
							if (obj  instanceof  Soldier && !((Soldier) obj).getAID().equals(myAgent.getAID())) {
								System.out.println("demole isto " + wallToBeAbolished);
								System.out.println("demolidor " + ((Soldier) obj).getAID());
								((Soldier) obj).setBerlimWall(wallToBeAbolished);
							}
						}
					}
				}
                responses.add(selectedMessage);
            }
        }

    }
	
	private class AnswerCallBehaviour extends ContractNetResponder {
		private static final long serialVersionUID = 1L;

		AnswerCallBehaviour() {
			super(Soldier.this, MessageTemplate.and(
                    MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET), MessageTemplate.MatchPerformative(ACLMessage.CFP)));
        }

        @Override
        public ACLMessage handleCfp(ACLMessage message) {
        	System.out.println("recebi esta msg " + message.getContent());
        	if(stage==State.WAITING_FOR_DECISION || stage==State.HELPING)
        		return null;
        	
            ACLMessage response = message.createReply();
            response.setPerformative(ACLMessage.PROPOSE);
            String[] coordinates = message.getContent().split("-");
            DecimalFormat format = new DecimalFormat("00.000");
            double cost = Math.abs(Double.parseDouble(coordinates[0])-space.getLocation(myAgent).getX())+ Math.abs(Double.parseDouble(coordinates[1])-space.getLocation(myAgent).getY());
            response.setContent("" + cost);
            System.out.println("mandei cost " + myAgent.getAID());
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
            response.setPerformative(ACLMessage.INFORM);
            stage=State.HELPING;
            return response;
        }
        
        public void handleRejectProposal(ACLMessage cfp, ACLMessage propose, ACLMessage reject) {
        	System.out.println("recebi reject " + myAgent.getAID());
        	stage=State.MOVING;
        }
    }
}
