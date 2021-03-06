/**
 * 
 */
package balle.strategy.planner;

import java.awt.Color;

import org.apache.log4j.Logger;

import balle.controller.Controller;
import balle.main.drawable.Dot;
import balle.strategy.ConfusedException;
import balle.strategy.executor.movement.MovementExecutor;
import balle.world.Coord;
import balle.world.Snapshot;
import balle.world.objects.FieldObject;
import balle.world.objects.Pitch;
import balle.world.objects.Point;
import balle.world.objects.Robot;

/**
 * @author s0909773
 * 
 */
public class GoToObject extends AbstractPlanner {

    protected static final Logger LOG = Logger.getLogger(GoToObject.class);

    MovementExecutor executorStrategy;

	private final double AVOIDANCE_GAP;
	private final double OVERSHOOT_GAP;
    private static final double DEFAULT_AVOIDANCE_GAP = 0.5;
    private static final double DEFAULT_OVERSHOOT_GAP = 0.8;
	private static final double DIST_DIFF_THRESHOLD = 0.2;

	private boolean approachTargetFromCorrectSide;
	private boolean shouldAvoidOpponent = true;

    public GoToObject(MovementExecutor movementExecutor) {
        this(movementExecutor, DEFAULT_AVOIDANCE_GAP, DEFAULT_OVERSHOOT_GAP,
                false);
    }

    /**
	 * Instantiates a new go to goal strategy.
	 * 
	 * @param movementExecutor
	 *            the movement executor
	 * @param approachTargetFromCorrectSide
	 *            whether to always approach target from correct side
	 */
    public GoToObject(MovementExecutor movementExecutor,
            boolean approachTargetFromCorrectSide) {
        this(movementExecutor, DEFAULT_AVOIDANCE_GAP, DEFAULT_OVERSHOOT_GAP,
                true);
    }

    public GoToObject(MovementExecutor movementExecutor, double avoidanceGap,
            double overshootGap, boolean approachfromCorrectSide) {
        executorStrategy = movementExecutor;
        this.approachTargetFromCorrectSide = approachfromCorrectSide;
        AVOIDANCE_GAP = avoidanceGap;
        OVERSHOOT_GAP = overshootGap;
    }

	@Override
	protected void onStep(Controller controller, Snapshot snapshot) throws ConfusedException {
		FieldObject target = getTarget(snapshot);

		if ((snapshot == null) || (snapshot.getBalle().getPosition() == null) || (target == null))
			return;

		if (shouldApproachTargetFromCorrectSide()
				&& (!snapshot.getBalle().isApproachingTargetFromCorrectSide(target, snapshot.getOpponentsGoal()))) {
			LOG.info("Approaching target from wrong side, calculating overshoot target");
			target = getOvershootTarget(target, OVERSHOOT_GAP, snapshot);
			addDrawable(new Dot(target.getPosition(), Color.BLUE));
		}

		// If we see the opponent
		if (shouldAvoidOpponent && snapshot.getOpponent().getPosition() != null) {
			if (!snapshot.getBalle().canReachTargetInStraightLine(target, snapshot.getOpponent())) {
				// pick a new target then
				LOG.info("Opponent is blocking the target, avoiding it");
				target = getAvoidanceTarget(snapshot);
				addDrawable(new Dot(target.getPosition(), Color.MAGENTA));
			}
		}

		// Check if the target is outwidth the bounding box of the pitch.
		// If so, find the closest point and update target.
		// TODO

		// Update the target's location in executorStrategy (e.g. if target
		// moved)
		executorStrategy.updateTarget(target);
		// Draw the target
		if (target.getPosition() != null)
			addDrawable(new Dot(target.getPosition(), getTargetColor()));

		// If it says it is not finished, tell it to do something for a step.
		if (!executorStrategy.isFinished(snapshot)) {
			executorStrategy.step(controller, snapshot);

			addDrawables(executorStrategy.getDrawables());
		} else {
			// Tell the strategy to stop doing whatever it was doing
			executorStrategy.stop(controller);
		}
	}

    public void setStopDistance(double distance) {
        executorStrategy.setStopDistance(distance);
    }
    public MovementExecutor getExecutorStrategy() {
        return executorStrategy;
    }

    public void setExecutorStrategy(MovementExecutor executorStrategy) {
        this.executorStrategy = executorStrategy;
    }

    public boolean shouldApproachTargetFromCorrectSide() {
        return approachTargetFromCorrectSide;
    }

    public void setApproachTargetFromCorrectSide(
            boolean approachTargetFromCorrectSide) {
        this.approachTargetFromCorrectSide = approachTargetFromCorrectSide;
    }
    
	public boolean shouldAvoidOpponent() {
    	return this.shouldAvoidOpponent;
    }
    
	public void setShouldAvoidOpponent(boolean shouldAvoidOpponent) {
		this.shouldAvoidOpponent = shouldAvoidOpponent;
    }

	// If we have the ball, our target is the goal.
	// Else, our target is the ball.
	protected FieldObject getTarget(Snapshot snapshot) {
		Robot ourRobot = snapshot.getBalle();
		if (ourRobot.possessesBall(snapshot.getBall())) {
			return new Point(snapshot.getOpponentsGoal().getPosition());
		}
		return new Point(snapshot.getBall().getPosition());
    }

    protected Color getTargetColor() {
		return Color.RED;
    }

