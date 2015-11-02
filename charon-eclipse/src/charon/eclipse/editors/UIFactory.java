package charon.eclipse.editors;

import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;

import charon.eclipse.CharonPlugin;
import json.JSONParser;

/**
 * supported "type"s are:<br/>
 * label [text ]<br/>
 * button [text default icon]<br/>
 * text [text readonly ]<br/>
 * table[ fullselection ]<br/>
 * list [multiselection ]<br/>
 * combo [readonly ]<br/>
 * spinner <br/>
 * container [columns +components]<br/>
 * section [text c+omponents]<br/>
 * splitter [horizontal +components]<br/>
 */
public class UIFactory {

	/**
	 * generate UI and return map with all id to component relation
	 */

	public final static Map<String, Object> createUI(String uiJSON, ResourceBundle i18n, Composite parent) {
		UIFactory ui = new UIFactory(i18n, null, null);
		return ui._createUI(uiJSON, parent);
	}

	public final static Map<String, Object> createUI(String uiJSON, ResourceBundle i18n, FormToolkit toolkit,
			Form form) {
		UIFactory ui = new UIFactory(i18n, form, toolkit);
		return ui._createUI(uiJSON, form.getBody());
	}

	public final static Map<String, Object> createUI(Map<?, ?> uiJSON, ResourceBundle i18n, FormToolkit toolkit,
			Form form) {
		UIFactory ui = new UIFactory(i18n, form, toolkit);
		return ui._createUI(uiJSON, form.getBody());
	}

	private static final String ID = "id";
	private static final String TYPE = "type";
	private static final String COMPONENTS = "components";
	private static final String TEXT = "text";
	private static final String ICON = "icon";
	private static final String READONLY = "readonly";

	/**
	 * this map contains all id to components
	 */
	private final Map<String, Object> components;
	private final Form form;
	FormToolkit toolkit;
	private final ResourceBundle i18n;

	private UIFactory(ResourceBundle i18n, Form form, FormToolkit toolkit) {
		components = new HashMap<String, Object>(256);
		this.form = form;
		this.toolkit = toolkit;
		this.i18n = i18n;
	}

	private final Map<String, Object> _createUI(String uiJSON, Composite parent) {
		JSONParser parser = new JSONParser();
		try {
			Map<?, ?> json = parser.parseJSONString(uiJSON);
			return _createUI(json, parent);
		} catch (Exception e) {
			CharonPlugin.reportException(e);
		}
		return null;
	}

	private final Map<String, Object> _createUI(Map<?, ?> json, Composite parent) {
		if (json == null || json.size() <= 0) {
			return null;
		}
		try {
			defineGrid(parent, json);
			defineLayout(parent, json);
			createSubComponents(parent, json);

			if (form != null) {
				createMenu(parent, json);
				createToolbar(parent, json);
				form.getToolBarManager().update(true);
				form.getMenuManager().update(true);
			}
			return components;

		} catch (Exception e) {
			CharonPlugin.reportException(e);
		}
		return null;
	}

	private final void createComponent(Composite parent, Map<?, ?> json) {
		String elementName = getAttribute(json, TYPE);
		if (elementName == null) {
			return;
		}
		if ("label".equals(elementName)) {
			createLabel(parent, json);
		} else if ("button".equals(elementName)) {
			createButton(parent, json);
		} else if ("text".equals(elementName)) {
			createText(parent, json);
		} else if ("table".equals(elementName)) {
			createTable(parent, json);
		} else if ("list".equals(elementName)) {
			createList(parent, json);
		} else if ("combo".equals(elementName)) {
			createCombo(parent, json);
		} else if ("spinner".equals(elementName)) {
			createSpinner(parent, json);
		} else if ("container".equals(elementName)) {
			createContainer(parent, json);
		} else if ("section".equals(elementName)) {
			createSection(parent, json);
		} else if ("splitter".equals(elementName)) {
			createSplitter(parent, json);
		}
	}

	private final void createSubComponents(Composite parent, Map<?, ?> json) {
		java.util.List<?> components = getListAttribute(json, COMPONENTS);
		if (components == null) {
			return;
		}
		for (Object component : components) {
			if (component instanceof Map) {
				createComponent(parent, (Map<?, ?>) component);
			}
		}
	}

