package balle.world.filter;

import java.util.Random;

import Jama.Matrix;
import balle.world.Coord;
import balle.world.Snapshot;
import balle.world.Velocity;
import balle.world.objects.Ball;
import balle.world.objects.MovingPoint;
import balle.world.objects.Robot;

public class KalmanFilter implements Filter {

	// the state of the robot; a vector [x y vx vy]
	// vx and vy are the components of the velocity
	public Matrix[] X = new Matrix[3];

	// the predicted vector state
	static Matrix[] Xk1 = new Matrix[3];

	// initial uncertainty matrix, which we will then update => error
	// covariance matrix
	static Matrix p = new Matrix(new double[][] { { 5 * 5.0, 0, 0, 0 },
			{ 0, 5 * 5.0, 0, 0 }, { 0, 0, 1.0, 0 }, { 0, 0, 0, 1.0 } });
	static Matrix[] P = { p, p, p };

	// predicted estimated covariance matrix - it predicts how much error there
	// is; it stores the newest estimate of the average error for each part of
	// the state
	static Matrix Pk1[] = new Matrix[3];

	// stores the innovation (measurement residual)
	static Matrix Vk[] = new Matrix[3];
	// innovation covariance
	static Matrix Sk1[] = new Matrix[3];
	// optimal kalman gain
	static Matrix Gain[] = new Matrix[3];

	// time interval
	double t[] = { 0, 0, 0 };

	double phi[] = { Math.toRadians(-180), Math.toRadians(-180),
			Math.toRadians(-180) };
	double w[] = { Math.toRadians(20.0f), Math.toRadians(20.0f),
			Math.toRadians(20.0f) };

	/* TODO: set these values appropriately */
	// time interval
	static double deltaT = 0.1;

	// previously set to 0.3 and 15.0
	static double rangeSensorNoise = 0.1f;
	static double bearingSensorNoise = Math.toRadians(10.0f);

	// these values describe how much noise we believe there is in the model
	// one value for coordinates, another one for velocity
	// previously set to 0.03 and 0.20
	static double transitionModelSTDxy = 0.0f;
	static double transitionModelSTDvxy = 0.0f;

	// the Jacobian of the *prediction model
	static Matrix f = new Matrix(new double[][] { { 1, 0, deltaT, 0 },
			{ 0, 1, 0, deltaT }, { 0, 0, 1, 0 }, { 0, 0, 0, 1 } });
	static Matrix F[] = { f, f, f };

	// B is the input gain matrix and U is the control input vector
	static Matrix B[] = new Matrix[3];
	static Matrix U[] = new Matrix[3];

	// process noise
	static Matrix q = new Matrix(new double[][] {
			{ transitionModelSTDxy * transitionModelSTDxy, 0, 0, 0 },
			{ 0, transitionModelSTDxy * transitionModelSTDxy, 0, 0 },
			{ 0, 0, transitionModelSTDvxy * transitionModelSTDvxy, 0 },
			{ 0, 0, 0, transitionModelSTDvxy * transitionModelSTDvxy } });
	static Matrix Q[] = { q, q, q };

	// the Jacobian of the *measurement model
	static Matrix H[] = new Matrix[3];;

	// the measurement error matrix
	static Matrix r = new Matrix(new double[][] {
			{ rangeSensorNoise * rangeSensorNoise, 0 },
			{ 0, bearingSensorNoise * bearingSensorNoise } });
	static Matrix R[] = { r, r, r };

	Random sensorNoise = new Random();

