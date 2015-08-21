/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.esa2000.internal;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.openhab.binding.esa2000.ESA2000BindingProvider;
import org.openhab.core.binding.AbstractActiveBinding;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.io.transport.cul.CULDeviceException;
import org.openhab.io.transport.cul.CULHandler;
import org.openhab.io.transport.cul.CULListener;
import org.openhab.io.transport.cul.CULManager;
import org.openhab.io.transport.cul.CULMode;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implement this class if you are going create an actively polling service like
 * querying a Website/Device.
 * 
 * @author sven.schreier
 * @since 1.8.0-SNAPSHOT
 */
public class ESA2000Binding extends
		AbstractActiveBinding<ESA2000BindingProvider> implements CULListener {

	private static final Logger logger = LoggerFactory
			.getLogger(ESA2000Binding.class);

	private final static String CONFIG_KEY_DEVICE_NAME = "device";
	private final static String CONFIG_KEY_SOCKET_PORT = "serverport";
	private final static String CONFIG_KEY_IP = "ip";

	private String deviceName;
	private Map<String, Integer> counterMap = new HashMap<String, Integer>();
	private CULHandler cul;
	private CULNetworkProxyService culProxy = null;

	/**
	 * The BundleContext. This is only valid when the bundle is ACTIVE. It is
	 * set in the activate() method and must not be accessed anymore once the
	 * deactivate() method was called or before activate() was called.
	 */
	private BundleContext bundleContext;

	/**
	 * the refresh interval which is used to poll values from the ESA2000 server
	 * (optional, defaults to 60000ms)
	 */
	private long refreshInterval = 60000;

	public ESA2000Binding() {
	}

	/**
	 * Called by the SCR to activate the component with its configuration read
	 * from CAS
	 * 
	 * @param bundleContext
	 *            BundleContext of the Bundle that defines this component
	 * @param configuration
	 *            Configuration properties for this component obtained from the
	 *            ConfigAdmin service
	 */
	public void activate(final BundleContext bundleContext,
			final Map<String, Object> configuration) {
		this.bundleContext = bundleContext;

		// the configuration is guaranteed not to be null, because the component
		// definition has the
		// configuration-policy set to require. If set to 'optional' then the
		// configuration may be null

		// to override the default refresh interval one has to add a
		// parameter to openhab.cfg like <bindingName>:refresh=<intervalInMs>
		String refreshIntervalString = (String) configuration.get("refresh");
		if (StringUtils.isNotBlank(refreshIntervalString)) {
			refreshInterval = Long.parseLong(refreshIntervalString);
		}

		// read further config parameters here ...
		String deviceName = (String) configuration.get(CONFIG_KEY_DEVICE_NAME);
		if (StringUtils.isNotBlank(deviceName)) {
			setNewDeviceName(deviceName);
		}

		// enable cul proxy
		String serverSocketPort = (String) configuration
				.get(CONFIG_KEY_SOCKET_PORT);
		if (StringUtils.isNotBlank(serverSocketPort)) {
			final int serverPort = Integer.parseInt(serverSocketPort);
			this.culProxy = new CULNetworkProxyService(serverPort, this);
		}

		setProperlyConfigured(true);
	}

	/**
	 * Called by the SCR when the configuration of a binding has been changed
	 * through the ConfigAdmin service.
	 * 
	 * @param configuration
	 *            Updated configuration properties
	 */
	public void modified(final Map<String, Object> configuration) {
		// update the internal configuration accordingly
	}

	/**
	 * Called by the SCR to deactivate the component when either the
	 * configuration is removed or mandatory references are no longer satisfied
	 * or the component has simply been stopped.
	 * 
	 * @param reason
	 *            Reason code for the deactivation:<br>
	 *            <ul>
	 *            <li>0 – Unspecified
	 *            <li>1 – The component was disabled
	 *            <li>2 – A reference became unsatisfied
	 *            <li>3 – A configuration was changed
	 *            <li>4 – A configuration was deleted
	 *            <li>5 – The component was disposed
	 *            <li>6 – The bundle was stopped
	 *            </ul>
	 */
	public void deactivate(final int reason) {
		this.bundleContext = null;
		// deallocate resources here that are no longer needed and
		// should be reset when activating this binding again

		closeCUL();
	}

	/**
	 * @{inheritDoc
	 */
	@Override
	protected long getRefreshInterval() {
		return refreshInterval;
	}

	/**
	 * @{inheritDoc
	 */
	@Override
	protected String getName() {
		return "ESA2000 Refresh Service";
	}

	/**
	 * @{inheritDoc
	 */
	@Override
	protected void execute() {
		// the frequently executed code (polling) goes here ...
		logger.debug("execute() method is called!");
	}

	/**
	 * @{inheritDoc
	 */
	@Override
	protected void internalReceiveCommand(String itemName, Command command) {
		// the code being executed when a command was sent on the openHAB
		// event bus goes here. This method is only called if one of the
		// BindingProviders provide a binding for the given 'itemName'.
		logger.debug("internalReceiveCommand({},{}) is called!", itemName,
				command);
	}

	/**
	 * @{inheritDoc
	 */
	@Override
	protected void internalReceiveUpdate(String itemName, State newState) {
		// the code being executed when a state was sent on the openHAB
		// event bus goes here. This method is only called if one of the
		// BindingProviders provide a binding for the given 'itemName'.
		logger.debug("internalReceiveUpdate({},{}) is called!", itemName,
				newState);
	}

	private void closeCUL() {
		if (cul != null) {
			cul.unregisterListener(this);
			CULManager.close(cul);
		}
	}

	/**
	 * If the device name has changed, try to close the old device handler and
	 * create a new one
	 * 
	 * @param deviceName
	 *            The new deviceName
	 */
	private void setNewDeviceName(String deviceName) {
		if (deviceName != null && this.deviceName != null
				&& this.deviceName.equals(deviceName)) {
			return;
		}
		if (this.deviceName != null && cul != null) {
			closeCUL();
		}
		try {
			cul = CULManager.getOpenCULHandler(deviceName, CULMode.SLOW_RF);
			cul.registerListener(this);
			this.deviceName = deviceName;
		} catch (CULDeviceException e) {
			logger.error("Can't get CULHandler", e);
		}
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

		// when enabled acting as cul proxy in the server role we want to send
		// all received data to our connected clients
		if (this.culProxy != null) {
			// this.culProxy.send(data);
		}
	}

	@Override
	public void error(Exception e) {
		// TODO Auto-generated method stub

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
		DecimalType status = new DecimalType(value
				/ config.getCorrectionFactor());
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

}
