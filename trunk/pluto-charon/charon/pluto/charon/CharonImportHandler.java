package pluto.charon;

import java.util.List;
import java.util.Map;

import cx.ast.Node;
import cx.ast.Visitor;
import cx.runtime.Handler;

public class CharonImportHandler implements Handler {
	private static final String INCLUDE = "include";
	private static final String IMPORT = "import";

	private final Charon charon;

	public CharonImportHandler(Charon charon) {
		this.charon = charon;
	}

	public void init(Visitor cx) {}

	@Override
	public Object[] supportedClasses() {
		return null;
	}

	public String[] supportedStaticCalls() {
		return new String[] { INCLUDE, IMPORT };
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
	public Object staticCall(String method, Object[] args) {
		if (!INCLUDE.equals(method) && !IMPORT.equals(method)) {
			return null;
		}
		Object result = null;
		
		for (Object arg : args) {
			if (arg == null) {
				continue;
			}
			String value;
			try {
				value = charon.plutoGet(arg.toString());
			} catch (Exception e) {
				continue;
			}
			List<Node> code = Utils.asCX(value);
			if (code != null) {
				result = charon.charonExecute(code);
				continue;
			}
			Map<Object, Object> json = Utils.asJSON(value);
			if (json != null) {
				result = json;
				continue;
			}
		}
		return result;
	}

}
