/**
 * Copyright (c) 2010-2016, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.mcp3424;

import org.openhab.core.binding.BindingProvider;
import org.openhab.core.items.Item;

import com.pi4j.io.gpio.Pin;

/**
 * @author Alexander Falkenstern
 * @since 1.9.0
 */
public interface MCP3424BindingProvider extends BindingProvider {

    /**
     * @return Bus address of ADC
     */
    public int getBusAddress(String itemName);

    /**
     * @return Pin
     */
    public Pin getPin(String itemName);

    /**
     * @return Gain of ADC amplifier
     */
    public int getGain(String itemName);

    /**
     * @return Resolution of conversion
     */
    public int getResolution(String itemName);

    /**
     * @return Configured item
     */
    public Item getItem(String itemName);
}
