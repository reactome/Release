<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib prefix="mytag" uri="/WEB-INF/tags/customTag.tld"%>

<c:import url="header.jsp"/>
<div class="ebi-content">
    <div class="grid_24">
        <h2>No results found for ${q}</h2>
        <c:choose>
            <c:when test="${empty suggestions}">
                <p class="alert">Sorry we could not find any entry matching '${q}'</p>
                <p>Please consider refining your terms:</p>
                <ul class="list paddingleft">
                    <li>Make sure all words are spelled correctly</li>
                    <li>Try different keywords</li>
                    <li>Be more precise: use gene or protein IDs, e.g. Ndc80 or Q04571</li>
                    <li>Remove quotes around phrases to search for each word individually</li>
                </ul>
            </c:when>
            <c:otherwise>
                <h4>Did you mean...</h4>
                <div class="paddingleft">
                    <ul class="list">
                        <c:forEach var="suggestion" items="${suggestions}">
                            <c:url var="url" value="">
                                <c:param name="q" value="${suggestion}"/>
                            </c:url>
                            <li><a href="./query${url}<mytag:today name="species" value="${species}"/><mytag:today name="types" value="${types}"/><mytag:today name="compartments" value="${compartments}"/><mytag:today name="keywords" value="${keywords}"/>&amp;cluster=${cluster}" title="search for ${suggestion}" rel="nofollow">${suggestion}</a></li>
                        </c:forEach>
                    </ul>
                </div>
            </c:otherwise>
        </c:choose>
    </div>
    <div class="clear"></div>
</div>

</div>            <%--A weird thing to avoid problems--%>
<c:import url="footer.jsp"/>