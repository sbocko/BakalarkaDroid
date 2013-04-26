package eu.jergus.crypto.util;

import java.math.BigInteger;

/**
 * Provides applications with simple logging capabilities.
 */
public class Log {

	/**
	 * Errors and warnings.
	 */
	public static final int WARN = 0;

	/**
	 * Informational messages.
	 */
	public static final int INFO = 1;

	/**
	 * Results of calculations.
	 */
	public static final int CALC = 2;

	/**
	 * Sent messages.
	 */
	public static final int SEND = 3;

	/**
	 * Received messages.
	 */
	public static final int RECV = 4;


	private static final String[] LABELS = {
		"WARN", "INFO", "CALC", "SEND", "RECV"
	};

	private static final String[] COLORS = {
		"\033[1;31m", "\033[1;35m", "\033[1;33m", "\033[1;34m", "\033[1;32m"
	};


	private static boolean color = true;
	private static int level = 0;

	/**
	 * Enable or disable color output for Linux consoles.
	 *
	 * @param color  true to enable (default)
	 */
	synchronized public static void setColor(boolean color) {
		Log.color = color;
	}

	/**
	 * Set logging level.
	 *
	 * @param level  highest level of message that should be output
	 */
	synchronized public static void setLevel(int level) {
		Log.level = level;
	}


	/**
	 * Output the specified message, labelled and optionally colored according
	 * to the given message type.
	 */
	synchronized public static void log(int type, String message) {
		if (type < level) {
			if (color) System.err.print(COLORS[type]);
			System.err.print("[" + LABELS[type] + "] ");

			System.err.print(message);

			if (color) System.err.print("\033[m");
			System.err.println();
		}
	}


	/**
	 * Output the specified message as an informational message, not ending the
	 * current line of output.
	 */
	synchronized public static void print(String message) {
		if (level > INFO) {
			System.err.print(message);
		}
	}


	/**
	 * Output the specified message as an informational message.
	 */
	synchronized public static void println(String message) {
		if (level > INFO) {
			System.err.println(message);
		}
	}


	/**
	 * Format a BigInteger value for user-friendly output.
	 * <p>
	 * For large numbers, this only shows the first and last few digits.
	 */
	public static String bigInteger(BigInteger value) {
		String res = value.toString();
		if (res.length() > 20) {
			res = res.substring(0, 8) + "..." + res.substring(res.length() - 8);
			res += " (" + value.bitLength() + " bits)";
		}
		return res;
	}

}