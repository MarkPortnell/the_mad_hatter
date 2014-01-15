package balle.strategy;

import java.awt.Color;

import org.apache.log4j.Logger;

import balle.controller.Controller;
import balle.main.drawable.Label;
import balle.misc.Globals;
import balle.strategy.planner.AbstractPlanner;
import balle.world.Coord;
import balle.world.Orientation;
import balle.world.Snapshot;
import balle.world.objects.Ball;
import balle.world.objects.Robot;


public class DribbleRefactored extends AbstractPlanner {

	private static final int INITIAL_TURN_SPEED = 100;

	private static final int INITIAL_CURRENT_SPEED = 100;

    private static Logger LOG = Logger.getLogger(Dribble.class);

	private int currentSpeed = INITIAL_CURRENT_SPEED;
	private int turnSpeed = INITIAL_TURN_SPEED;
    private long lastDribbled = 0;
    private long firstDribbled = 0;
	private double MAX_DRIBBLE_PAUSE = 700; // ms
    private double MAX_DRIBBLE_LENGTH = 500; // ms
    private static final double ABOUT_TO_LOSE_BALL_THRESHOLD = Globals.ROBOT_WIDTH
            / 2 + Globals.BALL_RADIUS - 0.02;

    private static final double FACING_WALL_THRESHOLD = Math.toRadians(25);
	private static final double SPINNING_DISTANCE = Globals.DISTANCE_TO_WALL;
    private boolean triggerHappy;
	private boolean kicked = false;

    public boolean isTriggerHappy() {
        return triggerHappy;
    }

	public boolean hasKicked() {
		return kicked;
	}

    public void setTriggerHappy(boolean triggerHappy) {
        this.triggerHappy = triggerHappy;
    }

	public void setKicked(boolean kicked) {
		this.kicked = kicked;
	}

	public DribbleRefactored() {
		this(false, false);
	}
	
	public DribbleRefactored(boolean triggerHappy, boolean kicked) {
        super();
        setTriggerHappy(triggerHappy);
		setKicked(kicked);
    }

	@FactoryMethod(designator = "DribbleRefactored", parameterNames = {})
	public static DribbleRefactored factoryMethod() {
		return new DribbleRefactored();
	}

	/**
	 * Returns true if we have dribbled for too long
	 */
    public boolean shouldStopDribblingDueToDribbleLength() {
        double deltaStart = (System.currentTimeMillis() - firstDribbled);
        return deltaStart > MAX_DRIBBLE_LENGTH;
    }

	/**
	 * Returns true if we have not dribbled for a while
	 */
    public boolean isInactiveForAWhile() {
        double deltaPause = (System.currentTimeMillis() - lastDribbled);
        return deltaPause > MAX_DRIBBLE_PAUSE;
    }

	/**
	 * Returns true if we are currently dribbling
	 */
    public boolean isDribbling() {
        return !isInactiveForAWhile()
                && (!shouldStopDribblingDueToDribbleLength());
    }

    public void spinLeft(Snapshot snapshot, Controller controller, int speed) {
        controller.setWheelSpeeds(-speed, speed);
        addDrawable(new Label("<---", snapshot.getBalle().getPosition(),
                Color.CYAN));
    }
	
    public void spinRight(Snapshot snapshot, Controller controller, int speed) {
        controller.setWheelSpeeds(speed, -speed);
        addDrawable(new Label("--->", snapshot.getBalle().getPosition(),
                Color.CYAN));
    }

	public boolean needToStraightenUp(Snapshot snapshot) {
		Robot ourBot = snapshot.getBalle();
		Ball ball = snapshot.getBall();
		Coord ballPos = ball.getPosition();
		double ballY = ballPos.getY();

		Coord midpoint = ourBot.getFrontSide().midpoint();
		double midpointY = midpoint.getY();
		Coord robotedgeA = ourBot.getFrontSide().getA();
		double edgeAY = robotedgeA.getY();
		Coord robotedgeB = ourBot.getFrontSide().getB();
		double edgeBY = robotedgeB.getY();
		double topEdge;
		double bottomEdge;

		if (edgeAY > edgeBY) {
			topEdge = edgeAY;
			bottomEdge = edgeBY;
		} else {
			bottomEdge = edgeAY;
			topEdge = edgeBY;
		}

		if (topEdge - ballY < ((topEdge - midpointY) / 2)) {
			return true;
		} else if (ballY - bottomEdge < ((midpointY - ballY) / 2)) {
			return true;	
		} else {
			return false;
		}
	}

