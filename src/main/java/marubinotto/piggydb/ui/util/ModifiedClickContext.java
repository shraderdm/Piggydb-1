package marubinotto.piggydb.ui.util;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.click.ClickServlet;
import org.apache.click.Context;

public class ModifiedClickContext extends Context {

	public ModifiedClickContext(
		ServletContext context, 
		ServletConfig config, 
		HttpServletRequest request, 
		HttpServletResponse response, 
		boolean isPost,
		ClickServlet clickServlet) {

		super(context, config, request, response, isPost, clickServlet);
	}

	@Override
	public HttpSession getSession() {
		return getRequest().getSession();
	}
}
