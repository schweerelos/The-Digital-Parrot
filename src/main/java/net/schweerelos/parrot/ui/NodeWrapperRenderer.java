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
import java.awt.Font;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.RowSorter.SortKey;
import javax.swing.table.DefaultTableCellRenderer;

import net.schweerelos.parrot.model.NodeWrapper;
import net.schweerelos.parrot.model.ParrotModel;

public class NodeWrapperRenderer extends DefaultTableCellRenderer {

	private static final Color COLOR_HERE_TOO_SELECTED_BORDER = UIConstants.ACCENT_MEDIUM;
	private static final Color COLOR_HIGHLIGHTED_BORDER = UIConstants.ENVIRONMENT_SHADOW_DARK;
	private static final Color COLOR_ODD_ROW_UNSELECTED_BG = UIConstants.ENVIRONMENT_LIGHTEST;
	private static final Color COLOR_EVEN_ROW_UNSELECTED_BG = Color.WHITE;
	private static final Color COLOR_SELECTED_BG = UIConstants.ACCENT_LIGHT;
	private static final Color COLOR_TEXT_DEEMPHASIZED = UIConstants.ENVIRONMENT_LIGHT;
	private static final Color COLOR_TEXT_NORMAL = Color.BLACK;
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private ParrotModel model;

	public NodeWrapperRenderer(ParrotModel model) {
		this.model = model;
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		Component renderer = super.getTableCellRendererComponent(table, value,
				isSelected, hasFocus, row, column);
		if (value instanceof NodeWrapper) {
			NodeWrapper node = (NodeWrapper) value;
			setToolTipText(node.getToolTipText(model));
			
			if (renderer instanceof JLabel) {
				JLabel label = (JLabel) renderer;
				label.setVerticalAlignment(JLabel.BOTTOM);
			}
			
			if (deemphasizeText(table, value, isSelected, hasFocus, row,
					column)) {
				renderer.setForeground(COLOR_TEXT_DEEMPHASIZED);
			} else {
				renderer.setForeground(COLOR_TEXT_NORMAL);
			}
			
			if (node.isHighlighted()) {
				Font renderFont = renderer.getFont();
				renderer.setFont(renderFont.deriveFont(Font.BOLD));
				setBorderColour(renderer, COLOR_HIGHLIGHTED_BORDER);
			}
			if (node.isHereTooSelected()) {
				Color borderColour = COLOR_HERE_TOO_SELECTED_BORDER;
				setBorderColour(renderer, borderColour);
			}
			if (isSelected || hasFocus) {
				renderer.setBackground(COLOR_SELECTED_BG);
			} else {
				if (row % 2 == 0) {
					renderer.setBackground(COLOR_EVEN_ROW_UNSELECTED_BG);
				} else {
					renderer.setBackground(COLOR_ODD_ROW_UNSELECTED_BG);
				}
			}
		}
		return renderer;
	}

	private boolean deemphasizeText(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		if (isSelected || hasFocus) {
			return false;
		}


		List<? extends SortKey> sortKeys = table.getRowSorter().getSortKeys();
		if (sortKeys.size() > 0) {
			int checkColumn = sortKeys.get(0).getColumn();
			if (!valueRepeatedFromRowAbove(table, row, checkColumn)) {
				return false;
			} else {
				// we can stop here if the value is from the major sort column
				if (checkColumn == column) {
					return true;
				}
			}
		}
		if (sortKeys.size() > 1) {
			int checkColumn = sortKeys.get(1).getColumn();
			if (!valueRepeatedFromRowAbove(table, row, checkColumn)) {
				return false;
			} else {
				// they are the same and we've already found out that the major
				// ones match too
				// so if this is the column we're looking at, we don't need to
				// go further
				if (checkColumn == column) {
					return true;
				}
			}
		}
		return valueRepeatedFromRowAbove(table, row, column);
	}
	
	private boolean valueRepeatedFromRowAbove(JTable table, int row, int column) {
		if (row < 1) {
			// we're looking at the first row
			return false;
		}
		Object valueInRowAbove, valueInThisRow;
		valueInRowAbove = table.getValueAt(row - 1, column);
		valueInThisRow = table.getValueAt(row, column);
		
		if (valueInRowAbove instanceof NodeWrapper && valueInThisRow instanceof NodeWrapper) {
			NodeWrapper wrapperInRowAbove = (NodeWrapper) valueInRowAbove;
			NodeWrapper wrapperInThisRow = (NodeWrapper) valueInThisRow;
			if (wrapperInRowAbove.isLiteral() && wrapperInThisRow.isLiteral()) {
				return wrapperInRowAbove.getLiteral().equals(wrapperInThisRow.getLiteral());
			} else if (wrapperInRowAbove.isOntResource() && wrapperInThisRow.isOntResource()) {
				return wrapperInRowAbove.getOntResource().equals(wrapperInThisRow.getOntResource());
			}
		}	
		// last resort: compare strings
		return valueInThisRow.toString().equals(valueInRowAbove.toString());
	}

	private void setBorderColour(Component renderer, Color borderColour) {
		if (renderer instanceof JComponent) {
			JComponent jRenderer = (JComponent) renderer;
			jRenderer
					.setBorder(BorderFactory.createLineBorder(borderColour, 2));
		}
	}

	// The following methods override the defaults for performance reasons
	@Override
	public void validate() {
	}

	@Override
	public void revalidate() {
	}

	@Override
	protected void firePropertyChange(String propertyName, Object oldValue,
			Object newValue) {
	}

	@Override
	public void firePropertyChange(String propertyName, boolean oldValue,
			boolean newValue) {
	}

}
