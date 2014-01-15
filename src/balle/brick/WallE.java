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

class ListenerThreadTheSecond extends Thread {
    DataInputStream input;
    boolean         shouldStop;
    int             command;
    boolean         commandConsumed;

	ListenerThreadTheSecond(DataInputStream input) {
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
 * the computer test out movements of Roboto. Implements PID controller.
 * 
 * @author s1027418
 */
public class WallE {

	private static boolean waiting = false;

	private boolean threadStopped;
	private boolean stopped;
	private boolean floating;
	private boolean first_move = true;
	private boolean kick;
	private boolean penalty;
	private boolean rotate;

	private static int PID_SETPOINT_LEFT = 300;
	private static int PID_SETPOINT_RIGHT = 300;
	private static int PID_DEADBAND = 1;
	private static float PID_KP = 0.0f;
	private static float PID_KI = 0.0f;
	private static float PID_KD = 0.5f;
	private static float PID_LIMITHIGH = 100.0f;
	private static float PID_LIMITLOW = -100.0f;
	private static float PID_I_LIMITHIGH = 0.0f;
	private static float PID_I_LIMITLOW = 0.0f;
	private static int PID_DT = 10;

	protected static PIDController leftPID;
	protected static PIDController rightPID;
	protected static BrickController controller;
	
	protected static WallE walle;

	private static AbstractMessage decodedMessage;

	public WallE() {
		controller = new BrickController();
		controller.resetLeftTacho();
		controller.resetRightTacho();
	}

	/**
	 * This thread processes a message. If the message is to move, it uses PID
	 * to control the movement. If it is to stop, it stops the robot and waits
	 * for another command. If it is to kick, it kicks while moving, or while
	 * stopped.
	 */
	public void start() {
		new Thread() {
			@Override
			public void run() {
				threadStopped = false;
				first_move = true;

				leftPID = new PIDController(PID_SETPOINT_LEFT, PID_DT);
				rightPID = new PIDController(PID_SETPOINT_RIGHT, PID_DT);
				setPIDparameters();

				while (!stopped) {
					if (waiting) continue;
					
					if (kick) {
						if (penalty) {
							controller.penaltyKick();
						} else {
							controller.kick();
						}
						kick = false;
					}
					
					if (rotate) {
						MessageRotate messageRotate = (MessageRotate) decodedMessage;
						controller.rotate(messageRotate.getAngle(),messageRotate.getSpeed());
						rotate = false;
					}

					// Calculate actual wheel speeds then doPID()
					double running_time = PID_DT / 1000.0;

					double left_actual = controller.getLeftTacho() / running_time;
					int left_mv = leftPID.doPID((int) left_actual);

					double right_actual = controller.getRightTacho() / running_time;
					int right_mv = rightPID.doPID((int) right_actual);

					LCD.clear();
					LCD.drawString("l_act: " + left_actual, 0, 0);
					LCD.drawString("r_act: " + right_actual, 0, 1);
					LCD.drawString("l_new: " + left_mv, 0, 2);
					LCD.drawString("r_new: " + right_mv, 0, 3);
					LCD.drawString("time: " + running_time, 0, 4);
					
					// If we're just moving off, set initial wheel speeds
					if (first_move) {
						controller.forward(PID_SETPOINT_LEFT, PID_SETPOINT_RIGHT);
						first_move = false;
					} else {
						controller.forward((PID_SETPOINT_LEFT + left_mv), (PID_SETPOINT_RIGHT + right_mv));
					}
				}
				
				if (kick) {
					if (penalty) {
						controller.penaltyKick();
					} else {
						controller.kick();
					}
					kick = false;
				} else if (rotate) {
					MessageRotate messageRotate = (MessageRotate) decodedMessage;
					controller.rotate(messageRotate.getAngle(),messageRotate.getSpeed());
					rotate = false;
				} else {
					if (floating) {
						controller.floatWheels();
					} else {
						controller.stop();
					}
				}
				
				controller.resetLeftTacho();
				controller.resetRightTacho();

				threadStopped = true;
			}
		}.start();
	}
	
	/**
	 * Sets the variables which change how the main thread is running, e.g.
	 * tells the robot when to move, kick etc..
	 */
	public static boolean processMessage() {
        String name = decodedMessage.getName();

		if (name.equals(MessageMove.NAME)) {
            MessageMove messageMove = (MessageMove) decodedMessage;

			int left = messageMove.getLeftWheelSpeed();
			int right = messageMove.getRightWheelSpeed();
			PID_SETPOINT_LEFT = left;
			PID_SETPOINT_RIGHT = right;

			walle.stopped = false;
			walle.floating = false;
			walle.rotate = false;
			walle.kick = false;
			walle.penalty = false;

			walle.start();
        
		} else if (name.equals(MessageStop.NAME)) {
            MessageStop messageStop = (MessageStop) decodedMessage;
			walle.stopped = true;
			walle.floating = messageStop.floatWheels();
		
		} else if (name.equals(MessageKick.NAME)) {
			MessageKick messageKick = (MessageKick) decodedMessage;
			walle.kick = true;
			walle.penalty = messageKick.isPenalty();

        } else if (name.equals(MessageRotate.NAME)) {
            walle.rotate = true;	
        } else {
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

		walle = new WallE();

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
			ListenerThreadTheSecond listener = new ListenerThreadTheSecond(input);

            MessageDecoder decoder = new MessageDecoder();

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
					decodedMessage = decoder.decodeMessage(hashedMessage);

					if (decodedMessage == null) {
                        drawMessage("Could not decode: " + hashedMessage);
						continue;
                    }

					drawMessage(decodedMessage.getName());

					boolean successful = processMessage();
                    if (!successful) {
						drawMessage("Unknown message received: " + hashedMessage);
						continue;
                    }

				} catch (Exception e) {
					drawMessage("Error in MainLoop: " + e.getMessage());
                }
            }

            connection.close();

			while (!walle.threadStopped) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {

				}
			}
        }
    }

	public void setPIDparameters() {
		leftPID.setPIDParam(PIDController.PID_SETPOINT, PID_SETPOINT_LEFT);
		leftPID.setPIDParam(PIDController.PID_DEADBAND, PID_DEADBAND);
		leftPID.setPIDParam(PIDController.PID_KP, PID_KP);
		leftPID.setPIDParam(PIDController.PID_KI, PID_KI);
		leftPID.setPIDParam(PIDController.PID_KD, PID_KD);
		leftPID.setPIDParam(PIDController.PID_LIMITHIGH, PID_LIMITHIGH);
		leftPID.setPIDParam(PIDController.PID_LIMITLOW, PID_LIMITLOW);
		leftPID.setPIDParam(PIDController.PID_I_LIMITHIGH, PID_I_LIMITHIGH);
		leftPID.setPIDParam(PIDController.PID_I_LIMITLOW, PID_I_LIMITLOW);

		rightPID.setPIDParam(PIDController.PID_SETPOINT, PID_SETPOINT_RIGHT);
		rightPID.setPIDParam(PIDController.PID_DEADBAND, PID_DEADBAND);
		rightPID.setPIDParam(PIDController.PID_KP, PID_KP);
		rightPID.setPIDParam(PIDController.PID_KI, PID_KI);
		rightPID.setPIDParam(PIDController.PID_KD, PID_KD);
		rightPID.setPIDParam(PIDController.PID_LIMITHIGH, PID_LIMITHIGH);
		rightPID.setPIDParam(PIDController.PID_LIMITLOW, PID_LIMITLOW);
		rightPID.setPIDParam(PIDController.PID_I_LIMITHIGH, PID_I_LIMITHIGH);
		rightPID.setPIDParam(PIDController.PID_I_LIMITLOW, PID_I_LIMITLOW);
	}

    private static void drawMessage(String message) {
        LCD.clear();
        LCD.drawString(message, 0, 0);
        LCD.refresh();
    }

}