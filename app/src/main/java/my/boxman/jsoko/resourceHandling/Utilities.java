/**
 *  JSoko - A Java implementation of the game of Sokoban
 *  Copyright (c) 2016 by Matthias Meger, Germany
 *
 *  This file is part of JSoko.
 *
 *	JSoko is free software; you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation; either version 3 of the License, or
 *	(at your option) any later version.
 *
 *	This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package my.boxman.jsoko.resourceHandling;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.InvalidParameterException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

/**
 * This class provides a mixture of helper functions and tools used throughout JSoko.
 */
public class Utilities {

	/** A table of hex digits (with upper case letters) */
	public static final char[] hexDigit = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

	/**
	 * Avoid instantiation of this class.
	 */
	private Utilities() {
		throw new AssertionError();
	}

	/**
	 * Converts a nibble to a hex character
	 *
	 * @param  nibble	the nibble to convert.
	 * @return character represented by the nibble
	 */
	public final static char toHex(int nibble) {
		return hexDigit[nibble & 0xF];
	}

	/**
	 * Computes a standard representation for the passed date.
	 *
	 * @param date the date to be converted, or <code>null</code> for "now"
	 * @return the date as a string
	 */
	public final static String dateString(Date date) {
		if (date == null) {
			date = new Date();			// now
		}
		return DateFormat.getInstance().format(date);
	}

	/**
	 * Computes a standard representation for "now".
	 * @return the current date and time as a string
	 */
	public final static String nowString() {
		return dateString(null);
	}

	/**
	 * Creates and returns a string consisting from {@code repcnt} copies
	 * of the specified string.
	 * A {@code null} string is replaced by an empty string.
	 * We do never return a {@code null}, not even when we get one.
	 *
	 * @param repcnt the number of copies we want
	 * @param toRep  the string to be repeated
	 * @return string built from repeated copies
	 */
	public static final String makeRepStr(int repcnt, String toRep) {
		if (repcnt <= 0) {
			return "";
		}
		if (toRep == null) {
			toRep = "";
		}
		if (repcnt == 1) {
			return toRep;
		}
		if (toRep.length() <= 0) {
			return "";
		}
		// repcnt>=2  |toRep|>=1
		int len = repcnt * toRep.length();
		StringBuilder sb = new StringBuilder(len);
		for (int i = 0; i < repcnt; i++) {
			sb.append(toRep);
		}
		return sb.toString();
	}

	/**
	 * The maximal length of a blank string created by {@link #blankStr(int)}.
	 */
	public static final int MAXLEN_BLANK_STR = 999;

	/**
	 * Here we cache some small blank strings.
	 * While this is not strictly necessary for performance, it is not bad.
	 * Consider it a demo cache implementation.
	 */
	private static final String[] cachedBlanksStrs = new String[20];

	/**
	 * Return a String containing just the specified amount of blanks.
	 * This is thought of as kind of statistics support, and so we
	 * deliberately limit the length to at most {@value #MAXLEN_BLANK_STR}.
	 *
	 * @param len how many blanks we want
	 * @return String with that many blanks
	 */
	public static final String blankStr(int len) {
		if (len <= 0) {
			return "";
		}
		if (len > MAXLEN_BLANK_STR) {
			len = MAXLEN_BLANK_STR;
		}

		String s = null;

		// Check the cache, whether the string is there, already...
		if (len < cachedBlanksStrs.length) {
			synchronized (cachedBlanksStrs) {
				s = cachedBlanksStrs[len];
				if (s != null) {
					return s;
				}
			}
		}

		// Really build the string
		s = makeRepStr(len, " ");

		// Check whether we want to feed the cache with it ...
		if (len < cachedBlanksStrs.length) {
			synchronized (cachedBlanksStrs) {
				String x = cachedBlanksStrs[len];
				if (x != null) {
					// Someone else was faster than we.
					// We use the early result, avoiding yet another copy
					s = x;
				} else {
					cachedBlanksStrs[len] = s;
				}
			}
		}

		return s;
	}

	/**
	 * Fill a string with blanks at the left side
	 * to reach the indicated length.
	 * @param len minimum string length wanted
	 * @param str string to be augmented (at left side)
	 * @return eventually augmented string
	 */
	public static final String fillL(int len, String str) {
		if (str == null) {
			str = "";
		}
		if (len > str.length()) {
			str = blankStr(len - str.length()) + str;
		}
		return str;
	}

	/**
	 * Statistics support: computes a quotient {@code (part / total)}
	 * as a {@code double}, avoiding division by zero.
	 *
	 * @param part
	 * @param total
	 * @return 0.0, or part / total
	 * @see Utilities#percOf(long, long)
	 */
	public static final double partOf(long part, long total) {
		if (total == 0) {
			return 0.0;
		}
		return (double)part / (double)total;
	}

