package pluto.servlets;

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
import pluto.core.Pluto;
import pluto.managers.DBManager;
import pluto.managers.UIHTMLManager;
import pluto.managers.UIHTMLManager.Page;
import pluto.utils.CSVTableBufferedWriter;
import sql.helper.QueryListener;
import sql.helper.SQLHelperManager;

public class LogViewServlet extends HttpServlet {
	public static final String PAGE_ENCODING = "text/html; charset=utf-8";
	private static String ROW_FILE = "LogViewServletRow.html";
	public static final int PAGE_SIZE = 24;
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
		public String documentUser;
		public String documentAction;
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
			final DBManager db = Pluto.getInstance().dbManager;
			long documentCount = db.searchAccessLogCount(cx.dateFromLong, cx.dateToLong, cx.documentAction,
					cx.documentUser);
			if (documentCount <= 0) {
				response.setContentType(PAGE_ENCODING);
				response.getWriter().print("no records to export!");
				return;
			}

			response.setContentType("text/csv");
			response.addHeader("Content-Disposition", "inline;filename=LogExport.csv");
			PrintWriter writer = response.getWriter();
			CSVTableBufferedWriter exporter = new CSVTableBufferedWriter(writer, 4096);

			try {
				ExportListener listener = new ExportListener(exporter);
				db.searchAccessLog(cx.dateFromLong, cx.dateToLong, cx.documentAction, cx.documentUser, listener);
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
		page.setVar("documentUser", cx.documentUser);
		page.setVar("documentAction", cx.documentAction);

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
		long documentCount = db.searchAccessLogCount(cx.dateFromLong, cx.dateToLong, cx.documentAction, cx.documentUser);
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
			db.searchAccessLog(cx.dateFromLong, cx.dateToLong, cx.documentAction, cx.documentUser, listener);
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

		@Override
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
			// LOG_WHEN, LOG_WHO, LOG_ACTION, DOC_GUID
			int t = 0;
			final long documentDate = db.getLong(rs, ++t);
			final String who = db.getString(rs, ++t);
			final String action = db.getString(rs, ++t);
			final String docGUID = db.getString(rs, ++t);

			row.setVar("when", YYYY_MM_DD_HH_MM_SS.format(new Date(documentDate)));
			row.setVar("who", who);
			row.setVar("action", action);
			row.setVar("what", docGUID);

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
			exporter.cell("LOG_WHEN");
			exporter.cell("LOG_WHO");
			exporter.cell("LOG_ACTION");
			exporter.cell("DOC_GUID");
			exporter.newRow();
		}

		@Override
		public boolean forRow(SQLHelperManager db, ResultSet rs) {
			// LOG_WHEN, LOG_WHO, LOG_ACTION, DOC_GUID
			int t = 0;
			final long documentDate = db.getLong(rs, ++t);
			final String who = db.getString(rs, ++t);
			final String action = db.getString(rs, ++t);
			final String docGUID = db.getString(rs, ++t);

			try {
				exporter.cell(YYYY_MM_DD_HH_MM_SS.format(new Date(documentDate)));
				exporter.cell(who);
				exporter.cell(action);
				exporter.cell(docGUID);

				exporter.newRow();
			} catch (IOException e) {
				e.printStackTrace();
				return false;
			}
			return true;
		}
	}
}