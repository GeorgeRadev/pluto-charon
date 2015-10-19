package pluto.core;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class LogFormater extends Formatter {

	StringBuffer sb = new StringBuffer(1024);
	Calendar cal = Calendar.getInstance();
	SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

	@Override
	public String format(LogRecord record) {
		synchronized (this) {
			sb.setLength(0);
			// Get the date from the LogRecord and add it to the buffer
			cal.setTimeInMillis(record.getMillis());
			sb.append(formatter.format(cal.getTime()));
			sb.append(' ');

			// Get the level name and add it to the buffer
			sb.append(record.getLevel().getName());
			sb.append(' ');

			// Get the formatted message (includes localization
			// and substitution of parameters) and add it to the buffer
			sb.append(formatMessage(record));
			sb.append("\r\n");

			return sb.toString();
		}
	}
}
