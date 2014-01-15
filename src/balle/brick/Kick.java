package balle.brick;

import balle.controller.Controller;

public class Kick {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Controller control = new BrickController();
		
		control.kick();
		try {
			Thread.sleep(1000);
		} catch (Exception e){
			e.printStackTrace();
		}
	}

}
