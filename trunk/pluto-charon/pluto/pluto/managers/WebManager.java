package pluto.managers;

import java.util.EnumSet;
import java.util.Properties;
import javax.servlet.DispatcherType;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import pluto.core.Log;
import pluto.servlets.AjaxServlet;
import pluto.servlets.DownloadServlet;
import pluto.servlets.FileFilter;
import pluto.servlets.LogViewServlet;
import pluto.servlets.LoginServlet;
import pluto.servlets.SearchServlet;
import pluto.servlets.UpdateServlet;
import pluto.servlets.UploadServlet;

public class WebManager {
	public static final String WEB_SERVER_NAME = "web.server.name";
	public static final String WEB_SERVER_PORT = "web.server.port";
	public static final int UPLOAD_LIMIT = 1024 * 1024; // 1mb

	private final org.eclipse.jetty.server.Server webServer;

	public WebManager(Properties properties) {
		String key;

		String serverName = properties.getProperty(key = WEB_SERVER_NAME, null);
		if (serverName == null) {
			Log.illegalState("Define " + key + " in property file");
		}

		String serverPort = properties.getProperty(key = WEB_SERVER_PORT, null);
		if (serverPort == null) {
			Log.illegalState("Define " + key + " in property file");
		}
		int port = 8080;
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
		context.addServlet(new ServletHolder(new SearchServlet()), "/search");
		context.addServlet(new ServletHolder(new LogViewServlet()), "/logview");
		context.addServlet(new ServletHolder(new AjaxServlet()), "/ajax");
		context.addServlet(new ServletHolder(new DownloadServlet()), "/download");
		context.addServlet(new ServletHolder(new UpdateServlet()), "/update");
		context.addServlet(new ServletHolder(new UploadServlet()), "/upload");

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
