package marubinotto.piggydb.ui.page.control.form;

import marubinotto.util.Assert;
import org.apache.click.Page;
import org.apache.click.control.Form;
import org.apache.click.control.HiddenField;
import org.apache.click.control.ImageSubmit;
import org.apache.click.control.Submit;
import org.apache.click.control.TextField;

public class SingleTagForm extends Form {
	
	private Page page;
	
	public TextField tagField = new TextField("tag");
	private String listenerForAdd;
	
	public HiddenField tagToDeleteField = new HiddenField("tagToDelete", String.class);
	private String listenerForDelete;

	public SingleTagForm(Page page) {
		this.page = page;
	}
	
	public void setListenerForAdd(String listenerForAdd) {
		this.listenerForAdd = listenerForAdd;
	}

	public void setListenerForDelete(String listenerForDelete) {
		this.listenerForDelete = listenerForDelete;
	}

	public void initialize() {
		Assert.Property.requireNotNull(listenerForAdd, "listenerForAdd");
		
		this.tagField.setSize(20);
		this.tagField.setAttribute("class", "single-tag");
		this.tagField.setAttribute("placeholder", getMessage("tag"));
		add(this.tagField);
		
		if (this.listenerForDelete != null) {
			add(this.tagToDeleteField);
			add(new ImageSubmit("deleteTag", "", this.page, this.listenerForDelete));
		}
		
		add(new Submit("addTag", getMessage("add"), this.page, this.listenerForAdd));		
	}
}