	private final void createToolbar(Composite parent, Map<?, ?> json) {
		java.util.List<?> components = getListAttribute(json, "toolbar");
		if (components == null) {
			return;
		}
		for (Object component : components) {
			if (component instanceof Map) {
				createToolbarAction(parent, (Map<?, ?>) component);
			}
		}
	}

	private final void createMenu(Composite parent, Map<?, ?> json) {
		java.util.List<?> components = getListAttribute(json, "menu");
		if (components == null) {
			return;
		}
		for (Object component : components) {
			if (component instanceof Map) {
				createMenuAction(parent, (Map<?, ?>) component);
			}
		}
	}

	private final void defineGrid(Composite component, Map<?, ?> json) {
		GridLayout layout = new GridLayout();
		int value = getIntAttribute(json, "columns", 1);
		layout.numColumns = value;
		layout.marginWidth = 1;
		layout.marginHeight = 1;
		component.setLayout(layout);
	}

	private final int getAlignment(String align) {
		final int len = (align == null) ? 0 : align.length();
		if (6 == len) {// CENTER
			return GridData.CENTER;
		} else if (3 == len) {// END
			return GridData.END;
		} else if (4 == len) {// FILL
			return GridData.FILL;
		}
		return GridData.BEGINNING;
	}

	// colspan:2, rowspan:2, width:100, height:20,
	// align:"BEGINING|CENTER|END|FILL", valign:"BEGIN|CENTER|END|FILL"
	private final void defineLayout(Control control, Map<?, ?> json) {

		int align = getAlignment(getAttribute(json, "align"));
		int valign = getAlignment(getAttribute(json, "valign"));
		GridData gdata = new GridData(align, valign, align == GridData.FILL, valign == GridData.FILL);
		gdata.horizontalIndent = 0;
		gdata.verticalIndent = 0;
		{
			final int value = getIntAttribute(json, "width", 0);
			if (value != 0)
				gdata.widthHint = value;
		}
		{
			final int value = getIntAttribute(json, "height", 0);
			if (value != 0)
				gdata.heightHint = value;
		}
		{
			final int value = getIntAttribute(json, "colspan", 0);
			if (value != 0)
				gdata.horizontalSpan = value;
		}
		{
			final int value = getIntAttribute(json, "rowspan", 0);
			if (value != 0)
				gdata.verticalSpan = value;
		}

		control.setLayoutData(gdata);
	}

	// type:button, id:"javaName", text:"buttonText", icon:"path",
	// default:"true", check: true
	private final void createButton(Composite parent, Map<?, ?> json) {
		int style = SWT.PUSH;
		final boolean readonly = getBooleanAttribute(json, "check");
		if (readonly)
			style = SWT.CHECK;

		Button button = (toolkit == null) ? new Button(parent, style) : toolkit.createButton(parent, "", style);
		defineLayout(button, json);
		{
			final String value = getAttribute(json, ID);
			if (value != null) {
				components.put(value, button);
			}
		}
		{
			final String value = getAttribute(json, TEXT);
			if (value != null) {
				button.setText(_(value));
			}
		}
		{
			final String value = getAttribute(json, ICON);
			if (value != null) {
				button.setImage(CharonPlugin.getImage(value));
			}
		}

		{
			final boolean value = getBooleanAttribute(json, "default");
			if (value) {
				Shell shell = button.getShell();
				if (shell != null) {
					shell.setDefaultButton(button);
				}
			}
		}
	}

	// type:label, id:"javaName", text:"labelText
	private final void createLabel(Composite parent, Map<?, ?> json) {
		Label label = (toolkit == null) ? new Label(parent, 0) : toolkit.createLabel(parent, "");
		defineLayout(label, json);
		{
			final String value = getAttribute(json, ID);
			if (value != null) {
				components.put(value, label);
			}
		}
		{
			final String value = getAttribute(json, TEXT);
			if (value != null) {
				label.setText(_(value));
			}
		}
	}

