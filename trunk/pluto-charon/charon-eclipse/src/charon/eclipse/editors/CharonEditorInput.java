package charon.eclipse.editors;

import java.util.Map;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

import charon.eclipse.CharonPlugin;
import charon.eclipse.CharonUI;

public class CharonEditorInput implements IEditorInput {
	public final CharonUI charonIU;
	String title;
	public final Map<?, ?> ui;
	public final Map<?, ?> handlers;

	public CharonEditorInput(CharonUI charonIU, String title, Map<?, ?> ui, Map<?, ?> handlers) {
		this.charonIU = charonIU;
		this.title = title;
		this.ui = ui;
		this.handlers = handlers;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return CharonPlugin.getImageDescriptor("icons/execute_command.png");
	}

	public void setName(String title) {
		this.title = title;
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