	/**
	 * Statistics support: computes a percentage
	 * as a {@code double}, avoiding division by zero.
	 * @param part
	 * @param total
	 * @return 0.0, or 100 * (part / total)
	 */
	public static final double percOf(long part, long total) {
		return 100.0 * partOf(part, total);
	}

	/**
	 * Statistics support: computes a percentage and converts it into
	 * a standard String of fixed length, like {@code " 97.12%"}.
	 * @param part
	 * @param total
	 * @return standard string for a percentage
	 */
	public static final String percStr(long part, long total) {
		return String.format("%6.2f%%", percOf(part, total));
	}

	/**
	 * Statistics support: computes a percentage and converts it into
	 * a standard String of fixed length, with a standard decoration,
	 * like {@code " ( 97.12%)"}.
	 * @param part
	 * @param total
	 * @return standard string for a percentage with decoration
	 */
	public static final String percStrEmb(long part, long total) {
		return String.format(" (%6.2f%%)", percOf(part, total));
	}

	/**
	 * A simple (debug) method to record a code location along with
	 * the current time (in ms).
	 * @param location number representing the code location
	 */
	public static final void loggTime(int location) {
		loggTime(location, "");
	}

	/**
	 * A simple (debug) method to record a code location along with
	 * the current time (in ms).
	 * @param location number representing the code location
	 * @param funcname function name within which this event happens
	 */
	public static final void loggTime(int location, String funcname) {
		log_Time(location, funcname);
	}

	private static final void log_Time(int location, String funcname) {
		final int loclen = 6;

		if (funcname == null) {
			funcname =  "";
		}

		String locstr = fillL(loclen, "" + location);

		System.out.println("" + System.currentTimeMillis()
				           + " " + locstr
				           + " " + funcname );
	}

	/**
	 * Computes the maximum amount of RAM (in bytes) the program may use.
	 * <p>
	 * This computation can be a bit confusing, so I explain it:<br>
	 * - The <code>freeMemory</code> is that part of
	 *   the <code>totalMemory</code>, which is just now
	 *   not allocated to some object.  Running the GC
	 *   may change this value.<br>
	 * - The <code>totalMemory</code> is what the JVM currently
	 *   has obtained from the OS for user objects.<br>
	 * - The <code>maxMemory</code> is the (upper) limit for
	 *   the <code>totalMemory</code>, which the JVM will never exceed.
	 *   If there is no limit, we get <code>Long.MAX_VALUE</code>.
	 * <p>
	 * Obviously, we want to include <code>freeMemory</code>.
	 * Adding <code>(maxMemory - totalMemory)</code> is the attempt
	 * to add that amount, which the JVM later will also allocate
	 * from the OS, in case we demand more memory.
	 *
	 * @return the maximum amount of RAM the program may use
	 * @see #getMaxUsableRAMinMiB()
	 */
	public static long getMaxUsableRAMInBytes() {
		Runtime rt = Runtime.getRuntime();
		long usableRAM = 0;

		usableRAM += rt.freeMemory();						// available just now
		usableRAM += rt.maxMemory() - rt.totalMemory();		// future potential
		return usableRAM;
	}

	/**
	 * Computes the maximum amount of RAM (in MiB) the program may use.
	 * @return  the maximum amount of MiB the program may use
	 * @see #getMaxUsableRAMInBytes()
	 */
	public static long getMaxUsableRAMinMiB() {
		return getMaxUsableRAMInBytes() / (1024 * 1024);
	}

	/**
	 * Normalize the result of a comparison
	 * (e.g. from {@link Comparable#compareTo(Object)}) to -1|0|+1.
	 * @param cmpres the comparison result so far
	 * @return the normalized comparison result: -1|0|+1.
	 */
	public static final int normCompareResult(int cmpres) {
		return Integer.signum(cmpres);
	}

	/**
	 * Compare two integer values in naive ascending order.
	 * Equivalent to {@code new Int(x).compareTo(y)}.
	 *
	 * @param x  first  value to compare
	 * @param y  second value to compare
	 * @return <code>-1</code> if x is less than    y,
	 *     <br><code> 0</code> if x is equal to     y and
	 *     <br><code>+1</code> if x is greater than y.
	 */
	public static final int intCompare1Pair(int x, int y) {
		if (x != y) {
			return (x < y) ? -1 : +1;
		}
		return 0;			// no difference detected
	}

