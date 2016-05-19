/**
 * Copyright (c) 2010-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.esa2000.internal;

import org.openhab.binding.esa2000.ESA2000BindingProvider;
import org.openhab.core.binding.BindingConfig;
import org.openhab.core.items.Item;
import org.openhab.core.library.items.NumberItem;
import org.openhab.model.item.binding.AbstractGenericBindingProvider;
import org.openhab.model.item.binding.BindingConfigParseException;

/**
 * This class is responsible for parsing the binding configuration.
 *
 * @author sven.schreier
 * @since 1.8.0-SNAPSHOT
 */
public class ESA2000GenericBindingProvider extends AbstractGenericBindingProvider implements ESA2000BindingProvider {

    /**
     * {@inheritDoc}
     */
    @Override
    public String getBindingType() {
        return "esa2000";
    }

    /**
     * @{inheritDoc
     */
    @Override
    public void validateItemType(Item item, String bindingConfig) throws BindingConfigParseException {
        if (!(item instanceof NumberItem)) {
            throw new BindingConfigParseException(
                    "item '" + item.getName() + "' is of type '" + item.getClass().getSimpleName()
                            + "', only NumberItems are allowed - please check your *.items configuration");
        }
    }

    /**
     * Binding config in the style {esa2000="device=AAAA;correctionFactor=NN"}
     * {@inheritDoc}
     */
    @Override
    public void processBindingConfiguration(String context, Item item, String bindingConfig)
            throws BindingConfigParseException {
        super.processBindingConfiguration(context, item, bindingConfig);

        // parse bindingconfig here ...
        final String[] parts = bindingConfig.split(";");
        String device = null;
        double correctionFactor = 0;
        for (String part : parts) {
            String[] keyValue = part.split("=");
            if ("device".equals(keyValue[0])) {
                device = keyValue[1];
            } else if ("correctionFactor".equals(keyValue[0])) {
                correctionFactor = Double.parseDouble(keyValue[1]);
            }
        }

        ESA2000BindingConfig config = new ESA2000BindingConfig(device, correctionFactor, item);
        addBindingConfig(item, config);
    }

    @Override
    public ESA2000BindingConfig getConfigByDevice(String device) {
        for (BindingConfig config : super.bindingConfigs.values()) {
            ESA2000BindingConfig esa2000Config = (ESA2000BindingConfig) config;
            if (esa2000Config.getDevice().equals(device)) {
                return esa2000Config;
            }
        }
        return null;
    }

}
