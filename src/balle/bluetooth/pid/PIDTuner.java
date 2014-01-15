package balle.bluetooth.pid;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

import lejos.pc.comm.NXTCommException;
import lejos.pc.comm.NXTCommFactory;
import lejos.pc.comm.NXTConnector;
import lejos.util.PIDController;
import balle.brick.pid.PIDConstants;

public class PIDTuner {

	public static int getKey(String name) {
		if (name != null) {
			name = name.toLowerCase().trim();
		}

		if ("setpoint".equals(name)) {
			return PIDController.PID_SETPOINT;
		}
		if ("deadband".equals(name)) {
			return PIDController.PID_DEADBAND;
		}
		if ("kp".equals(name)) {
			return PIDController.PID_KP;
		}
		if ("ki".equals(name)) {
			return PIDController.PID_KI;
		}
		if ("kd".equals(name)) {
			return PIDController.PID_KD;
		}
		if ("limithigh".equals(name)) {
			return PIDController.PID_LIMITHIGH;
		}
		if ("limitlow".equals(name)) {
			return PIDController.PID_LIMITLOW;
		}
		if ("integrallimithigh".equals(name)) {
			return PIDController.PID_I_LIMITHIGH;
		}
		if ("integrallimitlow".equals(name)) {
			return PIDController.PID_I_LIMITLOW;
		}
		if ("integral".equals(name)) {
			return PIDController.PID_I;
		}
		if ("rawoutputmv".equals(name)) {
			return PIDConstants.RAW_OUTPUT_MV;
		}
		if ("lefttacho".equals(name)) {
			return PIDConstants.LEFT_TACHO;
		}
		if ("righttacho".equals(name)) {
			return PIDConstants.RIGHT_TACHO;
		}
		if ("leftspeed".equals(name)) {
			return PIDConstants.LEFT_SPEED;
		}
		if ("rightspeed".equals(name)) {
			return PIDConstants.RIGHT_SPEED;
		}
		if ("maxtravelspeed".equals(name)) {
			return PIDConstants.MAX_TRAVEL_SPEED;
		}
		if ("maxrotatespeed".equals(name)) {
			return PIDConstants.MAX_ROTATE_SPEED;
		}
		if ("interval".equals(name)) {
			return PIDConstants.INTERVAL;
		}

		return -1;
	}

	public static void main(String argv[]) throws NXTCommException {
		String MAC = "00:16:53:08:A0:E6";
		String NAME = "group6";

		NXTConnector conn = new NXTConnector();
		conn.setDebug(true);

		boolean connected = conn.connectTo(NAME, MAC, NXTCommFactory.BLUETOOTH);

		if (!connected) {
			System.err.println("Failed to connect to any NXT");
			System.exit(1);
		}

		System.out.println("Connected!");

		DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
		DataInputStream dis = new DataInputStream(conn.getInputStream());

		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		try {
			while (true) {
				String line = reader.readLine().toLowerCase();

				if ("exit".equals(line)) {
					dos.writeInt(PIDConstants.SHUTDOWN_COMMAND);
					dos.flush();
					break;
				}
				if ("start".equals(line)) {
					dos.writeInt(PIDConstants.START_COMMAND);
					dos.flush();
				}
				if ("stop".equals(line)) {
					dos.writeInt(PIDConstants.STOP_COMMAND);
					dos.flush();
				}
				if ("float".equals(line)) {
					dos.writeInt(PIDConstants.FLOAT_COMMAND);
					dos.flush();
				}
				if (line.startsWith("read")) {
					int key = getKey(line.substring(4));

					if (key != -1) {
						dos.writeInt(PIDConstants.READ_COMMAND);
						dos.flush();
						dos.writeInt(key);
						dos.flush();
						System.out.println("received: " + dis.readFloat());
					} else {
						System.out.println("wrong key!");
					}
				}
				if (line.startsWith("write")) {
					StringTokenizer tokenizer = new StringTokenizer(line.substring(5));

					if (tokenizer.countTokens() == 2) {
						int key = getKey(tokenizer.nextToken());
						if (key != -1) {
							try {
								float value = Float.parseFloat(tokenizer.nextToken());
								dos.writeInt(PIDConstants.WRITE_COMMAND);
								dos.flush();
								dos.writeInt(key);
								dos.flush();
								dos.writeFloat(value);
								dos.flush();
								System.out.println("received: " + dis.readFloat());
							} catch (NumberFormatException e) {
								System.out.println("wrong number format!");
							}
						} else {
							System.out.println("wrong key!");
						}
					} else {
						System.out.println("wrong format!");
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			dis.close();
			dos.close();
			conn.close();
		} catch (IOException ioe) {
			System.out.println("IOException closing connection");
		}
	}
}