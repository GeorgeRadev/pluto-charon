package servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import managers.DBManager;
import managers.UIHTMLManager;
import managers.UIHTMLManager.Page;
import sql.helper.QueryListener;
import sql.helper.SQLHelperManager;
import utils.CSVTableBufferedWriter;
import engine.StorageEngine;

public class SearchServlet extends HttpServlet {
	public static final String PAGE_ENCODING = "text/html; charset=utf-8";
	private static String ROW_FILE = "SearchServletRow.html";
	public static final int PAGE_SIZE = 24;
	private static final SimpleDateFormat YYYY_MM_DD = new SimpleDateFormat("yyyy-MM-dd");
	private static final SimpleDateFormat YYYY_MM_DD_HH_MM_SS = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		UIHTMLManager.addPage(this.getClass());
		UIHTMLManager.addPage(this.getClass(), ROW_FILE);
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		doPost(request, response);
	}

	public static final class Context {
		public String operation;

		public int currentPage;
		public int sortIndex;

		public String dateFrom;
		public String dateTo;
		public long dateFromLong;
		public long dateToLong;
		public String documentType;
		public String searchName;
		public String searchMeta;
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

		final Context cx = new Context();
		UIHTMLManager.initFromRequest(request, cx);

		if ("export".equals(cx.operation) && validateDates(cx)) {
			final DBManager db = StorageEngine.getInstance().dbManager;
			long documentCount = db.searchDocumentsCount(cx.dateFromLong, cx.dateToLong, cx.documentType,
					cx.searchName, cx.searchMeta);
			if (documentCount <= 0) {
				response.setContentType(PAGE_ENCODING);
				response.getWriter().print("no records to export!");
				return;
			}

			response.setContentType("text/csv");
			response.addHeader("Content-Disposition", "inline;filename=SearchExport.csv");
			PrintWriter writer = response.getWriter();
			CSVTableBufferedWriter exporter = new CSVTableBufferedWriter(writer, 4096);

			try {
				ExportListener listener = new ExportListener(exporter);
				db.searchDocuments(cx.dateFromLong, cx.dateToLong, cx.documentType, cx.searchName, cx.searchMeta,
						listener);
			} catch (Throwable e) {
				exporter.newRow();
				exporter.cell("");
				exporter.cell(",ERROR: " + e.getMessage());

			} finally {
				exporter.done();
			}
			writer.flush();
			return;
		}

		response.setContentType(PAGE_ENCODING);
		final Page page = UIHTMLManager.getPage(this.getClass());
		page.setVar("SVNRevision", StorageEngine.getInstance().SVNRevision);

		// do a search
		// default page content
		page.setVar("dateFrom", (cx.dateFrom == null) ? "from Date" : cx.dateFrom, true);
		page.setVar("dateTo", (cx.dateTo == null) ? "to Date" : cx.dateTo, true);
		page.setVar("documentType", cx.documentType);
		page.setVar("searchName", cx.searchName);
		page.setVar("searchMeta", cx.searchMeta);

		page.setVar("sortIndex", cx.sortIndex);
		page.setVar("currentPage", 0);
		page.setVar("maxPages", 0);
		page.setVar("documentCount", 0);

		doSearch(page, cx);
		page.render(response.getWriter());
	}

	final public static boolean isEmpty(String str) {
		return str == null || str.length() <= 0;
	}

	private boolean validateDates(Context cx) {
		Date fromDate = UIHTMLManager.toDate(cx.dateFrom, false);
		Date toDate = UIHTMLManager.toDate(cx.dateTo, true);

		if (fromDate == null && toDate == null) {
			return false;
		}
		if (fromDate == null) {
			fromDate = UIHTMLManager.toDate(cx.dateTo, false);
		} else if (toDate == null) {
			toDate = UIHTMLManager.toDate(cx.dateFrom, true);
		}

		if (fromDate == null || toDate == null) {
			return false;
		}
		cx.dateFromLong = fromDate.getTime();
		cx.dateToLong = toDate.getTime();

		return true;
	}

	/**
	 * make the search
	 */
	private void doSearch(Page page, Context cx) throws IOException {
		page.setVar("documentCount", 0);
		// validate date range
		if (!validateDates(cx)) {
			page.setVar("resultMessage", "no documents for this criteria - defining date range is mandatory !");
			return;
		}

		final DBManager db = StorageEngine.getInstance().dbManager;
		final Connection conn = db.getConnection();
		if (conn == null) {
			return;
		}
		long documentCount = db.searchDocumentsCount(cx.dateFromLong, cx.dateToLong, cx.documentType, cx.searchName,
				cx.searchMeta);
		page.setVar("documentCount", documentCount);

		if (documentCount <= 0) {
			page.setVar("resultMessage", "no documents for this criteria!");

		} else {
			// do search if we have enough parameters

			// validate pages
			if (cx.currentPage < 0) {
				cx.currentPage = 0;
			}
			if (cx.currentPage * PAGE_SIZE >= documentCount) {
				cx.currentPage = 0;
			}

			// do the page search
			SearchListener listener = new SearchListener(cx.currentPage);
			db.searchDocuments(cx.dateFromLong, cx.dateToLong, cx.documentType, cx.searchName, cx.searchMeta, listener);
			page.setVar("tableRows", listener.rows.toString());

			if (listener.hasResult) {// set up paging
				long maxPages = (documentCount - 1) / PAGE_SIZE;

				page.setVar("currentPage", cx.currentPage + 1);
				page.setVar("maxPages", maxPages + 1);

				if (cx.currentPage <= 0) {
					page.setVar("buttonPageFirst", "disabled='disabled'");
					page.setVar("buttonPrevious", "disabled='disabled'");
				} else {
					page.setVar("buttonPageFirst", "onClick='javascript:gotoPage(0)'");
					page.setVar("buttonPrevious", "onClick='javascript:gotoPage(" + (cx.currentPage - 1) + ")'");
				}

				if (cx.currentPage >= maxPages) {
					page.setVar("buttonNext", "disabled='disabled'");
					page.setVar("buttonPageLast", "disabled='disabled'");
				} else {
					page.setVar("buttonNext", "onClick='javascript:gotoPage(" + (cx.currentPage + 1) + ")'");
					page.setVar("buttonPageLast", "onClick='javascript:gotoPage(" + maxPages + ")'");
				}

			} else {
				page.setVar("buttonPageFirst", "disabled='disabled'");
				page.setVar("buttonPrevious", "disabled='disabled'");
				page.setVar("buttonNext", "disabled='disabled'");
				page.setVar("buttonPageLast", "disabled='disabled'");

				page.setVar("resultMessage",
						"no documents for this criteria - defining atleast one date is mandatory !");
				page.setVar("currentPage", 0);
				page.setVar("maxPages", 0);
			}
		}
		db.close(conn);
	}

	public static class SearchListener extends QueryListener {
		public boolean hasResult;
		public final StringBuilder rows = new StringBuilder();
		final Page row = UIHTMLManager.getPage(ROW_FILE);
		long skip;
		long pageSize;

		public SearchListener(long currentPage) {
			skip = currentPage * PAGE_SIZE;
			pageSize = PAGE_SIZE;
			hasResult = false;
		}

		public boolean forRow(SQLHelperManager db, ResultSet rs) {
			hasResult = true;
			// skip previous pages
			if (skip > 0) {
				skip--;
				return true;
			}
			// fill only one page
			if (pageSize <= 0) {
				return false;
			}
			row.clearContext();
			// DOC_GUID, DOC_DATE, DOC_TYPE, DOC_NAME, DOC_MIME_TYPE, DOC_SIZE,
			// DOC_CREATOR, RETENTION_DATE
			int t = 0;
			final String documentGUID = db.getString(rs, ++t);
			final long documentDate = db.getLong(rs, ++t);
			final String documentType = db.getString(rs, ++t);
			final String documentName = db.getString(rs, ++t);
			final String documentMime = db.getString(rs, ++t);
			final long documentSize = db.getInt(rs, ++t);
			final String documentCreator = db.getString(rs, ++t);
			final long documentRetention = db.getLong(rs, ++t);

			row.setVar("documentGUID", documentGUID);
			row.setVar("documentDate", YYYY_MM_DD_HH_MM_SS.format(new Date(documentDate)));
			row.setVar("documentType", documentType);
			row.setVar("documentName", documentName);
			row.setVar("documentMime", documentMime);
			row.setVar("documentSize", documentSize);
			row.setVar("documentCreator", documentCreator);
			row.setVar("documentRetention", YYYY_MM_DD.format(new Date(documentRetention)));

			try {
				// add row to the result table
				rows.append(row.render());
			} catch (IOException e) {}
			pageSize--;
			return true;
		}
	}

	public static class ExportListener extends QueryListener {
		final CSVTableBufferedWriter exporter;

		public ExportListener(CSVTableBufferedWriter exporter) throws IOException {
			this.exporter = exporter;
			exporter.cell("DOC_GUID");
			exporter.cell("DOC_DATE");
			exporter.cell("DOC_TYPE");
			exporter.cell("DOC_NAME");
			exporter.cell("DOC_MIME_TYPE");
			exporter.cell("DOC_SIZE");
			exporter.cell("DOC_CREATOR");
			exporter.cell("RETENTION_DATE");
			exporter.newRow();
		}

		@Override
		public boolean forRow(SQLHelperManager db, ResultSet rs) {
			// DOC_GUID, DOC_DATE, DOC_TYPE, DOC_NAME, DOC_MIME_TYPE, DOC_SIZE,
			// DOC_CREATOR, RETENTION_DATE
			int t = 0;
			final String documentGUID = db.getString(rs, ++t);
			final long documentDate = db.getLong(rs, ++t);
			final String documentType = db.getString(rs, ++t);
			final String documentName = db.getString(rs, ++t);
			final String documentMime = db.getString(rs, ++t);
			final long documentSize = db.getInt(rs, ++t);
			final String documentCreator = db.getString(rs, ++t);
			final long documentRetention = db.getLong(rs, ++t);

			try {
				exporter.cell(documentGUID);
				exporter.cell(YYYY_MM_DD_HH_MM_SS.format(new Date(documentDate)));

				exporter.cell(documentType);
				exporter.cell(documentName);
				exporter.cell(documentMime);
				exporter.cell(documentSize);
				exporter.cell(documentCreator);
				exporter.cell(YYYY_MM_DD.format(new Date(documentRetention)));
				exporter.newRow();
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			return true;
		}
	}
}