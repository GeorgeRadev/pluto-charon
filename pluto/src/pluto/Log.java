package pluto;

import java.io.File;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Log {
	public final static Logger LOGGER = Logger.getLogger("pluto");

	static {
		try {
			FileHandler filehandler = new FileHandler("log" + File.separator + "pluto.log", 1024 * 1024, 20, true);
			filehandler.setFormatter(new LogFormater());
			LOGGER.addHandler(filehandler);
			LOGGER.setLevel(Level.ALL);
		} catch (Exception e) {
			System.err.println("Cannot initialize logging! please, check if log folder exists!");
			e.printStackTrace();
			System.exit(1);
		}
	}

	public static final void log(String msg) {
		LOGGER.log(Level.INFO, msg);
	}

	public static final void warning(String msg) {
		LOGGER.log(Level.WARNING, msg);
	}

	public static final void error(String msg) {
		LOGGER.log(Level.SEVERE, msg);
	}

	public static final void error(String msg, Throwable t) {
		LOGGER.log(Level.SEVERE, msg, t);
	}
}
