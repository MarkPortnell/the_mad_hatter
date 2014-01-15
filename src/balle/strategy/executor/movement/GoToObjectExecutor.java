package balle.strategy.executor.movement;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import android.util.Log;
import balle.controller.Controller;
import balle.main.drawable.Drawable;
import balle.misc.Globals;
import balle.strategy.ConfusedException;
import balle.strategy.basic.Initial;
import balle.strategy.executor.turning.RotateToOrientationExecutor;
import balle.world.Coord;
import balle.world.Orientation;
import balle.world.Snapshot;
import balle.world.objects.FieldObject;
import balle.world.objects.Robot;

public class GoToObjectExecutor implements MovementExecutor {
	
	private static final Logger LOG = Logger.getLogger(GoToObjectExecutor.class);

	private double stopDistance = 0.1;

	private final static double EPSILON = Globals.GO_TO_OBJ_EPSILON;
	private final static double DISTANCE_DIFF_TO_TURN_FOR = Globals.GO_TO_OBJ_DISTANCE_DIFF_TO_TURN_FOR;

	public final static int DEFAULT_MOVEMENT_SPEED = Globals.GO_TO_OBJ_DEFAULT_MOVEMENT_SPEED;

    protected FieldObject       target                    = null;
	private boolean isMoving = false;

	private int movementSpeed = DEFAULT_MOVEMENT_SPEED;

	RotateToOrientationExecutor turningExecutor = null;

	public GoToObjectExecutor(RotateToOrientationExecutor turningExecutor) {
		this.turningExecutor = turningExecutor;
	}

	@Override
    public void updateTarget(FieldObject target) {
		this.target = target;
	}

	@Override
	public boolean isFinished(Snapshot snapshot) {
		Robot robot = snapshot.getBalle();
		Coord currentPosition = robot.getPosition();
		if ((target == null) || (currentPosition == null)) {
			return false;
		}
		LOG.info("Finished: "+((currentPosition.dist(target.getPosition()) - stopDistance) < EPSILON));
		return ((currentPosition.dist(target.getPosition()) - stopDistance) < EPSILON);
	}

	@Override
	public boolean isPossible(Snapshot snapshot) {
		if (turningExecutor == null)
			return false;
		
		Robot robot = snapshot.getBalle();
		Coord currentPosition = robot.getPosition();
		Orientation currentOrientation = robot.getOrientation();
		Coord targetPosition = (target != null) ? target.getPosition() : null;
		LOG.info("Not possible:"+((currentOrientation != null) && (currentPosition != null) && (targetPosition != null)));
		return ((currentOrientation != null) && (currentPosition != null) && (targetPosition != null));
	}

	@Override
    public void step(Controller controller, Snapshot snapshot)
            throws ConfusedException {
		// Fail quickly if state not set
		if (snapshot == null)
			return;

		if (target == null) {
			target = snapshot.getBall();
		}
		Coord targetCoord = target.getPosition();
		Robot robot = snapshot.getBalle();

		Coord currentPosition = robot.getPosition();

		if (isFinished(snapshot)) {
			stop(controller);
			return;
		} else {
			// Fail quickly if not possible
//			if (!isPossible(snapshot))
//				return;
			if (turningExecutor.isFinished(snapshot)) {
				turningExecutor.stop(controller);
			}

			if (turningExecutor.isTurning()) // If we are still turning here
			{

                turningExecutor.step(controller, snapshot);
				return;
			} else {
				Orientation orientationToTarget = targetCoord.sub(
						currentPosition).orientation();
				turningExecutor.setTargetOrientation(orientationToTarget);
				double turnAngle = turningExecutor.getAngleToTurn(snapshot);
				double dist = targetCoord.dist(robot.getPosition());
				double distDiffFromTarget = Math
						.abs(Math.sin(turnAngle) * dist);

				// sin(180) = sin(0) thus the check
				// if we don't face the ball precisely but approximately we
				// might ignore
				// this difference if target position is less than
				// GO_TO_OBJ_DISTANCE_DIFF_TO_TURN_FOR far away from the
				// resulting position
				if ((Math.abs(turnAngle) > Math.PI / 2)
						|| (Math.abs(distDiffFromTarget) > DISTANCE_DIFF_TO_TURN_FOR
								* dist)) {

					if (isMoving) {
						controller.stop();
						isMoving = false;
					}

                    turningExecutor.step(controller, snapshot);

				} else {
					LOG.info("Move forward!");
                    controller.forward(movementSpeed);
                    isMoving = true;
				}
			}
		}

	}

	public void setMovementSpeed(int movementSpeed) {
		this.movementSpeed = movementSpeed;
	}

	@Override
	public void stop(Controller controller) {
		// If its doing anything, it will stop
		if (isMoving)
			controller.stop();

		// Otherwise it will just make sure to clean up
		isMoving = false;

		// Also make sure for turningExecutor to do the same
		if (turningExecutor != null) {
			turningExecutor.stop(controller);
		}

		// Note that we do not want to just call controller.stop()
		// blindly in case there are some other executors using it. (even though
		// there shouldn't be)
	}

	@Override
	public ArrayList<Drawable> getDrawables() {
		return new ArrayList<Drawable>();
	}

	@Override
	public void setStopDistance(double stopDistance) {
		this.stopDistance = stopDistance;

	}
}
