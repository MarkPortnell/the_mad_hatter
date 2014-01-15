package balle.brick.milestone1;

import lejos.nxt.LCD;
import balle.brick.BrickController;

public class RollThroughField {
	
	private static final int ROLL_SPEED = 400;
	private static final double ROLL_DISTANCE = 1300;

    private static void drawMessage(String message) {
        LCD.clear();
        LCD.drawString(message, 0, 0);
        LCD.refresh();
    }

    public static void main(String[] args) {

        BrickController controller = new BrickController();

        boolean movingForward = false;
		int left = ROLL_SPEED;
		int right = (int) (ROLL_SPEED * 1.01);
		double rollDistance = ROLL_DISTANCE;
        while (true) {
            if (!movingForward) {
                drawMessage("Roll");
                movingForward = true;
                controller.reset();
				controller.forward(left, right);
                
            } else {
            	float distance = controller.getTravelDistance();
				if (distance >= rollDistance) {
					drawMessage("Stop " + left + " " + right);
                	controller.stop();
					break;
                } else {
                    drawMessage(Float.toString(distance));
                }
            }
        }
    }
}
