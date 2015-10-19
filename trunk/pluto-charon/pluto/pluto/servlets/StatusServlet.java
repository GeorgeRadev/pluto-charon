package pluto.servlets;

import java.io.IOException;
import java.sql.Connection;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import pluto.core.Log;
import pluto.core.Pluto;
import pluto.managers.DBManager;
import pluto.managers.UIHTMLManager;
import pluto.managers.UIHTMLManager.Page;

@SuppressWarnings("serial")
public class StatusServlet extends HttpServlet {
	public static final String PAGE_ENCODING = "text/html; charset=utf-8";

	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		UIHTMLManager.addPage(this.getClass());
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		doPost(request, response);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		// check login
		final HttpSession session = request.getSession();
		if (!Boolean.TRUE.equals(session.getAttribute(LoginServlet.LOGGED_ATTRIBUTE))) {
			response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
			final String contextPath = request.getContextPath();
			response.setHeader("Location", contextPath + "/");
			return;
		}

		response.setContentType(PAGE_ENCODING);
		final Page page = UIHTMLManager.getPage(this.getClass());
		page.setVar("SVNRevision", Pluto.getInstance().SVNRevision);

		StringBuilder html = new StringBuilder(8192);

		{// log trace display
			html.setLength(0);
			for (String line : Log.logList) {
				if (line.startsWith("ERROR")) {
					html.append("<B style='color: red;'>");
					html.append(line);
					html.append("</B>");

				} else {
					html.append(line);
				}
				html.append("<br/>");
			}
			page.setVar("LogTrace", html.toString());
		}

		{// error trace display
			html.setLength(0);
			for (String line : Log.errorList) {
				html.append("<B style='color: red;'>");
				html.append(line);
				html.append("</B>");

				html.append("<br/>");
			}
			page.setVar("ErrorTrace", html.toString());
		}

		{// DB Status
			DBManager DB = Pluto.getInstance().dbManager;
			Connection conn = DB.getConnection();
			if (conn != null) {
				page.setVar("sqlStatus", "<b>ok</b>");
				DB.close(conn);
			} else {
				page.setVar("sqlStatus", "<b style='color:#FF0000;'>no connection</b>");
			}

			page.setVar("sqlError", DB.SQLError);
			Throwable e = DB.SQLException;
			page.setVar("sqlException", e == null ? "none" : e.getMessage());
		}

		Runtime rt = Runtime.getRuntime();
		long totalMem = rt.totalMemory();
		long freeMem = rt.freeMemory();
		page.setVar("jvmCores", rt.availableProcessors());
		page.setVar("jvmTotalMemory", totalMem);
		page.setVar("jvmFreeMemory", freeMem);
		page.setVar("jvmUsedMemory", totalMem - freeMem);
		page.setVar("jvmUpTime", getUpTime());

		page.render(response.getWriter());
	}

	private String getUpTime() {
		long milis = System.currentTimeMillis() - Pluto.getInstance().START_TIME;
		long sec = milis / 1000;
		long mins = sec / 60;
		long hours = mins / 60;
		long days = hours / 24;
		String str = String.format("%d days %02d:%02d:%02d.%03d", days, hours % 24, mins % 60, sec % 60, milis % 1000);
		return str;
	}
}