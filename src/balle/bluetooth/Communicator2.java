package balle.bluetooth;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import lejos.pc.comm.NXTComm;
import lejos.pc.comm.NXTCommException;
import lejos.pc.comm.NXTCommFactory;
import lejos.pc.comm.NXTInfo;

public class Communicator2 {

	private final static String NAME = "group6";
	private final static String MAC = "00:16:53:08:A0:E6";

	final NXTInfo nxtInfo = new NXTInfo(NXTCommFactory.BLUETOOTH, NAME, MAC);
	NXTComm nxtComm = null;

    private boolean             connected = false;
    DataOutputStream            dos;
	OutputStream os;
    int                         m         = 5;
    int                         m2        = 0;

    /**
     * Constructor method, initializes connections.
     */
	public Communicator2() {
		nxtComm = connect();
		os = nxtComm.getOutputStream();
		dos = new DataOutputStream(os);
    }

    /**
     * Connect method, initializes bluetooth connection to the NXT.
     * 
     * @return The connection object.
     */
	private NXTComm connect() {

		try {
			nxtComm = NXTCommFactory.createNXTComm(NXTCommFactory.BLUETOOTH);
		} catch (NXTCommException e) {
			System.err.println("Failed to load Bluetooth driver");
			System.exit(1);
		}

		try {
			connected = nxtComm.open(nxtInfo);
		} catch (NXTCommException e) {
			System.err.println("Failed to connect to device");
			System.exit(1);
		}

		System.out.println("Connection to " + NAME + " initialised\n");
		return nxtComm;
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
        try {
            // System.err.println("Sending: " + Integer.toHexString(message));
            dos.writeInt(message);
            dos.flush();
            return true;
        } catch (IOException e) {
            System.err.println("Sending failed, IOException: ");
            System.err.println(e.getMessage());
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
        try {
            dos.writeInt(-1);
            dos.flush();
            dos.close();
			nxtComm.close();
            connected = false;
        } catch (IOException e) {
            System.err.println(e);
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
