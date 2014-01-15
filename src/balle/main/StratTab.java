package balle.main;

import java.awt.Color;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.log4j.Logger;

import balle.controller.Controller;
import balle.memory.ConfigFile;
import balle.misc.Globals;
import balle.simulator.Simulator;
import balle.strategy.StrategyFactory;
import balle.strategy.StrategyRunner;
import balle.strategy.UnknownDesignatorException;
import balle.world.AbstractWorld;
import balle.world.objects.Ball;
import balle.world.objects.Goal;

@SuppressWarnings("serial")
public class StratTab extends JPanel implements ActionListener {

	private static final Logger LOG = Logger.getLogger(StratTab.class);

	private final Config config;

	// GUI declarations
	// Red = Yellow
	// Green = Blue

	private JPanel controlPanel;
	private JLabel blueLabel;
	private JLabel yellowLabel;
	private JComboBox blueStrategy;
	private JComboBox yellowStrategy;
	private JButton startButton;
	private JButton switchGoals;
	private JButton switchRobot;
	private JButton noiseButton;
	private JButton randomButton;
	private JButton resetButton;
	private JButton saveButton;
	private JButton penaltyButton;
	private JButton connectButton;
	private JButton disconnectButton;
	private boolean isBlue;
	private ArrayList<String> stratTabs;
	private String[] strings = new String[0];

	private StrategyConstructorSelector parametersPanelBlue = null;
	private StrategyConstructorSelector parametersPanelYellow = null;

	GridBagConstraints c = new GridBagConstraints();
	private StrategyRunner strategyRunner;
	private AbstractWorld worldA;
	private Simulator simulator;
	private StrategyFactory strategyFactory;

	private Controller controllerA;
	private Controller controllerB;

	private JLabel connectionText;
	private JLabel connectionStatus;
	private JPanel connectionPanel;
	private boolean tryingToConnect = false;
	private boolean connecting = false;

	private final static String BLUE_LABEL_TEXT = "Select Blue strategy";
	private final static String YELLOW_LABEL_TEXT = "Select Yellow strategy";

	private boolean isInGoal(Goal goal, Ball ball) {
		if (goal.isLeftGoal()) {
			return goal.getMaxX() - ball.getPosition().getX() > 0;
		} else if (goal.isRightGoal()) {
			return goal.getMinX() - ball.getPosition().getX() < 0;
		}
		return false;
	}

