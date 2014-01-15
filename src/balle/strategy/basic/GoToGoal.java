package balle.strategy.basic;

import java.awt.Color;

import org.apache.log4j.Logger;

import balle.controller.Controller;
import balle.main.drawable.DrawableLine;
import balle.strategy.ConfusedException;
import balle.strategy.FactoryMethod;
import balle.strategy.executor.movement.GoToObjectPFN;
import balle.strategy.planner.AbstractPlanner;
import balle.world.Coord;
import balle.world.Line;
import balle.world.Snapshot;
import balle.world.objects.Ball;
import balle.world.objects.Goal;
import balle.world.objects.Point;
import balle.world.objects.Robot;

/*
 * if (movementExecutor != null) {
                movementExecutor.updateTarget(new Point(goToCoord));
                addDrawables(movementExecutor.getDrawables());
                movementExecutor.step(controller, snapshot);
 */

public class GoToGoal extends AbstractPlanner {

	private static final Logger LOG = Logger.getLogger(Initial.class);

	GoToObjectPFN goto_executor;
	Boolean finished = false;
	Boolean arrived_at_goal = false;

	public GoToGoal() {
		goto_executor = new GoToObjectPFN(0, true);
	}

	@Override
	public void onStep(Controller controller, Snapshot snapshot) throws ConfusedException {

		Robot ourRobot = snapshot.getBalle();
		Robot oppRobot = snapshot.getOpponent();
		Goal ownGoal = snapshot.getOwnGoal();
		Ball ball = snapshot.getBall();
		Coord target = snapshot.getPitch().goToGoalTarget(snapshot);

		if (finished || ourRobot.getPosition() == null || oppRobot.getPosition() == null || ball.getPosition() == null) {
			return;
		}

		if (ourRobot.getPosition().dist(ownGoal.getPosition()) < 0.4) {

			goto_executor.stop(controller);
			finished = true;

			LOG.info("Reached our goal!");

			return;
		} else {
			
			goto_executor.updateTarget(new Point(target));
			addDrawable(new DrawableLine(new Line(target, target), Color.magenta));
			goto_executor.step(controller, snapshot);
			return;
		}

	}

	@Override
	public void stop(Controller controller) {
		goto_executor.stop(controller);
	}

	@FactoryMethod(designator = "GoToGoal", parameterNames = {})
	public static final GoToGoal factory() {
		return new GoToGoal();
	}

}

