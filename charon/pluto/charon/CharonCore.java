package pluto.charon;

/**
 * All client functions should be defined here
 */
public class CharonCore {
	protected final Charon charon;

	CharonCore(Charon charon) {
		this.charon = charon;
	}

	public Object executeOnServer(String eval) throws Exception {
		return charon.plutoExecute(eval);
	}

	public Object callOnServer(String functionName, Object[] arguments) throws Exception {
		return charon.plutoCall(functionName, arguments);
	}

	public void setValue(String key, String value) throws Exception {
		charon.plutoCall("core.setValue", key, value);
	}

	public Object getValue(String key) throws Exception {
		return charon.plutoCall("core.getValue", key);
	}

	public Object findKeys(String prefix) throws Exception {
		return charon.plutoCall("core.findKeys", prefix);
	}
}
