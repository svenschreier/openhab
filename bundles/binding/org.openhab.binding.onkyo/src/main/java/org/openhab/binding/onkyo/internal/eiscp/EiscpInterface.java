/**
 * Copyright (c) 2010-2016, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.onkyo.internal.eiscp;

import org.openhab.binding.onkyo.internal.OnkyoEventListener;

/**
 * <p>
 * <b>Note:</b>Created this interface to handle both ethernet and serial
 * connections to device
 * </p>
 * <br>
 *
 * @author Sriram Balakrishnan
 * @since 1.9.0
 */
public interface EiscpInterface {

    /**
     * Add event listener, which will be invoked when status upadte is received
     * from receiver.
     **/
    public void addEventListener(OnkyoEventListener listener);

    /**
     * Remove event listener.
     **/
    public void removeEventListener(OnkyoEventListener listener);

    /**
     * Get retry count value.
     **/
    public int getRetryCount();

    /**
     * Set retry count value. How many times command is retried when error
     * occurs.
     **/
    public void setRetryCount(int retryCount);

    /**
     * Connects to the receiver by opening a socket connection through the IP
     * and port defined on constructor.
     **/
    public boolean connectSocket();

    /**
     * Closes the socket connection.
     *
     * @return true if the closed successfully
     **/
    public boolean closeSocket();

    /**
     * Sends to command to the receiver. It does not wait for a reply.
     *
     * @param eiscpCmd
     *            the eISCP command to send.
     **/
    public void sendCommand(String eiscpCmd);

}
