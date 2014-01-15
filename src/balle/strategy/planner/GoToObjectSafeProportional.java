package balle.strategy.planner;

import java.awt.Color;

import balle.controller.Controller;
import balle.main.drawable.DrawableLine;
import balle.misc.Globals;
import balle.simulator.SnapshotPredictor;
import balle.strategy.ConfusedException;
import balle.strategy.FactoryMethod;
import balle.strategy.executor.movement.GoToObjectPFN;
import balle.strategy.executor.turning.FaceAngle;
import balle.strategy.executor.turning.RotateToOrientationExecutor;
import balle.world.Line;
import balle.world.Orientation;
import balle.world.Snapshot;
import balle.world.objects.Ball;
import balle.world.objects.FieldObject;
import balle.world.objects.Goal;
import balle.world.objects.Pitch;
import balle.world.objects.Point;
import balle.world.objects.Robot;

public class GoToObjectSafeProportional extends GoToObject {

	private static final double TARGET_SAFE_GAP = 0.4;

	private final AbstractPlanner turnHack;

	public GoToObjectSafeProportional() {
		super(new GoToObjectPFN(0, true));

		turnHack = new TurnHack();
	}

	public GoToObjectSafeProportional(double avoidanceGap, double overshootGap,
			boolean approachfromCorrectSide) {
		super(new GoToObjectPFN(0), avoidanceGap, overshootGap,
				approachfromCorrectSide);
		turnHack = new TurnHack();
	}


	@FactoryMethod(designator = "GoToObjectSafeProportional", parameterNames = {})
	public static GoToObjectSafeProportional factoryMethod() {
		return new GoToObjectSafeProportional();
	}

	@FactoryMethod(designator = "GTOSP", parameterNames = { "avoidanceGap",
			"overshootGap", "CorrectSide?" })
	public static GoToObjectSafeProportional gTBSP(double aG, double oG,
			boolean CorrectSide) {
		return new GoToObjectSafeProportional(aG, oG, CorrectSide);
	}

	@Override
	protected void onStep(Controller controller, Snapshot snapshot) throws ConfusedException {
		Robot ourRobot = snapshot.getBalle();
		Robot oppRobot = snapshot.getOpponent();
		Ball ball = snapshot.getBall();
		FieldObject target = getOriginalTarget(snapshot);
		Goal oppGoal = snapshot.getOpponentsGoal();

		if (snapshot == null || ourRobot == null || target == null || ball == null) {
			return;
		}

		if (ourRobot.isInScoringPosition(ball, oppGoal, oppRobot)) {
			//			controller.kick();
			LOG.info("Passed first Kick check");
			if (canStillScore(snapshot)){
				LOG.info("Passed second Kick check");
				controller.kick();
			} 
		}

		if (turnHack.shouldStealStep(snapshot)) {
			LOG.info("Letting TurnHack handle the step");
			turnHack.step(controller, snapshot);
			return;
		}

		if (ourRobot.isApproachingTargetFromCorrectSide(target,
				snapshot.getOpponentsGoal())) {
			setApproachTargetFromCorrectSide(false);
		} else {
			setApproachTargetFromCorrectSide(true);
		}

		super.onStep(controller, snapshot);
	}


	protected boolean canStillScore(Snapshot snapshot) {

		// Get predicted snapshot after 50 frames
		SnapshotPredictor sp = snapshot.getSnapshotPredictor();
		Snapshot predSnap = sp.getSnapshotAfterTime(50);

		boolean gotBall = predSnap.getBalle().possessesBall(predSnap.getBall());
		boolean intersectingGoalLine = predSnap.getBalle().getFacingLine()
				.intersects(predSnap.getOpponentsGoal().getGoalLine());

		return (gotBall && intersectingGoalLine);

	} 

	protected boolean targetSafeGapCanBeIncreased(Snapshot snapshot, Line newTargetLine) {
		Pitch pitch = snapshot.getPitch();
		Robot ourRobot = snapshot.getBalle();
		FieldObject target = getOriginalTarget(snapshot);

		// We cannot extend the line, if we cannot reach the endpoint
		if (!pitch.containsCoord(newTargetLine.extend(Globals.ROBOT_LENGTH).getB()))
			return false;

		// We must extend the line if we are not approaching the ball from
		// correct side
		Line targetGoalLine = new Line(snapshot.getBalle().getPosition(), target.getPosition());
		targetGoalLine = targetGoalLine.extend(Globals.PITCH_WIDTH);

		if (!targetGoalLine.intersects(snapshot.getOpponentsGoal().getGoalLine().extendBothDirections(0.7)))
			return true;
		else {
			// If we are approaching the ball from correct side
			// and we are far away from point, keep extending the line
			if (newTargetLine.getB().dist(ourRobot.getPosition()) > Globals.ROBOT_LENGTH / 2 + TARGET_SAFE_GAP / 3) {
				return true;
			} else
				// Otherwise do not extend it anymore
				return false;
		}
	}

