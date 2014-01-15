package balle.strategy;

import java.awt.Color;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import balle.controller.Controller;
import balle.main.drawable.Drawable;
import balle.main.drawable.Label;
import balle.misc.Globals;
import balle.strategy.basic.GoToGoal;
import balle.strategy.basic.Initial;
import balle.strategy.planner.AbstractPlanner;
import balle.strategy.planner.GoToObjectSafeProportional;
import balle.world.Coord;
import balle.world.Snapshot;
import balle.world.Velocity;
import balle.world.objects.Ball;
import balle.world.objects.Goal;
import balle.world.objects.Robot;

	
public class Game2 extends AbstractPlanner {

    private static final Logger LOG = Logger.getLogger(Game2.class);

	protected final Strategy defendAndIntercept;
	protected final Strategy goDirectToGoal;
	protected final GoToObjectSafeProportional goDribbleAndShoot;
	protected Strategy initialStrategy;

    protected boolean initial;

    private String currentStrategy = null;

	@FactoryMethod(designator = "Game2", parameterNames = { "init" })
	public static Game2 gameFactoryTesting2(boolean init) {
		Game2 game = new Game2(init);
		return game;
	}

    public String getCurrentStrategy() {
        return currentStrategy;
    }

    public Strategy getInitialStrategy() {
        return initialStrategy;
    }

    public void setInitialStrategy(Strategy initialStrategy) {
        this.initialStrategy = initialStrategy;
    }

    @Override
    public ArrayList<Drawable> getDrawables() {
        ArrayList<Drawable> drawables = super.getDrawables();
        if (currentStrategy != null)
            drawables.add(new Label(currentStrategy, new Coord(
                    Globals.PITCH_WIDTH - 0.5, Globals.PITCH_HEIGHT + 0.2),
                    Color.WHITE));
        return drawables;
    }

    public void setCurrentStrategy(String currentStrategy) {
        this.currentStrategy = currentStrategy;
    }

    public Game2() {
		initialStrategy = new Initial();
		goDribbleAndShoot = new GoToObjectSafeProportional();
		defendAndIntercept = new DefendAndIntercept();
		goDirectToGoal = new GoToGoal();

		initial = false;
    }

    public boolean isInitial(Snapshot snapshot) {
        if (initial == false)
            return false;

        LOG.info("isInitial(): "+initial);
        
        Ball ball = snapshot.getBall();

        Robot ourRobot = snapshot.getBalle();
        Robot oppRobot = snapshot.getOpponent();
        
        // If we have the ball, turn off initial strategy
		if (ourRobot.possessesBall(ball))
        {
            LOG.info("We possess the ball. Turning off initial strategy");
            setInitial(false);
        }
        // else If ball has moved 5 cm, turn off initial strategy
        else if (oppRobot.possessesBall(ball)) {
            LOG.info("Opp has ball. Turning off initial strategy");
            setInitial(false);
		} else if (Initial.finished) {
			setInitial(false);
        }
        
		
        return initial;

    }

    public void setInitial(boolean initial) {
        this.initial = initial;
    }

    public Game2(boolean startWithInitial) {
        this();
        initial = startWithInitial;
        LOG.info("Starting game strategy with initial strategy turned on");
    }

    @Override
    public void stop(Controller controller) {

		goDribbleAndShoot.stop(controller);
		initialStrategy.stop(controller);

	}

    @Override
    public void onStep(Controller controller, Snapshot snapshot)
            throws ConfusedException {

        if (isInitial(snapshot)) {
            setCurrentStrategy(initialStrategy.getClass().getName());

            initialStrategy.step(controller, snapshot);

            addDrawables(initialStrategy.getDrawables());
            return;
        }

		Strategy strategy = getStrategy(snapshot);
        LOG.debug("Selected strategy: " + strategy.getClass().getName());
		setCurrentStrategy(strategy.getClass().getName());
        try {
            strategy.step(controller, snapshot);
        } catch (ConfusedException e) {
			LOG.error("Game catch block in: " + getCurrentStrategy(), e);
			System.out.println("Error in: " + getCurrentStrategy());
			goDribbleAndShoot.step(controller, snapshot);
        }

		addDrawables(strategy.getDrawables());
    }

	private Strategy getStrategy(Snapshot snapshot) {
		Robot ourRobot = snapshot.getBalle();
		Robot oppRobot = snapshot.getOpponent();
		Goal ourGoal = snapshot.getOwnGoal();
		Goal oppGoal = snapshot.getOpponentsGoal();
		Ball ball = snapshot.getBall();

		boolean attack = weShouldAttack(ourRobot, oppRobot, ball, ourGoal, oppGoal);

		if (attack) {
			return goDribbleAndShoot;
		} else {
			double ourDistanceToGoal = ourRobot.getPosition().dist(ourGoal.getPosition());
			double oppDistanceToGoal = oppRobot.getPosition().dist(ourGoal.getPosition());

			if (oppDistanceToGoal < ourDistanceToGoal) {
				return goDirectToGoal;
			} else {
				Velocity oppVelocity = oppRobot.getVelocity();
				// If our opponent crashes and is not moving, we should go to
				// the ball.
				// Otherwise, we should retreat to our goal line and intercept
				// when possible.
				if (oppVelocity.abs() > 0) {
					if (!oppRobot.isFacingGoal(ourGoal)) {
						return goDirectToGoal;
					} else {
						return defendAndIntercept;
					}
				} else {
					return goDribbleAndShoot;
				}

			}
		}

	}

	/*
	 * We attack if we have the ball OR we are closer to the ball than the
	 * opponent
	 */

	private boolean weShouldAttack(Robot ourRobot, Robot oppRobot, Ball ball, Goal ourGoal, Goal oppGoal) {

		if (oppRobot.possessesBall(ball)) {
			return false;
		}

		double ourDistanceToGoal = ourRobot.getPosition().dist(ourGoal.getPosition());
		double oppDistanceToGoal = Double.POSITIVE_INFINITY;
		if (oppRobot != null && oppRobot.getPosition() != null) {
			// Opponent is on the pitch
			oppDistanceToGoal = oppRobot.getPosition().dist(ourGoal.getPosition());
		}
		double ballDistanceToGoal = ball.getPosition().dist(ourGoal.getPosition());
		
		// Opponent is closer to our goal and ball is between them and Goal
		if (oppDistanceToGoal < ourDistanceToGoal && ballDistanceToGoal <= oppDistanceToGoal) {
			return false;
		}

		return true;

	}


}
