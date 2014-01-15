package balle.strategy;

import java.awt.Color;

import org.apache.log4j.Logger;

import balle.controller.Controller;
import balle.main.drawable.Dot;
import balle.main.drawable.DrawableLine;
import balle.world.Coord;
import balle.world.Line;
import balle.world.Orientation;
import balle.world.Snapshot;
import balle.world.objects.Ball;
import balle.world.objects.Robot;

public class GameFromPenaltyDefence extends Game {

    private int SPEED;

    private static Logger LOG = Logger.getLogger(GameFromPenaltyDefence.class);

	private Snapshot firstSnapshot = null;
	String robotState = "Center";
	int rotateSpeed = 0;

    private enum MovementDirection {
        FORWARD, BACKWARD, NONE
    };

	private boolean finished = false;

    public GameFromPenaltyDefence(int speed) {
        super();
        SPEED = speed;
	}

    public GameFromPenaltyDefence() {
        this(200);
    }

	
    @FactoryMethod(designator = "Game (Penalty Defence)", parameterNames = {})
	public static GameFromPenaltyDefence gameFromPenaltyDefenceFactory()
	{
		return new GameFromPenaltyDefence(500);
	}

	public boolean isStillInPenaltyDefence(Snapshot snapshot) {

		if (snapshot.getBalle().possessesBall(snapshot.getBall())) {
			finished = true;
			return false;
		}
		
		Coord ball = snapshot.getBall().getPosition();
		if ((ball == null) && snapshot.getOwnGoal().isLeftGoal())
			ball = new Coord(0.5, 0.6); // assume that ball is on penalty spot,
										// if
									// we cannot see it.
		if ((ball == null) && !snapshot.getOwnGoal().isLeftGoal())
			ball = new Coord(1.8, 0.6); // assume that ball is on penalty spot,
										// if
									// we cannot see it.

		double minX = 0;
		double maxX = 0.75;
		if (!snapshot.getOwnGoal().isLeftGoal()) {
			maxX = snapshot.getPitch().getMaxX();
			minX = maxX - 0.75;
		}

		if (ball.isEstimated()
				|| (ball.getY() < snapshot.getOwnGoal().getMaxY())
				&& (ball.getY() > snapshot.getOwnGoal().getMinY())
				&& (ball.getX() > minX) && (ball.getX() < maxX)) {

			return true;
		}

		finished = true;
		return false;
	}

    public MovementDirection getMovementDirection(Snapshot snapshot) {
        Robot opponent = snapshot.getOpponent();
        Robot ourRobot = snapshot.getBalle();
        Ball ball = snapshot.getBall();

        if ((opponent.getPosition() == null)
                || (ourRobot.getPosition() == null))
            return MovementDirection.NONE;
        

		Coord intersectA = new Coord(ourRobot.getPosition().getX(), snapshot
				.getPitch().getMaxY());
		Coord intersectB = new Coord(ourRobot.getPosition().getX(), snapshot
				.getPitch().getMinY());
		Line ourBotLine = new Line(intersectA, intersectB);

		addDrawable(new DrawableLine(ourBotLine, Color.PINK));

        Coord intersectionPoint = null;
		if ((ball.getPosition() != null) && (!ball.getPosition().isEstimated())) {
            intersectionPoint = opponent.getBallKickLine(ball).getIntersect(
					ourBotLine);
			addDrawable(new DrawableLine(opponent.getBallKickLine(ball),
					Color.PINK));
		}

		if (intersectionPoint == null) {
            intersectionPoint = opponent.getFacingLine().getIntersect(
					ourBotLine);
			addDrawable(new DrawableLine(opponent.getFacingLine(), Color.PINK));

		}

		addDrawable(new Dot(intersectionPoint, Color.GREEN));

        if (intersectionPoint == null)
            return MovementDirection.NONE;
        
        boolean isUpward;
        Orientation ourOrientation = ourRobot.getOrientation();
        if ((ourOrientation.radians() >= 0)
                && (ourOrientation.radians() < Math.PI))
                isUpward = true;
        else
            isUpward = false;

        // If we are already blocking the point stop
		if (ourRobot.containsCoord(intersectionPoint))
            return MovementDirection.NONE;

		double diff = (intersectionPoint.getY() - ourRobot.getPosition().getY());
        if (diff > 0)
            if (isUpward)
                return MovementDirection.FORWARD;
            else
                return MovementDirection.BACKWARD;
        else if (isUpward)
            return MovementDirection.BACKWARD;
        else
            return MovementDirection.FORWARD;
            
    }
	@Override
	public void onStep(Controller controller, Snapshot snapshot) throws ConfusedException {



		if (finished || !isStillInPenaltyDefence(snapshot)) {
			super.onStep(controller, snapshot);
			return;
		}

		if (snapshot.getBalle().getPosition() == null) {
			return;
		}

		if ((firstSnapshot == null)
				&& (snapshot.getBall().getPosition() != null)) {
			firstSnapshot = snapshot;
		}

        MovementDirection movementDirection = getMovementDirection(snapshot);
        if (movementDirection == MovementDirection.FORWARD)
            controller.setWheelSpeeds(SPEED, SPEED);
        else if (movementDirection == MovementDirection.BACKWARD)
            controller.setWheelSpeeds(-SPEED, -SPEED);
		else
            controller.stop();
	}
}