package balle.world.filter;


import java.util.Random;

import lejos.nxt.SensorPort;
import lejos.nxt.UltrasonicSensor;
import lejos.robotics.RegulatedMotor;
import lejos.robotics.navigation.DifferentialPilot;
import lejos.util.KalmanFilter;
import lejos.util.Matrix;
import lejos.util.PilotProps;

public class KalmanTest {
	// Tyre diameter and distance between wheels

	public static void main(String[] args) throws Exception {
		PilotProps pp = new PilotProps();
		pp.loadPersistentValues();
		float wheelDiameter = Float.parseFloat(pp.getProperty(
				PilotProps.KEY_WHEELDIAMETER, "5.6"));
		float trackWidth = Float.parseFloat(pp.getProperty(
				PilotProps.KEY_TRACKWIDTH, "16.0"));
		RegulatedMotor leftMotor = PilotProps.getMotor(pp.getProperty(
				PilotProps.KEY_LEFTMOTOR, "B"));
		RegulatedMotor rightMotor = PilotProps.getMotor(pp.getProperty(
				PilotProps.KEY_RIGHTMOTOR, "C"));
		boolean reverse = Boolean.parseBoolean(pp.getProperty(
				PilotProps.KEY_REVERSE, "false"));

		UltrasonicSensor sonic = new UltrasonicSensor(SensorPort.S1);
		Random rand = new Random();
		Matrix a = new Matrix(new double[][] { { 1 } }); // Position is only
															// changed by
															// control
		Matrix b = new Matrix(new double[][] { { 1 } }); // Velocity is in
															// cm/sec
		Matrix c = new Matrix(new double[][] { { 1 } }); // Measurement is in cm
		Matrix q = new Matrix(new double[][] { { 4 } }); // Ultrasonic sensor
															// noise factor
		Matrix r = new Matrix(new double[][] { { 9 } }); // Movement noise
															// factor
		Matrix state = new Matrix(new double[][] { { 100 } }); // Start one
																// meter from
																// the wall
		Matrix covariance = new Matrix(new double[][] { { 100 } }); // Big error
		Matrix control = new Matrix(1, 1);
		Matrix measurement = new Matrix(1, 1);

		sonic.continuous();

		// RConsole.openBluetooth(0);
		// System.setOut(new PrintStream(RConsole.openOutputStream()));

		// Create the pilot
		DifferentialPilot pilot = new DifferentialPilot(wheelDiameter,
				trackWidth, leftMotor, rightMotor, reverse);

		// Create the filter
		KalmanFilter filter = new KalmanFilter(a, b, c, q, r);

		// Set the initial state
		filter.setState(state, covariance);

		// Loop 100 times setting velocity, reading the range and updating the
		// filter
		for (int i = 0; i < 100; i++) {
			// Generate a random velocity -20 to +20cm/sec
			double velocity = (rand.nextInt(41) - 20);

			// Adjust velocity so we keep in range
			double position = filter.getMean().get(0, 0);
			if (velocity < 0 && position < 20)
				velocity = -velocity;
			if (velocity > 0 && position > 220)
				velocity = -velocity;

			control.set(0, 0, velocity);
			System.out.println("Velocity: " + (int) velocity);

			// Move the robot
			pilot.setTravelSpeed((float) Math.abs(velocity));
			if (velocity > 0)
				pilot.backward();
			else
				pilot.forward();
			Thread.sleep(1000);
			pilot.stop();

			// Take a reading
			float range = sonic.getRange();
			System.out.println("Range: " + (int) range);
			measurement.set(0, 0, (double) range);

			// Update the state
			try {
				filter.update(control, measurement);
			} catch (Exception e) {
				System.out.println("Exception: " + e.getClass() + ":"
						+ e.getMessage());
			}

			// Print the results
			System.out.print("Mean:");
			filter.getMean().print(System.out);
			;
			System.out.print("Covariance:");
			filter.getCovariance().print(System.out);
		}
	}
	
}
