package charon.eclipse;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;

public class ToolbarContributor extends WorkbenchWindowControlContribution {
	private Combo commandsCombo;
	private Set<String> commandsHistory = new HashSet<String>();

	private void executeCommand() {
		CharonPlugin.getDefault().executeTransaction();
	}

	public String getCommand() {
		return commandsCombo.getText();
	}

	public void clearCommand(String command) {
		final String uppercaseCommand = command.toUpperCase();

		if (!commandsHistory.contains(uppercaseCommand)) {
			commandsHistory.add(uppercaseCommand);

			commandsCombo.add(command, 0);
			while (commandsCombo.getItemCount() > 30) {
				String toBeDeleted = commandsCombo.getItem(30);
				commandsHistory.remove(toBeDeleted.toUpperCase());
				commandsCombo.remove(30);
			}
		}
		commandsCombo.setText("");
	}

	@Override
	protected Control createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.SINGLE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		composite.setLayout(layout);

		Label myLabel = new Label(composite, SWT.SINGLE);
		myLabel.setText("Charon:");
		GridData gdata = new GridData(GridData.FILL, GridData.FILL, true, true);
		gdata.widthHint = 100;
		gdata.heightHint = 200;
		commandsCombo = new Combo(composite, SWT.BORDER | SWT.DROP_DOWN | SWT.V_SCROLL);
		commandsCombo.setLayoutData(gdata);
		commandsCombo.addKeyListener(new KeyListener() {
			@Override
			public void keyReleased(KeyEvent event) {
				if (event.keyCode == SWT.CR && event.stateMask == 0) {
					executeCommand();
				}
			}

			@Override
			public void keyPressed(KeyEvent e) {
			}
		});

		composite.pack();
		CharonPlugin.getDefault().setActiveToolbar(this);
		return composite;
	}

	@Override
	public void dispose() {
		super.dispose();
		CharonPlugin.getDefault().setActiveToolbar(null);
	}
}