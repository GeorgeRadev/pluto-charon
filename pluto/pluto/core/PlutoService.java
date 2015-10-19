package pluto.core;


/**
 * bootstrap for apache common service:<br/>
 * plutoService.exe //IS//pluto --Install="C:\pluto\pluto.exe" --Jvm=auto
 * --Startup=auto --StartMode=jvm --Classpath="C:\pluto\pluto.jar"
 * --StartClass=pluto.core.PlutoService
 */
public class PlutoService implements Runnable {
	private static Thread mainThread;

	public static void main(String[] args) {
		if ("stop".equals(args[0])) {
			if (mainThread != null) {
				mainThread.interrupt();
				mainThread = null;
				Thread.yield();
				System.out.println("PlutoService: stop");
				System.exit(0);
			} else {
				System.out.println("PlutoService: Already stopped!");
			}
		} else {
			if (mainThread == null) {
				mainThread = new Thread(new PlutoService());
				mainThread.start();
				System.out.println("PlutoService: start");
			} else {
				System.out.println("PlutoService: Already started!");
			}
		}
	}

	public void run() {
		try {
			Pluto.main(null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		mainThread = null;
	}
}
