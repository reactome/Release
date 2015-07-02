<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<c:import url="../header.jsp"/>
<div class="ebi-content">
    <div class="grid_23 padding">
        <h4>Application Error, please contact support.</h4>

            <p class="paddingleft">Please email us at <strong>help@reactome.org</strong>.</p>
            <h5>Debug Information:</h5>
            <p class="paddingleft">Exception: ${exception.message}
            <p class="paddingleft">Exception Stack Trace:</p>
            <ul class="list paddingleft">
                <c:forEach  var="stacktrace" items="${exception.stackTrace}" >
                    <li class="errorList">${stacktrace}</li>
                </c:forEach>
            </ul>
    </div>
</div>
<div class="clear"></div>

</div>            <%--A weird thing to avoid problems--%>
<c:import url="../footer.jsp"/>