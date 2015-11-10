package pluto.servlets;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import json.JSONBuilder;
import json.JSONParser;
import pluto.managers.UIHTMLManager;

public class AjaxServlet extends HttpServlet {
	private static final String EMPTY_JSON_OBJECT = "{}";
	public static final String PAGE_ENCODING = "text/html; charset=utf-8";

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		doPost(request, response);
	}

	public static final class Context {
		public String operation;
		public String documentGUID;
		public String meta;
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		// check login
		final HttpSession session = request.getSession();
		response.setContentType(PAGE_ENCODING);

		if (!Boolean.TRUE.equals(session.getAttribute(LoginServlet.LOGGED_ATTRIBUTE))) {
			response.getWriter().write(EMPTY_JSON_OBJECT);
			return;
		}
		String currentUser = String.valueOf(session.getAttribute(LoginServlet.LOGGED_NAME));

		final Context cx = new Context();
		UIHTMLManager.initFromRequest(request, cx);

		if ("meta".equals(cx.operation)) {
			if (cx.documentGUID == null) {
				response.getWriter().write(EMPTY_JSON_OBJECT);
				return;
			}
			try {

				JSONBuilder builder = new JSONBuilder(4096);
				builder.startObject();
				builder.addKeyValue("ok", true);
				builder.endObject();
				response.getWriter().write(builder.toString());
			} catch (Throwable e) {
				JSONBuilder builder = new JSONBuilder(4096);
				builder.startObject();
				builder.addKeyValue("title", "Error!!!");
				builder.addKeyValue("ok", false);
				builder.addKeyValue("content", e.getMessage());
				builder.endObject();
				response.getWriter().write(builder.toString());
			}
			return;
		} else if ("updateMeta".equals(cx.operation)) {
			if (cx.documentGUID == null || cx.documentGUID.length() <= 0) {
				response.getWriter().write(EMPTY_JSON_OBJECT);
				return;
			}
			if (cx.meta == null || cx.meta.length() <= 0) {
				response.getWriter().write(EMPTY_JSON_OBJECT);
				return;
			}
			try {
				JSONParser parser = new JSONParser();
				Map<Object, Object> newMeta = parser.parseJSONString(cx.meta);

				JSONBuilder builder = new JSONBuilder(4096);
				builder.startObject();
				builder.addKeyValue("ok", true);
				builder.endObject();
				response.getWriter().write(builder.toString());
			} catch (Throwable e) {
				JSONBuilder builder = new JSONBuilder(4096);
				builder.startObject();
				builder.addKeyValue("title", "Error!!!");
				builder.addKeyValue("content", e.getMessage());
				builder.addKeyValue("ok", false);
				builder.endObject();
				response.getWriter().write(builder.toString());
			}
			return;
		}

		response.setContentType(PAGE_ENCODING);
		response.getWriter().write(EMPTY_JSON_OBJECT);
	}
}
