package balle.brick;

import java.io.DataInputStream;
import java.io.IOException;

import lejos.nxt.Button;
import lejos.nxt.LCD;
import lejos.nxt.SensorPort;
import lejos.nxt.Sound;
import lejos.nxt.TouchSensor;
import lejos.nxt.comm.BTConnection;
import lejos.nxt.comm.Bluetooth;
import lejos.util.PIDController;
import balle.bluetooth.messages.AbstractMessage;
import balle.bluetooth.messages.MessageDecoder;
import balle.bluetooth.messages.MessageKick;
import balle.bluetooth.messages.MessageMove;
import balle.bluetooth.messages.MessageRotate;
import balle.bluetooth.messages.MessageStop;

class ListenerThreadMkII extends Thread {
    DataInputStream input;
    boolean         shouldStop;
    int             command;
    boolean         commandConsumed;

    ListenerThreadMkII(DataInputStream input) {
        this.input = input;
        this.shouldStop = false;
        this.commandConsumed = true;
    }

    @Override
    public void run() {

        while (!shouldStop) {
            try {
                int command = input.readInt();
                setCommand(command);
            } catch (IOException e) {
                shouldStop = true;
            }
        }
    }

    private synchronized void setCommand(int command) {
        this.command = command;
        commandConsumed = false;
    }

    public synchronized int getCommand() {
        commandConsumed = true;
        return command;
    }

    public synchronized boolean available() {
        return !commandConsumed;
    }

    public void cancel() {
        shouldStop = true;
    }

}

/**
 * Create a connection to Roboto from the computer. execute commands send from
 * the computer test out movements of Roboto.
 * 
 * @author s0815695
 * @author s1027418 (PID)
 */
public class RobotoMkII {

	private static BrickController controller;
	private static MessageDecoder decoder;
	
	private static boolean waiting = false;
	
	/*
	 * PID control
	 */
	private static PIDController pidController;
	
	private static boolean forward = false;
	private static double start_time;
	
	private static int PID_SETPOINT = 600;
	private static int PID_DEADBAND = 1;
	private static float PID_KP = 5f;
	private static float PID_KI = 0.0f;
	private static float PID_KD = 0.0f;
	private static float PID_LIMITHIGH = 450.0f;
	private static float PID_LIMITLOW = -PID_LIMITHIGH;
	private static float PID_I_LIMITHIGH = 10.0f;
	private static float PID_I_LIMITLOW = 10.0f;
	private static int PID_DT = 15;

    /**
     * Processes the decoded message and issues correct commands to controller
     * 
     * @param decodedMessage
     *            the decoded message
     * @param controller
     * @return true, if successful
     */
    public static boolean processMessage(AbstractMessage decodedMessage,
            BrickController controller) {
        String name = decodedMessage.getName();

        if (name.equals(MessageKick.NAME)) {
            MessageKick messageKick = (MessageKick) decodedMessage;
            if (messageKick.isPenalty()) {
                controller.penaltyKick();
            } else {
                controller.kick();
            }
        } else if (name.equals(MessageMove.NAME)) {
        	forward = true;
        	start_time = System.currentTimeMillis();
            MessageMove messageMove = (MessageMove) decodedMessage;
            int left = messageMove.getLeftWheelSpeed();
            int right = messageMove.getRightWheelSpeed();
            setPIDsetpoint(Math.max(left, right)); //TODO should we implement a seperate PID controller for each wheel?
            controller.setWheelSpeeds(left,right);
        } else if (name.equals(MessageStop.NAME)) {
        	forward = false;
        	MessageStop messageStop = (MessageStop) decodedMessage;
            if (messageStop.floatWheels())
                controller.floatWheels();
            else
                controller.stop();
    		controller.resetLeftTacho();
    		controller.resetRightTacho();
        } else if (name.equals(MessageRotate.NAME)) {
        	forward = false;
        	MessageRotate messageRotate = (MessageRotate) decodedMessage;
            controller.rotate(messageRotate.getAngle(),
                    messageRotate.getSpeed());
    		controller.resetLeftTacho();
    		controller.resetRightTacho();
        } else {
        	forward = false;
        	controller.floatWheels();
        	return false;
        }
        return true;
    }

