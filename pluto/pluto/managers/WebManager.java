package pluto.managers;

import java.util.EnumSet;
import java.util.Properties;

import javax.servlet.DispatcherType;

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import pluto.core.Log;
import pluto.servlets.FileFilter;
import pluto.servlets.LoginServlet;
import pluto.servlets.StatusServlet;

public class WebManager {
	public static final String WEB_SERVER_PORT = "PLUTO.Web.Port";
	public static final int UPLOAD_LIMIT = 1024 * 1024; // 1mb

	private final org.eclipse.jetty.server.Server webServer;

	public WebManager(Properties properties) {
		String key;

		String serverPort = properties.getProperty(key = WEB_SERVER_PORT, null);
		if (serverPort == null) {
			Log.illegalState("Define " + key + " in property file");
		}

		int port = 0;
		try {
			port = Integer.parseInt(serverPort, 10);
		} catch (Exception e) {
			Log.illegalState("Define correct web.server.port in property file");
		}

		webServer = new org.eclipse.jetty.server.Server(port);
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/");
		// define attachments size limit
		context.setMaxFormContentSize(UPLOAD_LIMIT);
		webServer.setHandler(context);

		final LoginServlet loginServlet = new LoginServlet();
		context.addServlet(new ServletHolder(loginServlet), "/");
		context.addServlet(new ServletHolder(loginServlet), "/login");
		context.addServlet(new ServletHolder(loginServlet), "/favicon.ico");
		context.addServlet(new ServletHolder(new StatusServlet()), "/status");

		context.addFilter(FileFilter.class, FileFilter.PREFIX + "*", EnumSet.of(DispatcherType.REQUEST));
	}

	public void start() throws Exception {
		webServer.start();
	}

	public void join() throws InterruptedException {
		webServer.join();
	}

	public void stop() throws Exception {
		webServer.stop();
	}
}
