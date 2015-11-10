package pluto.charon;

/**
 * All client functions should be defined here
 */
public class CharonCore {
	protected final Charon charon;

	CharonCore(Charon charon) {
		this.charon = charon;
	}

	public void set(String key, String value) throws Exception {
		charon.plutoSet(key, value);
	}

	public Object get(String key) throws Exception {
		return charon.plutoGet(key);
	}

	public Object search(String prefix) throws Exception {
		return charon.plutoSearch(prefix, 100);
	}

	public Object searchLimit(String prefix, int limit) throws Exception {
		return charon.plutoSearch(prefix, limit);
	}

	public Object executeOnServer(String eval) throws Exception {
		return charon.plutoExecute(eval);
	}

	public Object callOnServer(String functionName, Object[] arguments) throws Exception {
		return charon.plutoCall(functionName, arguments);
	}

	public Object includeOnServer(String argument) throws Exception {
		return charon.plutoCall("include", argument);
	}
}
