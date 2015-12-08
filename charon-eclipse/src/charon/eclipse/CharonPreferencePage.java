package charon.eclipse;

import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
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
		Composite parent = getFieldEditorParent();
		StringFieldEditor plutoHost = new StringFieldEditor(CharonPlugin.PLUTO_HOST, "Pluto &host/ip*:", parent);
		addField(plutoHost);
		IntegerFieldEditor plutoPort = new IntegerFieldEditor(CharonPlugin.PLUTO_PORT, "&Port*:", parent);
		addField(plutoPort);
		IntegerFieldEditor plutoTimeout = new IntegerFieldEditor(CharonPlugin.PLUTO_TIMEOUT, "&timeout (miliseconds)*:",
				parent);
		addField(plutoTimeout);

		addField(new SpacerFieldEditor(parent));

		StringFieldEditor plutoSSLContext = new StringFieldEditor(CharonPlugin.PLUTO_SSL_CONTEXT, "&SSL Context:",
				parent);
		addField(plutoSSLContext);
		StringFieldEditor plutoKeystoreType = new StringFieldEditor(CharonPlugin.PLUTO_KEYSTORE_TYPE, "&Keystore Type:",
				parent);
		addField(plutoKeystoreType);
		FileFieldEditor plutoCertificationFile = new FileFieldEditor(CharonPlugin.PLUTO_CERTIFICATION_FILE,
				"Certification &file:", parent);
		addField(plutoCertificationFile);
		StringFieldEditor plutoCertificationPassword = new StringFieldEditor(CharonPlugin.PLUTO_CERTIFICATION_PASSWORD,
				"&Certification file password:", parent);
		addField(plutoCertificationPassword);

		addField(new SpacerFieldEditor(parent));

		StringFieldEditor plutoUser = new StringFieldEditor(CharonPlugin.PLUTO_USER, "&User name*:", parent);
		addField(plutoUser);
		StringFieldEditor plutoPassword = new StringFieldEditor(CharonPlugin.PLUTO_PASSWORD, "Pass&word*:", parent);
		addField(plutoPassword);

	}

	public static class SpacerFieldEditor extends FieldEditor {
		Group group = null; 

		public SpacerFieldEditor(Composite parent) {
			super("", "", parent);
		}

		@Override
		protected void adjustForNumColumns(int numColumns) {
			if (group != null) {
				GridData gdata = new GridData(GridData.FILL, GridData.CENTER, true, false);
				gdata.horizontalIndent = 0;
				gdata.verticalIndent = 0;
				gdata.heightHint = 3;
				gdata.horizontalSpan = 3;
				group.setLayoutData(gdata);
			}
		}

		@Override
		protected void doFillIntoGrid(Composite parent, int numColumns) {
			if (group == null) {
				group = new Group(parent, SWT.SHADOW_IN);
			}
			adjustForNumColumns(numColumns);
		}

		@Override
		public void dispose() {
			group = null;
			super.dispose();
		}

		@Override
		protected void doLoad() {
		}

		@Override
		protected void doLoadDefault() {
		}

		@Override
		protected void doStore() {
		}

		@Override
		public int getNumberOfControls() {
			return 0;
		}
	}
}
