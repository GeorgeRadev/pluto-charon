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

import charon.eclipse.CharonUI;
import cx.runtime.Function;

public class CharonEditor extends EditorPart implements Listener {

	private FormToolkit toolkit;
	private Form form;
	private CharonEditorInput input;
	public final Map<String, Object> idToComponent = new HashMap<String, Object>();
	public final Map<Object, String> componentToId = new HashMap<Object, String>();

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		if (!(input instanceof CharonEditorInput)) {
			throw new IllegalStateException("EditorInput should be " + CharonEditorInput.class.getName());
		}
		this.input = (CharonEditorInput) input;
		setSite(site);
		setInput(input);
		setPartName(this.input.getName());
	}

	public void setName(String text) {
		input.setName(text);
		setPartName(input.getName());
	}

	public void dispose() {
		toolkit.dispose();
	}

	@Override
	public void createPartControl(Composite parent) {
		toolkit = new FormToolkit(parent.getDisplay());
		form = toolkit.createForm(parent);
		form.setText(((CharonEditorInput) getEditorInput()).getName());
		toolkit.decorateFormHeading(form);

		idToComponent.putAll(UIFactory.createUI(input.ui, null, toolkit, form));

		Object editorid = input.ui.get(UIFactory.ID);
		if (editorid != null) {
			idToComponent.put(editorid.toString(), input);
		}

		// create reverse map from object to ID
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
			Object handler = input.handlers.get(CharonUI.ON_CLICK);
			if (handler == null) {
				return;
			}
			if (handler instanceof Function) {
				if (event.data instanceof CharonAction) {
					String objectID = componentToId.get(event.data);
					try {
						// showBusy(true);
						input.charonIU.client.charonFunction((Function) handler, objectID);
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						// showBusy(false);
					}
				} else {
					String objectID = componentToId.get(event.widget);
					try {
						// showBusy(true);
						input.charonIU.client.charonFunction((Function) handler, objectID);
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						// showBusy(false);
					}
				}
			}
			break;
		}
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
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
	public void setFocus() {
	}

}
