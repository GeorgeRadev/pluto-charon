package pluto.servlets;

import java.io.IOException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import pluto.managers.UIHTMLManager;

public class DownloadServlet extends HttpServlet {
	public static final String PAGE_ENCODING = "text/html; charset=utf-8";

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		doPost(request, response);
	}

	public static final class Context {
		public String documentGUID;
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		// check login
		final HttpSession session = request.getSession();

		if (!Boolean.TRUE.equals(session.getAttribute(LoginServlet.LOGGED_ATTRIBUTE))) {
			response.setContentType(PAGE_ENCODING);
			response.getWriter().write("no authorized!");
			return;
		}
		final Context cx = new Context();
		UIHTMLManager.initFromRequest(request, cx);

		if (cx.documentGUID == null || cx.documentGUID.length() <= 0) {
			response.setContentType(PAGE_ENCODING);
			response.getWriter().write("documentGUID parameter is expected!");
			return;
		}

	}

}
