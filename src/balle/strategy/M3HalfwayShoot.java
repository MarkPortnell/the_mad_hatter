package balle.strategy;

import org.apache.log4j.Logger;

import balle.controller.Controller;
import balle.strategy.executor.dribbling.DribbleStraight;
import balle.strategy.executor.movement.GoToObjectPFN;
import balle.strategy.planner.AbstractPlanner;
import balle.world.Coord;
import balle.world.Snapshot;

public class M3HalfwayShoot extends AbstractPlanner {

	private static final Logger LOG = Logger.getLogger(M3HalfwayShoot.class);

	DribbleStraight dribble_executor;
	GoToObjectPFN goto_executor;
	Coord startingCoordinate = null;
	Coord currentCoordinate = null;
	Boolean dribbling_finished = false;
	Boolean kicking_finished = false;
	// Boolean atBall = false;
	private static final double DISTANCE_TO_TRAVEL = 0.3; // in metres

	public M3HalfwayShoot() {
		dribble_executor = new DribbleStraight();
		goto_executor = new GoToObjectPFN(0);
	}

	@Override
	public void onStep(Controller controller, Snapshot snapshot)
			throws ConfusedException {

		if (kicking_finished) {
			return;
		}

		if (dribbling_finished) {
			controller.kick();
			LOG.info("Kicking");
			try {
				Thread.sleep(1000);
			} catch (Exception e) {
				e.printStackTrace();
			}
			LOG.info("Kicking finished");
			kicking_finished = true;
			return;
		}

		if (dribble_executor.isPossible(snapshot)) {
			if (startingCoordinate == null) {
				startingCoordinate = snapshot.getBalle().getPosition();
				LOG.info("Beginning dribbling...");
			}

			currentCoordinate = snapshot.getBalle().getPosition();
			LOG.info(currentCoordinate.dist(startingCoordinate));
			if (currentCoordinate.dist(startingCoordinate) < DISTANCE_TO_TRAVEL) {
				LOG.info("Still dribbling, "
						+ (DISTANCE_TO_TRAVEL - currentCoordinate
								.dist(startingCoordinate)) + " to go");
				dribble_executor.step(controller, snapshot);
			} else {
				dribbling_finished = true;
				LOG.info("Finished dribbling.");
				controller.stop();
			}

		}

		else {

			LOG.info("Ball is too far away, moving closer");
			goto_executor.updateTarget(snapshot.getBall());
			if (goto_executor.isPossible(snapshot)) {
				goto_executor.step(controller, snapshot);
			}

			else {
				LOG.info("Fail");
				return;
			}

		}
		return;

	}

	@FactoryMethod(designator = "M3HalfwayShoot", parameterNames = {})
	public static final M3HalfwayShoot factory() {
		return new M3HalfwayShoot();
	}

}

