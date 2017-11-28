package evacuacao;

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
import sajas.core.Runtime;
import sajas.sim.repasts.RepastSLauncher;
import sajas.wrapper.ContainerController;

public class Launcher extends RepastSLauncher {
	
	private static int NUMBER_SOLDIERS = 100;
	private static int NUMBER_GENERAL = 10;

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
		//Parameters params = RunEnvironment.getInstance().getParameters();

		try {
			//NUMBER_SOLDIERS = params.getInteger("N_SOLDIERS");
			
			for (int i = 0; i < NUMBER_SOLDIERS; i++) {				
				Soldier s = new Soldier(space, grid,0,0);
				mainContainer.acceptNewAgent("Soldier" + i, s).start();								
			}
			
			for (int i = 0; i < NUMBER_GENERAL; i++) {				
				General s = new General(space, grid,0,0);
				mainContainer.acceptNewAgent("General" + i, s).start();								
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
				new repast.simphony.space.continuous.WrapAroundBorders(), 50, 50);

		GridFactory gridFactory = GridFactoryFinder.createGridFactory(null);
		grid = gridFactory.createGrid("grid", context, new GridBuilderParameters<Object>(new WrapAroundBorders(),
				new SimpleGridAdder<Object>(), true, 50, 50));

		Exit exit = new Exit(40,40);
		context.add(exit);
		space.moveTo(exit,40, 40);
		NdPoint pt = space.getLocation(exit);
		grid.moveTo(exit, (int) pt.getX(), (int) pt.getY());
		
	}
	
}
