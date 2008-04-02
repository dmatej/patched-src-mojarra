<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
  <head>
    <title>UICommand</title>
  </head>

  <body>

    <h1>UICommand</h1>

    <h3>$Id: UICommand.jsp,v 1.5 2002/10/03 18:13:28 rkitain Exp $</h3>

    <%@ taglib uri="http://java.sun.com/jsf/core/" prefix="f" %>
    <%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>
    <%@ taglib uri="http://java.sun.com/j2ee/html_basic/" prefix="h" %>

     <fmt:setBundle basename="basic.Resources" scope="session" 
                    var="basicBundle"/>

     <f:usefaces>  

       <p>Form is rendered after this.</p>
     
       <h:form id="standardRenderKitForm" 
                   formName="standardRenderKitForm">

         <h:command_button id="standardRenderKitSubmit" 
                      commandName="standardRenderKitSubmit"
            key="standardRenderKitSubmitLabel" bundle="basicBundle" >
         </h:command_button>

         <table width="100%" border="1" cellpadding="3" cellspacing="3">

<!-- Each included page should have table rows for the appropriate widget. -->

           <%@ include file="table_header.jsp" %>

           <%@ include file="command_button_row.jsp" %>

           <%@ include file="command_hyperlink_row.jsp" %>

         </table>

         <h:command_button id="standardRenderKitSubmit" 
             commandName="standardRenderKitSubmit"
             key="standardRenderKitSubmitLabel" bundle="basicBundle">
         </h:command_button>

       </h:form>

     </f:usefaces>   


  </body>
</html>
