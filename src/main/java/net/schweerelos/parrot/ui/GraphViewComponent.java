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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Paint;
import java.awt.Stroke;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.schweerelos.parrot.model.GraphParrotModel;
import net.schweerelos.parrot.model.NodeWrapper;
import net.schweerelos.parrot.model.ParrotModel;
import net.schweerelos.parrot.model.ParrotModelListener;

import org.apache.commons.collections15.Predicate;
import org.apache.commons.collections15.Transformer;

import edu.uci.ics.jung.algorithms.layout.CircleLayout;
import edu.uci.ics.jung.algorithms.layout.GraphElementAccessor;
import edu.uci.ics.jung.algorithms.layout.KKLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.util.Context;
import edu.uci.ics.jung.visualization.GraphZoomScrollPane;
import edu.uci.ics.jung.visualization.RenderContext;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.control.AbstractPopupGraphMousePlugin;
import edu.uci.ics.jung.visualization.control.DefaultModalGraphMouse;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse.Mode;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import edu.uci.ics.jung.visualization.decorators.ToStringLabeller;
import edu.uci.ics.jung.visualization.layout.PersistentLayout;
import edu.uci.ics.jung.visualization.picking.PickedInfo;
import edu.uci.ics.jung.visualization.picking.PickedState;
import edu.uci.ics.jung.visualization.renderers.DefaultEdgeLabelRenderer;
import edu.uci.ics.jung.visualization.renderers.DefaultVertexLabelRenderer;
import edu.uci.ics.jung.visualization.renderers.Renderer;
import edu.uci.ics.jung.visualization.renderers.VertexLabelAsShapeRenderer;

