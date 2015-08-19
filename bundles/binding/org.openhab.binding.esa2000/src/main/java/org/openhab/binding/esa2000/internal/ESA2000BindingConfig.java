package org.openhab.binding.esa2000.internal;

import org.openhab.core.binding.BindingConfig;
import org.openhab.core.items.Item;

/**
 * 
 * @author svenschreier
 * 
 */
public class ESA2000BindingConfig implements BindingConfig {

	private String device;
	private String code;
	private double correctionFactor;
	private Item item;

	public ESA2000BindingConfig(String device, String code, double correctionFactor, Item item) {
		this.device = device;
		this.code = code;
		this.correctionFactor = correctionFactor;
		this.item = item;
	}
	
	/**
	 * @return the device
	 */
	public String getDevice() {
		return device;
	}

	/**
	 * @param device
	 *            the device to set
	 */
	public void setDevice(String device) {
		this.device = device;
	}

	/**
	 * @return the code
	 */
	public String getCode() {
		return code;
	}

	/**
	 * @param code
	 *            the code to set
	 */
	public void setCode(String code) {
		this.code = code;
	}

	/**
	 * @return the correctionFactor
	 */
	public double getCorrectionFactor() {
		return correctionFactor;
	}

	/**
	 * @param correctionFactor
	 *            the correctionFactor to set
	 */
	public void setCorrectionFactor(double correctionFactor) {
		this.correctionFactor = correctionFactor;
	}

	/**
	 * @return the item
	 */
	public Item getItem() {
		return item;
	}

	/**
	 * @param item
	 *            the item to set
	 */
	public void setItem(Item item) {
		this.item = item;
	}
}