	// type:text, id:"javaName", text:"labelText", readonly:true, html:true
	// multi:true, wrap:true, hscroll:true, vscroll:true
	private final void createText(Composite parent, Map<?, ?> json) {
		int style = SWT.BORDER;

		if (getBooleanAttribute(json, READONLY)) {
			style |= SWT.READ_ONLY;
		}
		if (getBooleanAttribute(json, "multi")) {
			style |= SWT.MULTI;
		}
		if (getBooleanAttribute(json, "wrap")) {
			style |= SWT.WRAP;
		}
		if (getBooleanAttribute(json, "hscroll")) {
			style |= SWT.H_SCROLL;
		}
		if (getBooleanAttribute(json, "vscroll")) {
			style |= SWT.V_SCROLL;
		}

		final Control text;
		if (toolkit == null) {
			text = new Text(parent, style);
		} else {
			if (getBooleanAttribute(json, "html")) {
				text = toolkit.createFormText(parent, false);
			} else {
				text = toolkit.createText(parent, "", style);
			}
		}

		defineLayout(text, json);
		{
			final String value = getAttribute(json, ID);
			if (value != null) {
				components.put(value, text);
			}
		}
		{
			final String value = getAttribute(json, TEXT);
			if (value != null && text instanceof Text) {
				((Text) text).setText(_(value));
			}
		}
	}

	// type:table, id:"javaName", fullselection:true
	private final void createTable(Composite parent, Map<?, ?> json) {
		int style = 0;
		if (getBooleanAttribute(json, "fullselection")) {
			style |= SWT.FULL_SELECTION;
		}

		Table table = (toolkit == null) ? new Table(parent, style) : toolkit.createTable(parent, style);
		defineLayout(table, json);
		{
			final String value = getAttribute(json, ID);
			if (value != null) {
				components.put(value, table);
			}
		}
	}

	// type:list, id="javaName", multiselection:true
	private final void createList(Composite parent, Map<?, ?> json) {
		int style = SWT.BORDER | SWT.V_SCROLL;
		if (getBooleanAttribute(json, "multiselection")) {
			style |= SWT.MULTI;
		}

		List list = new List(parent, style);
		defineLayout(list, json);
		{
			final String value = getAttribute(json, ID);
			if (value != null) {
				components.put(value, list);
			}
		}
	}

	// type:combo, id:"javaName", readonly:true
	private final void createCombo(Composite parent, Map<?, ?> json) {
		int style = SWT.BORDER;
		if (getBooleanAttribute(json, READONLY)) {
			style |= SWT.READ_ONLY;
		}

		Combo combo = new Combo(parent, style);
		defineLayout(combo, json);
		{
			final String value = getAttribute(json, ID);
			if (value != null) {
				components.put(value, combo);
			}
		}
	}

	// type:spinner id:"javaName"
	private final void createSpinner(Composite parent, Map<?, ?> json) {
		Spinner spinner = new Spinner(parent, SWT.BORDER);
		defineLayout(spinner, json);
		{
			final String value = getAttribute(json, ID);
			if (value != null) {
				components.put(value, spinner);
			}
		}
	}

	// type:container
	private final void createContainer(Composite parent, Map<?, ?> json) {
		Composite grid = (toolkit == null) ? new Composite(parent, 0) : toolkit.createComposite(parent, 0);
		defineGrid(grid, json);
		defineLayout(grid, json);
		{
			final String value = getAttribute(json, ID);
			if (value != null) {
				components.put(value, grid);
			}
		}
		createSubComponents(grid, json);
	}

	// type:section text:"text"
	private final void createSection(Composite parent, Map<?, ?> json) {
		Section section = (toolkit == null) ? new Section(parent, Section.TITLE_BAR)
				: toolkit.createSection(parent, Section.TITLE_BAR);
		{
			final String value = getAttribute(json, ID);
			if (value != null) {
				components.put(value, section);
			}
		}
		{
			final String value = getAttribute(json, TEXT);
			if (value != null) {
				section.setText(_(value));
			}
		}
		section.marginWidth = 1;
		section.marginHeight = 1;
		defineLayout(section, json);

		Composite grid = (toolkit == null) ? new Composite(section, 0) : toolkit.createComposite(section, SWT.WRAP);
		section.setClient(grid);
		if (toolkit != null) {
			toolkit.paintBordersFor(grid);
		}
		defineGrid(grid, json);

		createSubComponents(grid, json);
	}

