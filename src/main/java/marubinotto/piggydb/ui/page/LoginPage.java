package marubinotto.piggydb.ui.page;

import marubinotto.piggydb.model.auth.User;
import marubinotto.piggydb.ui.page.common.AbstractBorderPage;
import org.apache.click.control.Checkbox;
import org.apache.click.control.Form;
import org.apache.click.control.HiddenField;
import org.apache.click.control.PasswordField;
import org.apache.click.control.Submit;
import org.apache.click.control.TextField;

import org.apache.commons.lang.StringUtils;

public class LoginPage extends AbstractBorderPage {

	@Override
	protected boolean needsAuthentication() {
		return false;
	}

	@Override
	protected User autoLoginAsAnonymous() {
		return null; // Disable anonymous auto login
	}

	//
	// Input
	//

	public String original;

	//
	// Control
	//

	public Form loginForm = new Form();
	private TextField userNameField = new TextField("userName", true);
	private PasswordField passwordField = new PasswordField("password", true);
	private Checkbox rememberMeField = new Checkbox("rememberMe", false);
	private HiddenField originalPathField = new HiddenField("original", String.class);

	@Override
	public void onInit() {
		super.onInit();
		initControls();
	}

	private void initControls() {
		this.loginForm.add(this.originalPathField);

		this.userNameField.setLabel(getMessage("LoginPage-userName"));
		this.userNameField.setSize(30);
		this.loginForm.add(this.userNameField);

		this.passwordField.setLabel(getMessage("LoginPage-password"));
		this.passwordField.setSize(30);
		this.loginForm.add(this.passwordField);

		this.rememberMeField.setLabel(getMessage("LoginPage-rememberMe"));
		this.loginForm.add(this.rememberMeField);

		this.loginForm.add(new Submit("ok", "  Log In  ", this, "onOkClick"));
	}

	public boolean onOkClick() throws Exception {
		if (!this.loginForm.isValid()) return true;

		getSession().invalidateIfExists();

		User user = getDomain().getAuthentication().
			authenticate(this.userNameField.getValue(), this.passwordField.getValue());
		if (user == null) {
			this.loginForm.setError(getMessage("LoginPage-login-error"));
			return true;
		}
		
		getSession().start(user, this.rememberMeField.isChecked());

		String originalPath = this.originalPathField.getValue();
		// Avoid redirecting to external unknown URLs
		if (StringUtils.isNotBlank(originalPath) && originalPath.startsWith("/")) {
			setRedirect(originalPath);
		}
		else {
			setRedirect(HomePage.class);
		}

		return false;
	}

	@Override
	public void onGet() {
		super.onGet();

		if (isAuthenticated()) {
			if (this.user.isAnonymous()) {
				getSession().invalidateIfExists();
				this.user = null;
				getLogger().info("Invalidated an anonymous session.");
			}
			else {
				setRedirect(HomePage.class);
			}
		}
	}

	@Override
	public void onRender() {
		super.onRender();
		if (this.original != null) this.originalPathField.setValue(this.original);
	}
}
