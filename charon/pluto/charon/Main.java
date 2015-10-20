package pluto.charon;

public class Main {

	private static void help() {
		System.out.println("usage: host port user pass filename.cx");
	}

	public static void main(String[] args) throws Throwable {
		if (args.length <= 4) {
			help();
			return;
		}

		String host = args[0];
		int port = Integer.parseInt(args[1], 10);
		String user = args[2];
		String pass = args[3];
		String filename = args[4];

		Charon client = new Charon(host, port, 20000);
		try {
			client.login(user, pass);

			 client.charonExecute("");

			client.logout();
		} finally {
			client.close();
		}
	}
}
