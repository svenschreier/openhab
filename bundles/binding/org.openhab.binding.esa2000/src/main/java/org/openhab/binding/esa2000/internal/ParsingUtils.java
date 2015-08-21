package org.openhab.binding.esa2000.internal;

public class ParsingUtils {
	/**
	 * Get the device address in the received data.
	 * 
	 * @param data
	 *            the received String
	 * @return the device address as String
	 */
	public static String parseDevice(String data) {
		return data.substring(2, 6);
	}

	/**
	 * Get the counter information from the received data.
	 * 
	 * @param data
	 * @return integer representation of the packet count
	 */
	public static int parseCounter(String data) {
		return Integer.parseInt(data.substring(0, 2), 16);
	}

	/**
	 * Get the cumulated value.
	 * 
	 * @param data
	 * @return the cumulated value as integers
	 */
	public static int parseCumulatedValue(String data) {
		return getIntFromChars(data.charAt(8), data.charAt(9), data.charAt(10),
				data.charAt(11), data.charAt(12), data.charAt(13),
				data.charAt(14), data.charAt(15));
	}

	//
	// Private methods.
	//

	private static int getIntFromChars(char... chars) {
		StringBuffer buffer = new StringBuffer(chars.length);
		for (int i = 0; i < chars.length; i++) {
			buffer.append(chars[i]);
		}
		return Integer.parseInt(buffer.toString(), 16);
	}
}
