package balle.strategy.basic;

import org.apache.log4j.Logger;

import balle.controller.Controller;
import balle.misc.Globals;
import balle.strategy.ConfusedException;
import balle.strategy.FactoryMethod;
import balle.strategy.executor.turning.FaceAngle;
import balle.strategy.planner.AbstractPlanner;
import balle.world.Coord;
import balle.world.Snapshot;
import balle.world.objects.Ball;
import balle.world.objects.Robot;

public class Initial extends AbstractPlanner {

	private static final Logger LOG = Logger.getLogger(Initial.class);

	public static boolean finished = false;

	private static Coord initialCoord = null;

	public Initial() {
	}

	@Override
	public void onStep(Controller controller, Snapshot snapshot) throws ConfusedException {

		Robot ourRobot = snapshot.getBalle();
		Robot oppRobot = snapshot.getOpponent();
		Ball ball = snapshot.getBall();
		
		if (initialCoord == null) {
			initialCoord = ourRobot.getPosition();
		}
		
		if (finished || ourRobot.getPosition() == null || oppRobot.getPosition() == null || ball.getPosition() == null) {
			LOG.info("Finished!");
			return;
		}

		if (ourRobot.getPosition().dist(initialCoord) >= 0.50) {

			controller.forward(200);
			finished = true;

			if (oppRobot.possessesBall(ball)) {
				LOG.info("Opponent has ball: Stopping.");
			} else {
				LOG.info("Close to ball: Stopping.");
			}

			return;
		} else {
			controller.forward(750);
			return;
		}

	}

	@Override
	public void stop(Controller controller) {
		controller.stop();
	}

	@FactoryMethod(designator = "Initial", parameterNames = {})
	public static final Initial factory() {
		return new Initial();
	}

}