	/**
	 * Compare two integer value sequences of length 2, in naive dictionary order.
	 * The element values of the vectors "x" and "y" are given in pairs,
	 * highest comparison order elements first.
	 *
	 * @param x1 first value of x vector
	 * @param y1 first value of y vector
	 * @param x2 second value of x vector
	 * @param y2 second value of y vector
	 * @return <code>-1</code> if (x1,x2) is less than    (y1,y2),
	 *     <br><code> 0</code> if (x1,x2) is equal to     (y1,y2) and
	 *     <br><code>+1</code> if (x1,x2) is greater than (y1,y2).
	 */
	public static final int intCompare2Pairs(int x1, int y1, int x2, int y2) {
		if (x1 != y1) {
			// This high pair is going to decide it...
			return (x1 < y1) ? -1 : +1;
		}
		if (x2 != y2) {
			// This second pair is going to decide it...
			return (x2 < y2) ? -1 : +1;
		}
		return 0;			// no difference detected
	}

	/**
	 * Compare two integer value sequences of length 3, in naive dictionary order.
	 * Values are given in pairs, highest order first.
	 * The element values of the vectors "x" and "y" are given in pairs,
	 * highest comparison order elements first.
	 *
	 * @param x1 first value of x vector
	 * @param y1 first value of y vector
	 * @param x2 second value of x vector
	 * @param y2 second value of y vector
	 * @param x3 third value of x vector
	 * @param y3 third value of y vector
	 * @return <code>-1</code> if (x1,x2,x3) is less than    (y1,y2,y3),
	 *     <br><code> 0</code> if (x1,x2,x3) is equal to     (y1,y2,y3) and
	 *     <br><code>+1</code> if (x1,x2,x3) is greater than (y1,y2,y3).
	 */
	public static final int intCompare3Pairs(int x1, int y1, int x2, int y2, int x3, int y3) {
		if (x1 != y1) {
			// This high pair is going to decide it...
			return (x1 < y1) ? -1 : +1;
		}
		if (x2 != y2) {
			// This second pair is going to decide it...
			return (x2 < y2) ? -1 : +1;
		}
		if (x3 != y3) {
			// This third pair is going to decide it...
			return (x3 < y3) ? -1 : +1;
		}

		return 0;			// no difference detected
	}

	/**
	 * Compare two integer value sequences of length 4, in naive dictionary order.
	 * Values are given in pairs, highest order first.
	 * The element values of the vectors "x" and "y" are given in pairs,
	 * highest comparison order elements first.
	 *
	 * @param x1 first value of x vector
	 * @param y1 first value of y vector
	 * @param x2 second value of x vector
	 * @param y2 second value of y vector
	 * @param x3 third value of x vector
	 * @param y3 third value of y vector
	 * @param x4 fourth value of x vector
	 * @param y4 fourth value of y vector
	 * @return <code>-1</code> if (x1,x2,x3,x4) is less than    (y1,y2,y3,y4),
	 *     <br><code> 0</code> if (x1,x2,x3,x4) is equal to     (y1,y2,y3,y4) and
	 *     <br><code>+1</code> if (x1,x2,x3,x4) is greater than (y1,y2,y3,y4).
	 */
	public static final int intCompare4Pairs( int x1, int y1,
			                                  int x2, int y2,
			                                  int x3, int y3,
			                                  int x4, int y4 )
	{
		if (x1 != y1) {
			// This high pair is going to decide it...
			return (x1 < y1) ? -1 : +1;
		}
		if (x2 != y2) {
			// This second pair is going to decide it...
			return (x2 < y2) ? -1 : +1;
		}
		if (x3 != y3) {
			// This third pair is going to decide it...
			return (x3 < y3) ? -1 : +1;
		}
		if (x4 != y4) {
			// This fourth pair is going to decide it...
			return (x4 < y4) ? -1 : +1;
		}

		return 0;			// no difference detected
	}

	/**
	 * Compares two integer value sequences in naive dictionary order.
	 * Values are given in pairs, highest order first.
	 * <p>
	 * Example:<br>
	 * intComparePairs(5, 5, 6, 17, 3, 2)<br>
	 * first compares 5 with 5 and since they are equal, compares 6 with 17.
	 * Since 6 is less than 17 "-1" is returned.
	 * <p>
	 * An even number of parameters must be passed to this method, otherwise
	 * an {@code InvalidParameterException} is thrown.
	 *
	 * @param valuePairs  the values to be compared (a1, a2, b1, b2, c1, c2, ...)
	 * @return <code>-1</code> if a1 < a2 or a1 == a2 and b1 < b2 or ...
	 *     <br><code> 0</code> if a1 == a2 and b1 == b2 and ...
	 *     <br><code>+1</code> if a1 > a2 or a1 == a2 and b1 > b2 or ...
	 */
	public static final int intComparePairs(int ... valuePairs) {
		if((valuePairs.length&1) != 0) {
			throw new InvalidParameterException();
		}

		for(int i=0; i<valuePairs.length; i+=2) {
			if(valuePairs[i] != valuePairs[i+1]) {
				return valuePairs[i] < valuePairs[i+1] ? -1 : +1;
			}
		}

		return 0;
	}