	protected Coord calculateAvoidanceCoord(double gap, boolean belowPoint,
			Snapshot snapshot) {
        int side = 1;
        if (belowPoint) {
            side = -1;
        }

		Robot robot = snapshot.getBalle();
		Coord point = snapshot.getOpponent().getPosition();

        // Gets the angle and distance between the robot and the ball
        double robotObstacleAngle = point.sub(robot.getPosition())
                .orientation().atan2styleradians();
        double robotObstacleDistance = point.dist(robot.getPosition());
        // Calculate the distance between the robot and the destination point
        double hyp = Math.sqrt((robotObstacleDistance * robotObstacleDistance)
                + (gap * gap));

        // Calculate the angle between the robot and the destination point
        double robotPointAngle = Math.asin(gap / hyp);
        // Calculate the angle between the robot and the destination point.
        // Side is -1 if robot is below the ball, so will get the angle needed
        // for a point
        // below the ball, whereas side = 1 will give a point above the ball
        double angle = robotObstacleAngle + (side * robotPointAngle);

        // Offsets are in relation to the robot
        double xOffset = hyp * Math.cos(angle);
        double yOffset = hyp * Math.sin(angle);

        return new Coord(robot.getPosition().getX() + xOffset, robot
                .getPosition().getY() + yOffset);
    }

    protected Coord calculateOvershootCoord(FieldObject target,
			double gap, boolean belowPoint, Snapshot snapshot) {
        int side = 1;
        if (belowPoint) {
            side = -1;
        }

		Robot robot = snapshot.getBalle();
		Coord point = target.getPosition();

        // Gets the angle and distance between the robot and the ball
        double robotTargetOrientation = point.sub(robot.getPosition())
                .orientation().atan2styleradians();
        double robotTargetDistance = point.dist(robot.getPosition());
        // Calculate the distance between the robot and the destination point
        double hyp = Math.sqrt((robotTargetDistance * robotTargetDistance)
                + (gap * gap));

        // Calculate the angle between the robot and the destination point
        double robotPointAngle = Math.asin(gap / hyp);
        // Calculate the angle between the robot and the destination point.
        // Side is -1 if robot is below the ball, so will get the angle needed
        // for a point
        // below the ball, whereas side = 1 will give a point above the ball
        double angle = robotTargetOrientation + (side * robotPointAngle);

        // Offsets are in relation to the robot
        double xOffset = hyp * Math.cos(angle);
        double yOffset = hyp * Math.sin(angle);

        return new Coord(robot.getPosition().getX() + xOffset, robot
                .getPosition().getY() + yOffset);
    }

    /**
     * Returns the target location the robot should go to in order to overshoot
     * the target and face the opponents goal from correct angle.
     * 
     * @param target
     *            the target
     * @param overshootGap
     *            the overshoot gap
     * @return the overshoot target
     */
    protected FieldObject getOvershootTarget(FieldObject target,
			double overshootGap, Snapshot snapshot) {
        // End case for recursive search for overshoot target
        if (overshootGap < 0.1)
            return target;

        boolean belowBall = true;
		Robot robot = snapshot.getBalle();
		Pitch pitch = snapshot.getPitch();

        if (robot.getPosition().getY() > target.getPosition().getY()) {
            belowBall = false;
        }

		if (snapshot.getOpponentsGoal().isRightGoal())
            belowBall = !belowBall;

        Coord overshootCoord = calculateOvershootCoord(target, overshootGap,
				belowBall, snapshot);

        // If the point is in the pitch
        if (pitch.containsCoord(overshootCoord)) {
            return new Point(overshootCoord);
        }


		return getOvershootTarget(target, overshootGap / 2.0, snapshot);
    }

	protected Point getAvoidanceTarget(Snapshot snapshot) {
		Coord pointAbove = calculateAvoidanceCoord(AVOIDANCE_GAP, true,
				snapshot);
		Coord pointBelow = calculateAvoidanceCoord(AVOIDANCE_GAP, false,
				snapshot);
		Pitch pitch = snapshot.getPitch();

		Coord currentPosition = snapshot.getBalle().getPosition();
        if (pitch.containsCoord(pointAbove) && pitch.containsCoord(pointBelow)) {
            // If both points happen to be in the pitch, return the closest one
            double distToPointAbove = currentPosition.dist(pointAbove);
            double distToPointBelow = currentPosition.dist(pointBelow);
            double distDiff = Math.abs(distToPointAbove - distToPointBelow);

            // If distances differ by much:
            if (distDiff > DIST_DIFF_THRESHOLD) {
                // Pick the shorter one
                if (distToPointAbove < distToPointBelow)
                    return new Point(pointAbove);
                else
                    return new Point(pointBelow);
            } else {
				double angleToTurnPointAbove = snapshot.getBalle()
                        .getAngleToTurnToTarget(pointAbove);
				double angleToTurnPointBelow = snapshot.getBalle()
                        .getAngleToTurnToTarget(pointBelow);

                if (Math.abs(angleToTurnPointAbove) < Math
                        .abs(angleToTurnPointBelow)) {
                    return new Point(pointAbove);
                } else
                    return new Point(pointBelow);

            }

        } else if (pitch.containsCoord(pointAbove)) {
            // Else if pitch contains only pointAbove, return it
            return new Point(pointAbove);
        } else
            // if it doesn't contain pointAbove, it should contain pointBelow
            return new Point(pointBelow);
        // TODO: what happens if it does not contain both points?
        // (it will return pointBelow now, but some other behaviour might be
        // desired)

    }

    @Override
    public void stop(Controller controller) {
		executorStrategy.stop(controller);

    }
}
