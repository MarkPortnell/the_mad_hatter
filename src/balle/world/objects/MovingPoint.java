package balle.world.objects;

import balle.world.Coord;
import balle.world.Velocity;

/**
 * Immutable
 */
public class MovingPoint {

	public Coord position;
	protected Velocity velocity;

	public Coord lastPosition;

    public MovingPoint(Coord position, Velocity velocity) {
        super();

        this.position = position;
        this.velocity = velocity;
    }

    public Coord getPosition() {
        return position;
    }

	public void setPosition(Coord position) {
		this.position = position;
	}

    public Velocity getVelocity() {
        return velocity;
    }


	public void setVelocity(Velocity velocity) {
		this.velocity = velocity;
	}

}
