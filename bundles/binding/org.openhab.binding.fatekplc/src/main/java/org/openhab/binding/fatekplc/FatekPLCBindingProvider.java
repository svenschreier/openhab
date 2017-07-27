/**
 * Copyright (c) 2010-2016, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.fatekplc;

import org.openhab.binding.fatekplc.items.FatekPLCItem;
import org.openhab.core.binding.BindingProvider;

/**
 * Binding provider interface for Fatek PLC
 * @author Slawomir Jaranowski
 * @since 1.9.0
 */
public interface FatekPLCBindingProvider extends BindingProvider {

	/**
	 * Get Fatek PLC item by item name
	 * @param name item name
	 * @return FatekPLCItem
	 */
	FatekPLCItem geFatektItem(String name);

}
