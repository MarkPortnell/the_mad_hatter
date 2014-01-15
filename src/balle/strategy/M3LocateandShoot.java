package balle.strategy;

import org.apache.log4j.Logger;

import balle.controller.Controller;
import balle.strategy.planner.AbstractPlanner;
import balle.strategy.planner.GoToBallSafeProportional;
import balle.world.Snapshot;
import balle.world.objects.Ball;
import balle.world.objects.Goal;
import balle.world.objects.Robot;

public class M3LocateandShoot extends AbstractPlanner {

	private static final Logger LOG = Logger.getLogger(M3LocateandShoot.class);

	Milestone3DribbleStrategy dribble_executor;
	GoToBallSafeProportional goto_executor;
	Boolean finished = false;
	Boolean arrived_at_ball = false;

	public M3LocateandShoot() {
		dribble_executor = new Milestone3DribbleStrategy();
		goto_executor = new GoToBallSafeProportional();
	}

	public static boolean haveScored(Goal opponentsGoal, Ball ball) {
		LOG.info("Checking if we've scored...");
		if (opponentsGoal.isLeftGoal()) {
			return opponentsGoal.getMaxX() - ball.getPosition().getX() > 0;
		}
		else {
			return opponentsGoal.getMaxX() - ball.getPosition().getX() > 0;
		}

	}

	@Override
	public void onStep(Controller controller, Snapshot snapshot)
			throws ConfusedException {

		Robot ourRobot = snapshot.getBalle();
		Ball ball = snapshot.getBall();
		Goal opponentsGoal = snapshot.getOpponentsGoal();


		if (finished) {
			return;
		}

		if (ourRobot.possessesBall(snapshot.getBall())
				&& !(ourRobot.getPosition() == null)) {


			if (!arrived_at_ball) {
				goto_executor.stop(controller);
				arrived_at_ball = true;
				LOG.info("Arrived at ball, beginning to dribble");
			}

			if (dribble_executor.hasKicked()) {

				if (haveScored(opponentsGoal, ball)) {
					LOG.info("GOAL!");
					controller.stop();
					dribble_executor.stop(controller);
					LOG.info("STOP");
					finished = true;
					return;
				}

				else {
					dribble_executor.setKicked(false);
					dribble_executor.step(controller, snapshot);
					return;
				}

			}

			else {
				dribble_executor.step(controller, snapshot);
				return;
			}
		} else {
			goto_executor.step(controller, snapshot);
			return;
		}

	}

	@FactoryMethod(designator = "M3LocandShoot", parameterNames = {})
	public static final M3LocateandShoot factory() {
		return new M3LocateandShoot();
	}

}