package balle.world.objects;

import jama.Matrix;
import balle.misc.Globals;
import balle.world.Coord;
import balle.world.Velocity;
import balle.world.filter.JKalman;

public class Ball extends CircularObject implements FieldObject {

	// private final BallPredictor predictor;
	JKalman kalman;
	Matrix s = new Matrix(4, 1);
	Matrix c = new Matrix(4, 1);
	Matrix m = new Matrix(2, 1);
	double[][] tr;

	public Ball(Coord position, Velocity velocity) {
		super(position, velocity, Globals.BALL_RADIUS);
		try {
			kalman = new JKalman(4, 2);

			double x = position.x;
			double y = position.y;

			m.set(0, 0, x);
			m.set(1, 0, y);

			double[][] tr = { { 1, 0, 1, 0 }, { 0, 1, 0, 1 }, { 0, 0, 1, 0 },
					{ 0, 0, 0, 1 } };

			kalman.setProcess_noise(1e-1);
			kalman.setMeasurement_noise(1e-100);

			kalman.setTransition_matrix(new Matrix(tr));
			kalman.setError_cov_post(kalman.getError_cov_post().identity());


		} catch (Exception ex) {
			System.out.println(ex.getMessage());
		}
		// this.predictor = predictor;
	}

	// public Ball(Coord position, Velocity velocity, BallPredictor predictor) {
	// super(position, velocity, Globals.BALL_RADIUS);
	//
	// this.predictor = predictor;
	// }

	// public Coord estimatePosition(double time) {
	// return predictor.getPosition(time);
	// }

	public void update(Coord newPosition, double timeDelta) {

		assert timeDelta > 0;

		s = kalman.Predict();

		// double dx = this.getVelocity().x;
		// double dy = this.getVelocity().y;

		// m.set(0, 0, (m.get(0, 0) + dx * timeDelta));
		// m.set(1, 0, (m.get(1, 0) + dy * timeDelta));

		if (position != null) {
			if (newPosition != null) {

				m.set(0, 0, newPosition.getX());
				m.set(1, 0, newPosition.getY());

				c = kalman.Correct(m);

				this.setPosition(new Coord(c.get(0, 0), c.get(1, 0)));
				this.setVelocity(new Velocity(s.get(2, 0) / timeDelta, s.get(3,
						0) / timeDelta, timeDelta));
			}
			else {
				reset(newPosition, timeDelta);
			}
			
		}
 else {
			this.setPosition(new Coord(s.get(0, 0), s.get(1, 0)));
			this.setVelocity(new Velocity(s.get(2, 0) / timeDelta, s.get(3, 0)
					/ timeDelta, timeDelta));
		}


	}

	public void reset(Coord newPosition, double timeDelta) {
		velocity = new Velocity(0, 0, timeDelta);
		position = newPosition;

		if (newPosition != null) {
			double array[][] = { { position.x }, { position.y }, { 0.0 },
					{ 0.0 } };
			Matrix state_pre = new Matrix(array);

			kalman.setState_pre(state_pre);

			kalman.setMeasurement_matrix(Matrix.identity(4, 4));
		}

	}

}
