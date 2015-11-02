package charon.eclipse.editors;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

public class CharonAction extends Action {
	Listener listener = null;

	CharonAction(String text) {
		super(text);
	}

	public CharonAction(String text, int style) {
		super(text, style);
	}

	public void setListener(Listener listener) {
		this.listener = listener;
	}

	public void run() {
		if (listener == null) {
			return;
		}
		Event event = new Event();
		event.type = SWT.Selection;
		event.data = this;
		listener.handleEvent(event);
	}
}