    /**
     * Main program
     * 
     * @param args
     */
    public static void main(String[] args) {

        TouchSensor touchRight = new TouchSensor(SensorPort.S2);
        TouchSensor touchLeft = new TouchSensor(SensorPort.S1);

        TouchSensor touchBackRight = new TouchSensor(SensorPort.S4);
        TouchSensor touchBackLeft = new TouchSensor(SensorPort.S3);

        while (true) {
            // Enter button click will halt the program
			if (Button.ENTER.isDown() || Button.ESCAPE.isDown())
                break;

            drawMessage("Connecting...");
            Sound.twoBeeps();

            BTConnection connection = Bluetooth.waitForConnection();

            drawMessage("Connected");
            Sound.beep();

            DataInputStream input = connection.openDataInputStream();
            ListenerThreadMkII listener = new ListenerThreadMkII(input);

            controller = new BrickController();
            decoder = new MessageDecoder();
            
            pidController = new PIDController(PID_SETPOINT, PID_DT);
            setPIDparameters();
    		controller.resetLeftTacho();
    		controller.resetRightTacho();
    		
            listener.start();

            while (true) {
                // Enter button click will halt the program
				if (Button.ENTER.isDown()) {
                    controller.stop();
                    listener.cancel();
                    break;
                }
				if (Button.ESCAPE.isDown()) {
					controller.stop();
					listener.cancel();
					break;
                }

				if (waiting) {
					continue;
				}
				
				// we're moving forward, do pid control!
				if (forward) {
					double current_time = System.currentTimeMillis();
					double running_time = (current_time - start_time) / 1000.0;

					double left_tacho = controller.getLeftTacho();
					double left_actual = left_tacho / running_time;
					int left_mv = pidController.doPID((int) left_actual);

					double right_tacho = controller.getRightTacho();
					double right_actual = right_tacho / running_time;
					int right_mv = pidController.doPID((int) right_actual);

					controller.forward((PID_SETPOINT + left_mv), (PID_SETPOINT + right_mv));
				}

                try {
                    // Check for sensors when idle
					if (touchLeft.isPressed() || touchRight.isPressed()) {
						waiting = true;
						controller.backward(controller.getMaximumWheelSpeed());
						drawMessage("Obstacle in front!");
						Thread.sleep(150);
						controller.stop();
						waiting = false;
                    }

                    // Check for back sensors as well
                    if (touchBackLeft.isPressed() || touchBackRight.isPressed()) {
						waiting = true;
						controller.forward(controller.getMaximumWheelSpeed());
						drawMessage("Obstacle behind!");
						Thread.sleep(150);
						controller.stop();
						waiting = false;
                    }

                    if (!listener.available())
                        continue;

                    int hashedMessage = listener.getCommand();
                    AbstractMessage message = decoder.decodeMessage(hashedMessage);
                    if (message == null) {
                        drawMessage("Could not decode: " + hashedMessage);
						continue;
                    }
                    String name = message.getName();
                    drawMessage(name);

                    boolean successful = processMessage(message, controller);
                    if (!successful) {
                        drawMessage("Unknown message received: "+ hashedMessage);
						continue;
                    }

				} catch (Exception e) {
					drawMessage("Error in MainLoop: " + e.getMessage());
                }
            }

            connection.close();
        }
    }

    private static void drawMessage(String message) {
        LCD.clear();
        LCD.drawString(message, 0, 0);
        LCD.refresh();
    }
    
    public static void setPIDparameters() {
		pidController.setPIDParam(PIDController.PID_SETPOINT, PID_SETPOINT);
		pidController.setPIDParam(PIDController.PID_DEADBAND, PID_DEADBAND);
		pidController.setPIDParam(PIDController.PID_KP, PID_KP);
		pidController.setPIDParam(PIDController.PID_KI, PID_KI);
		pidController.setPIDParam(PIDController.PID_KD, PID_KD);
		pidController.setPIDParam(PIDController.PID_LIMITHIGH, PID_LIMITHIGH);
		pidController.setPIDParam(PIDController.PID_LIMITLOW, PID_LIMITLOW);
		pidController.setPIDParam(PIDController.PID_I_LIMITHIGH, PID_I_LIMITHIGH);
		pidController.setPIDParam(PIDController.PID_I_LIMITLOW, PID_I_LIMITLOW);
	}
    
    public static void setPIDsetpoint(int NEW_PID_SETPOINT) {
    	PID_SETPOINT = NEW_PID_SETPOINT;
    	pidController.setPIDParam(PIDController.PID_SETPOINT, PID_SETPOINT);
    }

}