/*******************************************************************************
 * Copyright 2014 Rafael Garcia Moreno.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.bladecoder.engineeditor.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.bladecoder.engine.i18n.I18N;
import com.bladecoder.engine.model.Dialog;
import com.bladecoder.engine.model.DialogOption;
import com.bladecoder.engineeditor.Ctx;
import com.bladecoder.engineeditor.ui.components.CellRenderer;
import com.bladecoder.engineeditor.ui.components.EditModelDialog;
import com.bladecoder.engineeditor.ui.components.ModelList;
import com.bladecoder.engineeditor.undo.UndoDeleteOption;
import com.bladecoder.engineeditor.utils.ElementUtils;

public class DialogOptionList extends ModelList<Dialog, DialogOption> {
	Skin skin;

	private ImageButton upBtn;
	private ImageButton downBtn;

	public DialogOptionList(Skin skin) {
		super(skin, true);
		this.skin = skin;

		setCellRenderer(listCellRenderer);

		upBtn = new ImageButton(skin);
		downBtn = new ImageButton(skin);

		toolbar.addToolBarButton(upBtn, "ic_up", "Move up", "Move up");
		toolbar.addToolBarButton(downBtn, "ic_down", "Move down", "Move down");
		toolbar.pack();

		list.addListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event, Actor actor) {
				int pos = list.getSelectedIndex();

				toolbar.disableEdit(pos == -1);
				upBtn.setDisabled(pos == -1 || pos == 0);
				downBtn.setDisabled(pos == -1
						|| pos == list.getItems().size - 1);
			}
		});

		upBtn.addListener(new ChangeListener() {

			@Override
			public void changed(ChangeEvent event, Actor actor) {
				up();
			}
		});

		downBtn.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				down();
			}
		});
	}

	@Override
	protected EditModelDialog<Dialog, DialogOption> getEditElementDialogInstance(DialogOption e) {
		return new EditDialogOptionDialog(skin, parent, e, list.getSelectedIndex());
	}

	@Override
	protected void create() {
		EditDialogOptionDialog dialog = (EditDialogOptionDialog)getEditElementDialogInstance(null);
		dialog.show(getStage());
		dialog.setListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				int pos = list.getSelectedIndex() + 1;

				DialogOption e = ((EditDialogOptionDialog) actor).getElement();
				list.getItems().insert(pos, e);
				list.setSelectedIndex(pos);
				list.invalidateHierarchy();
				
				// Move model object inserted to the end to the selected position
				if (pos != 0 && pos < list.getItems().size) {
					DialogOption e2 = list.getItems().get(pos);
					parent.getOptions().set(pos, e);
					parent.getOptions().set(list.getItems().size - 1, e2);
				}
			}
		});
	}

	private void up() {
		int pos = list.getSelectedIndex();

		if (pos == -1 || pos == 0)
			return;

		Array<DialogOption> items = list.getItems();
		DialogOption e = items.get(pos);
		DialogOption e2 = items.get(pos - 1);


		items.set(pos - 1, e);
		items.set(pos, e2);
		
		parent.getOptions().set(pos - 1, e);
		parent.getOptions().set(pos, e2);

		list.setSelectedIndex(pos - 1);
		upBtn.setDisabled(list.getSelectedIndex() == 0);
		downBtn.setDisabled(list.getSelectedIndex() == list.getItems().size - 1);

		Ctx.project.setModified();
	}

	private void down() {
		int pos = list.getSelectedIndex();
		Array<DialogOption> items = list.getItems();

		if (pos == -1 || pos == items.size - 1)
			return;

		DialogOption e = items.get(pos);
		DialogOption e2 =  items.get(pos + 1);

		
		parent.getOptions().set(pos + 1, e);
		parent.getOptions().set(pos, e2);

		items.set(pos + 1, e);
		items.set(pos, e2);
		list.setSelectedIndex(pos + 1);
		upBtn.setDisabled(list.getSelectedIndex() == 0);
		downBtn.setDisabled(list.getSelectedIndex() == list.getItems().size - 1);

		Ctx.project.setModified();
	}
	
	@Override
	protected void delete() {
			
		DialogOption option = removeSelected();
		
		int idx = parent.getOptions().indexOf(option); 
			
		parent.getOptions().remove(option);
			
		// TRANSLATIONS
		Ctx.project.getI18N().putTranslationsInElement(option);

		// UNDO
		Ctx.project.getUndoStack().add(new UndoDeleteOption(parent, option, idx));
		
		Ctx.project.setModified();
	}
	
	@Override
	protected void copy() {
		DialogOption e = list.getSelected();

		if (e == null)
			return;

		clipboard = (DialogOption)ElementUtils.cloneElement(e);
		toolbar.disablePaste(false);

		// TRANSLATIONS
		Ctx.project.getI18N().putTranslationsInElement(clipboard);
	}

	@Override
	protected void paste() {
		DialogOption newElement = (DialogOption)ElementUtils.cloneElement(clipboard);
		
		int pos = list.getSelectedIndex() + 1;

		list.getItems().insert(pos, newElement);

		parent.addOption(newElement);
		// FIXME
		Ctx.project.getI18N().extractStrings(I18N.PREFIX + parent.getId(), newElement);

		list.setSelectedIndex(pos);
		list.invalidateHierarchy();
		
		Ctx.project.setModified();
	}	

	// -------------------------------------------------------------------------
	// ListCellRenderer
	// -------------------------------------------------------------------------
	private final CellRenderer<DialogOption> listCellRenderer = new CellRenderer<DialogOption>() {

		@Override
		protected String getCellTitle(DialogOption e) {
			String text = e.getText();
			
			int i = parent.getOptions().indexOf(e);

			return i + ". " + Ctx.project.translate(text);
		}

		@Override
		protected String getCellSubTitle(DialogOption e) {

			StringBuilder sb = new StringBuilder();
			String response = e.getResponseText();

			if (response != null && !response.isEmpty())
				sb.append("R: ").append(Ctx.project.translate(response)).append(' ');

//			NamedNodeMap attr = e.getAttributes();
//			
//			for (int i = 0; i < attr.getLength(); i++) {
//				org.w3c.dom.Node n = attr.item(i);
//				String name = n.getNodeName();
//
//				if (name.equals(XMLConstants.TEXT_ATTR) || name.equals(XMLConstants.RESPONSE_TEXT_ATTR))
//					continue;
//
//				String v = n.getNodeValue();
//				sb.append(name).append(':').append(Ctx.project.getSelectedChapter().getTranslation(v)).append(' ');
//			}

			return sb.toString();
		}

		@Override
		protected boolean hasSubtitle() {
			return true;
		}
	};

}
