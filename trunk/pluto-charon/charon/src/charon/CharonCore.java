package charon;

/**
 * All client functions should be defined here
 */
public class CharonCore {
	protected final Charon charon;

	CharonCore(Charon charon) {
		this.charon = charon;
	}

	public Object executeOnServer(String eval) throws Exception {
		return charon.executeOnServer(eval);
	}

	public Object callOnServer(String functionName, Object[] arguments) throws Exception {
		return charon.callOnServer(functionName, arguments);
	}

	public void setValue(String key, String value) throws Exception {
		charon.callOnServer("core.setValue", key, value);
	}

	public Object getValue(String key) throws Exception {
		return charon.callOnServer("core.getValue", key);
	}

	public Object findKeys(String prefix) throws Exception {
		return charon.callOnServer("core.findKeys", prefix);
	}
}
