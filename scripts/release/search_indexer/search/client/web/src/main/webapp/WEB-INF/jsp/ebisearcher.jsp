<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib prefix="mytag" uri="/WEB-INF/tags/customTag.tld"%>

<c:import url="header.jsp"/>

<div class="ebi-content">

    <div class="grid_24 ">
        <h2>Search results for <span class="searchterm">${q}</span></h2>
        <p class="paddingleft">Showing <strong>${groupedResult.rowCount}</strong>  of <strong>${groupedResult.numberOfMatches}</strong></p>
    </div>

    <div class="grid_18 push_6 " id="search-results">
        <c:choose>
            <c:when test="${not empty groupedResult.results}">
                <div class="groupedResult">
                    <c:forEach var="result" items="${groupedResult.results}">
                        <div class="result-category">
                            <c:url var="url" value="">
                                <c:param name="q" value="${q}"/>
                            </c:url>
                            <c:choose>
                                <c:when test="${cluster}">
                                    <h3><a href="./query${url}<mytag:today name="species" value="${species}"/>&amp;types=${result.typeName}<mytag:today name="compartments" value="${compartments}"/><mytag:today name="keywords" value="${keywords}"/>&amp;cluster=${cluster}" title="show all ${result.typeName}" rel="nofollow">${result.typeName}</a> <span>(${result.rowCount} results from a total of ${result.entriesCount})</span></h3>
                                </c:when>
                                <c:otherwise>
                                    <h3>${result.typeName} <span>(${result.rowCount} results from a total of ${result.entriesCount})</span></h3>
                                </c:otherwise>
                            </c:choose>
                            <c:forEach var="entry" items="${result.entries}">
                                <div class="result">
                                    <div class="result-title">
                                        <h4 class="title">
                                            <img src="./resources/images/${entry.exactType}.png" title="${entry.exactType}" width="14" height="13" alt=""/>
                                            <c:if test="${entry.isDisease}">
                                                <img src="./resources/images/isDisease.png" title="Disease related entry" width="13" height="14" alt=""/>
                                            </c:if>
                                            <c:choose>
                                                <c:when test="${entry.species == 'Entries without species'}" >
                                                    <a href="./detail/${entry.id}" class="" title="Show Details" rel="nofollow">${entry.name} <span>${entry.compartmentNames}</span></a>
                                                </c:when>
                                                <c:otherwise>
                                                    <a href="./detail/${entry.id}" class="" title="Show Details" rel="nofollow">${entry.name} <span>(${entry.species})</span></a>
                                                </c:otherwise>
                                            </c:choose>
                                        </h4>
                                    </div>
                                    <div class="result-detail">
                                        <c:if test="${not empty entry.regulator}">
                                            <span>Regulator: <a href="./detail/${entry.regulatorId}" class="" title="Show Details" rel="nofollow">${entry.regulator}</a></span>
                                            <br>
                                        </c:if>
                                        <c:if test="${not empty entry.regulatedEntity}">
                                            <span>Regulated entity: <a href="./detail/${entry.regulatedEntityId}" class="" title="Show Details" rel="nofollow">${entry.regulatedEntity}</a></span>
                                            <br>
                                        </c:if>
                                        <c:if test="${not empty entry.referenceIdentifier}">
                                            <%--<span>Primary external reference: ${entry.databaseName} <a href="${entry.referenceURL}" class="" title="show: ${entry.databaseName}" rel="nofollow">${entry.referenceName}: ${entry.referenceIdentifier}</a></span>--%>
                                            <span>Primary external reference: ${entry.databaseName} <a href="${entry.referenceURL}" class="" title="show: ${entry.databaseName}" rel="nofollow">${entry.referenceName}: ${entry.referenceIdentifier}</a></span>
                                            <br>
                                        </c:if>
                                        <%--<c:if test="${not empty entry.referenceName}">--%>
                                            <%--<span>Primary external reference: ${entry.databaseName} ${entry.referenceName}</span>--%>
                                            <%--<br>--%>
                                        <%--</c:if>--%>
                                        <c:if test="${not empty entry.summation}">
                                            <div class='summation'> ${entry.summation}</div>
                                        </c:if>
                                    </div>
                                </div> <!-- result -->
                            </c:forEach>
                        </div>
                    </c:forEach>
                </div>
                <div class="pagination">
                    <c:choose>
                        <c:when test="${maxpage>1}">
                            <c:choose>
                                <c:when test="${1 == page}">
                                    <span class="search-page active">first</span>
                                </c:when>
                                <c:otherwise>
                                    <c:url var="url" value="">
                                        <c:param name="q" value="${q}"/>
                                    </c:url>
                                    <a class="search-page" href="./query${url}<mytag:today name="species" value="${species}"/><mytag:today name="types" value="${types}"/><mytag:today name="compartments" value="${compartments}"/><mytag:today name="keywords" value="${keywords}"/>&amp;cluster=${cluster}&amp;page=1">first</a>
                                </c:otherwise>
                            </c:choose>
                            <c:forEach var="val" begin="2" end="${maxpage - 1}" >
                                <c:if test="${val > page-3 && val < page+3}">
                                    <c:choose>
                                        <c:when test="${val == page}">
                                            <span class="search-page active">${val}</span>
                                        </c:when>
                                        <c:otherwise>
                                            <c:url var="url" value="">
                                                <c:param name="q" value="${q}"/>
                                            </c:url>
                                            <a class="search-page" href="./query${url}<mytag:today name="species" value="${species}"/><mytag:today name="types" value="${types}"/><mytag:today name="compartments" value="${compartments}"/><mytag:today name="keywords" value="${keywords}"/>&amp;cluster=${cluster}&amp;page=${val}">${val}</a>
                                        </c:otherwise>
                                    </c:choose>
                                </c:if>
                            </c:forEach>
                            <c:choose>
                                <c:when test="${maxpage == page}">
                                    <span class="search-page active">last</span>
                                </c:when>
                                <c:otherwise>
                                    <c:url var="url" value="">
                                        <c:param name="q" value="${q}"/>
                                    </c:url>
                                    <a class="search-page" href="./query${url}<mytag:today name="species" value="${species}"/><mytag:today name="types" value="${types}"/><mytag:today name="compartments" value="${compartments}"/><mytag:today name="keywords" value="${keywords}"/>&amp;cluster=${cluster}&amp;page=${maxpage}">last</a>
                                </c:otherwise>
                            </c:choose>
                        </c:when>
                    </c:choose>
                </div>
            </c:when>
            <c:otherwise>
                <p class="alert">Sorry we could not find any entry matching "${q}" with the currently selected filters</p>
            </c:otherwise>
        </c:choose>
    </div>

    <div class="grid_6 pull_18 " id="search-filters">
        <div class="filter-wrapper">
            <form id="form_facets" action="./query" method="get">

                <input type="hidden" name="q" value="<c:out value='${q}'/>"/>
                <div class="facet" id="species">
                    <h5>Species</h5>
                    <ul class="term-list">
                        <c:forEach var="selected" items="${species_facet.selected}">
                            <li class="term-item">
                                <label><input type="checkbox" onclick="this.form.submit();"  name="species" value="${selected.name}" checked></label> ${selected.name} (${selected.count})</li>
                        </c:forEach>
                        <c:forEach var="available" items="${species_facet.available}">
                            <li class="term-item"><label>
                                <input type="checkbox" onclick="this.form.submit();" name="species"
                                       value="${available.name}">
                            </label>${available.name} (${available.count})</li>
                        </c:forEach>
                    </ul>
                </div>
                <c:if test="${not empty type_facet.available || not empty type_facet.selected }">
                    <div class="facet" id="type">
                        <h5>Types</h5>
                        <ul class="term-list">
                            <c:forEach var="selected" items="${type_facet.selected}">
                                <li class="term-item"><label>
                                    <input type="checkbox" onclick="this.form.submit();" name="types"
                                           value="${selected.name}" checked>
                                </label>${selected.name} (${selected.count})</li>
                            </c:forEach>
                            <c:forEach var="available" items="${type_facet.available}">
                                <li class="term-item"><label>
                                    <input type="checkbox" onclick="this.form.submit();" name="types"
                                           value="${available.name}">
                                </label>${available.name} (${available.count})</li>
                            </c:forEach>
                        </ul>
                    </div>
                </c:if>
                <c:if test="${not empty compartment_facet.available || not empty compartment_facet.selected }">
                    <div class="facet" id="compartment">
                        <h5>Compartments</h5>
                        <ul class="term-list">
                            <c:forEach var="selected" items="${compartment_facet.selected}">
                                <li class="term-item"><label>
                                    <input type="checkbox" onclick="this.form.submit();" name="compartments"
                                           value="${selected.name}" checked>
                                </label>${selected.name} (${selected.count})</li>
                            </c:forEach>
                            <c:forEach var="available" items="${compartment_facet.available}">
                                <li class="term-item"><label>
                                    <input type="checkbox" onclick="this.form.submit();" name="compartments"
                                           value="${available.name}">
                                </label>${available.name} (${available.count})</li>
                            </c:forEach>
                        </ul>
                    </div>
                </c:if>
                <c:if test="${not empty keyword_facet.available || not empty keyword_facet.selected }">
                    <div class="facet" id="keywords">
                        <h5>Reaction types</h5>
                        <ul class="term-list">
                            <c:forEach var="selected" items="${keyword_facet.selected}">
                                <li class="term-item"><label>
                                    <input type="checkbox" onclick="this.form.submit();" name="keywords"
                                           value="${selected.name}" checked>
                                </label>${selected.name} (${selected.count})</li>
                            </c:forEach>
                            <c:forEach var="available" items="${keyword_facet.available}">
                                <li class="term-item"><label>
                                    <input type="checkbox" onclick="this.form.submit();" name="keywords"
                                           value="${available.name}">
                                </label>${available.name} (${available.count})</li>
                            </c:forEach>
                        </ul>
                    </div>
                </c:if>
                <div class="facet" id="cluster">
                    <h5>Search properties</h5>
                    <ul class="term-list">
                        <c:choose>
                            <c:when test="${cluster}">
                                <li class="term-item"><label>
                                    <input type="checkbox" onclick="this.form.submit();" name="cluster"
                                           value="true" checked></label>clustered Search</li>
                            </c:when>
                            <c:otherwise>
                                <li class="term-item"><label>
                                    <input type="checkbox" onclick="this.form.submit();" name="cluster"
                                           value="true" ></label>clustered Search</li>
                            </c:otherwise>
                        </c:choose>

                    </ul>
                </div>
            </form>

            <form id="facetReset" action="./query" method="get">
                <div  class="filterButtons">
                    <input type="hidden" name="q" value="<c:out value='${q}'/>"/>
                    <input type="hidden" name="species" value="Homo sapiens"/>
                    <input type="hidden" name="species" value="Entries without species"/>
                    <input type="hidden" name="cluster" value="true"/>
                    <input type="submit" class="submit" value="Reset filters"  />
                </div>
            </form>
            <br>
        </div> <!-- id="search-filters"-->
    </div>
</div>
<div class="clear"></div>

</div>            <%--A weird thing to avoid problems--%>
<c:import url="footer.jsp"/>