	public StratTab(Config config, Controller controllerA,
			Controller controllerB, AbstractWorld worldA, AbstractWorld worldB,
			StrategyRunner strategyRunner, Simulator simulator,
			StrategyFactory strategyFactory) {

		super();

		this.config = config;
		this.worldA = worldA;
		this.simulator = simulator;
		// Initialise strategy runner
		this.strategyRunner = strategyRunner;
		this.strategyFactory = strategyFactory;

		this.controllerA = controllerA;
		this.controllerB = controllerB;

		// Declare layout of buttons etc
		// Layout composed of 3 by 6 grid (0 indexed)
		// (GridBagConstraints controls properties of grid
		// for each component. Leftmost column is gridx = 0
		// and topmost row is grid y = 0)

		String[] names = new String[strategyFactory.availableDesignators()
		                            .size()];
		for (int count = 0; count < strategyFactory.availableDesignators()
				.size(); count++) {
			names[count] = strategyFactory.availableDesignators().get(count);
		}

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		controlPanel = new JPanel(new GridBagLayout());
		stratTabs = new ArrayList<String>();
		blueLabel = new JLabel(BLUE_LABEL_TEXT);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 1;
		c.insets = new Insets(1, 5, 1, 5);
		if (!worldA.isBlue() && !isSimulator()){
			blueLabel.setText(YELLOW_LABEL_TEXT);
		} 
		controlPanel.add(blueLabel, c);

		blueStrategy = new JComboBox(names);
		c.gridx = 0;
		c.gridy = 2;
		c.gridwidth = 2;
		// blueStrategy.setEnabled(simulator != null || worldA.isBlue());
		controlPanel.add(blueStrategy, c);

		yellowLabel = new JLabel(YELLOW_LABEL_TEXT);
		c.gridx = 0;
		c.gridy = 3;
		c.gridwidth = 1;
		if (isSimulator()){
			controlPanel.add(yellowLabel, c);
		} 

		yellowStrategy = new JComboBox(names);
		c.gridx = 0;
		c.gridy = 4;
		c.gridwidth = 2;
		//yellowStrategy.setEnabled(simulator != null || !worldA.isBlue());
		if (isSimulator()) {
			controlPanel.add(yellowStrategy, c);
		} 

		startButton = new JButton("Start");
		startButton.addActionListener(this);
		startButton.setActionCommand("startstop");
		c.gridx = 2;
		c.gridy = 4;
		c.gridwidth = 1;
		controlPanel.add(startButton, c);

		switchGoals = new JButton("Switch Goals");
		switchGoals.addActionListener(this);
		switchGoals.setActionCommand("goals");
		c.gridx = 0;
		c.gridy = 5;
		controlPanel.add(switchGoals, c);

		isBlue = worldA.isBlue();
		if (isBlue) {
			switchRobot = new JButton("Robot: Blue");
		} else {
			switchRobot = new JButton("Robot: Yellow");
		}
		switchRobot.addActionListener(this);
		switchRobot.setActionCommand("robot");
		c.gridx = 1;
		c.gridy = 5;
		controlPanel.add(switchRobot, c);

		noiseButton = new JButton("Noise: Off");
		noiseButton.addActionListener(this);
		noiseButton.setEnabled(simulator != null);
		noiseButton.setActionCommand("noise");
		c.gridx = 2;
		c.gridy = 5;
		controlPanel.add(noiseButton, c);

		randomButton = new JButton("Randomise");
		randomButton.addActionListener(this);
		randomButton.setEnabled(simulator != null);
		randomButton.setActionCommand("randomise");
		c.gridx = 2;
		c.gridy = 6;
		controlPanel.add(randomButton, c);

		resetButton = new JButton("Reset");
		resetButton.addActionListener(this);
		resetButton.setEnabled(simulator != null);
		resetButton.setActionCommand("reset");
		c.gridx = 0;
		c.gridy = 6;
		controlPanel.add(resetButton, c);

		penaltyButton = new JButton("Penalty");
		penaltyButton.addActionListener(this);
		penaltyButton.setEnabled(simulator != null);
		penaltyButton.setActionCommand("penalty");
		c.gridx = 1;
		c.gridy = 6;
		controlPanel.add(penaltyButton, c);

		saveButton = new JButton("Save");
		saveButton.addActionListener(this);
		saveButton.setActionCommand("save");
		c.gridx = 2;
		c.gridy = 3;
		controlPanel.add(saveButton, c);

		connectButton = new JButton("Connect");
		connectButton.addActionListener(this);
		connectButton.setActionCommand("connect");
		c.gridx = 0;
		c.gridy = 0;
		controlPanel.add(connectButton, c);

		disconnectButton = new JButton("Disconnect");
		disconnectButton.addActionListener(this);
		disconnectButton.setActionCommand("disconnect");
		c.gridx = 1;
		c.gridy = 0;
		controlPanel.add(disconnectButton, c);

		// connectionText = new JLabel("Status:");
		connectionText = new JLabel("");
		connectionStatus = new JLabel("Disconnected");
		connectionPanel = new JPanel();
		connectionPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
		connectionPanel.add(connectionText);
		connectionPanel.add(connectionStatus);
		c.gridx = 2;
		c.gridy = 0;
		controlPanel.add(connectionPanel, c);

		blueStrategy.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (e.getActionCommand().equals("comboBoxChanged")) {
					generateStrategyConstructorSelector();
				}
			}
		});
		yellowStrategy.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (e.getActionCommand().equals("comboBoxChanged")) {
					generateStrategyConstructorSelector();
				}
			}
		});
		this.add(controlPanel);

		generateStrategyConstructorSelector();

		// Interpret Config object

		blueStrategy.setSelectedItem(config.get(Config.BLUE_STRATEGY));
		if (simulator != null)
			yellowStrategy.setSelectedItem(config.get(Config.YELLOW_STRATEGY));

	}

	public void generateStrategyConstructorSelector() {
		if (parametersPanelBlue != null) {
			this.remove(parametersPanelBlue);
			this.validate();
		}

		String designator = blueStrategy.getSelectedItem().toString();
		try {
			parametersPanelBlue = new StrategyConstructorSelector(designator,
					strategyFactory.getArgumentNames(designator),
					strategyFactory.getArguments(designator));
		} catch (UnknownDesignatorException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.add(parametersPanelBlue);

		if (parametersPanelYellow != null) {
			this.remove(parametersPanelYellow);
			this.invalidate();
		}

		String designatorYellow = yellowStrategy.getSelectedItem().toString();
		try {
			parametersPanelYellow = new StrategyConstructorSelector(
					designatorYellow,
					strategyFactory.getArgumentNames(designatorYellow),
					strategyFactory.getArguments(designatorYellow));
		} catch (UnknownDesignatorException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (isSimulator()){
			this.add(parametersPanelYellow);
		} 
		//this.add(parametersPanelYellow);

		this.validate();

	}

	// Listener for button clicks
	@Override
	public final void actionPerformed(ActionEvent event) {
		if (event.getActionCommand().equals("startstop")) {
			if (startButton.getText().equals("Start")) {

				String selectedStrategyA = (String) blueStrategy
						.getSelectedItem();

				String selectedStrategyB;
				if (simulator == null) {
					selectedStrategyB = (String) "NullStrategy";
				} else {
					selectedStrategyB = (String) yellowStrategy
							.getSelectedItem();
				}

				config.set(Config.BLUE_STRATEGY, selectedStrategyA);
				if (simulator != null)
					config.set(Config.YELLOW_STRATEGY, selectedStrategyB);

				try {
					strategyRunner.startStrategy(
							strategyFactory.createClass(selectedStrategyA,
									parametersPanelBlue.getValues()),
									strategyFactory.createClass(selectedStrategyB,
											parametersPanelYellow.getValues()));

				} catch (UnknownDesignatorException e) {
					LOG.error("Cannot start strategy \"" + selectedStrategyA
							+ "\": " + e.toString());
					System.out.println("Valiant effort, chaps");
					return;
				}
				startButton.setText("Stop");
				switchGoals.setEnabled(false);
				switchRobot.setEnabled(false);
			} else {
				startButton.setText("Start");
				strategyRunner.stopStrategy();
				switchGoals.setEnabled(true);
				switchRobot.setEnabled(true);
			}
		} else if (event.getActionCommand().equals("randomise")) {
			try {
				strategyRunner.stopStrategy();
			} catch (NullPointerException e) {
				System.err
				.println("No currently running Strategy. World randomised "
						+ "\": " + e);
			}
			startButton.setText("Start");
			strategyRunner.stopStrategy();
			switchGoals.setEnabled(true);
			switchRobot.setEnabled(true);
			randomiseRobots(simulator);
			randomiseBall(simulator);
		} else if (event.getActionCommand().equals("reset")) {
			try {
				strategyRunner.stopStrategy();
			} catch (NullPointerException e) {
				System.err
				.println("No currently running Strategy. World reset "
						+ "\": " + e);
			}
			startButton.setText("Start");
			strategyRunner.stopStrategy();
			switchGoals.setEnabled(true);
			switchRobot.setEnabled(true);
			resetRobots(simulator);
			resetBall(simulator);
		} else if ((event.getActionCommand().equals("penalty"))) {
			try {
				strategyRunner.stopStrategy();
			} catch (NullPointerException e) {
				System.err
				.println("No currently running Strategy. World reset "
						+ "\": " + e);
			}
			startButton.setText("Start");
			strategyRunner.stopStrategy();
			switchGoals.setEnabled(true);
			switchRobot.setEnabled(true);
			setUpPenalty(simulator, worldA.isBlue(), worldA.getOpponentsGoal()
					.isLeftGoal());
		} else if (event.getActionCommand().equals("noise")) {
			if (noiseButton.getText().equals("Noise: Off")) {
				noiseButton.setText("Noise: On");
				simulator.setIsNoisy(true);
			} else {
				noiseButton.setText("Noise: Off");
				simulator.setIsNoisy(false);
			}
		} else if (event.getActionCommand().equals("goals")) {
			if (worldA.getGoalPosition()) {
				worldA.setGoalPosition(false);
			} else {
				worldA.setGoalPosition(true);
			}
		} else if (event.getActionCommand().equals("robot")) {

			if (worldA.isBlue()) {
				switchRobot.setText("Robot: Yellow");
				if (!isSimulator()){
					blueLabel.setText(YELLOW_LABEL_TEXT);
				} 
				worldA.setIsBlue(false);
				if (simulator != null) {
					strategyRunner.setController(simulator.getYellowSoft(),
							simulator.getBlueSoft());
				}
			} else {
				switchRobot.setText("Robot: Blue");
				if (!isSimulator()){
					blueLabel.setText(BLUE_LABEL_TEXT);
				} 
				worldA.setIsBlue(true);
				if (simulator != null) {
					strategyRunner.setController(simulator.getBlueSoft(),
							simulator.getYellowSoft());
				}
			}
		} else if (event.getActionCommand().equals("save")) {
			try {
				ConfigFile cf = new ConfigFile(Globals.resFolder,
						Globals.configFolder);
				cf.write(config);
			} catch (IOException e) {
				System.err.println("Couldn't save configurations.");
			}

		} else if (event.getActionCommand().equals("connect")) {
			// Do in another thread:
			tryingToConnect = true;
			new ConnectThread().start();
		} else if (event.getActionCommand().equals("disconnect")) {
			tryingToConnect = false;
			connectionStatus.setForeground(Color.BLACK);
			connectionStatus.setText("Disconnected");
			System.out.println("Disconnected.");
			controllerA.disconnect();
		}
	}

	public boolean isSimulator() {
		return simulator != null;
	}

	// Called from reset/randomise buttons

	public void randomiseBall(Simulator s) {
		s.randomiseBallPosition();
	}

	public void randomiseRobots(Simulator s) {
		s.randomiseRobotPositions();
	}

	public void resetBall(Simulator s) {
		s.resetBallPosition();
	}

	public void setUpPenalty(Simulator s, boolean blue, boolean attackLeft) {
		s.setUpPenalty(blue, attackLeft);
	}

	public void resetRobots(Simulator s) {
		s.resetRobotPositions();
	}

	private class ConnectThread extends Thread {
		public void run() {
			if (!tryingToConnect) {
				return;
			}
			if (connecting) {
				// Another instance exists.
				return;
			}
			connecting = true;
			System.out.println("Connecting...");
			connectionStatus.setForeground(Color.YELLOW);
			connectionStatus.setText("Connecting");
			controllerA.connect();
			if (controllerA.isReady()) {
				connectionStatus.setForeground(new Color(50, 150, 50));
				connectionStatus.setText("Connected");
				System.out.println("Yay!");
				connecting = false;
				tryingToConnect = false;
			} else {
				connectionStatus.setForeground(Color.RED);
				connectionStatus.setText("Error");
				System.out.println("Try again?");
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				connecting = false;
				new ConnectThread().start();

			}
		}
	}
}
