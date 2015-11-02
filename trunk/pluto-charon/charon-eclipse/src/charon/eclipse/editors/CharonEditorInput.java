package charon.eclipse.editors;

import java.util.Map;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

import charon.eclipse.CharonPlugin;
import pluto.charon.Charon;

public class CharonEditorInput implements IEditorInput {
	String title;
	final Charon client;
	final Map<?, ?> ui;
	final Map<?, ?> handlers;

	public CharonEditorInput(Charon client, String title, Map<?, ?> ui, Map<?, ?> handlers) {
		this.title = title;
		this.client = client;
		this.ui = ui;
		this.handlers = handlers;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return CharonPlugin.getImageDescriptor("icons/execute_command.png");
	}

	@Override
	public String getName() {
		return title;
	}

	@Override
	public String getToolTipText() {
		return title;
	}

	@Override
	public boolean exists() {
		return true;
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return null;
	}
}
