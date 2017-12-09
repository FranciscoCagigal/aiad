package evacuacao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridDimensions;
import repast.simphony.space.grid.GridPoint;
import sajas.core.Agent;

public class MovableAgent extends Agent{
	
	ContinuousSpace<Object> space;
	Grid<Object> grid;
	int visionRadius, speakRadius;
	double speed;
	int MAPX = 50, MAPY=50;
	boolean[][] myMap= new boolean[MAPX+1][MAPY+1];
	
	public MovableAgent(ContinuousSpace<Object> space, Grid<Object> grid, double speed, int vision_radius, int speak_radius) {
		this.space = space;
		this.grid = grid;
		this.visionRadius = vision_radius;
		this.speakRadius = speak_radius;
		this.speed=speed;
	}
	
	NdPoint moveToPlace(double x, double y) {
		NdPoint  otherPoint = new  NdPoint(x,y);
		NdPoint lastPoint = space.getLocation(this);
		double distance = Math.sqrt(Math.pow(lastPoint.getX()-x,2) + Math.pow(lastPoint.getY()-y,2));
		double delta_x = otherPoint.getX() - lastPoint.getX();
		double delta_y = otherPoint.getY() - lastPoint.getY();
		double angle = Math.atan2(delta_y, delta_x);
		//double  angle = SpatialMath.calcAngleFor2DMovement(space ,lastPoint , otherPoint );
		if (distance >speed)
			space.moveByVector(this , speed, angle , 0);
		else {
			space.moveByVector(this , distance, angle , 0);
		}
		NdPoint myPoint = space.getLocation(this);
		if(canMove(grid,lastPoint,  myPoint)) {
			
		}
		grid.moveTo(this , (int)myPoint.getX(), (int)myPoint.getY ());
		
		return myPoint;
	}
	