	/**
	 * Returns true if we are facing the opponents goal
	 */
	public boolean facingOppGoal(Snapshot snapshot) {
		Robot ourBot = snapshot.getBalle();
		Coord robotPos = ourBot.getPosition();
		Ball ball = snapshot.getBall();

		boolean facingGoal = ourBot.getFacingLine().intersects(
				snapshot.getOpponentsGoal().getGoalLine());

		if (robotPos != null) {
			facingGoal = facingGoal
					|| ourBot.getBallKickLine(ball).intersects(
							snapshot.getOpponentsGoal().getGoalLine());
		}
		return facingGoal;

	}

	/**
	 * Returns true if we are close enough to the opponents goal to shoot.
	 */
	public boolean closeEnoughToShoot(Snapshot snapshot) {
		Robot ourBot = snapshot.getBalle();
		Coord ourPos = ourBot.getPosition();
		Coord oppGoal = snapshot.getOpponentsGoal().getPosition();
		double distance = ourPos.dist(oppGoal);
		return distance < (Globals.PITCH_WIDTH * 0.35);
	}

	@Override
	public void onStep(Controller controller, Snapshot snapshot) throws ConfusedException {
		Robot ourBot = snapshot.getBalle();
		Coord robotPos = ourBot.getPosition();
		Ball ball = snapshot.getBall();

		if (robotPos == null) {
            return;
		}


        long currentTime = System.currentTimeMillis();
        boolean facingOwnGoalSide = ourBot.isFacingGoalHalf(snapshot.getOwnGoal());
      
        if (!isDribbling()) {
            // Kick the ball if we're triggerhappy and should stop dribbling
            if (isTriggerHappy() && !isInactiveForAWhile()
                    && shouldStopDribblingDueToDribbleLength()
					&& !facingOwnGoalSide && closeEnoughToShoot(snapshot)) {
				controller.kick();
				LOG.info("Dribble: KICK 1");
				setKicked(true);
			}
        }
		firstDribbled = currentTime;
		lastDribbled = currentTime;

		Coord midpoint = ourBot.getFrontSide().midpoint();
		double distanceToBall = midpoint.dist(ball.getPosition());

		// if robot position is Estimated assume we are at the ball
		if (robotPos.isEstimated()) {
            distanceToBall = 0;
		}

        boolean aboutToLoseBall = distanceToBall >= ABOUT_TO_LOSE_BALL_THRESHOLD;
        Color c = Color.BLACK;

		if (aboutToLoseBall) {
            c = Color.PINK;
		}

		addDrawable(new Label(String.format("%.5f", distanceToBall), new Coord(
				robotPos.getX(), robotPos.getY()), c));
                
        int turnSpeedToUse = turnSpeed;
		boolean isLeftGoal = snapshot.getOpponentsGoal().isLeftGoal();
		double angle = ourBot.getOrientation().radians();
		double threshold = Math.toRadians(5);
        boolean nearWall = snapshot.getBall().isNearWall(snapshot.getPitch());
		boolean wereNearWall = ourBot.isNearWall(
				snapshot.getPitch(), SPINNING_DISTANCE);
        boolean closeToGoal = snapshot.getOpponentsGoal().getGoalLine()
				.dist(robotPos) < SPINNING_DISTANCE;

        // Actually it might be helpful to turn when we're in this situation
        // close to our own goal
		closeToGoal = closeToGoal
				|| snapshot.getOwnGoal().getGoalLine().dist(robotPos) < SPINNING_DISTANCE;

        // Turn twice as fast near walls
		if (nearWall) {
            turnSpeedToUse *= 2;
		}
        
		if ((!closeToGoal) && (nearWall) && wereNearWall) {
			Coord goalVector = snapshot.getOwnGoal().getGoalLine().midpoint()
					.sub(robotPos);
            Orientation angleTowardsGoal = goalVector.orientation();

            // Always turn opposite from own goal
            boolean shouldTurnRight = !angleTowardsGoal.isFacingRight(0);

            // If we're facing wall
            if ((Math.abs(angle) <= FACING_WALL_THRESHOLD)
                || (Math.abs(angle - Math.PI / 2) <= FACING_WALL_THRESHOLD)
                || (Math.abs(angle - Math.PI) <= FACING_WALL_THRESHOLD)
                    || (Math.abs(angle - 3 * Math.PI / 2) <= FACING_WALL_THRESHOLD)) {
                LOG.info("Spinning!!!");

                // If We are facing the bottom wall we should flip the spinning
                // directions
                if (Math.abs(angle - 3 * Math.PI / 2) <= FACING_WALL_THRESHOLD)
                    shouldTurnRight = !shouldTurnRight;
                else if ((Math.abs(angle) <= FACING_WALL_THRESHOLD)
                        || (Math.abs(angle - Math.PI) <= FACING_WALL_THRESHOLD))
                {
                    // If we're facing one of the walls with goals
                    boolean facingLeftWall = (Math.abs(angle) <= FACING_WALL_THRESHOLD);
                    if (facingLeftWall) {
                        shouldTurnRight = snapshot.getOpponentsGoal()
								.getGoalLine().midpoint().getY() > robotPos
								.getY();

                        // Turn away from own goal
                        if (snapshot.getOwnGoal().isLeftGoal())
                            shouldTurnRight = !shouldTurnRight;
                    } else {
                        shouldTurnRight = snapshot.getOpponentsGoal()
								.getGoalLine().midpoint().getY() < robotPos
                                .getY();

                        // Turn away from own goal
                        if (snapshot.getOwnGoal().isRightGoal())
                            shouldTurnRight = !shouldTurnRight;
                    }
                }
				if (shouldTurnRight) {
                    spinRight(snapshot, controller, Globals.MAXIMUM_MOTOR_SPEED);
				} else {
                    spinLeft(snapshot, controller, Globals.MAXIMUM_MOTOR_SPEED);
				}
                return;
			}
        }

		boolean facingGoal = facingOppGoal(snapshot);
		if (isLeftGoal) {
			if (facingGoal) {
                controller.setWheelSpeeds(Globals.MAXIMUM_MOTOR_SPEED,
                        Globals.MAXIMUM_MOTOR_SPEED);
			} else if ((!facingGoal) && (angle < Math.PI - threshold)) {

				if (needToStraightenUp(snapshot)) {
					controller.setWheelSpeeds(
							currentSpeed + turnSpeedToUse / 2, currentSpeed);
					LOG.info("staightening");
				} else {
					controller.setWheelSpeeds(currentSpeed, currentSpeed
							+ turnSpeedToUse);
				}

			} else if ((!facingGoal) && (angle > Math.PI + threshold)) {

				if (needToStraightenUp(snapshot)) {
					controller.setWheelSpeeds(currentSpeed, currentSpeed
							+ turnSpeedToUse / 2);
					LOG.info("staightening");
				} else {
					controller.setWheelSpeeds(currentSpeed + turnSpeedToUse,
							currentSpeed);
				}

			} else {
                controller.setWheelSpeeds(currentSpeed, currentSpeed);
			}
        } else {
			if (facingGoal) {
                controller.setWheelSpeeds(Globals.MAXIMUM_MOTOR_SPEED,
                        Globals.MAXIMUM_MOTOR_SPEED);
			} else if ((!facingGoal) && (angle > threshold)
					&& (angle < Math.PI)) {
				if (needToStraightenUp(snapshot)) {
					controller.setWheelSpeeds(currentSpeed, currentSpeed
							+ turnSpeedToUse / 2);
					LOG.info("staightening");
				} else {
					controller.setWheelSpeeds(currentSpeed + turnSpeedToUse,
							currentSpeed);
				}

			} else if ((!facingGoal) && (angle < (2 * Math.PI) - threshold)
					&& (angle > Math.PI)) {
				if (needToStraightenUp(snapshot)) {
					controller.setWheelSpeeds(
							currentSpeed + turnSpeedToUse / 2, currentSpeed);
					LOG.info("staightening");
				} else {
					controller.setWheelSpeeds(currentSpeed, currentSpeed
							+ turnSpeedToUse);
				}
			} else {
                controller.setWheelSpeeds(currentSpeed, currentSpeed);
			}
		}

        if (facingGoal || (isTriggerHappy() && nearWall && !facingOwnGoalSide)
                || (isTriggerHappy() && aboutToLoseBall && !facingOwnGoalSide)) {
            controller.kick();
			LOG.info("Dribble: KICK 2");
			setKicked(true);
        }
	}
}