package pluto.servlets;

import java.io.IOException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import pluto.core.Pluto;
import pluto.managers.IAuthenticationManager;
import pluto.managers.UIHTMLManager;
import pluto.managers.UIHTMLManager.Page;

public class LoginServlet extends HttpServlet {
	public static final String PAGE_ENCODING = "text/html; charset=utf-8";
	public static final String LOGGED_ATTRIBUTE = "isLoggedOn";
	public static final String LOGGED_NAME = "username";

	public static final class Context {
		public String username;
		public String password;
		public String action;
	}

	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		UIHTMLManager.addPage(this.getClass());
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		doPost(request, response);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		final HttpSession session = request.getSession();
		final Context cx = new Context();
		UIHTMLManager.initFromRequest(request, cx);
		if ("/favicon.ico".equals(request.getServletPath())) {
			// get rid of double requests
			return;
		}
		if ("logout".equals(cx.action)) {
			response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
			session.setAttribute(LOGGED_ATTRIBUTE, Boolean.FALSE);
			final String contextPath = request.getContextPath();
			response.setHeader("Location", contextPath + "/");
			return;
		}

		// check login
		if (Boolean.TRUE.equals(session.getAttribute(LOGGED_ATTRIBUTE))) {
			response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
			final String contextPath = request.getContextPath();
			response.setHeader("Location", contextPath + "/search");
			return;
		}

		final Page page = UIHTMLManager.getPage(this.getClass());

		if ("login".equals(cx.action)) {
			// try to authenticate
			if (cx.username != null) {
				cx.username = cx.username.toUpperCase().trim();
			}
			IAuthenticationManager authenticator = Pluto.getInstance().authenticationManager;
			if (authenticator.checkAuthentication(cx.username, cx.password)) {
				if (authenticator.checkAutorization(cx.username)) {
					response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
					// go to status page and add session if for logged on
					session.setAttribute(LOGGED_ATTRIBUTE, Boolean.TRUE);
					session.setAttribute(LOGGED_NAME, cx.username);
					final String contextPath = request.getContextPath();
					response.setHeader("Location", contextPath + "/search");
					return;
				} else {
					page.setVar("errorMessage", "user is not autorized for access!");
				}
			} else {
				page.setVar("errorMessage", "invalid password!");
			}
		}
		response.setContentType(PAGE_ENCODING);
		page.setVar("username", cx.username);
		page.setVar("SVNRevision", Pluto.getInstance().SVNRevision);
		page.render(response.getWriter());
	}
}