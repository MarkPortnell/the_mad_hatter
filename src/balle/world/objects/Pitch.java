package balle.world.objects;

import balle.misc.Globals;
import balle.world.Coord;
import balle.world.Line;
import balle.world.Snapshot;

public class Pitch extends StaticFieldObject {

    private final double minX, maxX, minY, maxY;

    public double getMinX() {
        return minX;
    }

    public double getMaxX() {
        return maxX;
    }

    public double getMinY() {
        return minY;
    }

    public double getMaxY() {
        return maxY;
    }

    public Pitch(double minX, double maxX, double minY, double maxY) {
        this.minX = minX;
        this.maxX = maxX;
        this.minY = minY;
        this.maxY = maxY;
    }

    @Override
    public Coord getPosition() {
        return new Coord((minX + maxX) / 2, (minY + maxY / 2));
    }

    @Override
    public boolean containsCoord(Coord point) {
        if (point.getX() > maxX)
            return false;
        if (point.getX() < minX)
            return false;
        if (point.getY() > maxY)
            return false;
        if (point.getY() < minY)
            return false;
        return true;
    }

	public Line getLeftWall() {
		return new Line(new Coord(minX, maxY), new Coord(minX, minY));
	}

	public Line getLeftLowerWall(Goal goal) {
		double newY = goal.getMinY();
		return new Line(new Coord(minX, minY), new Coord(minX, newY));
	}

	public Line getLeftUpperWall(Goal goal) {
		double newY = goal.getMaxY();
		return new Line(new Coord(minX, maxY), new Coord(minX, newY));
	}

	public Line getRightWall() {
		return new Line(new Coord(maxX, maxY), new Coord(maxX, minY));
	}

	public Line getRightLowerWall(Goal goal) {
		double newY = goal.getMinY();
		return new Line(new Coord(maxX, minY), new Coord(maxX, newY));
	}

	public Line getRightUpperWall(Goal goal) {
		double newY = goal.getMaxY();
		return new Line(new Coord(maxX, maxY), new Coord(maxX, newY));
	}

	public Line getTopWall() {
		return new Line(new Coord(minX, maxY), new Coord(maxX, maxY));
	}

	public Line getBottomWall() {
		return new Line(new Coord(minX, minY), new Coord(maxX, minY));
	}
	public Line[] getWalls() {
		return new Line[] { getTopWall(), getRightWall(), getBottomWall(), getLeftWall() };
	}

	public Coord goToGoalTarget(Snapshot snapshot) {
		double center = Globals.PITCH_GOAL_MIN_Y + ((Globals.PITCH_GOAL_MAX_Y - Globals.PITCH_GOAL_MIN_Y) / 2.0);
		Coord middleCoord;
		if (snapshot.getOwnGoal().isLeftGoal()) {
			middleCoord = new Coord(0.2, center);
		} else {
			middleCoord = new Coord(Globals.PITCH_MAX_X - 0.2, center);
		}

		return middleCoord;

	}

	public void printCoordinates(Goal goal) {
		System.out.println("Top wall: " + getTopWall().getA() + " " + getTopWall().getB());
		System.out.println("Bottom wall: " + getBottomWall().getA() + " " + getBottomWall().getB());

		System.out.println("LeftUpper wall: " + getLeftUpperWall(goal).getA() + " " + getLeftUpperWall(goal).getB());
		System.out.println("LeftLower wall: " + getLeftLowerWall(goal).getA() + " " + getLeftLowerWall(goal).getB());
		System.out.println("RightUpper wall: " + getRightUpperWall(goal).getA() + " " + getRightUpperWall(goal).getB());
		System.out.println("RightLower wall: " + getRightLowerWall(goal).getA() + " " + getRightLowerWall(goal).getB());
	}

	@Override
	public boolean intersects(Line line) {
		return containsCoord(line.getA()) != containsCoord(line.getB());
	}


	/**
	 * Get half of the pitch this point is on.
	 * 
	 * @param position
	 *            Point in the pitch.
	 * @return True if on left, false otherwise.
	 */
	public Object getHalf(Coord position) {
		return position.getX() < getPosition().getX();
	}

}