	static boolean canMove (Grid<Object> grid, NdPoint pt1, NdPoint pt2) {
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

	void moveRnd() {
		
		int nr_tries= 0;
		NdPoint myPoint;
		double angle;
		int radius = 1;
		ArrayList<NdPoint> positions = new ArrayList<NdPoint> ();
		while(true){
			NdPoint lastPoint = space.getLocation(this);
			if(positions.isEmpty())
				positions = getPositionsNotVisited(radius);

			int direction = chooseRandom(positions,getPossibleDirections());
			if(direction!=-1) {
				angle= direction*45*Math.PI/180;
				space.moveByVector(this , speed, angle , 0);
				myPoint = space.getLocation(this);
				
				if (canMove(grid,lastPoint,  myPoint)){
					
					grid.moveTo(this, (int)myPoint.getX(), (int)myPoint.getY());
					break;
				}else {
					moveToAngle(angle+Math.PI);
				}
			}else {
				positions.clear();
				radius++;
			}
			nr_tries++;
			if(nr_tries==50)
				break;
		}
		updateMap();
	}
	
	ArrayList<NdPoint> getPositionsNotVisited(int radius){
		ArrayList<NdPoint> result = new ArrayList<NdPoint> ();
		NdPoint myPoint = space.getLocation(this);
		int x = (int) myPoint.getX();
		int y = (int) myPoint.getY();
		System.out.println("começo " + this.getAID() + " " + myPoint + " " + myMap[44][44]);
		System.out.println("começo " + radius);
		for(int i =-radius; i<= radius;i++) {
			for(int j =-radius; j<= radius;j++) {
				if(x+i>=0 && x+i<=MAPX && y+j>=0 && y+j<MAPY && !myMap[x+i][y+j]) {
					System.out.println("entrei");
					result.add(new NdPoint(x+i,y+j));
				}
			}
		}
		return result;
	}
	
	ArrayList<Integer> getPossibleDirections() {
		ArrayList<Integer> result = new ArrayList<Integer>();
		NdPoint myPoint = space.getLocation(this);
		for(int i=0;i<7;i++) {
			NdPoint nextPosition = new NdPoint(myPoint.getX()+speed*Math.cos(i*45*Math.PI/180),myPoint.getY()+speed*Math.sin(45*i*Math.PI/180));
			if(myPoint.getX()+speed*Math.cos(i*45*Math.PI/180)<=MAPX && myPoint.getY()+speed*Math.sin(45*i*Math.PI/180)<=MAPY && myPoint.getY()+speed*Math.sin(45*i*Math.PI/180)>=0 && myPoint.getX()+speed*Math.cos(i*45*Math.PI/180)>=0 && canMove(grid,myPoint,  nextPosition)) {
				result.add(i);
			}
				
		}		
		return result;
	}
	
	void updateMap() {
		NdPoint myPoint = space.getLocation(this);
		System.out.println("minha posicao " + myPoint);
		for(int i=-visionRadius;i<=visionRadius;i++) {
			for(int j=-visionRadius;j<=visionRadius;j++) {
				if(Math.pow(i, 2)+Math.pow(j, 2)<=Math.pow(visionRadius, 2)) {
					if((int)myPoint.getY () + j>=0 && (int)myPoint.getY () + j<=MAPY && (int)myPoint.getX () + i>=0 && (int)myPoint.getX () + i<=MAPX) {
						myMap[(int)myPoint.getX()+i][(int)myPoint.getY () + j] = true;				
					}
				}
			}			
		}
		
	}
	

	
	NdPoint moveToAngle(double angle) {
		space.moveByVector(this , speed, angle , 0);	
		NdPoint myPoint = space.getLocation(this);
		
		grid.moveTo(this , (int)myPoint.getX(), (int)myPoint.getY ());	
		return myPoint;
	}
	
	int chooseRandom(ArrayList<NdPoint> positions ,ArrayList<Integer> array) {
		NdPoint myPoint = space.getLocation(this);
		while(true) {
			if (positions.isEmpty())
				return -1;
			NdPoint position =  positions.get(RandomHelper.nextIntFromTo(0, positions.size()-1));
			
			double delta_x = position.getX() - myPoint.getX();
			double delta_y = position.getY() - myPoint.getY();
			int angle = (int) (Math.atan2(delta_y, delta_x)*180/Math.PI);
			
			if(angle<0)
				angle+=360;
			int int_angle = angle / 45;
			
			System.out.println("angle " + position + " " + angle + " " + Arrays.deepToString(array.toArray()));
			if(array.contains(int_angle)) {
				System.out.println("int_angle " + int_angle);
				return int_angle;
			}else {
				positions.remove(position);
			}
		}
		
	}
	
	public ArrayList<GridPoint> shortestPath (NdPoint pt1) {
		GridDimensions gridDim = grid.getDimensions();
		int gridWidth = gridDim.getWidth();
		//System.out.println("gridWidth: " + gridWidth);
		int gridHeight = gridDim.getHeight();
		//System.out.println("gridHeight: " + gridHeight);
		int[][] visitedCells = new int[gridWidth][gridHeight];
		for (int[] row: visitedCells)
			Arrays.fill(row, Integer.MAX_VALUE);
		LinkedList<GridPoint> queue = new LinkedList<GridPoint>();
		
		NdPoint pt0 = space.getLocation(this);
		GridPoint orig = new GridPoint((int) pt0.getX(), (int) pt0.getY());
		//System.out.println("orig: " + orig);
		GridPoint dest = new GridPoint((int) pt1.getX(), (int) pt1.getY());
		//System.out.println("dest: " + dest);
		visitedCells[orig.getX()][orig.getY()] = 0;
		queue.add(orig);
		//System.out.println("queue size: " + queue.size());
		
		for (boolean[] row: myMap)
			Arrays.fill(row, true);
		
		boolean destReached = false; 
		while (!destReached){
			GridPoint current = queue.poll();
			if (current == null){
				System.out.println("queue is null");
				return null;
			}
			else{
				//System.out.println("queue is not null");
			}
			int currentValue = visitedCells[current.getX()][current.getY()];
			
			// get neighbours
			ArrayList<GridPoint> neighbours = new ArrayList<GridPoint>();
			if (current.getY() > 0) {
				if (current.getX() > 0)
					neighbours.add(new GridPoint(current.getX() - 1, current.getY() - 1));
				neighbours.add(new GridPoint(current.getX(), current.getY() - 1));
				if (current.getX() < gridWidth - 1)
					neighbours.add(new GridPoint(current.getX() + 1, current.getY() - 1));
			}
			if (current.getX() > 0)
				neighbours.add(new GridPoint(current.getX() - 1, current.getY()));
			if (current.getX() < gridWidth - 1)
				neighbours.add(new GridPoint(current.getX() + 1, current.getY()));
			if (current.getY() < gridHeight - 1) {
				if (current.getX() > 0)
					neighbours.add(new GridPoint(current.getX() - 1, current.getY() + 1));
				neighbours.add(new GridPoint(current.getX(), current.getY() + 1));
				if (current.getX() < gridWidth - 1)
					neighbours.add(new GridPoint(current.getX() + 1, current.getY() + 1));
			}
			
			for (GridPoint neighbour : neighbours){
				if (neighbour.equals(dest)){
					destReached = true;
					break;
				}
				if (myMap[neighbour.getX()][neighbour.getY()] &&
						visitedCells[neighbour.getX()][neighbour.getY()] == Integer.MAX_VALUE &&
						canMove(grid, new NdPoint(current.getX(), current.getY()), new NdPoint(neighbour.getX(), neighbour.getY()))){
					queue.add(neighbour);
					visitedCells[neighbour.getX()][neighbour.getY()] = currentValue + 1;
				}
			}			
		}
		
		ArrayList<GridPoint> path = new ArrayList<GridPoint>();
		if (destReached){
			GridPoint current = dest;
			while (true){
				// get neighbours
				ArrayList<GridPoint> neighbours = new ArrayList<GridPoint>();
				if (current.getY() > 0) {
					if (current.getX() > 0)
						neighbours.add(new GridPoint(current.getX() - 1, current.getY() - 1));
					neighbours.add(new GridPoint(current.getX(), current.getY() - 1));
					if (current.getX() < gridWidth)
						neighbours.add(new GridPoint(current.getX() + 1, current.getY() - 1));
				}
				if (current.getX() > 0)
					neighbours.add(new GridPoint(current.getX() - 1, current.getY()));
				if (current.getX() < gridWidth)
					neighbours.add(new GridPoint(current.getX() + 1, current.getY()));
				if (current.getY() < gridHeight) {
					if (current.getX() > 0)
						neighbours.add(new GridPoint(current.getX() - 1, current.getY() + 1));
					neighbours.add(new GridPoint(current.getX(), current.getY() + 1));
					if (current.getX() < gridWidth)
						neighbours.add(new GridPoint(current.getX() + 1, current.getY() + 1));
				}
				
				int minValue = Integer.MAX_VALUE;
				GridPoint minNeighbour = null;
				for (GridPoint neighbour : neighbours){
					int value = visitedCells[neighbour.getX()][neighbour.getY()];
					if (value < minValue){
						minValue = value;
						minNeighbour = neighbour;
					}					
				}
				path.add(0, minNeighbour);
				if (minValue > 1)
					current = minNeighbour;
				else break;
			}
		}
		return path;		
	}
	
	
	
}
