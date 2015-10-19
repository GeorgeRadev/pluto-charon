package servlets;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import cch.storage.engine.Utils;
import engine.StorageEngine;

public class UpdateServlet extends HttpServlet {
	public static final String PAGE_ENCODING = "text/html; charset=utf-8";

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		doPost(request, response);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setContentType(PAGE_ENCODING);
		PrintWriter writer = response.getWriter();

		long time = System.currentTimeMillis();
		StorageEngine.getInstance().authenticationManager.update();
		writer.write("LDAP updated: " + Utils.timeSinceInSeconds(time));
	}

}
