package balle.brick.pid;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import lejos.nxt.Button;
import lejos.nxt.LCD;
import lejos.nxt.comm.Bluetooth;
import lejos.nxt.comm.NXTConnection;
import lejos.util.PIDController;
import balle.brick.BrickController;

public class GoStraight {
	
	private boolean isThreadStopped;
	private boolean stopped;
	private boolean floating;

	private static double start_time;
	private static boolean first_move;

	private int PID_SETPOINT = 600;
	private int PID_DEADBAND = 1;
	private float PID_KP = 1.5f;
	private float PID_KI = 0.0f;
	private float PID_KD = 0.5f;
	private float PID_LIMITHIGH = 450.0f;
	private float PID_LIMITLOW = -PID_LIMITHIGH;
	private float PID_I_LIMITHIGH = 10.0f;
	private float PID_I_LIMITLOW = 10.0f;
	private int PID_DT = 15;

	private PIDController pidController;
	private BrickController controller;
	
	private static DataInputStream input;
	private static DataOutputStream output; 
	
	public GoStraight() {
		controller = new BrickController();
		controller.resetLeftTacho();
		controller.resetRightTacho();
		start_time = 0;
	}

	public void setPIDparameters() {
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

	public void start() {
		new Thread() {
			@Override
			public void run() {
				isThreadStopped = false;
				first_move = true;

				pidController = new PIDController(PID_SETPOINT, 0);
				setPIDparameters();

				while (!stopped) {

					// Can we just use PID_DT as this is
					// the thread sleep time?
					double current_time = System.currentTimeMillis();
					double running_time = (current_time - start_time) / 1000.0;

					double left_tacho = controller.getLeftTacho();
					double left_actual = left_tacho / running_time;
					int left_mv = pidController.doPID((int) left_actual);

					double right_tacho = controller.getRightTacho();
					double right_actual = right_tacho / running_time;
					int right_mv = pidController.doPID((int) right_actual);
					
					LCD.clear();
					LCD.drawString("l_act: " + left_actual, 0, 0);
					LCD.drawString("r_act: " + right_actual, 0, 1);
					LCD.drawString("l_new: " + left_mv, 0, 2);
					LCD.drawString("r_new: " + right_mv, 0, 3);
					LCD.drawString("time: " + running_time, 0, 4);

					if (first_move) {
						controller.forward(PID_SETPOINT, PID_SETPOINT);
						first_move = false;
					} else {
						controller.forward((PID_SETPOINT + left_mv), (PID_SETPOINT + right_mv));
					}

					try {
						Thread.sleep(PID_DT);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

				if (floating) {
					controller.floatWheels();
				} else {
					controller.stop();
				}
				controller.resetLeftTacho();
				controller.resetRightTacho();

				isThreadStopped = true;
			}
		}.start();
	}
	
	public static void main(String argv[]) {
	      LCD.drawString("waiting...", 0, 0);
	      NXTConnection connection = Bluetooth.waitForConnection();
	      if (connection == null)
	         return;

	      GoStraight go = new GoStraight();
	      LCD.clear();
	      LCD.drawString("connected...", 0, 0);

	      input = connection.openDataInputStream();
	      output = connection.openDataOutputStream();
	   
	      try {
	         while (!Button.ESCAPE.isDown()) {
	            int command = input.readInt();
				LCD.clear();
	            LCD.drawString("command: " + command, 0, 1);

	            if (PIDConstants.SHUTDOWN_COMMAND == command) {
					go.stopped = true;
					break;
	            } else if (PIDConstants.STOP_COMMAND == command) {
					go.stopped = true;
					start_time = 0;
				} else if (PIDConstants.FLOAT_COMMAND == command) {
					go.stopped = true;
					go.floating = true;
					start_time = 0;
	            } else if (PIDConstants.START_COMMAND == command) {
					go.stopped = false;
					go.floating = false;
					start_time = System.currentTimeMillis();
					go.start();
	            } else if (PIDConstants.WRITE_COMMAND == command) {
	               int key = input.readInt();
	               float value = input.readFloat();
	               if (PIDController.PID_SETPOINT == key) {
	                  go.PID_SETPOINT = (int) value;
	               } else if (PIDController.PID_DEADBAND == key) {
	                  go.PID_DEADBAND = (int) value;
	               } else if (PIDController.PID_KP == key) {
	                  go.PID_KP = value;
	               } else if (PIDController.PID_KI == key) {
	                  go.PID_KI = value;
	               } else if (PIDController.PID_KD == key) {
	                  go.PID_KD = value;
	               } else if (PIDController.PID_LIMITHIGH == key) {
	                  go.PID_LIMITHIGH = value;
	               } else if (PIDController.PID_LIMITLOW == key) {
	                  go.PID_LIMITLOW = value;
	               } else if (PIDController.PID_I_LIMITHIGH == key) {
	                  go.PID_I_LIMITHIGH = value;
	               } else if (PIDController.PID_I_LIMITLOW == key) {
	                  go.PID_I_LIMITLOW = value;
	               } else if (PIDConstants.LEFT_SPEED == key) {
	                  go.controller.forward((int)value);
	               } else if (PIDConstants.RIGHT_SPEED == key) {
	                  go.controller.forward((int)value);
	               } else if (PIDConstants.INTERVAL == key) {
	                  go.PID_DT = (int) value;
	               } else {
	                  value = -1;
	               }               
	               output.writeFloat(value);
	            } else if (PIDConstants.READ_COMMAND == command) {
					int key = input.readInt();
					LCD.clear();
	               LCD.drawString("key: " + key, 0, 2);
	               float value = 0;
	               if (PIDController.PID_SETPOINT == key) {
	                  value = go.PID_SETPOINT;
	               } else if (PIDController.PID_DEADBAND == key) {
	                  value = go.PID_DEADBAND;
	               } else if (PIDController.PID_KP == key) {
	                  value = go.PID_KP;
	               } else if (PIDController.PID_KI == key) {
	                  value = go.PID_KI;
	               } else if (PIDController.PID_KD == key) {
	                  value = go.PID_KD;
	               } else if (PIDController.PID_LIMITHIGH == key) {
	                  value = go.PID_LIMITHIGH;
	               } else if (PIDController.PID_LIMITLOW == key) {
	                  value = go.PID_LIMITLOW;
	               } else if (PIDController.PID_I_LIMITHIGH == key) {
	                  value = go.PID_I_LIMITHIGH;
	               } else if (PIDController.PID_I_LIMITLOW == key) {
	                  value = go.PID_I_LIMITLOW;
	               } else if (PIDController.PID_I == key) {
	                  value = -999999;
	                  if (go.pidController != null) {
	                     value = go.pidController.getPIDParam(PIDController.PID_I);
	                  }
	               } else if (PIDConstants.RAW_OUTPUT_MV == key) {
	                  value = -999999;
	                  if (go.pidController != null) {
	                     //value = p.pidController.rawOutputMV;
	                  }
	               } else if (PIDConstants.LEFT_TACHO == key) {
	                  value = go.controller.getLeftTacho();
	               } else if (PIDConstants.RIGHT_TACHO == key) {
	            	  value = go.controller.getRightTacho();
	               } else if (PIDConstants.LEFT_SPEED == key) {
	                  value = go.controller.getLeftSpeed();
	               } else if (PIDConstants.RIGHT_SPEED == key) {
	                  value = go.controller.getRightSpeed();
	               } else if (PIDConstants.MAX_TRAVEL_SPEED == key) {
	                  value = go.controller.getMaximumWheelSpeed();
	               } else if (PIDConstants.INTERVAL == key) {
	                  value = go.PID_DT;
	               } else {
	                  value = -1;
	               }
	               output.writeFloat(value);
	            } else {
	               // unknown command
	               output.writeInt(-1);
	            }
	            output.flush();
	         }
	         go.stopped = true;
	         connection.close();
	      } catch (IOException e) {
	         e.printStackTrace();
	      }

	      go.stopped = true;

	      while (!go.isThreadStopped) {
	         try {
	            Thread.sleep(100);
	         } catch (InterruptedException e) {

	         }
	      }
	}
}



