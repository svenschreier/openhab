package org.openhab.binding.plugwise.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caches all {@link PlugwiseDevice} instances and allows for querying them by mac, name and device class.
 *
 * @author Wouter Born
 * @since 1.9.0
 */
public class PlugwiseDeviceCache {

    private Map<String, PlugwiseDevice> macDeviceMapping = new ConcurrentHashMap<String, PlugwiseDevice>();
    private Map<String, PlugwiseDevice> nameDeviceMapping = new ConcurrentHashMap<String, PlugwiseDevice>();

    public void add(PlugwiseDevice device) {
        macDeviceMapping.put(device.getMAC(), device);
        nameDeviceMapping.put(device.getName(), device);
    }

    public void clear() {
        macDeviceMapping.clear();
        nameDeviceMapping.clear();
    }

    public PlugwiseDevice get(String id) {
        PlugwiseDevice device = getByMAC(id);
        if (device == null) {
            return getByName(id);
        } else {
            return device;
        }
    }

    public PlugwiseDevice getByMAC(String mac) {
        return macDeviceMapping.get(mac);
    }

    public PlugwiseDevice getByName(String name) {
        return nameDeviceMapping.get(name);
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> getByClass(Class<T> deviceClass) {

        List<T> result = new ArrayList<T>();
        for (PlugwiseDevice device : macDeviceMapping.values()) {
            if (deviceClass.isAssignableFrom(device.getClass())) {
                result.add((T) device);
            }
        }

        return result;
    }
}
