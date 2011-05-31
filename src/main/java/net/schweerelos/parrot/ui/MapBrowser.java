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


package net.schweerelos.parrot.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import net.schweerelos.parrot.model.CenteredThing;
import net.schweerelos.parrot.model.NoSuchNodeWrapperException;
import net.schweerelos.parrot.model.NodeWrapper;
import net.schweerelos.parrot.model.ParrotModel;
import net.schweerelos.parrot.util.LatLonBounds;
import net.schweerelos.parrot.util.LatLonBounds.InvalidStringException;

import com.webrenderer.swing.BrowserFactory;
import com.webrenderer.swing.IMozillaBrowserCanvas;
import com.webrenderer.swing.ProxySetting;
import com.webrenderer.swing.event.JavascriptEvent;
import com.webrenderer.swing.event.JavascriptListener;
import com.webrenderer.swing.event.NetworkAdapter;
import com.webrenderer.swing.event.NetworkEvent;

@SuppressWarnings("serial")
public class MapBrowser extends JPanel {

	private List<MapBrowserListener> listeners;
	private IMozillaBrowserCanvas browser;

	private int zoomLevel;
	private LatLonBounds mapBounds;
	private ParrotModel model;
	private LatLonBounds lastBoundsFromMap;
	private Timer timer;

	public MapBrowser(String licenseType, String licenseData) {
		listeners = new ArrayList<MapBrowserListener>();

		setLayout(new BorderLayout());

		BrowserFactory.setLicenseData(licenseType, licenseData);
		browser = BrowserFactory.spawnMozilla();
		
		setupProxy();
		
		browser.enableDefaultContextMenu(false);
		browser.setJavascriptEnabled(true);

		browser.addJavascriptListener(new JavascriptListener() {

			@Override
			public void onJavascriptStatusChange(JavascriptEvent e) {
				if (!isShowing()) {
					return;
				}
				String status = e.getJavascriptStatus();
				if (status.startsWith("boundsChanged")) {
					LatLonBounds newBounds;
					try {
						newBounds = LatLonBounds.fromString(status);
						if (!newBounds.equals(mapBounds)) {
							maybeBoundsChanged(newBounds);
						}
					} catch (InvalidStringException e1) {
						e1.printStackTrace();
					}
				} else if (status.startsWith("zoomChanged")) {
					try {
						int newZoomLevel = Integer
								.parseInt(status.split("=")[1]);
						if (newZoomLevel != zoomLevel) {
							zoomLevel = newZoomLevel;
							fireZoomed(zoomLevel);
						}
					} catch (NumberFormatException nfe) {
						nfe.printStackTrace();
					}
				} else if (status.startsWith("markerClicked")) {
					String markerID = status.split("=")[1];
					try {
						NodeWrapper wrapper = model
								.getNodeWrapperForString(markerID);
						fireMarkerClicked(wrapper);
					} catch (NoSuchNodeWrapperException e1) {
						// ignore
					}
				}
			}

			@Override
			public void onJavascriptMessage(JavascriptEvent e) {
				// ignore
			}

			@Override
			public void onJavascriptDialog(JavascriptEvent e) {
				// ignore
			}
		});

		browser.addNetworkListener(new NetworkAdapter() {
			@Override
			public void onDocumentComplete(NetworkEvent ne) {
				if (mapBounds != null || lastBoundsFromMap != null) {
					return;
				}
				// if there are no bounds yet, get them from the document
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						String javaScript = "map.getBounds();";
						String boundsString = browser.executeScriptWithReturn(javaScript);
						try {
							mapBounds = LatLonBounds.fromString(boundsString);
						} catch (InvalidStringException e) {
							// ignore
							e.printStackTrace();
						}
					}
				});
			}			
		});
		
		String fileName = constructMapHTML();
		browser.loadURL(fileName);
		
		add(browser.getComponent(), BorderLayout.CENTER);
		setPreferredSize(new Dimension(320, 300));

		timer = new Timer(500, new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mapBounds = lastBoundsFromMap;
				firePanned(mapBounds);
			}
		});
		timer.setRepeats(false);
	}

	private void setupProxy() {
		// http://java.sun.com/javase/6/docs/technotes/guides/net/properties.html
		String proxyHost = System.getProperty("http.proxyHost");
		if (proxyHost != null && !proxyHost.equals("")) {
			// this is the default anyway when proxyHost is specified
			int port = 80;
			try {
				String proxyPort = System.getProperty("http.proxyPort");
				port = Integer.parseInt(proxyPort);
			} catch (NumberFormatException nfe) {
				// ignore
			}
			browser.setProxyProtocol(new ProxySetting(ProxySetting.PROTOCOL_HTTP,
					proxyHost,
					port));
			// try to get auth info -- this is non-standard
			String username = System.getProperty("http.proxyUser", "");
			String password = System.getProperty("http.proxyPass", "");
			if (!username.equals("") && !password.equals("")) {
				browser.setProxyAuthentication(username, password);
			}
			browser.enableProxy();
		}
	}

	private String constructMapHTML() {
		InputStream templateFile = this.getClass().getResourceAsStream("/maps/gmap-template.html");
		File outputFile = new File(System.getProperty("user.home") + File.separator + ".digital-parrot" + File.separator + "gmap.html");

		String html = "";
		Scanner scanner = null;

		try {
			scanner = new Scanner(templateFile);
			html = scanner.useDelimiter("\\A").next();
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			if (scanner != null) {
				scanner.close();
			}
		}

		// center on Hamilton initially
		html = html.replace("MAPCENTER",
				"new google.maps.LatLng(-37.783333, 175.283333)");
		// zoom level is an integer
		html = html.replace("ZOOMLEVEL", "6");

		try {
			if (!outputFile.getParentFile().canWrite()) {
				outputFile.getParentFile().mkdirs();
			}
			BufferedWriter output = new BufferedWriter(new FileWriter(
					outputFile));
			output.write(html.toString());
			output.close();
		} catch (IOException ex) {
			Logger.getLogger(MapBrowser.class.getName()).log(Level.SEVERE,
					null, ex);
		}
		return outputFile.getAbsolutePath();
	}

	public void focusMapOn(CenteredThing<NodeWrapper> thing) {
		String location = String.format("new google.maps.LatLng(%f, %f)", thing
				.getLat(), thing.getLon());
		String javaScript = "map.panTo(" + location + ");";
		browser.executeScript(javaScript);

		javaScript = String.format("map.setZoom(%d);", thing.getPrecision()
				.toZoomLevel());
		browser.executeScript(javaScript);
	}

	public void setModel(ParrotModel model) {
		this.model = model;
		List<CenteredThing<NodeWrapper>> allLocatedThings = model
				.getLocatedThings().getAll();
		int i = 0;
		for (CenteredThing<NodeWrapper> locatedThing : allLocatedThings) {
			createMarker(i, locatedThing);
			i++;
		}
	}

	private void createMarker(int i, CenteredThing<NodeWrapper> locatedThing) {
		String javaScript = String.format(
				"var marker%d = new google.maps.Marker({ "
						+ "position: new google.maps.LatLng(%f, %f), "
						+ "map: map, " + "title: '%s', "
						+ "clickable: true, " + "draggable: false" + "});",
				i, locatedThing.getLat(), locatedThing.getLon(),
				locatedThing.getLabel());
		browser.executeScript(javaScript);
		javaScript = String.format(
				"google.maps.event.addListener(marker%d, 'click', function() {"
						+ "    window.status = 'markerClicked, marker=%s';"
						+ "  });", i, locatedThing.getValue()
						.getOntResource().getURI());
		browser.executeScript(javaScript);
	}

	private synchronized void maybeBoundsChanged(LatLonBounds newBounds) {
		lastBoundsFromMap = newBounds;
		if (timer.isRunning()) {
			timer.restart();
		} else {
			timer.start();
		}
	}

	/* listener support start */

	public synchronized void addMapBrowserListener(
			MapBrowserListener mapBrowserListener) {
		listeners.add(mapBrowserListener);
	}

	public synchronized void removeMapBrowserListener(
			MapBrowserListener mapBrowserListener) {
		listeners.remove(mapBrowserListener);
	}

	private void firePanned(LatLonBounds newBounds) {
		List<MapBrowserListener> listeners;
		synchronized (this) {
			listeners = Collections.synchronizedList(this.listeners);
		}
		synchronized (listeners) {
			for (MapBrowserListener listener : listeners) {
				try {
					listener.panned(newBounds);
				} catch (RuntimeException re) {
					re.printStackTrace();
					this.listeners.remove(listener);
				}
			}
		}
	}

	private void fireMarkerClicked(NodeWrapper wrapper) {
		List<MapBrowserListener> listeners;
		synchronized (this) {
			listeners = Collections.synchronizedList(this.listeners);
		}
		synchronized (listeners) {
			for (MapBrowserListener listener : listeners) {
				try {
					listener.markerClicked(wrapper);
				} catch (RuntimeException re) {
					re.printStackTrace();
					this.listeners.remove(listener);
				}
			}
		}
	}

	private void fireZoomed(int zoom) {
		List<MapBrowserListener> listeners;
		synchronized (this) {
			listeners = Collections.synchronizedList(this.listeners);
		}
		synchronized (listeners) {
			for (MapBrowserListener listener : listeners) {
				try {
					listener.zoomed(zoom);
				} catch (RuntimeException re) {
					re.printStackTrace();
					this.listeners.remove(listener);
				}
			}
		}
	}
	/* end listener support */

	public LatLonBounds getMapBounds() {
		return mapBounds;
	}

}
