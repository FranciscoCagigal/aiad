package evacuacao;

import repast.simphony.context.Context;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.grid.Grid;

public class Wall {
	private double x1, y1, x2, y2;
	
	public Wall(Context<Object> context, ContinuousSpace<Object> space, Grid<Object> grid, double x1, double y1, double x2, double y2) {
		this.x1=x1;
		this.y1=y1;
		this.x2=x2;
		this.y2=y2;
		
		double length = Math.sqrt(Math.pow(this.getX2() - this.getX1(), 2) + Math.pow(this.getY2() - this.getY1(), 2)); 
		for (double i = 0; i < length; ){
			double x = this.x1 + (this.x2 - this.x1)*i/length;
			double y = this.y1 + (this.y2 - this.y1)*i/length;
			WallChunk wc = new WallChunk(this, (int)x, (int)y);
			context.add(wc);
			space.moveTo(wc, x, y);
			NdPoint pt2 = space.getLocation(wc);
			grid.moveTo(wc, (int) pt2.getX(), (int) pt2.getY());
			i += 0.1;
		}
	}
	
	public double getX1() {
		return x1;
	}
	
	public double getY1() {
		return y1;
	}
	
	public double getX2() {
		return x2;
	}
	
	public double getY2() {
		return y2;
	}
}
