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
		return data.substring(3, 7);
	}

	/**
	 * Get the counter information from the received data.
	 * 
	 * @param data
	 * @return integer representation of the packet count
	 */
	public static int parseCounter(String data) {
		final String counterData = data.substring(1, 3);
		return Integer.parseInt(counterData, 16);
	}

	/**
	 * Get the cumulated value.
	 * 
	 * @param data
	 * @return the cumulated value as integers
	 */
	public static int parseCumulatedValue(String data) {
		return getIntFromChars(data.charAt(11), data.charAt(12), data.charAt(13),
				data.charAt(14), data.charAt(15), data.charAt(16),
				data.charAt(17), data.charAt(18));
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
