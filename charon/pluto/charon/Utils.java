package pluto.charon;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Vector;

public class Utils {

	public final static SimpleDateFormat dateFormater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	final static String[] emptyStringArray = new String[0];

	/** convert String to long or null */
	public static Long toLong(String str) {
		Long res = null;
		if (str != null) {
			try {
				res = Long.parseLong(str, 10);
			} catch (Exception e) {}
		}
		return res;
	}

	/** convert String to long or null */
	public static Integer toInteger(String str) {
		Integer res = null;
		if (str != null) {
			try {
				res = Integer.parseInt(str, 10);
			} catch (Exception e) {}
		}
		return res;
	}

	/** return occurrences of separator in string */
	public static int count(final String string, final char separator) {
		int count = 0;
		for (int i = 0, len = string.length(); i < len; i++) {
			final char c = string.charAt(i);
			if (c == separator) count++;
		}
		return count;
	}

	/** tokenize string by separator without empties */
	public static String[] split(final String string, final char separator) {
		if (string == null || string.length() <= 0) {
			return emptyStringArray;
		}
		int count = count(string, separator);
		if (count <= 0) {
			return new String[] { string };
		}

		Vector<String> tokens = new Vector<String>(count + 2);
		int i = 0, len = string.length(), s = 0;
		for (; i < len; i++) {
			final char c = string.charAt(i);
			if (c == separator) {
				if (i - s > 0) {
					final String addstr = string.substring(s, i).trim();
					if (addstr.length() > 0) tokens.addElement(addstr);
				}
				s = i + 1;
			}
		}
		// if there is remaining string - add it
		if (len - s > 0) {
			final String addstr = string.substring(s, i).trim();
			if (addstr.length() > 0) tokens.addElement(addstr);
		}

		final String[] result = new String[tokens.size()];
		tokens.copyInto(result);
		return result;
	}

	/** tokenize string by separator with empties */
	public static String[] tokenize(final String string, final char separator) {
		if (string == null || string.length() <= 0) {
			return emptyStringArray;
		}
		int count = count(string, separator);
		if (count <= 0) {
			return new String[] { string };
		}

		Vector<String> tokens = new Vector<String>(count + 2);
		int i = 0, len = string.length(), s = 0;
		for (; i < len; i++) {
			final char c = string.charAt(i);
			if (c == separator) {
				final String addstr = string.substring(s, i).trim();
				tokens.addElement(addstr);
				s = i + 1;
			}
		}
		final String addstr = string.substring(s, i).trim();
		tokens.addElement(addstr);

		final String[] result = new String[tokens.size()];
		tokens.copyInto(result);
		return result;
	}

	/**
	 * converts "YYYY-MM-DD" to Date. On error it returns null
	 */
	public static Date stringToDate(String date) {
		if (date == null || date.length() < 10 || date.charAt(4) != '-' || date.charAt(7) != '-') {
			return null;
		}
		try {
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(0);
			if (date.length() >= 8) {
				calendar.set(Calendar.YEAR, Integer.parseInt(date.substring(0, 4)));
				calendar.set(Calendar.MONTH, Integer.parseInt(date.substring(5, 7)) - 1);
				calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(date.substring(8, 10)));
			}
			{
				calendar.set(Calendar.HOUR_OF_DAY, 0);
				calendar.set(Calendar.MINUTE, 0);
				calendar.set(Calendar.SECOND, 0);
			}
			return calendar.getTime();

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * converts Date to "YYYY-MM-DD".
	 */
	public static String dateToString(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		StringBuilder result = new StringBuilder();

		result.append(calendar.get(Calendar.YEAR));
		result.append('-');

		int month = calendar.get(Calendar.MONTH) + 1;
		if (month <= 9) result.append('0');
		result.append(month);
		result.append('-');

		int day = calendar.get(Calendar.DAY_OF_MONTH);
		if (day <= 9) result.append('0');
		result.append(day);

		return result.toString();
	}

	/**
	 * move file to the : directory/system/YYYY/MM/DD/file .<br/>
	 * if file exists. adds ".new" before the extension. if timeStamp is not
	 * given(null) moves it to directory/system
	 * 
	 * @param file
	 * @param directory
	 * @param system
	 * @param timeStamp
	 * @return
	 */
	public static boolean moveFileToDirWithTimestamp(final File file, String directory, final String system,
			final Calendar timeStamp) {
		StringBuilder pathName = new StringBuilder(directory.length() + 1024);
		pathName.append(directory);
		if (system != null && system.length() > 0) {
			pathName.append(system);
			pathName.append(File.separatorChar);
		}

		if (timeStamp != null) {
			pathName.append(timeStamp.get(Calendar.YEAR));
			pathName.append(File.separatorChar);
			final int month = timeStamp.get(Calendar.MONTH) + 1;
			if (month <= 9) {
				pathName.append('0');
			}
			pathName.append(month);
			pathName.append(File.separatorChar);
			final int day = timeStamp.get(Calendar.DAY_OF_MONTH);
			if (day <= 9) {
				pathName.append('0');
			}
			pathName.append(day);
			final File dir = new File(pathName.toString());
			if (!dir.exists() && !dir.mkdirs()) {
				return false;
			}
			pathName.append(File.separatorChar);

		} else {
			final File dir = new File(pathName.toString());
			if (!dir.exists() && !dir.mkdirs()) {
				return false;
			}
		}
		pathName.append(file.getName());

		File destinationFile = new File(pathName.toString());
		while (destinationFile.exists()) {
			pathName.insert(pathName.length() - 3, "new.");
			destinationFile = new File(pathName.toString());
		}
		return file.renameTo(destinationFile);
	}

	/**
	 * return tomorrow in milliseconds
	 */
	public static long tomorrow() {
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		calendar.add(Calendar.DAY_OF_MONTH, 1);

		return calendar.getTimeInMillis();
	}

	public static String timeSinceInSeconds(long time) {
		int sec = (int) ((System.currentTimeMillis() - time) / 1000);
		int min = sec / 60;
		sec %= 60;
		return min + "'" + sec + "''";
	}

	public static File validateDirectory(String dirName) {
		File dir = new File(dirName);
		if (!dir.exists() || !dir.isDirectory()) {
			throw new IllegalStateException("Directory [" + dirName + "] does not exists or is not a directory!");
		}
		return dir;
	}
}
