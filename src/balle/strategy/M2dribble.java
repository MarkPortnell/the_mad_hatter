package balle.strategy;

import org.apache.log4j.Logger;

import balle.controller.Controller;
import balle.strategy.planner.AbstractPlanner;
import balle.strategy.planner.GoToBallSafeProportional;
import balle.world.Coord;
import balle.world.Snapshot;
import balle.world.objects.Ball;
import balle.world.objects.Robot;

public class M2dribble extends AbstractPlanner {

	private static final Logger LOG = Logger.getLogger(M2dribble.class);

	Milestone2DribbleStrategy dribble_executor;
	GoToBallSafeProportional goto_executor;
	Coord startingCoordinate = null;
	Coord currentCoordinate = null;
	Boolean finished = false;
	private static final double DISTANCE_TO_TRAVEL = 0.3; // in metres

	public M2dribble() {
		dribble_executor = new Milestone2DribbleStrategy();
		goto_executor = new GoToBallSafeProportional();
	}

	@Override
	public void onStep(Controller controller, Snapshot snapshot)
			throws ConfusedException {

		Robot ourRobot = snapshot.getBalle();
		Ball ball = snapshot.getBall();

		if (finished) {
			controller.stop();
			return;
		}


		if (ourRobot.possessesBall(ball)) {

			if(startingCoordinate == null){
				startingCoordinate = snapshot.getBalle().getPosition();
			}

			currentCoordinate = snapshot.getBalle().getPosition();
			LOG.info(currentCoordinate.dist(startingCoordinate));
			if (currentCoordinate.dist(startingCoordinate) < DISTANCE_TO_TRAVEL) {
				LOG.info("Beginning dribbling...");
				dribble_executor.step(controller, snapshot);
				return;
			} else {
				finished = true;
				LOG.info("Finished.");
				controller.stop();
				return;
			}

		}

		else {

			goto_executor.step(controller, snapshot);
			return;
		}


	}

	@FactoryMethod(designator = "M2dribble", parameterNames = {})
	public static final M2dribble factory() {
		return new M2dribble();
	}

}

