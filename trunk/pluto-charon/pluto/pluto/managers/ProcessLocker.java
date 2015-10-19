package pluto.managers;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

public class ProcessLocker {
	private ProcessLocker() {}
	private static File processLockFile;
	private static FileChannel processLockChannel;
	private static FileLock processLock;

	public static void lock() {
		try {
			processLockFile = new File("process.lock");
			// Check if the lock exist
			if (processLockFile.exists()) { // if exist try to delete it
				processLockFile.delete();
			}
			// Try to get the lock
			processLockChannel = new RandomAccessFile(processLockFile, "rw").getChannel();
			processLock = processLockChannel.tryLock();
			if (processLock == null) {
				// File is lock by other application
				processLockChannel.close();
				System.err.println("Two instances can't run at a time.");
				System.exit(1);
			}
		} catch (IOException e) {
			System.err.println("Could not start process!");
			e.printStackTrace(System.err);
			System.exit(1);
		}
	}

	public static void unlock() {
		// release and delete file lock
		try {
			if (processLock != null) {
				processLock.release();
				processLock = null;
				processLockChannel.close();
				processLockChannel = null;
				processLockFile.delete();
				processLockFile = null;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
