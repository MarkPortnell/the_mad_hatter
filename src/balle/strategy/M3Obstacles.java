package balle.strategy;

import org.apache.log4j.Logger;

import balle.controller.Controller;
import balle.strategy.planner.AbstractPlanner;
import balle.strategy.planner.GoToBallSafeProportional;
import balle.world.Coord;
import balle.world.Snapshot;

//Use vision and touch sensors to respond appropriately to obstacles. When set
//rolling toward another robot from more than 30cm away, your robot should
//avoid the other robot. Once the start point is too close for visual avoidance,
//your robot should back off if it contacts the other robot.

public class M3Obstacles extends AbstractPlanner {

	private static final Logger LOG = Logger.getLogger(M3Obstacles.class);

	//GoToObjectPFN goto_executor;
	GoToBallSafeProportional goto_executor;
	Coord startingCoordinate = null;
	Coord initialCoordinate = null;
	boolean startRecorded = false;
	boolean goneForward = false;

	public M3Obstacles() {
		goto_executor = new GoToBallSafeProportional(0.5, 0.2, true);
	}

	@Override
	public void onStep(Controller controller, Snapshot snapshot)
			throws ConfusedException {

		startingCoordinate = snapshot.getBalle().getPosition();

		// if (!startRecorded) {
		// initialCoordinate = startingCoordinate;
		// startRecorded = true;
		// }
		//
		// if (initialCoordinate.dist(snapshot.getOpponent().getPosition()) <=
		// 0.30) {
		// // Starting closer than 15 centimetres
		// LOG.info("Too close can't avoid");
		// controller.forward(500);
		// goneForward = true;
		// }
		//
		// if (goneForward
		// && snapshot.getOpponent().getPosition()
		// .dist(startingCoordinate) > 0.3) {
		// // GoneForward and crash and backed up so now stop
		// controller.stop();
		// }

		LOG.info("Navigating to ball");
		goto_executor.step(controller, snapshot);

		if (snapshot.getBall().getPosition().dist(startingCoordinate) <= 0.17) {
			LOG.info("Reached Goal");
			controller.stop();
			return;
		}

				// controller.stop();

		return;
	}

	@FactoryMethod(designator = "M3Avoid", parameterNames = {})
	public static final M3Obstacles factory() {
		return new M3Obstacles();
	}

}
