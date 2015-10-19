package pluto.servlets;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.eclipse.jetty.server.Request;

@SuppressWarnings("serial")
public class FileFilter implements Filter {
	public static final String PREFIX = "/file/";
	private static final Map<String, byte[]> file2content = new HashMap<String, byte[]>();

	public void destroy() {}

	public void init(FilterConfig config) throws ServletException {}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
			ServletException {
		final String path = ((Request) request).getServletPath();
		if (path.startsWith(PREFIX)) {

			final String filename = path.substring(PREFIX.length());
			byte[] fileContent = file2content.get(filename);
			if (fileContent == null) {
				try {
					InputStream in = this.getClass().getResourceAsStream(filename);
					ByteArrayOutputStream out = new ByteArrayOutputStream(16384);

					int nRead;
					byte[] data = new byte[16384];
					while ((nRead = in.read(data, 0, data.length)) != -1) {
						out.write(data, 0, nRead);
					}

					fileContent = out.toByteArray();
					file2content.put(filename, fileContent);

				} catch (Exception e) {
					fileContent = new byte[0];
				}
			}

			defineResponseFromFileName(response, filename);
			OutputStream out = response.getOutputStream();
			out.write(fileContent);

		} else {
			chain.doFilter(request, response);
		}
	}

	private void defineResponseFromFileName(ServletResponse response, String filename) {
		if (filename.endsWith(".css")) {
			response.setContentType("text/css; charset=utf-8");
		} else if (filename.endsWith(".js")) {
			response.setContentType("application/javascript; charset=utf-8");
		} else if (filename.endsWith(".html")) {
			response.setContentType("text/html; charset=utf-8");
		} else if (filename.endsWith(".gif")) {
			response.setContentType("image/gif");
		} else if (filename.endsWith(".jpg")) {
			response.setContentType("image/jpg");
		} else {
			response.setContentType("text/plain; charset=utf-8");
		}
	}
}