public class GraphViewComponent extends JPanel implements MainViewComponent,
		ParrotStateListener {

	private static final String LAYOUT_SUFFIX = ".layout";

	private static final int PADDING_NODE_BORDER = 12;

	private static final Color COLOR_BACKGROUND = Color.WHITE;

	private static final Color COLOR_NODE_BG = UIConstants.TT_ENVIRONMENT_LIGHT;
	private static final Color COLOR_NODE_PICKED_BG = UIConstants.ACCENT_LIGHT;
	private static final Color COLOR_NODE_HIGHLIGHTED_BG = UIConstants.ENVIRONMENT_LIGHTEST;
	private static final Color COLOR_NODE_WITH_PICKED_NEIGHBOUR_BG = UIConstants.ACCENT_LIGHTEST;
	private static final Color COLOR_NODE_ADJACENT_EDGE_PICKED_BG = COLOR_NODE_WITH_PICKED_NEIGHBOUR_BG;
	private static final Color COLOR_NODE_WITH_HIGHLIGHTED_NEIGHBOUR_BG = UIConstants.T_ENVIRONMENT_LIGHTEST;

	private static final Color COLOR_NODE_BORDER = UIConstants.TT_ENVIRONMENT_MEDIUM;
	private static final Color COLOR_NODE_PICKED_BORDER = UIConstants.ACCENT_MEDIUM;
	private static final Color COLOR_NODE_HIGHLIGHTED_BORDER = UIConstants.ENVIRONMENT_SHADOW_DARK;
	private static final Color COLOR_NODE_WITH_HIGHLIGHTED_NEIGHBOUR_BORDER = COLOR_NODE_HIGHLIGHTED_BORDER;
	private static final Color COLOR_NODE_WITH_PICKED_NEIGHBOUR_BORDER = UIConstants.T_ACCENT_MEDIUM;
	private static final Color COLOR_NODE_ADJACENT_EDGE_PICKED_BORDER = COLOR_NODE_PICKED_BORDER;

	private static final Color COLOR_NODE_TEXT = UIConstants.TT_TEXT;
	private static final Color COLOR_NODE_PICKED_TEXT = UIConstants.TEXT;
	private static final Color COLOR_NODE_HIGHLIGHTED_TEXT = UIConstants.TEXT;
	private static final Color COLOR_NODE_WITH_HIGHLIGHTED_NEIGHBOUR_TEXT = UIConstants.T_TEXT;
	private static final Color COLOR_NODE_WITH_PICKED_NEIGHBOUR_TEXT = COLOR_NODE_WITH_HIGHLIGHTED_NEIGHBOUR_TEXT;
	private static final Color COLOR_NODE_ADJACENT_EDGE_PICKED_TEXT = COLOR_NODE_WITH_HIGHLIGHTED_NEIGHBOUR_TEXT;

	private static final Color COLOR_EDGE = COLOR_NODE_BORDER;
	private static final Color COLOR_EDGE_PICKED = COLOR_NODE_PICKED_BORDER;
	private static final Color COLOR_EDGE_HIGHLIGHTED = COLOR_NODE_HIGHLIGHTED_BORDER;
	private static final Color COLOR_EDGE_ADJACENT_VERTEX_PICKED = COLOR_EDGE_PICKED;
	private static final Color COLOR_EDGE_ADJACENT_VERTEX_HIGHLIGHTED = COLOR_EDGE_HIGHLIGHTED;

	private static final Color COLOR_EDGE_LABEL = UIConstants.TEXT;

	private static final BasicStroke STROKE_EDGE_DEFAULT = new BasicStroke(2);
	private static final BasicStroke STROKE_EDGE_PICKED = new BasicStroke(3);
	private static final BasicStroke STROKE_EDGE_ADJACENT_NODE_PICKED = STROKE_EDGE_PICKED;

	private static final Stroke STROKE_VERTEX_DEFAULT = new BasicStroke(2);
	private static final Stroke STROKE_VERTEX_PICKED = new BasicStroke(3);
	private static final Stroke STROKE_VERTEX_INCOMING_EDGE_PICKED = STROKE_VERTEX_PICKED;
	private static final Stroke STROKE_VERTEX_OUTGOING_EDGE_PICKED = STROKE_VERTEX_DEFAULT;
	private static final Stroke STROKE_VERTEX_HIGHLIGHTED = STROKE_VERTEX_PICKED;

	private static final long serialVersionUID = 1L;

	static final Icon MOVING_ICON = new ImageIcon("images/graph-move.png");
	static final Icon SELECTING_ICON = new ImageIcon(
			"images/graph-select.png");

	private static final GridBagConstraints CONTENT_CONSTRAINTS = new GridBagConstraints();

	static {
		CONTENT_CONSTRAINTS.fill = GridBagConstraints.BOTH;
		CONTENT_CONSTRAINTS.weightx = 1;
		CONTENT_CONSTRAINTS.weighty = 1;
	}

	private VisualizationViewer<NodeWrapper, NodeWrapper> vv;
	private Layout<NodeWrapper, NodeWrapper> layout;
	private Graph<NodeWrapper, NodeWrapper> graph;
	private ParrotModel model;
	private NodeWrapperPopupMenu popup;
	private IncludePredicate<Context<Graph<NodeWrapper, NodeWrapper>, NodeWrapper>> includePredicate;

	private DoubleClickPickingModalGraphMouse<NodeWrapper, NodeWrapper> mouse;

	private JComponent view;

	private List<PickListener> pickListeners = new ArrayList<PickListener>();

	public GraphViewComponent() {
		super();
		setLayout(new GridBagLayout());
		setBorder(BorderFactory.createCompoundBorder(BorderFactory
				.createEmptyBorder(PADDING_NODE_BORDER, PADDING_NODE_BORDER,
						PADDING_NODE_BORDER, PADDING_NODE_BORDER),
				BorderFactory.createLoweredBevelBorder()));

		layout = new NodeWrapperPersistentLayoutImpl(
							     new CircleLayout<NodeWrapper, NodeWrapper>(new DirectedSparseMultigraph<NodeWrapper, NodeWrapper>()));
		vv = new VisualizationViewer<NodeWrapper, NodeWrapper>(layout);

		setupRenderContext(vv);

		view = new JScrollPane(vv);
		add(view, CONTENT_CONSTRAINTS);
	}

	@SuppressWarnings("serial")
	private void setupRenderContext(
			final VisualizationViewer<NodeWrapper, NodeWrapper> vis) {
		vis.setRenderer(new ParrotGraphRenderer());
		vis.setPickSupport(new ParrotPickSupport(vis));
		
		RenderContext<NodeWrapper, NodeWrapper> renderContext = vis
				.getRenderContext();

		final PickedInfo<NodeWrapper> vertexPickInfo = vis
				.getPickedVertexState();
		final PickedState<NodeWrapper> edgePickInfo = vis.getPickedEdgeState();

		// hide all edge arrows except for those on outgoing edges of picked
		// nodes
		renderContext
				.setEdgeArrowPredicate(new Predicate<Context<Graph<NodeWrapper, NodeWrapper>, NodeWrapper>>() {
					@Override
					public boolean evaluate(
							Context<Graph<NodeWrapper, NodeWrapper>, NodeWrapper> context) {
						NodeWrapper edge = context.element;
						NodeWrapper source = graph.getSource(edge);
						return vertexPickInfo.isPicked(source);
					}
				});

		// make edges straight lines to collapse parallel edges
		renderContext
				.setEdgeShapeTransformer(new EdgeShape.Line<NodeWrapper, NodeWrapper>());

		// hide text of all edges except for outgoing edges of picked nodes
		renderContext
				.setEdgeLabelTransformer(new Transformer<NodeWrapper, String>() {
					@Override
					public String transform(NodeWrapper edge) {
						NodeWrapper source = graph.getSource(edge);
						NodeWrapper destination = graph.getDest(edge);
						if (vertexPickInfo.isPicked(source)
								&& !vertexPickInfo.isPicked(destination)) {
							return edge.toString();
						} else {
							return "";
						}
					}
				});
		renderContext.setEdgeLabelRenderer(new DefaultEdgeLabelRenderer(
				COLOR_EDGE_LABEL) {
			@Override
			public <E> Component getEdgeLabelRendererComponent(JComponent vv,
					Object value, Font font, boolean isSelected, E edge) {
				Component component = super.getEdgeLabelRendererComponent(vv,
						value, font, isSelected, edge);
				component.setForeground(COLOR_EDGE_LABEL);
				return component;
			}

		});

		// start from VertexLabelAsShapeDemo

		// this class will provide both label drawing and vertex shapes
		VertexLabelAsShapeRenderer<NodeWrapper, NodeWrapper> vlasr = new VertexLabelAsShapeRenderer<NodeWrapper, NodeWrapper>(
				renderContext);
		renderContext.setVertexShapeTransformer(vlasr);

		vis.setForeground(COLOR_NODE_TEXT);

		// customize the render context

		renderContext
				.setVertexLabelTransformer(new ToStringLabeller<NodeWrapper>());

		renderContext.setVertexLabelRenderer(new DefaultVertexLabelRenderer(
				COLOR_NODE_PICKED_TEXT) {
			@Override
			public <V> Component getVertexLabelRendererComponent(JComponent vv,
					Object value, Font font, boolean isSelected,
					V vertexToRender) {
				Component component = super.getVertexLabelRendererComponent(vv,
						value, font, isSelected, vertexToRender);
				if (component instanceof JLabel) {
					JLabel label = (JLabel) component;
					// add a little bit of padding around the text
					Border originalBorder = label.getBorder();
					label.setBorder(BorderFactory.createCompoundBorder(
							originalBorder, BorderFactory.createEmptyBorder(3,
									2, 4, 2)));
				}
				// now set the colour/font too
				if (vertexToRender instanceof NodeWrapper) {
					NodeWrapper vertex = (NodeWrapper) vertexToRender;
					if (vertexPickInfo.isPicked(vertex)) {
						component.setForeground(COLOR_NODE_PICKED_TEXT);
					} else if (vertex.isHighlighted()) {
						component.setForeground(COLOR_NODE_HIGHLIGHTED_TEXT);
						component.setFont(font.deriveFont(Font.BOLD));
					} else if (GraphViewHelper.hasPickedNeighbour(vertex, vertexPickInfo, graph)) {
						component
								.setForeground(COLOR_NODE_WITH_PICKED_NEIGHBOUR_TEXT);
					} else if (GraphViewHelper.hasPickedAdjacentEdge(vertex, edgePickInfo, graph)) {
						component.setForeground(COLOR_NODE_ADJACENT_EDGE_PICKED_TEXT);
					} else if (GraphViewHelper.hasHighlightedNeighbour(vertex, graph)) {
						component.setForeground(COLOR_NODE_WITH_HIGHLIGHTED_NEIGHBOUR_TEXT);
					} else {
						component.setForeground(COLOR_NODE_TEXT);
					}
				}
				
				return component;
			}
		});

		// end from VertexLabelAsShapeDemo

		vis.getRenderer().getVertexLabelRenderer().setPosition(
				Renderer.VertexLabel.Position.CNTR);

		vis.setVertexToolTipTransformer(new Transformer<NodeWrapper, String>() {
			@Override
			public String transform(NodeWrapper vertex) {
				return vertex.getToolTipText(model);
			}
		});

		// inspired by PluggableRendererDemo
		Transformer<NodeWrapper, Paint> vertexOutline = new Transformer<NodeWrapper, Paint>() {
			@Override
			public Paint transform(NodeWrapper vertex) {
				if (vertexPickInfo.isPicked(vertex)) {
					return COLOR_NODE_PICKED_BORDER;
				} else if (vertex.isHighlighted()) {
					return COLOR_NODE_HIGHLIGHTED_BORDER;
				} else {
					if (GraphViewHelper.hasPickedAdjacentEdge(vertex, edgePickInfo, graph)) {
						return COLOR_NODE_ADJACENT_EDGE_PICKED_BORDER;
					}
					if (GraphViewHelper.hasPickedNeighbour(vertex, vertexPickInfo, graph)) {
						return COLOR_NODE_WITH_PICKED_NEIGHBOUR_BORDER;
					} else if (GraphViewHelper.hasHighlightedNeighbour(vertex, graph)) {
						return COLOR_NODE_WITH_HIGHLIGHTED_NEIGHBOUR_BORDER;
					}
					// will get here only if no neighbour is picked/highlighted
					return COLOR_NODE_BORDER;
				}
			}
		};
		renderContext.setVertexDrawPaintTransformer(vertexOutline);

		Transformer<NodeWrapper, Paint> vertexBackground = new Transformer<NodeWrapper, Paint>() {
			@Override
			public Paint transform(NodeWrapper vertex) {
				if (vertexPickInfo.isPicked(vertex)) {
					return COLOR_NODE_PICKED_BG;
				} else if (vertex.isHighlighted()) {
					return COLOR_NODE_HIGHLIGHTED_BG;
				} else {
					if (GraphViewHelper.hasPickedAdjacentEdge(vertex, edgePickInfo, graph)) {
						return COLOR_NODE_ADJACENT_EDGE_PICKED_BG;
					}
					if (GraphViewHelper.hasPickedNeighbour(vertex, vertexPickInfo, graph)) {
						return COLOR_NODE_WITH_PICKED_NEIGHBOUR_BG;
					} else if (GraphViewHelper.hasHighlightedNeighbour(vertex, graph)) {
						return COLOR_NODE_WITH_HIGHLIGHTED_NEIGHBOUR_BG;
					}
					return COLOR_NODE_BG;
				}
			}
		};
		renderContext.setVertexFillPaintTransformer(vertexBackground);

		Transformer<NodeWrapper, Stroke> vertexStroke = new Transformer<NodeWrapper, Stroke>() {
			@Override
			public Stroke transform(NodeWrapper vertex) {
				if (vertexPickInfo.isPicked(vertex)) {
					return STROKE_VERTEX_PICKED;
				} else if (vertex.isHighlighted()) {
					return STROKE_VERTEX_HIGHLIGHTED;
				}

				Collection<NodeWrapper> edges = graph.getInEdges(vertex);
				for (NodeWrapper edge : edges) {
					if (edgePickInfo.isPicked(edge)) {
						return STROKE_VERTEX_INCOMING_EDGE_PICKED;
					}
				}
				edges = graph.getOutEdges(vertex);
				for (NodeWrapper edge : edges) {
					if (edgePickInfo.isPicked(edge)) {
						return STROKE_VERTEX_OUTGOING_EDGE_PICKED;
					}
				}

				// we'll only get here if none of the cases above applies
				return STROKE_VERTEX_DEFAULT;
			}
		};
		renderContext.setVertexStrokeTransformer(vertexStroke);

		Transformer<NodeWrapper, Stroke> edgeStroke = new Transformer<NodeWrapper, Stroke>() {
			@Override
			public Stroke transform(NodeWrapper edge) {
				NodeWrapper edgeSource = graph.getSource(edge);
				if (edgePickInfo.isPicked(edge)) {
					return STROKE_EDGE_PICKED;
				} else if (vertexPickInfo.isPicked(edgeSource)) {
					return STROKE_EDGE_ADJACENT_NODE_PICKED;
				} else {
					return STROKE_EDGE_DEFAULT;
				}
			}
		};
		renderContext.setEdgeStrokeTransformer(edgeStroke);

		Transformer<NodeWrapper, Paint> edgeColor = new Transformer<NodeWrapper, Paint>() {
			@Override
			public Paint transform(NodeWrapper edge) {
				if (edgePickInfo.isPicked(edge)) {
					return COLOR_EDGE_PICKED;
				} else if (GraphViewHelper.hasPickedAdjacentVertex(edge, vertexPickInfo, graph)) {
					return COLOR_EDGE_ADJACENT_VERTEX_PICKED;
				} else if (edge.isHighlighted()) {
					return COLOR_EDGE_HIGHLIGHTED;
				} else if (GraphViewHelper.hasHighlightedAdjacentVertex(edge, graph)) {
					return COLOR_EDGE_ADJACENT_VERTEX_HIGHLIGHTED;
				} else {
					return COLOR_EDGE;
				}
			}
		};
		renderContext.setEdgeDrawPaintTransformer(edgeColor);
		// draw arrows in the same colour as edges
		renderContext.setArrowDrawPaintTransformer(edgeColor);
		renderContext.setArrowFillPaintTransformer(edgeColor);

		includePredicate = new IncludePredicate<Context<Graph<NodeWrapper, NodeWrapper>, NodeWrapper>>();
		renderContext.setEdgeIncludePredicate(includePredicate);
		renderContext.setVertexIncludePredicate(includePredicate);

		vis.setBackground(COLOR_BACKGROUND);
		
		mouse = new DoubleClickPickingModalGraphMouse<NodeWrapper, NodeWrapper>();
		mouse.add(new AbstractPopupGraphMousePlugin() {
			@Override
			protected void handlePopup(MouseEvent e) {
				if (!e.isPopupTrigger()) {
					return;
				}
				GraphElementAccessor<NodeWrapper, NodeWrapper> pickSupport = vis
						.getPickSupport();
				if (pickSupport == null) {
					return;
				}

				NodeWrapper node = pickSupport.getVertex(layout, e.getX(), e
						.getY());
				if (node == null) {
					node = pickSupport.getEdge(layout, e.getX(), e.getY());
				}
				if (node == null) {
					return;
				}
				popup.setNodeWrapper(node);
				popup.show(vis, e.getX(), e.getY());
			}
		});
		mouse.setDoubleClickPickingPlugin(new DoubleClickPickingPlugin() {
			@Override
			void doubleClickOccurred(MouseEvent e) {
				GraphElementAccessor<NodeWrapper, NodeWrapper> pickSupport = vis
						.getPickSupport();
				if (pickSupport == null) {
					return;
				}

				NodeWrapper node = pickSupport.getVertex(layout, e.getX(), e
						.getY());
				if (node == null) {
					return;
				}
				fireNodeSelected(node);
			}
		});
		vis.setGraphMouse(mouse);
		
	}

	@Override
	public void setModel(ParrotModel model) {
		removeAll();

		if (model == null) {
			maybeSaveLayout();
			layout = null;
			vv.setGraphLayout(null);
			return;
		}

		if (!(model instanceof GraphParrotModel)) {
			throw new IllegalArgumentException("model must be a graph model");
		}

		this.model = model;
		graph = ((GraphParrotModel) model).asGraph();
		popup = new NodeWrapperPopupMenu(SwingUtilities.getRoot(this), model);

		model.addParrotModelListener(new ParrotModelListener() {
			@Override
			public void highlightsChanged() {
				SwingUtilities.invokeLater(new Runnable() {

					@Override
					public void run() {
						vv.fireStateChanged();
						vv.repaint();
					}
				});
			}

			@Override
			public void restrictionsChanged(
					final Collection<NodeWrapper> currentlyHidden) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						includePredicate.setCurrentlyHidden(currentlyHidden);
						vv.repaint();
					}
				});
			}

			@Override
			public void modelBusy() {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						synchronized (GraphViewComponent.this.model) {
							if (!GraphViewComponent.this.model.isBusy()) {
								return;
							}
							vv.setEnabled(false);
							view.setEnabled(false);
							GraphViewComponent.this.setCursor(Cursor
									.getPredefinedCursor(Cursor.WAIT_CURSOR));
							view.setCursor(Cursor
									.getPredefinedCursor(Cursor.WAIT_CURSOR));
							vv.setCursor(Cursor
									.getPredefinedCursor(Cursor.WAIT_CURSOR));
						}
					}
				});
			}

			@Override
			public void modelIdle() {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						synchronized (GraphViewComponent.this.model) {
							if (GraphViewComponent.this.model.isBusy()) {
								return;
							}
							view.setEnabled(true);
							vv.setEnabled(true);
							GraphViewComponent.this.setCursor(Cursor
									.getDefaultCursor());
							view.setCursor(Cursor.getDefaultCursor());
							vv.setCursor(Cursor.getDefaultCursor());
						}
					}
				});
			}
		});

		layout = new NodeWrapperPersistentLayoutImpl(
				new KKLayout<NodeWrapper, NodeWrapper>(graph));
		layout.setSize(new Dimension(880, 600));
		String layoutFilename = getLayoutFilename();
		try {
			if (new File(layoutFilename).canRead()) {
				((PersistentLayout<NodeWrapper, NodeWrapper>) layout)
						.restore(layoutFilename);
			}
		} catch (IOException e) {
			// TODO #1 Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO #1 Auto-generated catch block
			e.printStackTrace();
		}

		vv.setGraphLayout(layout);
		GraphZoomScrollPane pane = new GraphZoomScrollPane(vv);
		ModeToggle modeToggle = new ModeToggle(mouse);
		pane.setCorner(modeToggle);
		add(pane, CONTENT_CONSTRAINTS);
		view = pane;
	}
	
	@Override
	public Collection<NodeWrapper> getSelectedNodes() {
		return vv.getPickedVertexState().getPicked();
	}

	private String getLayoutFilename() {
		String dataID = model.getDataIdentifier();
		int lastSlashIndex = dataID.lastIndexOf(File.separatorChar);
		int lastDotIndex = dataID.lastIndexOf('.');
		String filename;
		if (lastSlashIndex < lastDotIndex) {
			// this is what we prefer
			filename = dataID.substring(lastSlashIndex, lastDotIndex);	
		} else {
			filename = new StringBuilder(dataID.hashCode()).toString();
		}
		return System.getProperty("user.home") + File.separator + ".digital-parrot" + File.separator + filename + LAYOUT_SUFFIX;
	}

	private void maybeSaveLayout() {
		if (layout instanceof PersistentLayout) {
			PersistentLayout<NodeWrapper, NodeWrapper> theLayout = (PersistentLayout<NodeWrapper, NodeWrapper>) layout;
			try {
				String filename = getLayoutFilename();
				File directory = new File(filename).getParentFile();
				if (!directory.canWrite()) {
					directory.mkdirs();
				}
				theLayout.persist(filename);
			} catch (IOException e) {
				// TODO #1 Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public JComponent asJComponent() {
		return this;
	}

	@Override
	public void parrotExiting() {
		maybeSaveLayout();
	}

	@SuppressWarnings("serial")
	class ModeToggle extends JToggleButton {

		ModeToggle(final DefaultModalGraphMouse<NodeWrapper, NodeWrapper> mouse) {
			setIcon(MOVING_ICON);
			setSelectedIcon(SELECTING_ICON);
			setText("");
			setToolTipText(isSelected() ? "Make mouse move graph"
					: "Make mouse select");
			mouse.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					if (e.getStateChange() == ItemEvent.SELECTED) {
						setSelected(e.getItem() == Mode.PICKING);
						setText("");
						setToolTipText(isSelected() ? "Make mouse move graph"
								: "Make mouse select");
					}
				}
			});
			addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					if (isSelected()) {
						mouse.setMode(Mode.PICKING);
					} else {
						mouse.setMode(Mode.TRANSFORMING);
					}
				}
			});
			setSelected(true);
		}

	}

	class IncludePredicate<T> implements
			Predicate<Context<Graph<NodeWrapper, NodeWrapper>, NodeWrapper>> {

		private Collection<NodeWrapper> currentlyHidden;

		@Override
		public boolean evaluate(
				Context<Graph<NodeWrapper, NodeWrapper>, NodeWrapper> context) {
			if (currentlyHidden == null) {
				return true;
			}
			NodeWrapper element = context.element;
			return !currentlyHidden.contains(element);
		}

		void setCurrentlyHidden(Collection<NodeWrapper> currentlyHidden) {
			this.currentlyHidden = currentlyHidden;
		}

	}

	private void fireNodeSelected(NodeWrapper newSelection) {
		List<PickListener> listeners;
		synchronized (this) {
			listeners = Collections.synchronizedList(pickListeners);
		}
		synchronized (listeners) {
			for (PickListener listener : listeners) {
				try {
					listener.picked(newSelection);
				} catch (RuntimeException re) {
					re.printStackTrace();
					pickListeners.remove(listener);
				}
			}
		}
	}

	@Override
	public void addPickListener(PickListener listener) {
		pickListeners.add(listener);
	}

	@Override
	public void removePickListener(PickListener listener) {
		pickListeners.remove(listener);
	}

	@Override
	public String getTitle() {
		return "Graph";
	}

}
