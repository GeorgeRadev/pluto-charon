package charon.eclipse;

import java.util.Map;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import charon.eclipse.editors.CharonEditorInput;
import pluto.charon.Charon;

/**
 * offers following cx calls:<br/>
 * ui.createUI(title, ui, handlers) <br/>
 * ui.messageBox(title, message)<br/>
 * ui.errorBox(title, message)
 */

public class CharonUI {
	private final Charon client;

	public CharonUI(Charon client) {
		this.client = client;
	}

	public void createUI(String title, Map<?, ?> ui, Map<?, ?> handlers) {
		try { // create editor context

			CharonEditorInput editorInput = new CharonEditorInput(client, title, ui, handlers);

			// open new editor
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			page.openEditor(editorInput, CharonPlugin.CHARON_UI);

		} catch (Exception e) {
			CharonPlugin.reportException(e);
		}
	}

	public void messageBox(String title, String message) {
		MessageDialog.openInformation(null, title, message);
	}

	public void errorBox(String title, String message) {
		MessageDialog.openError(null, title, message);
	}
}
