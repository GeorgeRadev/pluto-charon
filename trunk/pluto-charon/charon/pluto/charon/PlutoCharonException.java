package pluto.charon;

public class PlutoCharonException extends Exception {

	public PlutoCharonException(String msg) {
		super(msg);
	}

	public PlutoCharonException(String msg, Exception e) {
		super(msg, e);
	}

	public PlutoCharonException(String msg, Throwable e) {
		super(msg, e);
	}
}
