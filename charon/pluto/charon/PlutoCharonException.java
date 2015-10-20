package pluto.charon;

public class PlutoCharonException extends Exception { 
	private static final long serialVersionUID = 0xDEADBEEF81421940L;

	public PlutoCharonException(String msg) {
		super(msg);
	}

	public PlutoCharonException(Throwable e) {
		super(e);
	}

	public PlutoCharonException(String msg, Exception e) {
		super(msg, e);
	}

	public PlutoCharonException(String msg, Throwable e) {
		super(msg, e);
	}
}
