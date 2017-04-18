package utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Tools for parsing strategy strings into parameters.
 */
public abstract class EncodingUtils {

	/**
	 * @param strategyString a string of the form
	 * STRATEGY_NAME:param1_42_param2_7 . . .
	 * @return everything up to the colon.
	 */
	public static String getStrategyName(final String strategyString) {
		if (strategyString == null) {
			throw new IllegalArgumentException();
		}
		final int colonIndex = strategyString.indexOf(':');
		if (
			colonIndex <= 0
			|| strategyString.indexOf(':', colonIndex + 1) >= 0
		) {
			throw new IllegalArgumentException(strategyString);
		}
		return strategyString.substring(0, colonIndex);
	}
	
	/**
	 * @param strategyString a string of the form
	 * STRATEGY_NAME:param1_42_param2_7 . . .
	 * @return a map of key-value pairs, taken from splitting
	 * the second part of the strategyString (after the colon)
	 * on underbars, taking the first part of each pair as a String
	 * name, and the second part as a Double value.
	 */
	public static Map<String, Double> getStrategyParams(
		final String strategyString
	) {
		if (strategyString == null) {
			throw new IllegalArgumentException();
		}
		final int colonIndex = strategyString.indexOf(':');
		if (
			colonIndex <= 0
			|| strategyString.indexOf(':', colonIndex + 1) >= 0
		) {
			throw new IllegalArgumentException();
		}
		final Map<String, Double> result = new HashMap<String, Double>();
		if (colonIndex == strategyString.length() - 1) {
			// colon is the last character. no params.
			return result;
		}
		final String paramsPart = strategyString.substring(colonIndex + 1);
		final String[] splitParams = paramsPart.split("_");
		if (splitParams.length % 2 == 1) {
			throw new IllegalArgumentException("mismatched param pair");
		}
		for (
			int nameIndex = 0;
			nameIndex + 1 < splitParams.length;
			nameIndex += 2
		) {
			String name = splitParams[nameIndex];
			if (result.containsKey(name)) {
				throw new IllegalArgumentException();
			}
			try {
				Double value = Double.parseDouble(splitParams[nameIndex + 1]);
				result.put(name, value);
			} catch (final NumberFormatException e) {
				e.printStackTrace();
				throw new IllegalArgumentException();
			}
		}
		return result;
	}
}
