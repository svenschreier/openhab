/**
 * Copyright (c) 2010-2016, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.esa2000;

import org.openhab.binding.esa2000.internal.ESA2000BindingConfig;
import org.openhab.core.binding.BindingProvider;

/**
 * @author sven.schreier
 * @since 1.9.0
 */
public interface ESA2000BindingProvider extends BindingProvider {
    public ESA2000BindingConfig getConfigByDevice(String device);
}