	/**
	 * Converts the passed collection to an integer array.
	 *
	 * @param coll the collection to convert
	 * @return an integer array containing the content of the passed collection
	 */
	public static int[] toIntArray(Collection<Integer> coll) {
		Iterator<Integer> iter = coll.iterator();
		int[] arr = new int[coll.size()];
		int i = 0;
		while (iter.hasNext()) {
			arr[i++] = iter.next().intValue();
		}
		return arr;
	}

    /**
     * Shuts down the passed {@code ExecutorService} and waits until all tasks have terminated.
     *
     * @param executor {@code ExecutorService} to shutdown and wait for termination
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     */
	public static void shutdownAndAwaitTermination(ExecutorService executor, long timeout, TimeUnit unit) {

		// Disable new tasks from being submitted
		executor.shutdown();

		try {
			// Wait for existing tasks to terminate.
			if (!executor.awaitTermination(timeout, unit)) {
				executor.shutdownNow(); // Cancel currently executing tasks
				// Wait a while for tasks to respond to being cancelled
				executor.awaitTermination(3, TimeUnit.SECONDS);
				return;
			}
		} catch (InterruptedException ie) {
			// (Re-)Cancel if current thread also interrupted
			executor.shutdownNow();

			try {
				executor.awaitTermination(timeout, unit);
			} catch (InterruptedException e) { }

			// Preserve interrupt status
			Thread.currentThread().interrupt();
		}
	}

    /**
     * Returns the name of the file corresponding to the passed file path.
     *
     * @param filePath the path to the file
     * @return the file name
     */
    public static String getFileName(String filePath) {
    	if(filePath != null) {
    		int indexOfFileSeparator = filePath.lastIndexOf(File.separator);
    		if(indexOfFileSeparator == -1) {
    			indexOfFileSeparator = filePath.lastIndexOf("/");
    		}
    		String fileNameWithoutPath = (indexOfFileSeparator == -1) ? filePath : filePath.substring(++indexOfFileSeparator);
    		filePath = fileNameWithoutPath;
    	}

    	return filePath;
    }

    /**
     * Fills a two dimensional array with the given value.
     *
     * @param array  the array to be filled
     * @param value  the value to be set
     */
    public static void fillArray(int[][] array, int value) {
        for(int[] subarray : array) {
            Arrays.fill(subarray, value);
        }
    }

    /**
     * Fills a three dimensional array with the given value.
     *
     * @param array  the array to be filled
     * @param value  the value to be set
     */
    public static void fillArray(int[][][] array, int value) {
        for(int[][] subarray : array) {
        	 for(int[] subarray2 : subarray) {
                 Arrays.fill(subarray2, value);
             }
        }
    }

    /**
     * Fills a two dimensional array with the given value.
     *
     * @param array  the array to be filled
     * @param value  the value to be set
     */
    public static void fillArray(short[][] array, short value) {
        for(short[] subarray : array) {
            Arrays.fill(subarray, value);
        }
    }

    /**
     * Fills a three dimensional array with the given value.
     *
     * @param array  the array to be filled
     * @param value  the value to be set
     */
    public static void fillArray(short[][][] array, short value) {
        for(short[][] subarray : array) {
        	 for(short[] subarray2 : subarray) {
                 Arrays.fill(subarray2, value);
             }
        }
    }

    /**
     * Removes all trailing space characters.
     *
     * @param data the data to remove trailing spaces from
     */
    public static void removeTrailingSpaces(StringBuilder data) {
		int lastIndex = data.length() - 1;
		while(lastIndex > 0 && data.charAt(lastIndex) == ' ') {
			lastIndex--;
		}
		data.setLength(lastIndex+1);
    }

    /**
     * Creates the given directory if it does not exist, otherwise expects
     * it to be writable.
     *
     * @param directory the <code>File</code> specifying the required directory
     * @return the required directory, or <code>null</code> on failure
     */
    public static File createDiretory(String directoryPath) {

    	if(directoryPath == null) {
			return null;
		}

    	File directory = new File(directoryPath);
        if (directory.exists()) {
            if (directory.isDirectory() && directory.canWrite()) {
				return directory;
			}
        } else {
            if (directory.mkdir()) {
				return directory;
			}
        }
        return null;
    }

}