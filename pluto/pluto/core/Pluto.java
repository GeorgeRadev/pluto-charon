package pluto.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.util.Properties;
import pluto.charon.Utils;
import pluto.managers.DBManager;
import pluto.managers.IAuthenticationManager;
import pluto.managers.ProcessLocker;
import pluto.managers.SessionManager;
import pluto.managers.WebManager;
import utils.LocalAuthentication;

public class Pluto {

	private static Pluto instance;
	private static String processName;

	public final DBManager dbManager;
	public final WebManager webManager;
	public final SessionManager sessionManager;
	public final IAuthenticationManager authenticationManager;

	public final static String PROPERTIES_FILE = Pluto.class.getSimpleName() + ".properties";
	public final Properties properties = new Properties();
	public final long START_TIME = System.currentTimeMillis();
	public final String SVNRevision;

	public static void main(String[] args) throws Exception {
		try {
			ProcessLocker.lock();
			new Pluto();
		} catch (Throwable e) {
			e.printStackTrace();
		} finally {
			ProcessLocker.unlock();
		}
		System.exit(1);
	}

	public static Pluto getInstance() {
		return instance;
	}

	private Pluto() throws InterruptedException {
		instance = this;
		processName = ManagementFactory.getRuntimeMXBean().getName();

		try {// Load application properties
			properties.load(new FileInputStream(new File(PROPERTIES_FILE)));
		} catch (Exception e) {
			Log.error(e.getMessage(), e);
			System.out.println("Error loading :" + PROPERTIES_FILE);
			System.exit(1);
		}

		try {// load SVN version
			final Class<?> clazz = Pluto.class;
			final String filename = "SVNRevision.properties";
			InputStream is = clazz.getResourceAsStream(filename);
			if (is != null) {
				properties.load(is);
				is.close();
			}
		} catch (Exception e) {
			Log.error(e.getMessage(), e);
		}
		SVNRevision = properties.getProperty("SVNRevision", "N/A");

		// log stating
		Log.log("------------- starting: " + processName + " version: " + SVNRevision);

		// initialize managers
		dbManager = new DBManager(properties);
		webManager = new WebManager(properties);
		authenticationManager = new LocalAuthentication();
		sessionManager = new SessionManager(properties, authenticationManager, dbManager);

		Thread hook = new Thread(new ShutdownRunnable());
		Runtime.getRuntime().addShutdownHook(hook);

		try {
			// get ready...
			authenticationManager.init();
			// start Server
			long time = System.currentTimeMillis();
			sessionManager.start();
			Log.log("STORAGE updated: " + Utils.timeSinceInSeconds(time));
			// start web server
			webManager.start();

		} catch (java.net.BindException e) {
			System.err.println(e.getMessage());
			System.err.println("Two instances can't run at same time.");
			System.exit(1);
		} catch (Exception e) {
			Log.error("Cannot start managers!", e);
			System.exit(1);
		}

		System.out.println("to close this application, press Ctrl+C or kill the process: " + processName);
		webManager.join();
	}

	public static class ShutdownRunnable implements Runnable {
		public void run() {
			ProcessLocker.unlock();

			Log.log("------------- stoping: " + processName);
			try {
				instance.webManager.stop();
			} catch (Throwable e) {
			}
			try {
				instance.dbManager.flushPool();
			} catch (Throwable e) {
			}
			Log.log("------------- stoped: " + processName);
		}
	}
}
