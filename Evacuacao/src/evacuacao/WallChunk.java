package evacuacao;

public class WallChunk {
	private Wall wall;
	private int x, y;

	public WallChunk(Wall wall, int x, int y) {
		this.wall = wall;
		this.x=x;
		this.y=y;
	}

	public Wall getWall() {
		return wall;
	}
	
	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}
}
