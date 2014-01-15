package balle.strategy;

import org.apache.log4j.Logger;

import balle.controller.Controller;
import balle.strategy.bezierNav.BezierNav;
import balle.strategy.curve.CustomCHI;
import balle.strategy.pathFinding.SimplePathFinder;
import balle.strategy.planner.AbstractPlanner;
import balle.strategy.planner.SimpleGoToBallFaceGoal;
import balle.world.Snapshot;
import balle.world.objects.Ball;
import balle.world.objects.Robot;

public class M3Avoid extends AbstractPlanner {

	private static final Logger LOG = Logger.getLogger(M2dribble.class);
	Strategy goToBallBezier;
	Boolean has_ball = false;

	public M3Avoid() {
		goToBallBezier = new SimpleGoToBallFaceGoal(new BezierNav(
				new SimplePathFinder(new CustomCHI())));
	}

	@Override
	protected void onStep(Controller controller, Snapshot snapshot)
			throws ConfusedException {

		Robot ourRobot = snapshot.getBalle();
		Ball ball = snapshot.getBall();

		if (has_ball) {
			return;
		}

		else if (ourRobot.possessesBall(ball)) {
			controller.stop();
			LOG.info("Has possesion");
			has_ball = true;
			return;
		}

		else {
			LOG.info("Navigating to ball");
			goToBallBezier.step(controller, snapshot);
			return;
		}

	}

	/*
	 * Commenting out strategy in simulator
	 * 
	 * @FactoryMethod(designator = "Avoid", parameterNames = {}) public static
	 * final M3Avoid factory() { return new M3Avoid(); }
	 */

}
