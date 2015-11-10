package pluto.core;

import java.io.File;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import pluto.charon.Utils;
import pluto.managers.PlutoCore;
import pluto.utils.RotatingQueue;

public class Log {
	public final static Logger LOGGER = Logger.getLogger(PlutoCore.class.getSimpleName());

	public static final int LOG_LIST_SIZE = 42;
	public final static RotatingQueue<String> logList = new RotatingQueue<String>(LOG_LIST_SIZE);

	public static final int ERROR_LIST_SIZE = 16;
	public final static RotatingQueue<String> errorList = new RotatingQueue<String>(ERROR_LIST_SIZE);

	static {
		try {
			FileHandler filehandler = new FileHandler("log" + File.separator + PlutoCore.class.getName() + ".log",
					1024 * 1024, 20, true);
			filehandler.setFormatter(new LogFormater());
			LOGGER.addHandler(filehandler);
			LOGGER.setLevel(Level.ALL);
		} catch (Exception e) {
			System.err.println("Cannot initialize logging");
			e.printStackTrace();
			System.exit(1);
		}
	}

	public static final void log(String msg) {
		LOGGER.log(Level.INFO, msg);
		logList.add("INFO: " + msg);
	}

	public static final void warning(String msg) {
		LOGGER.log(Level.WARNING, msg);
		logList.add("WARN: " + msg);
	}

	public static final void error(String msg) {
		LOGGER.log(Level.SEVERE, msg);
		final String err = "ERROR: " + msg;
		logList.add(err);
		errorList.add(Utils.dateFormater.format(new Date()) + err);
	}

	public static final void error(String msg, Throwable t) {
		LOGGER.log(Level.SEVERE, msg, t);
		final String err = "ERROR: " + msg;
		logList.add(err);
		errorList.add(Utils.dateFormater.format(new Date()) + err);
	}

	public static final void screen(String msg) {
		logList.add("SCREEN: " + msg);
	}

	public static void illegalState(String error) {
		Log.error(error);
		throw new IllegalStateException(error);
	}

	public static void illegalState(String error, Throwable x) {
		Log.error(error, x);
		throw new IllegalStateException(error, x);
	}
}
