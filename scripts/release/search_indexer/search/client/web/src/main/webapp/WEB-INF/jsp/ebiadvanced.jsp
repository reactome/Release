<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib prefix="mytag" uri="/WEB-INF/tags/customTag.tld"%>

<c:import url="header.jsp"/>
<div class="ebi-content">
    <div class="grid_23 padding">
        <h2>Advanced search</h2>
        <form id="adv-search" action="./query" method="get">
            <div class="advSearchBox">
                <br>
                <p>This parser supports full <a href="http://lucene.apache.org/core/2_9_4/queryparsersyntax.html">Lucene QueryParser syntax</a>  including:</p>
                <ul>
                    <li>Quotation marks for exact searches and brackets for grouping terms together</li>
                    <li>Boolean operators: 'AND', 'OR', 'NOT', '+' and '-'</li>
                    <li>Wildcard operators: '?' and '*'</li>
                    <li> Proximity matching: "raf map"~4  searches for raf and map within 4 words from each other</li>
                </ul>
                <%--<p>Additionally you can specify the field you want to search in with:  </p>--%>
                <%--<ul>--%>
                    <%--<li>Available fields: dbId, stId, name, type, species, synonyms, summation, compartmentName, compartmentAccession, goBiologicalProcessName, goBiologicalProcessAccession, goCellularComponentName, goCellularComponentAccession, goMolecularFunctionName, goMolecularFunctionAccession, literatureReferenceTitle, literatureReferenceAuthor, literatureReferencePubMedId, literatureReferenceIsbn, crossReferences, referenceCrossReferences, referenceName, referenceSynonyms, referenceIdentifier, referenceOtherIdentifier, referenceGeneNames</li>--%>
                    <%--<li>Syntax: fieldname:searchterm</li>--%>
                <%--</ul>--%>
                <label for="querySearchBox"></label><textarea name="q" rows="10" id="querySearchBox" class="searchBox">(raf AND map) OR (name:"PTEN S170N") OR (apoptosis) OR stID:"REACT_12858.1"</textarea>
            </div>
            <br>
            <div>
                <h4>Filtering Parameters</h4>
                <table class="advTable">
                    <thead>
                    <tr class="tableHead">
                        <td>Species</td>
                        <td>Types</td>
                        <td>Compartments</td>
                        <td>Reaction types</td>
                    </tr>
                    </thead>
                    <tbody>
                    <tr class="tableBody">
                        <td>
                            <ul class="adv-list">

                                <c:forEach var="available" items="${species_facet.available}">
                                    <li class="term-item"><label>
                                        <input type="checkbox" name="species" value="${available.name}">
                                    </label>${available.name} (${available.count})</li>
                                </c:forEach>
                            </ul>
                        </td>
                        <td>
                            <ul class="adv-list">
                                <c:forEach var="available" items="${type_facet.available}">
                                    <li class="term-item"><label>
                                        <input type="checkbox" name="types" value="${available.name}">
                                    </label>${available.name} (${available.count})</li>
                                </c:forEach>
                            </ul>
                        </td>
                        <td>
                            <ul class="adv-list">

                                <c:forEach var="available" items="${compartment_facet.available}">
                                    <li class="term-item"><label>
                                        <input type="checkbox" name="compartments" value="${available.name}">
                                    </label>${available.name} (${available.count})</li>
                                </c:forEach>
                            </ul>
                        </td>

                        <td>
                            <ul class="adv-list">
                                <c:forEach var="available" items="${keyword_facet.available}">
                                    <li class="term-item"><label>
                                        <input type="checkbox" name="keywords" value="${available.name}">
                                    </label>${available.name} (${available.count})</li>
                                </c:forEach>
                            </ul>
                        </td>
                    </tr>
                    </tbody>
                </table>
            </div>
            <input type="hidden" name="cluster" value="true"/>
            <input type="submit" name="submitSearch" value="Search" class="submit" />
        </form>
    </div>
</div>

<div class="clear"></div>

</div>            <%--A weird thing to avoid problems--%>
<c:import url="footer.jsp"/>