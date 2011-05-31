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


package net.schweerelos.parrot.model;

import java.util.Collection;

/**
 * Convenience class for ParrotModelListener so that only those methods need to be overridden that one is interested in.
 * @author as151
 *
 */
public class ParrotModelAdapter implements ParrotModelListener {

	@Override
	public void highlightsChanged() {
	}

	@Override
	public void modelBusy() {
	}

	@Override
	public void modelIdle() {
	}

	@Override
	public void restrictionsChanged(Collection<NodeWrapper> currentlyRestricted) {
	}

}
