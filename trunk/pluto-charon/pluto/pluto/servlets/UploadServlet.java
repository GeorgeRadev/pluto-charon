package servlets;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import managers.StorageDocument;
import managers.UIHTMLManager;
import managers.UIHTMLManager.Page;
import managers.WebManager;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FilenameUtils;
import engine.StorageEngine;

public class UploadServlet extends HttpServlet {
	public static final String PAGE_ENCODING = "text/html; charset=utf-8";

	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		UIHTMLManager.addPage(this.getClass());
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		doPost(request, response);
	}

	public static final class Context {
		public String action;
		public String documentType;
	}

	@SuppressWarnings("unchecked")
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		// check login
		final HttpSession session = request.getSession();
		if (!Boolean.TRUE.equals(session.getAttribute(LoginServlet.LOGGED_ATTRIBUTE))) {
			response.setStatus(HttpServletResponse.SC_MOVED_TEMPORARILY);
			final String contextPath = request.getContextPath();
			response.setHeader("Location", contextPath + "/");
			return;
		}
		String currentUser = String.valueOf(session.getAttribute(LoginServlet.LOGGED_NAME));

		final Context cx = new Context();
		UIHTMLManager.initFromRequest(request, cx);

		response.setContentType(PAGE_ENCODING);
		final Page page = UIHTMLManager.getPage(this.getClass());
		page.setVar("SVNRevision", StorageEngine.getInstance().SVNRevision);

		// do upload
		boolean isMultipart = ServletFileUpload.isMultipartContent(request);
		if (isMultipart) {
			// Create a factory for disk-based file items
			FileItemFactory factory = new DiskFileItemFactory(WebManager.UPLOAD_LIMIT,
					StorageEngine.getInstance().tmpDirectory);

			// Create a new file upload handler
			ServletFileUpload upload = new ServletFileUpload(factory);

			// Parse the request
			String fileName = null;
			long fileSize = 0;
			InputStream fileContent = null;
			List<FileItem> items = null;
			try {
				items = upload.parseRequest(request);
			} catch (FileUploadException e) {
				throw new IOException(e);
			}
			for (FileItem item : items) {
				if (item.isFormField()) {
					String fieldname = item.getFieldName();
					String fieldvalue = item.getString();

					if ("documentType".equals(fieldname)) {
						cx.documentType = fieldvalue;
					} else if ("action".equals(fieldname)) {
						cx.action = fieldvalue;
					}

				} else {
					// Process form file field (input type="file").
					String fieldname = item.getFieldName();
					if ("FileUpload".equals(fieldname)) {
						fileName = FilenameUtils.getName(item.getName());
						fileSize = item.getSize();
						fileContent = item.getInputStream();
					}
				}
			}

			if ("upload".equals(cx.action)) {
				String mimeType = getServletContext().getMimeType(fileName);
				if (mimeType == null) {
					mimeType = "application/octet-stream";
				}
				if (cx.documentType == null) {
					page.setVar("errorMessage", "Enter document type!");

				} else if (fileName == null || fileName.length() <= 0) {
					page.setVar("errorMessage", "Select file to upload!");

				} else if (fileSize <= 0) {
					page.setVar("errorMessage", "Select file size should greater than 0!");

				} else if (fileContent == null) {
					page.setVar("errorMessage", "Error while trying to load the file!");

				} else {
					StorageDocument document = StorageEngine.getInstance().sessionManager.createDocument(
							cx.documentType, fileName, fileContent, mimeType, currentUser);
					page.setVar("infoMessage", "document [" + document.guid + "] was uploaded!");
				}
			}
		}

		page.render(response.getWriter());
	}
}