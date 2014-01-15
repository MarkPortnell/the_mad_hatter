package balle.strategy;

import java.awt.Color;

import org.apache.log4j.Logger;

import balle.controller.Controller;
import balle.main.drawable.Circle;
import balle.main.drawable.Dot;
import balle.main.drawable.DrawableLine;
import balle.main.drawable.DrawableVector;
import balle.main.drawable.Label;
import balle.misc.Globals;
import balle.strategy.executor.movement.GoToObjectPFN;
import balle.strategy.executor.movement.MovementExecutor;
import balle.strategy.planner.AbstractPlanner;
import balle.world.Coord;
import balle.world.Line;
import balle.world.Orientation;
import balle.world.Snapshot;
import balle.world.Velocity;
import balle.world.objects.Ball;
import balle.world.objects.CircularBuffer;
import balle.world.objects.Point;
import balle.world.objects.Robot;

public class DefendAndIntercept extends AbstractPlanner {
    private Coord   intercept          = new Coord(0, 0);
    private boolean shouldPlayGame;
    private static final double STRATEGY_STOP_DISTANCE = 0.3;
    private static final double GO_DIRECTLY_TO_BALL_DISTANCE = STRATEGY_STOP_DISTANCE * 1.75;

	protected final boolean mirror;
    protected CircularBuffer<Coord> ballCoordBuffer;

    private static Logger LOG                = Logger.getLogger(DefendAndIntercept.class);

    private MovementExecutor movementExecutor;
    private AbstractPlanner gameStrategy;


    protected void setIAmDoing(String message) {
        LOG.info(message);
    }

	public DefendAndIntercept() {
		this(new GoToObjectPFN(Globals.ROBOT_LENGTH / 3, false), true);
	}

	public DefendAndIntercept(MovementExecutor movementExecutor, boolean mirror) {
        super();
        ballCoordBuffer = new CircularBuffer<Coord>(6);
		this.mirror = mirror;
        
        this.movementExecutor = movementExecutor;
        shouldPlayGame = false;
        this.gameStrategy = new Game(false);

    }


    @Override
    public void onStep(Controller controller, Snapshot snapshot) throws ConfusedException {

        Ball ball = snapshot.getBall();
        if (ball.getPosition() == null)
            return;

        ballCoordBuffer.add(ball.getPosition());

		intercept = getPredictionCoordVelocityvector(snapshot, mirror);


        if (!shouldPlayGame) {
            addDrawable(new Circle(snapshot.getBalle().getPosition(),
                    STRATEGY_STOP_DISTANCE, Color.red));
            addDrawable(new Circle(snapshot.getBalle().getPosition(),
                    STRATEGY_STOP_DISTANCE * 1.75, Color.red));
        }
		if (snapshot.getBalle().getPosition().dist(snapshot.getBall().getPosition()) < STRATEGY_STOP_DISTANCE) {
			shouldPlayGame = true;
        }

        if (shouldPlayGame) {
			setIAmDoing("Game");
            gameStrategy.step(controller, snapshot);
            addDrawables(gameStrategy.getDrawables());
        } else {
            setIAmDoing("Going to point - predict");

            Coord goToCoord = intercept;
            if (snapshot.getBalle().getPosition()
                    .dist(snapshot.getBall().getPosition()) < GO_DIRECTLY_TO_BALL_DISTANCE) {
                goToCoord = snapshot.getBall().getPosition();
            }
            addDrawable(new Dot(goToCoord, Color.BLACK));
            addDrawable(new Dot(intercept, new Color(0, 0, 0, 100)));
            if (movementExecutor != null) {
                movementExecutor.updateTarget(new Point(goToCoord));
                addDrawables(movementExecutor.getDrawables());
                movementExecutor.step(controller, snapshot);

            }
		}
    }

	protected Coord getPredictionCoordVelocityvector(Snapshot s, boolean mirror) {
        Ball ball = s.getBall();
        Robot ourRobot = s.getBalle();

		Coord ballPos, currPos;
		ballPos = ball.getPosition();
		currPos = ourRobot.getPosition();

		double dist = (new Line(currPos, s.getOwnGoal().getPosition()))
				.length();
		if (mirror && dist > (Globals.PITCH_WIDTH / 2)) {

			addDrawable(new Label("length = " + dist, new Coord(0, 0),
					Color.ORANGE));

			return s.getOwnGoal().getPosition();

		}


        Velocity vel = ball.getVelocity();
        Coord vec = new Coord(vel.getX(), vel.getY());
        vec = vec.mult(0.5 / vec.abs());
        
		Line ballRobotLine = new Line(ballPos, currPos);

		Line ballDirectionLine = new Line(ballPos, currPos.add(vec));
        ballDirectionLine = ballDirectionLine.extend(Globals.PITCH_WIDTH);


        Line rotatedRobotBallLine = ballRobotLine.rotateAroundPoint(
                ballRobotLine.midpoint(), Orientation.rightAngle)
                .extendBothDirections(Globals.PITCH_WIDTH);

        Coord pivot = rotatedRobotBallLine.getIntersect(ballDirectionLine);
       
		Coord CP = ballDirectionLine.closestPoint(currPos);
        addDrawable(new DrawableLine(ballDirectionLine, Color.WHITE));
        addDrawable(new Dot(CP, Color.WHITE));
		addDrawable(new DrawableLine(new Line(CP, currPos),
                Color.CYAN));


        if (pivot == null)
            return CP;

        Coord scaler = CP.sub(pivot);
		Orientation theta = ballPos.angleBetween(currPos, CP);
		Orientation theta2 = ballPos.angleBetween(CP, currPos);
        
		double minTheta = Math.min(theta.radians(), theta2.radians());

        scaler = scaler.mult(Math.abs(minTheta) / (Math.PI / 2));
        
		Coord predictCoord = pivot.sub(scaler);
		Line robotPredictLine = new Line(currPos, predictCoord);
		robotPredictLine = robotPredictLine.extend(1);
        addDrawable(new DrawableLine(robotPredictLine, Color.PINK));
		Line oppFacingLine = s.getOpponent().getFacingLine();
		predictCoord = robotPredictLine.getIntersect(oppFacingLine);

        addDrawable(new DrawableVector(pivot, scaler, Color.BLACK));
        return predictCoord;
    }


}
