/*
 * Copyright (C) 2011 Andrea Schweer
 *
 * This file is part of the Digital Parrot. 
 *
 * The Digital Parrot is free software; you can redistribute it and/or modify
 * it under the terms of the Eclipse Public License as published by the Eclipse
 * Foundation or its Agreement Steward, either version 1.0 of the License, or
 * (at your option) any later version.
 *
 * The Digital Parrot is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the Eclipse Public License for
 * more details.
 *
 * You should have received a copy of the Eclipse Public License along with the
 * Digital Parrot. If not, see http://www.eclipse.org/legal/epl-v10.html. 
 *
 */


package net.schweerelos.parrot;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.schweerelos.parrot.model.NodeWrapper;
import net.schweerelos.parrot.model.ParrotModel;
import net.schweerelos.parrot.model.ParrotModelFactory;
import net.schweerelos.parrot.model.ParrotModelFactory.Style;
import net.schweerelos.parrot.ui.MainViewComponent;
import net.schweerelos.parrot.ui.NavigatorComponent;
import net.schweerelos.parrot.ui.ParrotStateListener;
import net.schweerelos.parrot.ui.PickListener;
import net.schweerelos.parrot.ui.UnknownStyleException;
import net.schweerelos.parrot.ui.UserInterfaceManager;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;

public class CombinedParrotApp extends JFrame {
	private static final long serialVersionUID = 1L;
	private static final String APP_TITLE = "The Digital Parrot";

	private ParrotModel model;

	private List<MainViewComponent> mainViews = new ArrayList<MainViewComponent>(2);
	private MainViewComponent listView;
	private MainViewComponent graphView;
	private MainViewComponent activeMainView;
	
	private List<NavigatorComponent> navigators;

	private Map<Window, Point> preferredFrameLocations = new HashMap<Window, Point>();
	
	public CombinedParrotApp(Properties properties) {
		super();
		setModalExclusionType(Dialog.ModalExclusionType.APPLICATION_EXCLUDE);

		model = ParrotModelFactory.getInstance(Style.COMBINED).createModel();
		
		initGUI(properties);
	}

