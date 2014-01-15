package balle.bluetooth;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import lejos.pc.comm.NXTCommFactory;
import lejos.pc.comm.NXTConnector;

public class Communicator {

	private final static String NAME = "group6";
	private final static String MAC = "00:16:53:08:A0:E6";

	private boolean connected = false;
	NXTConnector conn;
	DataOutputStream dos;
	int m = 5;
	int m2 = 0;

	/**
	 * Constructor method, initializes connections.
	 */
	public Communicator() {
		this(false);

	}

	/**
	 * Constructor method to run in different thread.
	 * 
	 * @param threaded
	 *            If true, runs in different thread.
	 */
	public Communicator(boolean threaded) {
		/*
		if (threaded) {
			(new Thread() {
				public void run() {
					conn = connect();
					if (connected) {
						OutputStream os = conn.getOutputStream();
						dos = new DataOutputStream(os);
					}
				}
			}).start();
		} else {
			conn = connect();
			if (connected) {
				OutputStream os = conn.getOutputStream();
				dos = new DataOutputStream(os);
			}
		}*/
	}

	/**
	 * Initializes connections.
	 */
	public void init() {
		conn = connect();
		if (connected) {
			OutputStream os = conn.getOutputStream();
			dos = new DataOutputStream(os);
		}
	}

	/**
	 * Connect method, initializes bluetooth connection to the NXT.
	 * 
	 * @return The connection object.
	 */
	private NXTConnector connect() {
		NXTConnector conn = new NXTConnector();
		conn.setDebug(true);
		System.out.println("Connecting to " + NAME + "...");
		connected = conn.connectTo(NAME, MAC, NXTCommFactory.BLUETOOTH);

		if (!connected) {
			System.err.println("Failed to connect to any NXT");
			// System.exit(1);
			return null;
		} else {
			System.out.println("Connection initialised\n");
			return conn;
		}
	}

	/**
	 * The public sender method. Sends commands.
	 * 
	 * @param message
	 *            The command to send
	 * @see balle.brick.BotCommunication balle.brick.BotCommunication for
	 *      meanings
	 */
	public boolean send(Integer message) {
		if (connected && dos != null) {
			try {
				// System.err.println("Sending: " +
				// Integer.toHexString(message));
				dos.writeInt(message);
				dos.flush();
				return true;
			} catch (IOException e) {
				System.err.println("Sending failed, IOException: ");
				System.err.println(e.getMessage());
				return false;
			}
		} else {
			System.err.println("Sending failed, not connected.");
			return false;
		}
	}

	/**
	 * Closes the data connection, required if the robot is still waiting for a
	 * command.
	 * 
	 * @param dataIn
	 *            The DataInputStream from the bluetooth connection.
	 * @param dataOut
	 *            The DataOutputStream from the bluetooth connection.
	 */
	public void close() {
		if (connected) {
			try {
				// This was making robot to rotate for a really long time:
				// dos.writeInt(-1);
				dos.flush();
				dos.close();
				conn.close();
				connected = false;
			} catch (IOException e) {
				System.err.println(e);
			}
		} else {
			System.out.println("Not connected.");
		}
	}

	/**
	 * Checks whether we are okay to send commands. In other words checks if the
	 * connector has initialised and is working fine.
	 * 
	 * @return true if connector is currently connected
	 */
	public boolean isConnected() {
		return connected;
	}
}