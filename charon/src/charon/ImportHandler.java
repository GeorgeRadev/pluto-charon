package charon;

import cx.ast.Visitor;
import cx.runtime.Handler;

public class ImportHandler implements Handler {

	private static final String IMPORT = "import";
	private final CharonCore core;

	public ImportHandler(CharonCore core) {
		this.core = core;
	}

	public void init(Visitor cx) {
	}

	public boolean accept(Object object) {
		return false;
	}

	public void set(Object object, String variable, Object value) {
	}

	public Object get(Object object, String variable) {
		return null;
	}

	public Object call(Object object, Object[] args) {
		return null;
	}

	public boolean acceptStaticCall(String method, Object[] args) {
		return IMPORT.equals(method);
	}

	public Object staticCall(String method, Object[] args) {
		if (IMPORT.equals(method)) {
			Object result = null;
			for (Object arg : args) {
				if (arg != null) {
					try {
						Object importObj = core.getValue(IMPORT + "." + arg.toString());
						if (importObj instanceof String) {
							result = core.charon.execute((String) importObj);
						}
					} catch (Exception e) {
					}
				}
			}
			return result;
		}
		return null;
	}
}