	private void initGUI(Properties properties) {
		try {
			this.setTitle(APP_TITLE);
			this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			this.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent e) {
					for (MainViewComponent mainViewComponent : mainViews) {	
						if (mainViewComponent instanceof ParrotStateListener) {
							((ParrotStateListener) mainViewComponent).parrotExiting();
						}
					}
				}
			});
			setSize(920, 690);

			getContentPane().setLayout(new BorderLayout());

			UserInterfaceManager uiManager = new UserInterfaceManager(properties);
			navigators = new ArrayList<NavigatorComponent>(4);

			// main view
			listView = uiManager.createMainViewComponent(Style.TABLE);
			graphView = uiManager.createMainViewComponent(Style.GRAPH);
			mainViews.add(listView);
			mainViews.add(graphView);

			final JTabbedPane mainPanel = new JTabbedPane();
			mainPanel.add(graphView.getTitle(), graphView.asJComponent());
			mainPanel.add(listView.getTitle(), listView.asJComponent());
			
			mainPanel.setSelectedIndex(0);
			activeMainView = graphView;
			mainPanel.addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					if (e.getSource() != mainPanel) {
						return;
					}
					int selectedIndex = mainPanel.getSelectedIndex();
					if (selectedIndex == 0) {
						activeMainView = graphView;
					} else if (selectedIndex == 1) {
						activeMainView = listView;
					} else {
						Logger logger = Logger.getLogger(CombinedParrotApp.class);
						logger.warn("unknown tab index selected: " + selectedIndex);
					}
				}
			});
			
			add(mainPanel, BorderLayout.CENTER);

			// navigators
			JToolBar navigatorsBar = new JToolBar(JToolBar.HORIZONTAL);
			navigatorsBar.setMargin(new Insets(0, 11, 0, 0));
			navigatorsBar.setFloatable(false);
			getContentPane().add(navigatorsBar, BorderLayout.PAGE_START);

			// timeline
			NavigatorComponent timelineNavigator = uiManager.createTimelineNavigationComponent();
			navigators.add(timelineNavigator);

			JFrame timelineFrame = new JFrame(timelineNavigator.getNavigatorName() + " – " + APP_TITLE);

			timelineFrame.getContentPane().add(timelineNavigator.asJComponent());
			timelineFrame.pack();
			Point preferredLocation = new Point(0,0);
			preferredFrameLocations.put(timelineFrame, preferredLocation);

			if (timelineNavigator.hasShowHideListener()) {
				timelineFrame.addComponentListener(timelineNavigator.getShowHideListener());
			}
			timelineFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

			JToggleButton timelineButton 
			= setupNavigatorButton(timelineNavigator.getNavigatorName(), 
					timelineNavigator.getAcceleratorKey(), 
					timelineNavigator);
			navigatorsBar.add(timelineButton);

			// map		
			NavigatorComponent mapNavigator = uiManager.createMapNavigationComponent();
			navigators.add(mapNavigator);

			JFrame mapFrame = new JFrame(mapNavigator.getNavigatorName() + " – " + APP_TITLE);

			mapFrame.getContentPane().add(mapNavigator.asJComponent());
			mapFrame.pack();
			preferredLocation = new Point(0, Toolkit.getDefaultToolkit().getScreenSize().height - mapFrame.getHeight());
			preferredFrameLocations.put(mapFrame, preferredLocation);

			if (mapNavigator.hasShowHideListener()) {
				mapFrame.addComponentListener(mapNavigator.getShowHideListener());
			}
			mapFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

			JToggleButton mapButton 
			= setupNavigatorButton(mapNavigator.getNavigatorName(),
					mapNavigator.getAcceleratorKey(),
					mapNavigator);
			navigatorsBar.add(mapButton);

			// search		
			NavigatorComponent searchNavigator = uiManager.createSearchComponent();
			navigators.add(searchNavigator);

			JFrame searchFrame = new JFrame(searchNavigator.getNavigatorName() + " – " + APP_TITLE);

			searchFrame.getContentPane().add(searchNavigator.asJComponent());
			searchFrame.pack();
			preferredLocation = new Point(Toolkit.getDefaultToolkit().getScreenSize().width - searchFrame.getWidth(), 0);
			preferredFrameLocations.put(searchFrame, preferredLocation);

			if (searchNavigator.hasShowHideListener()) {
				searchFrame.addComponentListener(searchNavigator.getShowHideListener());
			}
			searchFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

			JToggleButton searchButton 
			= setupNavigatorButton(searchNavigator.getNavigatorName(),
					searchNavigator.getAcceleratorKey(),
					searchNavigator);
			navigatorsBar.add(searchButton);

			// connections
			NavigatorComponent chainNavigator = uiManager.createChainNavigationComponent();
			navigators.add(chainNavigator);

			if (chainNavigator instanceof PickListener) {
				for (MainViewComponent mainViewComponent : mainViews) {	
					mainViewComponent.addPickListener((PickListener) chainNavigator);
				}
			}

			if (chainNavigator.hasShowHideListener()) {
				chainNavigator.asJComponent().addComponentListener(chainNavigator.getShowHideListener());
			}

			JToggleButton connectionsButton 
			= setupNavigatorButton(chainNavigator.getNavigatorName(), 
					chainNavigator.getAcceleratorKey(),
					chainNavigator);
			navigatorsBar.add(connectionsButton);

			add(chainNavigator.asJComponent(), BorderLayout.PAGE_END);
			chainNavigator.asJComponent().setVisible(false);
		} catch (RuntimeException e) {
			e.printStackTrace(System.err);
			System.exit(1);
		} catch (UnknownStyleException e) {
			e.printStackTrace(System.err);
			System.exit(1);
		}
	}

	public Map<String, List<Action>> getNavigatorActionsForNode(NodeWrapper node) {
		Map<String, List<Action>> map = new HashMap<String, List<Action>>();
		for (NavigatorComponent navigator : navigators) {
			map.put(navigator.getNavigatorName(), navigator.getActionsForNode(node));
		}
		return map;
	}

	public Map<String, List<Action>> getNavigatorActionsForType(NodeWrapper type) {
		Map<String, List<Action>> map = new HashMap<String, List<Action>>();
		for (NavigatorComponent navigator : navigators) {
			map.put(navigator.getNavigatorName(), navigator.getActionsForType(type));
		}
		return map;
	}
	/**
	 * @param args
	 */
	@SuppressWarnings("static-access")
	public static void main(String[] args) {
		CommandLineParser parser = new PosixParser();
		// create the Options
		Options options = new Options();
		options.addOption(OptionBuilder.withLongOpt("help")
				.withDescription("Shows usage information and quits the program")
				.create("h"));

		options.addOption(OptionBuilder.withLongOpt("datafile")
				.withDescription("The file with instances to use (required)")
				.hasArg()
				.withArgName("file")
				.isRequired()
				.create("f"));
		
		options.addOption(OptionBuilder.withLongOpt("properties")
				.withDescription("Properties file to use. Default: " + System.getProperty("user.home") + File.separator + ".digital-parrot" + File.separator + "parrot.properties")
				.hasArg()
				.withArgName("prop")
				.create("p"));
		try {
			// parse the command line arguments
			CommandLine line = parser.parse(options, args);

			if (line.hasOption("h")) {
				// this is only executed when all required options are present _and_ the help option is specified!
				printHelp(options);
				return;
			}

			String datafile = line.getOptionValue("f");
			if (!datafile.startsWith("file:") || !datafile.startsWith("http:")) {
				datafile = "file:" + datafile;
			}
			
			String propertiesPath = System.getProperty("user.home") + File.separator + ".digital-parrot" + File.separator + "parrot.properties";
			if (line.hasOption("p")) {
				propertiesPath = line.getOptionValue("p");
			}
			final Properties properties = new Properties();
			File propertiesFile = new File(propertiesPath);
			if (propertiesFile.exists() && propertiesFile.canRead()) {
				try {
					properties.load(new FileReader(propertiesFile));
				} catch (FileNotFoundException e) {
					e.printStackTrace(System.err);
					System.exit(1);
				} catch (IOException e) {
					e.printStackTrace(System.err);
					System.exit(1);
				}
			}
			
			
			final String url = datafile; // we need a "final" var for the anonymous class
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					CombinedParrotApp inst = null;
					try {
						inst = new CombinedParrotApp(properties);
						inst.loadModel(url);
					} catch (Exception e) {
						JOptionPane.showMessageDialog(null, 
								"There has been an error while starting the program.\nThe program will exit now.", 
								APP_TITLE + " – Error",
								JOptionPane.ERROR_MESSAGE);
						e.printStackTrace(System.err);
						System.exit(1);
					}
					if (inst != null) {
						inst.setLocationRelativeTo(null);
						inst.setVisible(true);
					}
				}
			});
		} catch (ParseException exp) {
			printHelp(options);
		}
	}



	private void loadModel(String url) {
		model.loadData(url);
		
		graphView.setModel(model.asGraphModel());
		listView.setModel(model.asListModel());
		
		for (NavigatorComponent navigator : navigators) {
			if (navigator != null) {
				navigator.setModel(model);
			}
		}

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				validate();
			}
		});
	}

	@SuppressWarnings("serial")
	private JToggleButton setupNavigatorButton(final String name, final String accelerator, final NavigatorComponent navigator) {
		final Component component = navigator.asJComponent();
		AbstractAction showNavigatorAction = new AbstractAction(name) {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!(e.getSource() instanceof JToggleButton)) {
					return;
				}
				final Window window;
				if (component instanceof Window) {
					window = (Window) component;
				} else {
					window = SwingUtilities.getWindowAncestor(component);
				}
				JToggleButton button = (JToggleButton) e.getSource();
				boolean show = button.isSelected();
				if (show) {
					if (window != CombinedParrotApp.this && preferredFrameLocations.containsKey(window)) {
						window.setLocation(preferredFrameLocations.get(window));
					}
				}
				if (navigator.tellSelectionWhenShown()) {
					Collection<NodeWrapper> selectedNodes = activeMainView.getSelectedNodes();
					navigator.setSelectedNodes(selectedNodes);
				}
				component.setVisible(show);
				if (show) {
					window.setVisible(true);
				} else if (window != CombinedParrotApp.this) {
					window.setVisible(false);
				}
			}
		};
		showNavigatorAction.putValue(Action.ACCELERATOR_KEY, 
				KeyStroke.getKeyStroke("control " + accelerator));
		final JToggleButton button = new JToggleButton(showNavigatorAction);
		button.setToolTipText("Show " + name.toLowerCase());
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				button.setToolTipText((button.isSelected() ? "Hide " : "Show ") + name.toLowerCase());
			}
		});
		final Window window;
		if (component instanceof Window) {
			window = (Window) component;
		} else {
			window = SwingUtilities.getWindowAncestor(component);
		}
		if (window != null) {
			window.addComponentListener(new ComponentAdapter() {
				@Override
				public void componentHidden(ComponentEvent e) {
					button.setSelected(false);
					if (window != CombinedParrotApp.this) {
						preferredFrameLocations.put(window, window.getLocation());	
					}
				}
				@Override
				public void componentShown(ComponentEvent e) {
					button.setSelected(true);
				}
			});
		}
		return button;
	}


	private static void printHelp(Options options) {
		HelpFormatter helpFormat = new HelpFormatter();
		helpFormat.printHelp(APP_TITLE, options, true);	
	}
}