package charon.eclipse.editors;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.EditorPart;

import charon.eclipse.CharonPlugin;
import cx.runtime.Function;

public class CharonEditor extends EditorPart implements Listener {

	private FormToolkit toolkit;
	private Form form;
	private CharonEditorInput input;
	private Map<String, Object> idToComponent;
	private Map<Object, String> componentToId;

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		if (input instanceof CharonEditorInput) {
			this.input = (CharonEditorInput) input;
		} else {
			throw new IllegalStateException("EditorInput should be " + CharonEditorInput.class.getName());
		}
		setSite(site);
		setInput(input);
		setTitleImage(CharonPlugin.getImage("icons/execute_command.png"));
	}

	public void dispose() {
		toolkit.dispose();
		super.dispose();
	}

	@Override
	public void createPartControl(Composite parent) {
		toolkit = new FormToolkit(parent.getDisplay());
		form = toolkit.createForm(parent);
		form.setText(((CharonEditorInput) getEditorInput()).getName());
		toolkit.decorateFormHeading(form);

		// createPartControl1(parent);
		idToComponent = UIFactory.createUI(input.ui, null, toolkit, form);

		// create reverse map from object to ID
		componentToId = new HashMap<Object, String>(idToComponent.size());
		for (Entry<String, Object> entry : idToComponent.entrySet()) {
			Object component = entry.getValue();
			componentToId.put(component, entry.getKey());
			// register listeners
			if (component instanceof Control) {
				((Control) component).addListener(SWT.Selection, this);
			} else if (component instanceof CharonAction) {
				((CharonAction) component).setListener(this);
			}
		}
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.type) {
		case SWT.Selection:
			// on click
			// get onClick function and call it
			Object handler = input.handlers.get("onClick");
			if (handler == null) {
				return;
			}
			if (handler instanceof Function) {
				String objectID = componentToId.get(event.widget);
				try {
					input.client.charonFunction((Function) handler, objectID);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (handler instanceof CharonAction) {
				String objectID = componentToId.get(event.data);
				try {
					input.client.charonFunction((Function) handler, objectID);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			break;
		}
	}

	@Override
	public void setFocus() {
	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
	}

}
