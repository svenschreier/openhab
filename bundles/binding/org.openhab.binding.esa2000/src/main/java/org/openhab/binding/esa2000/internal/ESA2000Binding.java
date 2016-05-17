/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.esa2000.internal;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.openhab.binding.esa2000.ESA2000BindingProvider;
import org.openhab.core.binding.AbstractBinding;
import org.openhab.core.library.types.DecimalType;
import org.openhab.io.transport.cul.CULLifecycleListenerListenerRegisterer;
import org.openhab.io.transport.cul.CULLifecycleManager;
import org.openhab.io.transport.cul.CULListener;
import org.openhab.io.transport.cul.CULMode;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implement this class if you are going create an actively polling service like
 * querying a Website/Device.
 *
 * @author sven.schreier
 * @since 1.9.0-SNAPSHOT
 */
public class ESA2000Binding extends AbstractBinding<ESA2000BindingProvider>implements ManagedService, CULListener {

    private static final Logger logger = LoggerFactory.getLogger(ESA2000Binding.class);

    private Map<String, Integer> counterMap = new HashMap<String, Integer>();
    private final CULLifecycleManager culHandlerLifecycle;

    public ESA2000Binding() {
        this.culHandlerLifecycle = new CULLifecycleManager(CULMode.SLOW_RF,
                new CULLifecycleListenerListenerRegisterer(this));
    }

    @Override
    public void activate() {
        this.culHandlerLifecycle.open();
    }

    @Override
    public void deactivate() {
        this.culHandlerLifecycle.close();
    }

    @Override
    public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
        this.culHandlerLifecycle.config(properties);
    }

    protected void addBindingProvider(ESA2000BindingProvider bindingProvider) {
        super.addBindingProvider(bindingProvider);
    }

    protected void removeBindingProvider(ESA2000BindingProvider bindingProvider) {
        super.removeBindingProvider(bindingProvider);
    }

    //
    // Private methods
    //

    private void parseData(String data) {
        final String device = ParsingUtils.parseDevice(data);
        if (!checkNewMessage(device, ParsingUtils.parseCounter(data))) {
            logger.warn("Received message from " + device + " more than once");
            return;
        }

        ESA2000BindingConfig config = findConfig(device);
        if (config != null) {
            updateItem(config, ParsingUtils.parseCumulatedValue(data));
        }
    }

    private void updateItem(ESA2000BindingConfig config, int value) {
        DecimalType status = new DecimalType(value / config.getCorrectionFactor());
        eventPublisher.postUpdate(config.getItem().getName(), status);
    }

    private boolean checkNewMessage(String address, int counter) {
        Integer lastCounter = counterMap.get(address);
        if (lastCounter == null) {
            lastCounter = -1;
        }
        if (counter > lastCounter) {
            return true;
        }
        return false;
    }

    private ESA2000BindingConfig findConfig(String device) {
        ESA2000BindingConfig config = null;
        for (ESA2000BindingProvider provider : this.providers) {
            config = provider.getConfigByDevice(device);
            if (config != null) {
                return config;
            }
        }
        return null;
    }

    //
    // CULListener
    //

    @Override
    public void dataReceived(String data) {
        if (!StringUtils.isEmpty(data) && data.startsWith("S")) {
            parseData(data);
            logger.info("ESA2000 data received: " + data);
        }
    }

    @Override
    public void error(Exception e) {
        // TODO Auto-generated method stub
    }
}
