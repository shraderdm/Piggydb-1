package marubinotto.piggydb.api;

import static java.lang.Integer.parseInt;
import static marubinotto.util.CollectionUtils.map;
import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.apache.commons.lang.StringUtils.isNumeric;
import static spark.Spark.before;
import static spark.Spark.get;
import static spark.Spark.halt;
import marubinotto.piggydb.model.auth.User;
import marubinotto.piggydb.service.DomainModelBeans;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import spark.Filter;
import spark.Request;
import spark.Response;
import spark.servlet.SparkApplication;

public class PiggydbApi implements SparkApplication {
  
  private static Log log = LogFactory.getLog(PiggydbApi.class);
  
  protected DomainModelBeans domain;
  
  @Override
  public void init() {
    before(new Filter() {
      @Override
      public void handle(Request request, Response response) {
        log.debug("path: " + request.raw().getRequestURI());
        
        if (domain == null) {
            synchronized(PiggydbApi.this) {
                if (domain == null) {
                    ApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(
                        request.raw().getServletContext());
                    domain = new DomainModelBeans(context);
                }
            }
        }

        Session session = new Session(
          request.raw(), 
          response.raw(), 
          domain.getAuthentication().isEnableAnonymous());
        
        User user = session.getUser();
        if (user == null) {
            // Auto login as anonymous
            user = domain.getAuthentication().authenticateAsAnonymous();
            if (user != null) {
                session.start(user, null);
                log.debug("Anonymous session created");
            }
        }

        request.attribute("piggydb.session", session);
        request.attribute("piggydb.user", user);
        
        if (!request.raw().getRequestURI().equals("/login")) {
          if (user == null) {
            halt(401, "Unauthorized");
          }
        }
      }
    });
    
    register(new ApiRoute("/login") {
      @Override
      protected Object doHandle(Request request, Response response) throws Exception {
        Session session = request.attribute("piggydb.session");

        String userName = request.queryParams("user");
        String password = request.queryParams("password");
        String maxAge = request.queryParams("maxAge");

        session.invalidateIfExists();
        User user = null;
        if (isNotBlank(userName) && isNotBlank(password)) {
          user = domain.getAuthentication().authenticate(userName, password);
        }
        
        if (user == null) {
          return error("invalid-credentials", "Couldn't log in");
        }
        
        session.start(user, isNumeric(maxAge) ? parseInt(maxAge) : null);
        
        return map("sessionId", session.getId());
      }
    });
    
    register(new ApiRoute("/logout") {
      @Override
      protected Object doHandle(Request request, Response response) throws Exception {
        Session session = request.attribute("piggydb.session");
        session.invalidateIfExists();
        return "Bye";
      }
    });
    
    register(new ApiRoute("/hello") {
      @Override
      protected Object doHandle(Request request, Response response) throws Exception {
        return "Hello!";
      }
    });
  }
  
  private void register(ApiRoute route) {
    if (route.getAcceptType() != null)
      get(route.getPath(), route.getAcceptType(), route);
    else
      get(route.getPath(), route);
  }
}
