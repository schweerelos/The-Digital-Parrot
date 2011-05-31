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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

import net.schweerelos.parrot.model.NodeWrapper;
import net.schweerelos.parrot.model.filters.Chain;
import net.schweerelos.parrot.model.filters.ChainLink;

import org.apache.log4j.Logger;

@SuppressWarnings("serial")
public class ChainLinkView extends JPanel {
	private static final String ANY_TYPE = "any";
	private static final Object ANY_INSTANCE = "any";

	private static final String REMOVE_ICON = "images/remove.png";
	private static final Color COLOR_BORDER = UIConstants.ENVIRONMENT_SHADOW_DARK;

	private JButton removeButton;

	private JComboBox instanceCombo;
	private JComboBox typesCombo;

	private ToStringComparator comparator = new ToStringComparator();
	private ToStringListCellRenderer renderer = new ToStringListCellRenderer(30);
	private ChainLink link;
	private Chain chain;

	public ChainLinkView(Chain chain, ChainLink link) {
		this.chain = chain;
		this.link = link;

		typesCombo = new JComboBox();
		typesCombo.setToolTipText("Type");

		typesCombo.setModel(getTypesForInstance(link.getInstance()));
		if (link.hasType()) {
			typesCombo.setSelectedItem(link.getType());
		} else {
			typesCombo.setSelectedItem(ANY_TYPE);
		}
		typesCombo.setRenderer(renderer);
		typesCombo.setFont(typesCombo.getFont().deriveFont(Font.PLAIN));

		instanceCombo = new JComboBox();
		instanceCombo.setToolTipText("Item");

		instanceCombo.setModel(getInstancesForType(link.getType()));
		if (link.hasInstance()) {
			instanceCombo.setSelectedItem(link.getInstance());
		} else {
			instanceCombo.setSelectedItem(ANY_INSTANCE);
		}
		instanceCombo.setRenderer(renderer);
		instanceCombo.setFont(instanceCombo.getFont().deriveFont(Font.PLAIN));

		typesCombo.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Object selectedType = typesCombo.getSelectedItem();
				if (selectedType instanceof NodeWrapper) {
					NodeWrapper type = (NodeWrapper) selectedType;
					getChainLink().setType(type);
				} else if (selectedType.toString().equals(ANY_TYPE)) {
					getChainLink().setType(null);
				}
				// ignore otherwise
			}
		});

		instanceCombo.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				Object selectedInstance = instanceCombo.getSelectedItem();
				if (selectedInstance instanceof NodeWrapper) {
					NodeWrapper instance = (NodeWrapper) selectedInstance;
					getChainLink().setInstance(instance);
				} else if (selectedInstance.toString().equals(ANY_INSTANCE)) {
					getChainLink().setInstance(null);
				}
				// ignore otherwise
			}
		});

		setBorder(BorderFactory.createLineBorder(COLOR_BORDER, 1));
		setLayout(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();

		constraints.gridwidth = 1;
		constraints.gridheight = 1;
		constraints.gridx = 0;
		constraints.anchor = GridBagConstraints.LINE_START;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.weightx = 1.0;
		constraints.insets = new Insets(6, 6, 0, 0);
		add(typesCombo, constraints);
		constraints.anchor = GridBagConstraints.FIRST_LINE_START;
		constraints.weighty = 1.0;
		constraints.insets = new Insets(5, 6, 5, 0);
		add(instanceCombo, constraints);

		constraints.gridx = 1;
		constraints.gridy = 0;
		constraints.gridheight = GridBagConstraints.REMAINDER;
		constraints.anchor = GridBagConstraints.CENTER;
		constraints.fill = GridBagConstraints.NONE;
		constraints.weightx = 0;
		constraints.insets = new Insets(6, 5, 5, 5);
		removeButton = new JButton();
		removeButton.setPreferredSize(new Dimension(16, 16));
		add(removeButton, constraints);

		link.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent pce) {
				if (pce.getPropertyName().equals(ChainLink.INSTANCE_PROPERTY)) {
					if (pce.getNewValue() == null) {
						// TODO adjust type combo accordingly?
					} else if (pce.getNewValue() instanceof NodeWrapper) {
						final NodeWrapper instance = (NodeWrapper) pce
								.getNewValue();
						instanceCombo.setSelectedItem(instance);
						// did it work? it won't if instance isn't in
						// the combo box model yet
						if (!instanceCombo.getSelectedItem().equals(instance)) {
							new OneInstanceModelMaker(instance).execute();
						}
						new TypesForInstanceModelMaker(instance).execute();
					}
				} else if (pce.getPropertyName()
						.equals(ChainLink.TYPE_PROPERTY)) {
					if (pce.getNewValue() == null) {
						// TODO adjust instance combo accordingly?
					} else if (pce.getNewValue() instanceof NodeWrapper) {
						final NodeWrapper type = (NodeWrapper) pce
								.getNewValue();
						typesCombo.setSelectedItem(type);
						// did it work? it won't if type isn't in the combo box
						// model yet
						if (!typesCombo.getSelectedItem().equals(type)) {
							// it didn't work, we need to add type to the combo
							// box model first
							new OneTypeModelMaker(type).execute();
						}
						new InstancesForTypeModelMaker(type).execute();
					}
				}
			}
		});
	}

	private ComboBoxModel getTypesForInstance(NodeWrapper instance) {
		List<Object> typesList = new ArrayList<Object>();
		if (instance == null) {
			typesList.addAll(chain.getPossibleTypes(link));
			Collections.sort(typesList, comparator);
		} else {
			if (!instance.isType()) {
				typesList.addAll(chain.getPossibleTypesForIndividual(instance));
				Collections.sort(typesList, comparator);
			}
		}
		typesList.add(0, ANY_TYPE);
		return new DefaultComboBoxModel(typesList.toArray());
	}

	private ComboBoxModel getInstancesForType(NodeWrapper type) {
		List<Object> instanceList = new ArrayList<Object>();
		if (type != null) {
			if (type.isType()) {
				instanceList.addAll(chain.getPossibleInstancesForType(link));
				Collections.sort(instanceList, comparator);
			} else {
				instanceList.add(type);
			}
		}
		instanceList.add(0, ANY_INSTANCE);
		return new DefaultComboBoxModel(instanceList.toArray());
	}

	public void setRemoveAction(Action removeAction) {
		removeButton.setAction(removeAction);
		removeButton.setIcon(new ImageIcon(REMOVE_ICON));
		removeButton.setText(null);
		removeButton.setContentAreaFilled(false);
		removeButton.setIconTextGap(0);
		removeButton.setBorderPainted(false);
	}

	public void setCanChange(boolean canChange) {
		typesCombo.setEnabled(canChange);
		instanceCombo.setEnabled(canChange);
	}

	public ChainLink getChainLink() {
		return link;
	}

	@Override
	public Dimension getMinimumSize() {
		return getLayout().minimumLayoutSize(this);
	}

	@Override
	public Dimension getPreferredSize() {
		return getLayout().preferredLayoutSize(this);
	}



	private final class TypesForInstanceModelMaker extends
			SwingWorker<ComboBoxModel, Void> {
		private final NodeWrapper instance;

		private TypesForInstanceModelMaker(NodeWrapper instance) {
			this.instance = instance;
		}

		@Override
		protected ComboBoxModel doInBackground() throws Exception {
			return getTypesForInstance(instance);
		}

		@Override
		protected void done() {
			try {
				typesCombo.setModel(get());
				typesCombo.setSelectedItem(ANY_TYPE);
			} catch (InterruptedException e) {
				Logger logger = Logger.getLogger(ChainLinkView.this.getClass());
				logger.warn("problem setting instances model", e);
			} catch (ExecutionException e) {
				Logger logger = Logger.getLogger(ChainLinkView.this.getClass());
				logger.warn("problem setting instances model", e);
			}
		}
	}

	private final class InstancesForTypeModelMaker extends
			SwingWorker<ComboBoxModel, Void> {
		private final NodeWrapper type;

		private InstancesForTypeModelMaker(NodeWrapper type) {
			this.type = type;
		}

		@Override
		protected ComboBoxModel doInBackground() throws Exception {
			return getInstancesForType(type);
		}

		@Override
		protected void done() {
			try {
				instanceCombo.setModel(get());
				// does that mean we have to ignore the
				// event that this
				// generates?
				instanceCombo.setSelectedItem(ANY_INSTANCE);
			} catch (InterruptedException e) {
				Logger logger = Logger.getLogger(ChainLinkView.this.getClass());
				logger.warn("problem setting instances model", e);
			} catch (ExecutionException e) {
				Logger logger = Logger.getLogger(ChainLinkView.this.getClass());
				logger.warn("problem setting instances model", e);
			}
		}
	}

	private final class OneTypeModelMaker extends
			SwingWorker<ComboBoxModel, Void> {
		private final NodeWrapper type;

		private OneTypeModelMaker(NodeWrapper type) {
			this.type = type;
		}

		@Override
		protected ComboBoxModel doInBackground() throws Exception {
			List<Object> typeModel = new ArrayList<Object>();
			typeModel.add(ANY_TYPE);
			typeModel.add(type);
			return new DefaultComboBoxModel(typeModel.toArray());
		}

		@Override
		protected void done() {
			try {
				typesCombo.setModel(get());
				// now try again
				typesCombo.setSelectedItem(type);
			} catch (InterruptedException e) {
				Logger logger = Logger.getLogger(ChainLinkView.this.getClass());
				logger.warn("problem setting types model", e);
			} catch (ExecutionException e) {
				Logger logger = Logger.getLogger(ChainLinkView.this.getClass());
				logger.warn("problem setting types model", e);
			}
		}
	}

	private final class OneInstanceModelMaker extends
			SwingWorker<ComboBoxModel, Void> {
		private final NodeWrapper instance;

		private OneInstanceModelMaker(NodeWrapper instance) {
			this.instance = instance;
		}

		@Override
		protected ComboBoxModel doInBackground() throws Exception {
			// it didn't work, we need to add instance
			// to the combo box model first
			List<Object> instanceModel = new ArrayList<Object>();
			instanceModel.add(ANY_INSTANCE);
			instanceModel.add(instance);
			return new DefaultComboBoxModel(instanceModel.toArray());
		}

		@Override
		protected void done() {
			try {
				instanceCombo.setModel(get());
				// now try again
				instanceCombo.setSelectedItem(instance);
			} catch (InterruptedException e) {
				Logger logger = Logger.getLogger(ChainLinkView.this.getClass());
				logger.warn("problem setting types model", e);
			} catch (ExecutionException e) {
				Logger logger = Logger.getLogger(ChainLinkView.this.getClass());
				logger.warn("problem setting types model", e);
			}
		}
	}

	private static final class ToStringComparator implements Comparator<Object> {
		@Override
		public int compare(Object o1, Object o2) {
			return o1.toString().compareTo(o2.toString());
		}
	}

	private static final class ToStringListCellRenderer extends
			DefaultListCellRenderer {
		private boolean shorten = false;
		private int maxCharacters = 0;
		
		public ToStringListCellRenderer() {
			// leave fields at default value
		}
		
		public ToStringListCellRenderer(int maxCharacters) {
			if (maxCharacters >= 0) {
				this.shorten = true;
				this.maxCharacters = maxCharacters;
			}
		}

		@Override
		public Component getListCellRendererComponent(JList list, Object value,
				int index, boolean isSelected, boolean cellHasFocus) {
			String toStringValue = value.toString();
			if (shorten && toStringValue.length() > maxCharacters) {
				int lastSpaceAt = toStringValue.lastIndexOf(" ", maxCharacters);
				// if there is a space character not too far before the cut-off point, 
				// cut off at the space instead
				if (lastSpaceAt < maxCharacters - 1 && maxCharacters - lastSpaceAt < Math.max(maxCharacters / 4.0, 5)) {
					toStringValue = toStringValue.substring(0, lastSpaceAt) + " ...";
				} else {
					toStringValue = toStringValue.substring(0, maxCharacters) + "...";
				}
			}
			return super.getListCellRendererComponent(list, toStringValue,
					index, isSelected, cellHasFocus);
		}
	}

}
