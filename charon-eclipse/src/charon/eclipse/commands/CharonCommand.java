package charon.eclipse.commands;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

import charon.eclipse.CharonPlugin;

public class CharonCommand extends AbstractHandler {

	public Object execute(ExecutionEvent event) throws ExecutionException {
		CharonPlugin.getDefault().executeTransaction();
		return null;
	}
}
