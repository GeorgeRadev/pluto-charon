package charon.eclipse;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class CharonPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public CharonPreferencePage() {
		super(FieldEditorPreferencePage.GRID);
		// Set the preference store for the preference page.
		IPreferenceStore store = CharonPlugin.getDefault().getPreferenceStore();
		setPreferenceStore(store);
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	protected void createFieldEditors() {
		// Initialize all field editors.
		StringFieldEditor plutoHost = new StringFieldEditor(CharonPlugin.PLUTO_HOST, "Pluto &host/ip:",
				getFieldEditorParent());
		addField(plutoHost);
		IntegerFieldEditor plutoPort = new IntegerFieldEditor(CharonPlugin.PLUTO_PORT, "&Port:", getFieldEditorParent());
		addField(plutoPort);
		IntegerFieldEditor plutoTimeout = new IntegerFieldEditor(CharonPlugin.PLUTO_TIMEOUT, "&timeout (miliseconds):",
				getFieldEditorParent());
		addField(plutoTimeout);
		StringFieldEditor plutoUser = new StringFieldEditor(CharonPlugin.PLUTO_USER, "&User name:",
				getFieldEditorParent());
		addField(plutoUser);
		StringFieldEditor plutoPassword = new StringFieldEditor(CharonPlugin.PLUTO_PASSWORD, "Pass&word:",
				getFieldEditorParent());
		addField(plutoPassword);
	}
}
