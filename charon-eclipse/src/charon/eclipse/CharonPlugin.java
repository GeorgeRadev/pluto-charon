package charon.eclipse;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import charon.eclipse.editors.CharonEditor;
import cx.ast.Node;
import pluto.charon.Charon;
import pluto.charon.Utils;

/**
 * The activator class controls the plug-in life cycle
 */
public class CharonPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "charon-eclipse"; //$NON-NLS-1$
	public static final String PLUTO_HOST = "plutoHost";
	public static final String PLUTO_PORT = "plutoPort";
	public static final String PLUTO_TIMEOUT = "plutoTimeout";
	public static final String PLUTO_USER = "plutoUser";
	public static final String PLUTO_PASSWORD = "plutoPassword";

	public static final String COMMAND_HANDLER = "charon.eclipse.CommandHandler";
	public static final String CHARON_UI = "charon.eclipse.editors.CharonUI";

	// The shared instance
	private static CharonPlugin plugin;

	private ToolbarContributor toolbarContributor = null;

	/**
	 * The constructor
	 */
	public CharonPlugin() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.
	 * BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;

		IWorkbench workbench = PlatformUI.getWorkbench();

		workbench.addWorkbenchListener(new IWorkbenchListener() {
			public boolean preShutdown(IWorkbench workbench, boolean forced) {
				final IWorkbenchPage activePage = workbench.getActiveWorkbenchWindow().getActivePage();
				for (CharonEditor editor : editors.keySet()) {
					activePage.closeEditor(editor, false);
				}
				return true;
			}

			public void postShutdown(IWorkbench workbench) {

			}
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.
	 * BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static CharonPlugin getDefault() {
		return plugin;
	}

	static HashMap<String, Image> imageCache = new HashMap<String, Image>();
	static HashMap<String, ImageDescriptor> descriptorCache = new HashMap<String, ImageDescriptor>();

	/**
	 * Returns an image descriptor for the image file at the given plug-in
	 * relative path
	 *
	 * @param path
	 *            the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		ImageDescriptor descriptor = descriptorCache.get(path);
		if (descriptor == null) {
			descriptor = imageDescriptorFromPlugin(PLUGIN_ID, path);
			descriptorCache.put(path, descriptor);
		}
		return descriptor;
	}

	public static Image getImage(String path) {
		Image image = imageCache.get(path);
		if (image == null) {
			ImageDescriptor descriptor = getImageDescriptor(path);
			if (descriptor != null) {
				image = descriptor.createImage();
				imageCache.put(path, image);
			}
		}
		return image;
	}

	public void executeTransaction() {
		if (toolbarContributor != null) {
			String transaction = toolbarContributor.getCommand();
			if (transaction != null && transaction.length() > 0) {

				// connect new client to the pluto server
				IPreferenceStore store = CharonPlugin.getDefault().getPreferenceStore();
				String host = store.getString(PLUTO_HOST);
				if (host == null || host.length() <= 0) {
					MessageDialog.openError(null, "Pluto Charon Settings",
							"Define host in : Windows -> Preferences... -> Pluto-Charon Settings");
				}
				int port = store.getInt(PLUTO_PORT);
				if (port <= 0) {
					MessageDialog.openError(null, "Pluto Charon Settings",
							"Define port in : Windows -> Preferences... -> Pluto-Charon Settings");
				}
				int timeout = store.getInt(PLUTO_TIMEOUT);
				if (timeout < 0) {
					MessageDialog.openError(null, "Pluto Charon Settings",
							"Define timeout in : Windows -> Preferences... -> Pluto-Charon Settings");
				}
				String user = store.getString(PLUTO_USER);
				if (user == null || user.length() <= 0) {
					MessageDialog.openError(null, "Pluto Charon Settings",
							"Define user in : Windows -> Preferences... -> Pluto-Charon Settings");
				}
				String pass = store.getString(PLUTO_PASSWORD);
				if (pass == null || pass.length() <= 0) {
					MessageDialog.openError(null, "Pluto Charon Settings",
							"Define user in : Windows -> Preferences... -> Pluto-Charon Settings");
				}
				try {
					Charon client = new Charon(host, port, timeout);
					client.login(user, pass);
					client.addContextHandler("charonUI", new CharonUI(client));

					// get transaction code
					String transactionCode = client.plutoGet(transaction);
					if (transactionCode != null) {
						List<Node> tranzactionAST = Utils.asCX(transactionCode);
						if (tranzactionAST != null) {
							client.charonExecute(tranzactionAST);
						} else {
							MessageDialog.openInformation(null, "Transaction not executable!",
									"Cannot execute transaction: " + transaction);
						}
					} else {
						MessageDialog.openInformation(null, "Transaction not found!",
								"Cannot find transaction: " + transaction);
					}

				} catch (Exception e) {
					CharonPlugin.reportException(e);
				}
			}
		}
	}

	public void setActiveToolbar(ToolbarContributor toolbarContributor) {
		this.toolbarContributor = toolbarContributor;
	}

	public static void reportException(Throwable e) {
		String stackTrace = Arrays.asList(e.getStackTrace()).toString();
		int len = 2048;
		if (stackTrace.length() > len) {
			stackTrace = stackTrace.substring(0, len);
		}
		MessageDialog.openError(null, "error", e.getMessage() + "\n" + stackTrace);
	}

	private ConcurrentHashMap<CharonEditor, String> editors = new ConcurrentHashMap<CharonEditor, String>();

	public void addEditor(CharonEditor editor) {
		editors.put(editor, "");
	}

	public void removeEditor(CharonEditor editor) {
		editors.remove(editor);
	}
}
