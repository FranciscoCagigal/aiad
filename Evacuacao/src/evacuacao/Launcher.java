package evacuacao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.StaleProxyException;
import repast.simphony.context.Context;
import repast.simphony.context.space.continuous.ContinuousSpaceFactory;
import repast.simphony.context.space.continuous.ContinuousSpaceFactoryFinder;
import repast.simphony.context.space.grid.GridFactory;
import repast.simphony.context.space.grid.GridFactoryFinder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.continuous.RandomCartesianAdder;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridBuilderParameters;
import repast.simphony.space.grid.SimpleGridAdder;
import repast.simphony.space.grid.WrapAroundBorders;
import sajas.core.AID;
import sajas.core.Runtime;
import sajas.sim.repasts.RepastSLauncher;
import sajas.wrapper.ContainerController;

public class Launcher extends RepastSLauncher {
	

	private static double MAP_X = 50;
	private static double MAP_Y = 50;
	
	private static List<AID> listOfSoldiers = new ArrayList<AID>();

	private ContinuousSpace<Object> space;
	private Grid<Object> grid;

	private ContainerController mainContainer;

	@Override
	public String getName() {
		return "context";
	}

	@Override
	protected void launchJADE() {
		Runtime rt = Runtime.instance();
		Profile p1 = new ProfileImpl();
		mainContainer = rt.createMainContainer(p1);

		launchAgents();	
	}
	
	private void launchAgents() {
		Parameters params = RunEnvironment.getInstance().getParameters();

		try {
			int NUMBER_SOLDIERS = params.getInteger("N_SOLDIERS");
			int NUMBER_GENERAL = params.getInteger("N_GENERALS");
			int VISION_RADIUS = params.getInteger("VISION_RADIUS");
			int SPEAK_RADIUS = params.getInteger("SPEAK_RADIUS");
			int TYPE_OF_GAME = params.getInteger("TYPE_OF_GAME");
			
			
			for (int i = 0; i < NUMBER_SOLDIERS; i++) {				
				Soldier s = new Soldier(space, grid,0,0,VISION_RADIUS,SPEAK_RADIUS,TYPE_OF_GAME);
				mainContainer.acceptNewAgent("Soldier" + i, s).start();
				listOfSoldiers.add((AID) s.getAID());
			}
			
			if(TYPE_OF_GAME!=2)
				for (int i = 0; i < NUMBER_GENERAL; i++) {				
					General s = new General(space, grid,0,0);
					mainContainer.acceptNewAgent("General" + i, s).start();								
				}
			else {
				double totalArea = MAP_X*MAP_Y;
				double areaPerGeneral = totalArea/NUMBER_GENERAL;
				double heightToSearch = areaPerGeneral/MAP_X;
				double soldierPerGeneral = NUMBER_SOLDIERS/NUMBER_GENERAL;
				
				for (int i = 0; i < NUMBER_GENERAL; i++) {
					listOfSoldiers.subList((int)soldierPerGeneral*(i), (int)soldierPerGeneral*(i+1));
					General s = new General(space, grid,0,0,0.01,heightToSearch*(i*2+1)/2,listOfSoldiers.subList((int)soldierPerGeneral*(i), (int)soldierPerGeneral*(i+1)),MAP_X,MAP_Y,VISION_RADIUS,SPEAK_RADIUS);
					mainContainer.acceptNewAgent("General" + i, s).start();								
				}
			}
			
			
		} catch (StaleProxyException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public Context<?> build(Context<Object> context) {
		context.setId("context");

		launchEnvironment(context);

		return super.build(context);
	}

	private void launchEnvironment(Context<Object> context) {
		
		ContinuousSpaceFactory spaceFactory = ContinuousSpaceFactoryFinder.createContinuousSpaceFactory(null);
		space = spaceFactory.createContinuousSpace("space", context, new RandomCartesianAdder<Object>(),
				new repast.simphony.space.continuous.WrapAroundBorders(), MAP_X, MAP_Y);

		GridFactory gridFactory = GridFactoryFinder.createGridFactory(null);
		grid = gridFactory.createGrid("grid", context, new GridBuilderParameters<Object>(new WrapAroundBorders(),
				new SimpleGridAdder<Object>(), true, (int)MAP_X, (int)MAP_Y));

		Exit exit = new Exit(40,40);
		context.add(exit);
		space.moveTo(exit,40, 40);
		NdPoint pt = space.getLocation(exit);
		grid.moveTo(exit, (int) pt.getX(), (int) pt.getY());
		
	}
	
}
