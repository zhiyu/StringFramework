### What is StringFramework

StringFramework is a free open-source java web framework with a very small footprint. It is based on experience and designed for those who need a simple, elegant and pragmatic toolkit to build high-performing, full-featured web applications.

### Processing of request

* http://Host/

    for example http://localhost/
  
    by default, this request will cause the framework invoke method "index" of class "app.controller.DefaultController.class" and jsp file "WebRoot/view/default/default/index.jsp" will be rendered.

* http://Host/Controller/

    for example http://localhost/user/
  
    by default, this request will cause the framework invoke method "index" of class "app.controller.UserController.class" and jsp file "WebRoot/view/default/user/index.jsp" will be rendered.

* http://Host/Controller/Action

    for example http://localhost/user/login
  
    by default, this request will cause the framework invoke method "login" of class "app.controller.UserController.class" and jsp file "WebRoot/view/default/user/login.jsp" will be rendered.

* http://Host/Package/Controller/

    for example http://localhost/admin/user/
  
    by default, this request will cause the framework invoke method "index" of class "app.controller.admin.UserController.class" and jsp file "WebRoot/view/default/admin/user/index.jsp" will be rendered.

* http://Host/Package/Controller/Action

    for example http://localhost/admin/user/add
  
    by default, this request will cause the framework invoke method "add" of class "app.controller.admin.UserController.class" and jsp file "WebRoot/view/default/admin/user/add.jsp" will be rendered.

### How to use

* Create a java web application project in your IDE, such as Eclipse or Netbeans.
* Go to download page and get the latest release version of StringFramework.
* Copy stringframework.jar to the web application's class path (WEB-INF\lib).
* Create a new file string.properties and move it into the web application's class path.
* To enable the StringFramework to work with the web application, you need to add a Servlet filter class and filter mapping to web.xml. Below is the filter and filter-mapping nodes you should add to web.xml.

        <filter>
            <filter-name>dispatcher</filter-name>
            <filter-class>org.stringframework.dispatcher.FilterDispatcher</filter-class>
        </filter>
        <filter-mapping>
            <filter-name>dispatcher</filter-name>
            <url-pattern>/*</url-pattern>
        </filter-mapping>

* The configuration file sring.properties must be on the web application's class path. 
For more information about the sring.properties configuration file, see next Configuration section.
* Build and run the application.

### Configuration

The only configuration file for StringFramework is string.properties under classpath.

    string.debug                = true
    string.uriEncodingEnabled   = true
    string.uriEncoding          = utf-8
    string.suffix               = 
    string.defaultAction        = index
    string.viewRoot             = view
    string.controllerRoot       = app.controller
    string.applicationName      = Welcome to StringFramework!
    string.applicationDefaultControllerEnabled  = true
    string.applicationDefaultController         = Default
    string.applicationDefaultAction             = index
    string.errorPage404         = 404.jsp
    string.errorPage500         = 500.jsp