package pluto.charon;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Map;
import json.JSONBuilder;
import json.JSONParser;

public class Main {

	private static void help() {
		System.out.println("usage: host port user pass [add,get,meta] ...");
		System.out.println("         add:    filename [json]   uploads a document from filecontent and return a GUID");
		System.out.println("         get:    GUID filename     saves document content into a file");
		System.out.println("        meta:    GUID              returns a json string of the document meta data");
		System.out.println("        meta:    GUID json         set document meta data");
	}

	public static void main(String[] args) throws Throwable {
		if (args.length <= 5) {
			help();
			return;
		}

		String host = args[0];
		int port = Integer.parseInt(args[1], 10);
		String user = args[2];
		String pass = args[3];
		String operation = args[4];

		Charon client = new Charon(host, port, 20000);
		try {
			client.login(user, pass);

			if ("add".equals(operation)) {
				uploadDocument(client, args);
			} else if ("get".equals(operation)) {
				getDocument(client, args);
			} else {
				help();
			}

			client.logout();
		} finally {
			client.close();
		}
	}

	private static void uploadDocument(Charon client, String[] args) throws Exception {
		String filename = args[5];
		Map<Object, Object> metaObj = null;
		if (args.length > 6) {
			// get meta
			String meta = args[6];
			JSONParser parser = new JSONParser();
			try {
				metaObj = parser.parseJSONString(meta);
			} catch (Exception e) {
				System.err.println("invalid json object: " + e.getMessage());
				return;
			}

		}
		File documentFile = new File(filename);
		if (!documentFile.exists() || !documentFile.isFile()) {
			System.err.println("File [" + filename + "] does not exists or it is not a file!");
			return;
		}
		long len = documentFile.length();
		if (len > Integer.MAX_VALUE) {
			System.err.println("File [" + filename + "] is too big " + len + ">" + Integer.MAX_VALUE + "!");
			return;
		}
		byte[] content = new byte[(int) len];
		FileInputStream fis = new FileInputStream(documentFile);
		fis.read(content);
		fis.close();
		// String id = client.plutoSet(content);
		// System.out.println("OK: file uploaded with GUID: " + guid);
		 
		JSONBuilder builder = new JSONBuilder(4096);
		builder.addValue(metaObj);
		System.out.println(builder.toString());
	}

	private static void getDocument(Charon client, String[] args) throws Exception {
		String guid = args[5];
		String filename = args[6];

		// byte[] content = client.plutoGet(guid);
		FileOutputStream fos = new FileOutputStream(filename);
		// fos.write(content);
		fos.close();
	}

}
