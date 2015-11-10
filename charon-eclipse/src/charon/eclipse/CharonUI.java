package charon.eclipse;

import java.util.Map;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.Section;

import charon.eclipse.editors.CharonAction;
import charon.eclipse.editors.CharonEditor;
import charon.eclipse.editors.CharonEditorInput;
import cx.runtime.Function;
import pluto.charon.Charon;

/**
 * offers following cx calls:<br/>
 * ui.createUI(title, ui, handlers) <br/>
 * ui.messageBox(title, message)<br/>
 * ui.errorBox(title, message)<br/>
 * ui.getText(id)<br/>
 * ui.setText(id, text)<br/>
 * ui.addItem(id, text)<br/>
 * ui.setFocus(id)
 */

public class CharonUI {
	public static final String ON_INIT = "onInit";
	public static final String ON_CLICK = "onClick";

	public final Charon client;
	// currently supports only one opened editor
	private CharonEditor editor = null;

	public CharonUI(Charon client) {
		this.client = client;
	}

	public void createUI(String title, Map<?, ?> ui, Map<?, ?> handlers) {
		try { // create editor context

			CharonEditorInput editorInput = new CharonEditorInput(this, title, ui, handlers);

			// open new editor
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			{
				IEditorPart _editor = page.openEditor(editorInput, CharonPlugin.CHARON_UI);
				if (_editor instanceof CharonEditor) {
					CharonPlugin charon = CharonPlugin.getDefault();
					if (editor != null) {
						// close previous editor
						IWorkbench workbench = PlatformUI.getWorkbench();
						final IWorkbenchPage activePage = workbench.getActiveWorkbenchWindow().getActivePage();
						charon.removeEditor(editor);
						activePage.closeEditor(editor, false);
						editor = null;
					}
					// define the new one
					editor = (CharonEditor) _editor;
					charon.addEditor(editor);

					{// call onInit event
						Object handler = editorInput.handlers.get(ON_INIT);
						if (handler == null) {
							return;
						}
						if (handler instanceof Function) {
							try {
								client.charonFunction((Function) handler);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
				}
			}

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

	public String getText(String id) {
		if (editor == null) {
			return null;
		}
		Object control = editor.idToComponent.get(id);
		if (control instanceof Control) {
			if (control instanceof Button) {
				return ((Button) control).getText();
			} else if (control instanceof Text) {
				return ((Text) control).getText();
			} else if (control instanceof Text) {
				return ((Text) control).getText();
			} else if (control instanceof Label) {
				return ((Label) control).getText();
			} else if (control instanceof Label) {
				return ((Label) control).getText();
			} else if (control instanceof Combo) {
				return ((Combo) control).getText();
			} else if (control instanceof Section) {
				return ((Section) control).getText();
			} else if (control instanceof List) {
				String[] selection = ((List) control).getSelection();
				if (selection.length > 0) {
					return selection[0];
				} else {
					return null;
				}
			}
		} else if (control instanceof CharonAction) {
			return ((CharonAction) control).getText();
		} else if (control instanceof CharonEditorInput) {
			return ((CharonEditorInput) control).getName();
		}
		return null;
	}

	public void setText(String id, String text) {
		if (editor == null) {
			return;
		}
		Object control = editor.idToComponent.get(id);
		if (control instanceof Control) {
			if (control instanceof Button) {
				((Button) control).setText(text);
			} else if (control instanceof Text) {
				((Text) control).setText(text);
			} else if (control instanceof Text) {
				((Text) control).setText(text);
			} else if (control instanceof Label) {
				((Label) control).setText(text);
			} else if (control instanceof Label) {
				((Label) control).setText(text);
			} else if (control instanceof Combo) {
				((Combo) control).setText(text);
			} else if (control instanceof Section) {
				((Section) control).setText(text);
			}
		} else if (control instanceof CharonAction) {
			((CharonAction) control).setText(text);
		} else if (control instanceof CharonEditorInput) {
			editor.setName(text);
		}
	}

	public void clear(String id) {
		if (editor == null) {
			return;
		}
		Object control = editor.idToComponent.get(id);
		if (control instanceof Control) {
			if (control instanceof Button) {
				((Button) control).setText("");
			} else if (control instanceof Text) {
				((Text) control).setText("");
			} else if (control instanceof Text) {
				((Text) control).setText("");
			} else if (control instanceof Label) {
				((Label) control).setText("");
			} else if (control instanceof Label) {
				((Label) control).setText("");
			} else if (control instanceof Combo) {
				((Combo) control).setItems(new String[0]);
				((Combo) control).clearSelection();
			} else if (control instanceof List) {
				((List) control).setItems(new String[0]);
			}
		} else if (control instanceof CharonAction) {
			((CharonAction) control).setText("");
		} else if (control instanceof CharonEditorInput) {
			editor.setName("");
		}
	}

	public void addItem(String id, String text) {
		if (editor == null) {
			return;
		}
		Object control = editor.idToComponent.get(id);
		if (control instanceof Control) {
			if (control instanceof List) {
				((List) control).add(text);
			}
		}
	}

	public void setFocus(String id) {
		if (editor == null) {
			return;
		}
		Object control = editor.idToComponent.get(id);
		if (control instanceof Control) {
			((Control) control).setFocus();
		}
	}
}