	// type:splitter horizontal:10 vertical:20 +components
	private final void createSplitter(Composite parent, Map<?, ?> json) {
		final int horizontal = getIntAttribute(json, "horizontal", 0);
		final int vertical = getIntAttribute(json, "vertical", 0);

		int style;
		int size;
		if (vertical != 0) {
			style = SWT.VERTICAL;
			size = vertical;
		} else {
			style = SWT.HORIZONTAL;
			size = horizontal;
		}
		SashForm splitter = new SashForm(parent, style);
		splitter.setSashWidth(2);
		defineLayout(splitter, json);
		{
			final String value = getAttribute(json, ID);
			if (value != null) {
				components.put(value, splitter);
			}
		}

		createSubComponents(splitter, json);
		final int segments = splitter.getChildren().length;
		final int[] weights = new int[segments];

		if (size > 0 && size < 100) {
			int weight = (100 - size) / (segments - 1);
			weights[0] = size;
			for (int i = 1; i < weights.length; i++) {
				weights[i] = weight;
			}
		} else {
			int weight = 100 / segments;
			for (int i = 0; i < weights.length; i++) {
				weights[i] = weight;
			}
		}
		splitter.setWeights(weights);
	}

	// type:action, id:refresh, text:refresh, icon:'icon/execute_command.png'
	private final void createToolbarAction(Composite parent, Map<?, ?> json) {
		final String type = getAttribute(json, TYPE);
		if (!"action".equals(type)) {
			return;
		}

		final String id = getAttribute(json, ID);
		if (id == null) {
			return;
		}

		CharonAction action = new CharonAction(id, Action.AS_PUSH_BUTTON);
		components.put(id, action);
		{
			final String tooltip = getAttribute(json, TEXT);
			if (tooltip != null) {
				action.setToolTipText(_(tooltip));
			}
		}
		{
			final String icon = getAttribute(json, ICON);
			if (icon != null) {
				action.setImageDescriptor(CharonPlugin.getImageDescriptor(icon));
			}
		}
		if (form != null) {
			form.getToolBarManager().add(action);
		}
	}

	// type:action, id:refresh, text:refresh,
	private final void createMenuAction(Composite parent, Map<?, ?> json) {
		final String type = getAttribute(json, TYPE);
		if (!"action".equals(type)) {
			return;
		}

		final String id = getAttribute(json, ID);
		if (id == null) {
			return;
		}
		final String text = getAttribute(json, TEXT);
		if (text == null) {
			return;
		}
		CharonAction action = new CharonAction(text);
		components.put(id, action);
		{
			final String icon = getAttribute(json, ICON);
			if (icon != null) {
				action.setImageDescriptor(CharonPlugin.getImageDescriptor(icon));
			}
		}
		if (form != null) {
			form.getMenuManager().add(action);
		}
	}

	private int getIntAttribute(Map<?, ?> json, String name, int defaultValue) {
		Object result = json.get(name);
		if (result instanceof Number) {
			return ((Number) result).intValue();
		} else {
			return defaultValue;
		}
	}

	private String getAttribute(Map<?, ?> json, String name) {
		Object result = json.get(name);
		if (result != null) {
			return result.toString();
		} else {
			return null;
		}
	}

	private boolean getBooleanAttribute(Map<?, ?> json, String name) {
		Object result = json.get(name);
		if (result instanceof Boolean) {
			return ((Boolean) result).booleanValue();
		} else {
			return "true".equals(result);
		}
	}

	private java.util.List<?> getListAttribute(Map<?, ?> json, String name) {
		Object result = json.get(name);
		if (result instanceof java.util.List) {
			return ((java.util.List<?>) result);
		} else {
			return null;
		}
	}

	private String _(String key) {
		if (i18n == null) {
			return key;
		}
		String translation = i18n.getString(key);
		return (translation == null) ? key : translation;
	}
}