	public Snapshot filter(Snapshot s) {

		MovingPoint objects[] = { s.getBalle(), s.getOpponent(), s.getBall() };

		for (int object = 0; object < 3; object++) {

			if (objects[object] == null)
				continue;
			if (objects[object].getPosition() == null)
				continue;

			// Get current state
			double x = objects[object].getPosition().x;
			double y = objects[object].getPosition().y;
			double vx = objects[object].getVelocity().x;
			double vy = objects[object].getVelocity().y;

			X[object] = new Matrix(new double[][] { { x }, { y }, { vx },
					{ vy } });
			B[object] = new Matrix(new double[][] { { vx, vy, 0, 0 } })
					.transpose();
			U[object] = new Matrix(new double[][] { { deltaT } });

			double sqxy = (x * x + y * y);
			double term00 = -y / sqxy;
			double term01 = 1 / (x * (1 + (y / x * y / x)));
			double term10 = x / Math.sqrt(sqxy);
			double term11 = y / Math.sqrt(sqxy);

			// the Jacobian of the *measurement model
			H[object] = new Matrix(new double[][] { { term00, term01, 0, 0 },
					{ term10, term11, 0, 0 } });

			for (int i = 0; i < 10; i++) {
				// predict
				Xk1[object] = F[object].times(X[object]).plus(
						B[object].times(U[object]));
				Pk1[object] = F[object].times(P[object])
						.times(F[object].transpose()).plus(Q[object]);

				// get observations
				double realBearing = Math.atan2(y, x);
				double obsBearing = realBearing + bearingSensorNoise
						* sensorNoise.nextGaussian();

				double realRange = Math.sqrt(sqxy);
				double obsRange = Math.max(0.0, realRange + rangeSensorNoise
						* sensorNoise.nextGaussian());

				Matrix z = new Matrix(new double[][] { { obsRange },
						{ obsBearing } });

				// Observation
				Vk[object] = z.minus(H[object].times(Xk1[object]));

				// Update
				// innovation covariance
				Sk1[object] = H[object].times(Pk1[object])
						.times(H[object].transpose()).plus(R[object]);

				// compute kalman gain
				Gain[object] = Pk1[object].times(H[object].transpose()).times(
						Sk1[object].inverse());

				// update state estimate via Z
				X[object] = Xk1[object].plus(Gain[object].times(Vk[object]));

				// update error covariance
				Matrix I = Matrix.identity(Pk1[object].getRowDimension(),
						Pk1[object].getColumnDimension());
				P[object] = (I.minus(Gain[object].times(H[object])))
						.times(Pk1[object]);

				// update coordinates
				x += vx + deltaT
						* (Math.cos(phi[object]) - Math.sin(phi[object]));
				y += vy + deltaT
						* (Math.cos(phi[object]) + Math.sin(phi[object]));
				phi[object] += w[object] * deltaT;

				vx += 1.0 * deltaT * Math.cos(t[object]);
				vy += 1.0 * deltaT * Math.cos(t[object]);
				w[object] -= 0.1 * deltaT * Math.sin(t[object]);

				t[object] += deltaT;
			}
		}

		// Robot updatedRobot = new Robot(new Coord(x, y), new Velocity(vx, vy,
		// deltaT), robot.getAngularVelocity(), robot.getOrientation());
		Robot updatedRobot;
		Robot updatedOpponent;
		Ball updatedBall;

		// Balle:
		if (s.getBalle() == null) {
			updatedRobot = s.getBalle();
		} else {
			updatedRobot = new Robot(new Coord(X[0].get(0, 0), X[0].get(1, 0)),
					new Velocity(X[0].get(2, 0), X[0].get(3, 0), deltaT), s
							.getBalle().getAngularVelocity(), s.getBalle()
							.getOrientation());
		}

		// Opponent:
		if (s.getOpponent() == null) {
			updatedOpponent = s.getOpponent();
		} else {
			updatedOpponent = new Robot(new Coord(X[1].get(0, 0),
					X[1].get(1, 0)), new Velocity(X[1].get(2, 0),
					X[1].get(3, 0), deltaT), s.getOpponent()
					.getAngularVelocity(), s.getOpponent().getOrientation());
		}

		// Ball:
		updatedBall = new Ball(new Coord(X[2].get(0, 0), X[2].get(1, 0)),
				new Velocity(X[2].get(2, 0), X[2].get(3, 0), deltaT));
		Snapshot updatedSnapshot = new Snapshot(s.getWorld(), updatedOpponent,
				updatedRobot, updatedBall, s.getTimestamp(),
				s.getControllerHistory());

		// System.out.println("this is X updated" + X.get(0, 0) + " "
		// + "this is updated in robot" + updatedRobot.getPosition().x
		// + "this is updated in x " + x);
		return updatedSnapshot;
	}

}
