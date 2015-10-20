package pluto.charon;

import cx.ast.Visitor;
import cx.runtime.Handler;

public class CharonHandler implements Handler {

	private static final String IMPORT = "import";
	private final CharonCore core;

	public CharonHandler(CharonCore core) {
		this.core = core;
	}

	@Override
	public void init(Visitor cx) {

	}

	@Override
	public Object[] supportedClasses() {
		return null;
	}

	@Override
	public void set(Object object, String variable, Object value) {

	}

	@Override
	public Object get(Object object, String variable) {
		return null;
	}

	@Override
	public Object call(Object object, Object[] args) {
		return null;
	}


	@Override
	public String[] supportedStaticCalls() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object staticCall(String method, Object[] args) {
		if (IMPORT.equals(method)) {
			Object result = null;
			for (Object arg : args) {
				if (arg != null) {
					try {
						Object importObj = core.getValue(IMPORT + "." + arg.toString());
						if (importObj instanceof String) {
							result = core.charon.charonExecute((String) importObj);
						}
					} catch (Exception e) {}
				}
			}
			return result;
		}
		return null;
	}

}
