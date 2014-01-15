package balle.strategy;

import org.apache.log4j.Logger;

import balle.controller.Controller;
import balle.strategy.executor.movement.GoToObjectPFN;
import balle.strategy.planner.AbstractPlanner;
import balle.world.Coord;
import balle.world.Snapshot;

public class M2locate extends AbstractPlanner {

	private static final Logger LOG = Logger.getLogger(M2locate.class);

	GoToObjectPFN goto_executor;
	Coord startingCoordinate = null;
	Coord currentCoordinate = null;
	Boolean finished = false;

	public M2locate() {
		goto_executor = new GoToObjectPFN(0);
	}

	@Override
	public void onStep(Controller controller, Snapshot snapshot)
			throws ConfusedException {

		if (finished) {
			return;
		}

		startingCoordinate = snapshot.getBalle().getPosition();
		goto_executor.updateTarget(snapshot.getBall());

		if (goto_executor.isPossible(snapshot)) {

			if (snapshot.getBall().getPosition().dist(startingCoordinate) > 0.17) {
				LOG.info("Navigate to ball");
				goto_executor.step(controller, snapshot);
			} else {
				finished = true;
				LOG.info("At the ball");
				controller.stop();
			}
		}
		return;

	}

	@FactoryMethod(designator = "M2locate", parameterNames = {})
	public static final M2locate factory() {
		return new M2locate();
	}

}
