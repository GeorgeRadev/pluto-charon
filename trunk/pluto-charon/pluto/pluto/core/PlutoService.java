package pluto.core;

/**
 * bootstrap for Apache common service:<br/>
 * <br/>
 * register: <br/>
 * pluto.exe //IS//plutoService --Install="C:\pluto\pluto.exe" --Description=
 * "Pluto-Charon Service" --Jvm=auto --Startup=auto --StartPath="C:\pluto"
 * --JvmMs=128 --JvmMx=128 --Classpath="C:\pluto\pluto.jar"
 * --StartClass=pluto.core.PlutoService --StartMode=jvm --StartParams=start
 * --StopMode=jvm --StopClass=pluto.core.PlutoService --StopParams=stop
 * --StopTimeout=20 --LogPath="C:\pluto\log" --LogPrefix="plutoservice"
 * --StdOutput="auto" --StdError="auto" <br/>
 * test: <br/>
 * pluto.exe //TS//plutoService <br/>
 * remove: <br/>
 * prunsrv.exe //DS//plutoService
 */
public class PlutoService implements Runnable {
	private static Thread mainThread;

	public static void main(String[] args) {
		if (args.length > 0 && "stop".equals(args[0])) {
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