	protected FieldObject getOriginalTarget(Snapshot snapshot) {
		return super.getTarget(snapshot);
	}

	@Override
	protected FieldObject getTarget(Snapshot snapshot) {
		FieldObject target = getOriginalTarget(snapshot);
		Robot ourRobot = snapshot.getBalle();

		if (target.getPosition() == null) {
			LOG.warn("Cannot see the target");
			return null;
		}
		if (ourRobot.getPosition() == null) {
			LOG.warn("Cannot see self");
			return null;
		}
		Goal targetGoal = snapshot.getOpponentsGoal();

		Line targetLine = new Line(targetGoal.getPosition(), target.getPosition());

		double ballSafeGap = 0.005;
		Line newTargetLine = targetLine;

		Line targetGoalLine = new Line(snapshot.getBalle().getPosition(), target.getPosition());
		targetGoalLine = targetGoalLine.extend(Globals.PITCH_WIDTH);

		while (ballSafeGap < TARGET_SAFE_GAP && targetSafeGapCanBeIncreased(snapshot, newTargetLine)) {
			ballSafeGap *= 1.05;
			newTargetLine = targetLine.extend(ballSafeGap);
		}
		targetLine = newTargetLine;

		addDrawable(new DrawableLine(targetLine, Color.ORANGE));
		return new Point(targetLine.getB());
	}

	/**
	 * PFN's large turning radius mean that we usually overshoot the ball when
	 * we're really close and trying to turn towards it. This strategy just
	 * captures onStep() in that case and makes the robot turn normally
	 * 
	 * @author s0913664
	 * 
	 */
	private class TurnHack extends AbstractPlanner {

		private static final double ANGLE_THRESH = Math.PI / 4;
		private static final double DIST_THRESH = 0.08;

		private RotateToOrientationExecutor turnExecutor;

		private boolean isTurning = false;

		public TurnHack() {
			turnExecutor = new FaceAngle();
		}

		@Override
		public boolean shouldStealStep(Snapshot snapshot) {

			// Steal step when the line from the centre of the goal through the
			// wall intersects the robot, and the robot is close to the ball

			return isTurning || needsToTurn(snapshot) || shouldTurnToGoal(snapshot);

		}

		private boolean needsToTurn(Snapshot snapshot) {
			Robot ourRobot = snapshot.getBalle();
			Ball ball = snapshot.getBall();
			Goal opponentsGoal = snapshot.getOpponentsGoal();

			Line line = new Line(opponentsGoal.getPosition(), ball.getPosition()).extend(Globals.PITCH_WIDTH);
			boolean isOnCorrectSide = ourRobot.isApproachingTargetFromCorrectSide(ball, opponentsGoal);
			double absAngleToTurn = Math.abs(ourRobot.getAngleToTurnToTarget(ball
					.getPosition()));

			return isOnCorrectSide && absAngleToTurn > ANGLE_THRESH
					&& line.dist(ourRobot.getPosition()) < DIST_THRESH;
		}

		/*
		 * The opponent is behind us, we have a clear shot and goal and should
		 * therefore face it as soon as we can. Return false if we're already
		 * facing the goal.
		 */
		private boolean shouldTurnToGoal(Snapshot snapshot) {
			Robot ourRobot = snapshot.getBalle();
			Robot oppRobot = snapshot.getOpponent();
			Ball ball = snapshot.getBall();
			Goal opponentsGoal = snapshot.getOpponentsGoal();

			double oppDistToGoal = Double.POSITIVE_INFINITY;
			if (oppRobot != null && oppRobot.getPosition() != null) {
				// Opponent is on the pitch
				oppDistToGoal = oppRobot.getPosition().dist(opponentsGoal.getPosition());
			}
			double ourDistToGoal = ourRobot.getPosition().dist(opponentsGoal.getPosition());
			boolean weAreCloser = ourDistToGoal <= oppDistToGoal;

			/*
			 * are we already facing the goal?
			 */
			boolean facingGoal = ourRobot.isFacingGoal(opponentsGoal);

			return !facingGoal && weAreCloser && ourRobot.possessesBall(ball);
		}

		@Override
		protected void onStep(Controller controller, Snapshot snapshot) throws ConfusedException {

			Robot ourRobot = snapshot.getBalle();
			Ball ball = snapshot.getBall();
			Goal oppGoal = snapshot.getOpponentsGoal();

			if (!isTurning) {
				Orientation targetAngle;
				if (!ourRobot.possessesBall(ball)) {
					targetAngle = ball.getPosition().sub(ourRobot.getPosition()).orientation();
				} else {
					LOG.info("turn to goal");
					targetAngle = oppGoal.getPosition().sub(ourRobot.getPosition()).orientation();
				}
				LOG.info("TurnHack: Setting target orientation");
				turnExecutor.setTargetOrientation(targetAngle);

				isTurning = true;
			} else if (turnExecutor.isFinished(snapshot)
					|| !needsToTurn(snapshot)) {
				isTurning = false;
			}

			turnExecutor.step(controller, snapshot);

		}

	}
}
