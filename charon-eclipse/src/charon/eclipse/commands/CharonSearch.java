package charon.eclipse.commands;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;

public class CharonSearch extends AbstractHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		MessageDialog.openInformation(null, "search", "search will be here");
		return null;
	}

}
