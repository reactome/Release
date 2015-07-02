<%--suppress XmlPathReference HtmlUnknownTarget --%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<c:import url="header.jsp"/>

<div class="wrapper">
    <div id="restDocIframeContainer" style="margin: 20px 0 20px 0">
        <iframe src="restIframe.html" width="100%" scrolling="no" id="restDocIframe"></iframe>
    </div>
    <div class="clear"></div>
</div>

</div>            <%--A weird thing to avoid problems--%>
<c:import url="footer.jsp"/>