package evacuacao;

public class Geometry {

    public static final double EPSILON = 0.000001;

    public static double crossProduct(double x1, double y1, double x2, double y2) {
        return x1 * y2 - x2 * y1;
    }

    public static boolean doBoundingBoxesIntersect(
    		double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4) {
        return Math.min(x1, x2) <= Math.max(x3, x4) && Math.max(x1, x2) >= Math.min(x3, x4)
        		&& Math.min(y1, y2) <= Math.max(y3, y4) && Math.max(y1, y2) >= Math.min(y3, y4);
    }
    
    public static boolean isPointOnLine(double x1, double y1, double x2, double y2, double x3, double y3) {
        double r = crossProduct(x2 - x1, y2 - y1, x3 - x1, y3 - y1);
        return Math.abs(r) < EPSILON;
    }    
    
    public static boolean isPointRightOfLine(double x1, double y1, double x2, double y2, double x3, double y3) {
        return crossProduct(x2 - x1, y2 - y1, x3 - x1, y3 - y1) < 0;
    }

    public static boolean lineSegmentTouchesOrCrossesLine(
    		double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4) {
        return isPointOnLine(x1, y1, x2, y2, x3, y3) || isPointOnLine(x1, y1, x2, y2, x4, y4)
        		|| (isPointRightOfLine(x1, y1, x2, y2, x3, y3) ^ isPointRightOfLine(x1, y1, x2, y2, x4, y4));
    }
    
    public static boolean doLinesIntersect(
    		double x1, double y1, double x2, double y2, double x3, double y3, double x4, double y4) {
        return doBoundingBoxesIntersect(x1, y1, x2, y2, x3, y3, x4, y4)
                && lineSegmentTouchesOrCrossesLine(x1, y1, x2, y2, x3, y3, x4, y4)
                && lineSegmentTouchesOrCrossesLine(x3, y3, x4, y4, x1, y1, x2, y2);
    }
}

/*
if (Geometry.doLinesIntersect(0, 0, 1, 1, 0, 1, 1, 0))
	System.out.println("ok");
else
	System.out.println("KO");

if (Geometry.doLinesIntersect(0, 0, 1, 1, 0, 1, 1, 1))
	System.out.println("ok");
else
	System.out.println("KO");

if (!Geometry.doLinesIntersect(0, 0, 1, 1, 0, 1, 1, 2))
	System.out.println("ok");
else
	System.out.println("KO");

if (Geometry.doLinesIntersect(0, 0, 1, 1, 1, 1, 2, 2))
	System.out.println("ok");
else
	System.out.println("KO");

if (!Geometry.doLinesIntersect(0, 0, 1, 1, 0, 1, 1, 1.001))
	System.out.println("ok");
else
	System.out.println("KO");

if (!Geometry.doLinesIntersect(0, 0, 0, 1, 1, 0, 1, 1))
	System.out.println("ok");
else
	System.out.println("KO");
	
if (!Geometry.doLinesIntersect(0, 0.5, 0.999999, 0.5, 1, 0, 1, 1))
	System.out.println("ok");
else
	System.out.println("KO");
	
if (Geometry.doLinesIntersect(0, 0.5, 1.0, 0.5, 1, 0, 1, 1))
	System.out.println("ok");
else
	System.out.println("KO");
	
if (Geometry.doLinesIntersect(5, 0, -5, 0, 0, -5, 0, 5))
	System.out.println("ok");
else
	System.out.println("KO");

if (Geometry.doLinesIntersect(5, 0, -5, 0, 0, 5, 0, -5))
	System.out.println("ok");
else
	System.out.println("KO");
*/
