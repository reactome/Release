<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib prefix="mytag" uri="/WEB-INF/tags/customTag.tld"%>

<c:import url="header.jsp"/>
<div class="ebi-content" >

<div class="grid_23 padding">
    <h3>${entry.name}
        <c:if test="${not empty entry.stId}">
            <span> (${entry.stId})</span>
        </c:if>
    </h3>
    <c:if test="${not empty entry.species && entry.species != 'Entries without species'}">
        <h4>Species ${entry.species}</h4>
    </c:if>
    <c:if test="${not empty entry.exactType}">
        <h4>Type ${entry.exactType}</h4>
    </c:if>
</div>

<div class="grid_23  padding">
    <c:if test="${not empty entry.summations}">
        <h5>Summation</h5>
        <div class="paddingleft">
            <c:forEach var="summation" items="${entry.summations}">
                <p>${summation}</p>
            </c:forEach>
        </div>
    </c:if>
    <c:if test="${not empty entry.locationsPathwayBrowser}">
        <h5>Locations in the PathwayBrowser</h5>
        <div class="paddingleft">
            <c:forEach var="topLvl" items="${entry.locationsPathwayBrowser}">
                <div class="plus" title="click here to expand or collapse the tree"><img class="image" src="../resources/images/plus.png" title="${entry.exactType}" width="14" height="13" alt=""/> ${topLvl.name}(${topLvl.species})</div>
                <div class="treeContent">
                    <ul class="tree">
                        <li><a href="${topLvl.url}" class=""   title="goto Reactome Pathway Browser" rel="nofollow">${topLvl.name}(${topLvl.species})</a></li>
                        <c:set var="node" value="${topLvl}" scope="request"/>
                        <li> <c:import url="/WEB-INF/jsp/node.jsp"/></li>
                    </ul>
                </div>
            </c:forEach>
        </div>
    </c:if>
    <c:if test="${not empty entry.referenceEntity.derivedEwas}">
        <h4>Other forms of this molecule</h4>

        <ul class="list overflowList paddingleft">
            <c:forEach var="derivedEwas" items="${entry.referenceEntity.derivedEwas}">
                <li><a href="../detail/${derivedEwas.dbId}" class="" title="Show Details" rel="nofollow">${derivedEwas.name} (${derivedEwas.compartment})</a></li>
            </c:forEach>
        </ul>
    </c:if>

</div>

<c:if test="${not empty entry.referenceEntity || not empty entry.compartments || not empty entry.synonyms || not empty entry.reverseReaction || not empty entry.goBiologicalProcess || not empty entry.goMolecularComponent}">
    <div class="grid_23  padding">
        <h5>Additional Information</h5>
        <table class="fixedTable">
            <thead>
            <tr class="tableHead">
                <td></td>
                <td></td>
            </tr>
            </thead>
            <tbody>
            <c:if test="${not empty entry.referenceEntity}">
                <c:if test="${not empty entry.referenceEntity.referenceName}">
                    <tr>
                        <td><strong>External reference name</strong></td>
                        <td><a href="${entry.referenceEntity.database.url}" class="" title="Show Details" rel="show ${entry.referenceEntity.database.url}"> ${entry.referenceEntity.referenceName}</a></td>
                    </tr>
                </c:if>
                <c:if test="${not empty entry.referenceEntity.referenceIdentifier}">
                    <tr>
                        <td><strong>External reference id</strong></td>
                        <td><a href="${entry.referenceEntity.database.url}" class="" title="Show Details" rel="show ${entry.referenceEntity.database.url}"> ${entry.referenceEntity.referenceIdentifier}</a></td>
                    </tr>
                </c:if>
                <c:if test="${not empty entry.referenceEntity.referenceSynonyms}">
                    <tr>
                        <td><strong>external Synonyms</strong></td>
                        <td class="block">
                            <c:forEach var="synonym" items="${entry.referenceEntity.referenceSynonyms}" varStatus="loop">${synonym}<c:if test="${!loop.last}">, </c:if></c:forEach>
                        </td>
                    </tr>
                </c:if>
            </c:if>
                <c:if test="${not empty entry.synonyms}">
                    <tr>
                        <td><strong>Synonyms</strong></td>
                        <td class="block">
                            <c:forEach var="synonym" items="${entry.synonyms}" varStatus="loop">${synonym}<c:if test="${!loop.last}">, </c:if></c:forEach>
                        </td>
                    </tr>
                </c:if>
                <c:if test="${not empty entry.compartments}">
                    <tr>
                        <td><strong>Compartment</strong></td>
                        <td>
                            <c:forEach var="compartment" items="${entry.compartments}" varStatus="loop">
                                <span><a href="${compartment.database.url}" title="show ${compartment.database.name}" rel="nofollow">${compartment.name}</a></span>
                                <c:if test="${!loop.last}">, </c:if>
                            </c:forEach>
                        </td>
                    </tr>
                </c:if>
                <c:if test="${not empty entry.reverseReaction}">
                    <tr>
                        <td><strong>Reverse Reaction</strong></td>
                        <td>
                            <a href="../detail/${entry.reverseReaction.dbId}" class="" title="show Reactome ${entry.reverseReaction.dbId}" rel="nofollow">${entry.reverseReaction.name}</a>
                        </td>
                    </tr>
                </c:if>
            <c:if test="${not empty entry.referenceEntity}">
                <c:if test="${not empty entry.referenceEntity.otherIdentifier}">
                    <tr>
                        <td><strong>Other Identifiers</strong></td>
                        <td class="block">
                            <c:forEach var="otherIdentifier" items="${entry.referenceEntity.otherIdentifier}" varStatus="loop">${otherIdentifier}<c:if test="${!loop.last}">, </c:if></c:forEach>
                        </td>
                    </tr>
                </c:if>
                <c:if test="${not empty entry.referenceEntity.secondaryIdentifier}">
                    <tr>
                        <td><strong>Secondary Identifiers</strong></td>
                        <td>
                            <c:forEach var="secondaryIdentifier" items="${entry.referenceEntity.secondaryIdentifier}" varStatus="loop">${secondaryIdentifier}<c:if test="${!loop.last}">, </c:if>
                            </c:forEach>
                        </td>
                    </tr>
                </c:if>
                <c:if test="${not empty entry.referenceEntity.geneNames}">
                    <tr>
                        <td><strong>Gene Names</strong></td>
                        <td>
                            <c:forEach var="geneNames" items="${entry.referenceEntity.geneNames}" varStatus="loop">${geneNames}<c:if test="${!loop.last}">, </c:if>
                            </c:forEach>
                        </td>
                    </tr>
                </c:if>
                <c:if test="${not empty entry.referenceEntity.chain}">
                    <tr>
                        <td><strong>Chain</strong></td>
                        <td>
                            <c:forEach var="chain" items="${entry.referenceEntity.chain}" varStatus="loop">${chain}<c:if test="${!loop.last}">, </c:if>
                            </c:forEach>
                        </td>
                    </tr>
                </c:if>
            </c:if>
            <c:if test="${not empty entry.goMolecularComponent}">
                <tr>
                    <td><strong>GO Molecular Component</strong></td>
                    <td>
                        <ul class="list overflowList">
                            <c:forEach var="goMolecularComponent" items="${entry.goMolecularComponent}">
                                <li><a href="${goMolecularComponent.database.url}" class=""  title="show ${goMolecularComponent.database.name}" rel="nofollow">${goMolecularComponent.name}</a>( ${goMolecularComponent.accession})</li>
                            </c:forEach>
                        </ul>
                    </td>
                </tr>
            </c:if>
            <c:if test="${not empty entry.goBiologicalProcess}">
                <tr>
                    <td><strong>GO Biological Process</strong></td>
                    <td><a href="${entry.goBiologicalProcess.database.url}" class=""  title="go to ${entry.goBiologicalProcess.database.name}" rel="nofollow">${entry.goBiologicalProcess.name} (${entry.goBiologicalProcess.accession})</a></td>
                </tr>
            </c:if>
            </tbody>
        </table>
    </div>
</c:if>

<c:if test="${not empty entry.input || not empty entry.output || not empty entry.components || not empty entry.candidates || not empty entry.member || not empty entry.repeatedUnits || not empty entry.entityOnOtherCell}">
    <div class="grid_23  padding">
        <h5>Components of this entry</h5>
        <table class="fixedTable">
            <thead>
            <tr class="tableHead">
                <td></td>
                <td></td>
            </tr>
            </thead>
            <tbody>
            <c:if test="${not empty entry.input}">
            <tr>
                <td><strong>Input entries</strong></td>
                <td>
                    <ul class="list overflowAuto">
                        <c:forEach var="input" items="${entry.input}">
                            <li><a href="../detail/${input.dbId}" class="" title="Show Details" rel="nofollow">${input.name} <c:if test="${not empty input.species}">(${input.species})</c:if></a></li>
                        </c:forEach>
                    </ul>
                </td>
            </tr>
            </c:if>
            <c:if test="${not empty entry.output}">
            <tr>
                <td><strong>Output entries</strong></td>
                <td><ul class="list overflowList">
                    <c:forEach var="output" items="${entry.output}">
                        <li><a href="../detail/${output.dbId}" class="" title="Show Details" rel="nofollow">${output.name}<c:if test="${not empty output.species}">(${output.species})</c:if></a></li>
                    </c:forEach>
                </ul></td>
            </tr>
            </c:if>
            <c:if test="${not empty entry.components}">
            <tr>
                <td><strong>Components entries</strong></td>
                <td><ul class="list overflowList">
                    <c:forEach var="component" items="${entry.components}">
                        <li><a href="../detail/${component.dbId}" class="" title="show Reactome" rel="nofollow">${component.name} <c:if test="${not empty component.species}">(${component.species})</c:if></a></li>
                    </c:forEach>
                </ul></td>
            </tr>
            </c:if>
            <c:if test="${not empty entry.candidates}">
            <tr>
                <td><strong>CandidateSet entries</strong></td>
                <td><ul  class="list overflowList">
                    <c:forEach var="candidates" items="${entry.candidates}">
                        <li><a href="../detail/${candidates.dbId}" class="" title="show Reactome ${candidates.dbId}" rel="nofollow">${candidates.name} <c:if test="${not empty candidates.species}">(${candidates.species})</c:if></a></li>
                    </c:forEach>
                </ul></td>
            </tr>
            </c:if>
            <c:if test="${not empty entry.member}">
            <tr>
                <td><strong>Member</strong></td>
                <td><ul class="list overflowList">
                    <c:forEach var="member" items="${entry.member}">
                        <li><a href="../detail/${member.dbId}" class="" title="show Reactome ${member.dbId}" rel="nofollow">${member.name} <c:if test="${not empty member.species}">(${member.species})</c:if></a></li>
                    </c:forEach>
                </ul></td>
            </tr>
            </c:if>
            <c:if test="${not empty entry.repeatedUnits}">
            <tr>
                <td><strong>repeatedUnits</strong></td>
                <td><ul class="list overflowList">
                    <c:forEach var="repeatedUnit" items="${entry.repeatedUnits}">
                        <li><a href="../detail/${repeatedUnit.dbId}" class="" title="show Reactome ${repeatedUnit.dbId}" rel="nofollow">${repeatedUnit.name}</a></li>
                    </c:forEach>
                </ul></td>
            </tr>
            </c:if>
            <c:if test="${not empty entry.entityOnOtherCell}">
            <tr>
                <td><strong>EntityOnOtherCell</strong></td>
                <td><ul class="list overflowList">
                    <c:forEach var="entityOnOtherCell" items="${entry.entityOnOtherCell}">
                        <li><a href="../detail/${entityOnOtherCell.dbId}" class="" title="show Reactome ${entityOnOtherCell.dbId}" rel="nofollow">${entityOnOtherCell.name}</a></li>
                    </c:forEach>
                </ul></td>
            </tr>
            </c:if>
        </table>
    </div>
</c:if>

<c:if test="${not empty entry.referedEntities}">
    <div class="grid_23  padding">
        <h5>This entry is a component of:</h5>
        <table class="fixedTable">
            <thead>
            <tr class="tableHead">
                <td></td>
                <td></td>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="referrers" items="${entry.referedEntities}">
                <tr>
                    <td><strong>${referrers.key}</strong></td>
                    <td>
                        <ul class="list overflowList">
                            <c:forEach var="entityReferenceList" items="${referrers.value}">
                                <li><c:if test="${not empty entityReferenceList.name}"><a href="../detail/${entityReferenceList.dbId}" class="" title="Show Details" rel="nofollow">${entityReferenceList.name}</a></c:if></li>
                            </c:forEach>
                        </ul>
                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </div>
</c:if>

<c:if test="${not empty entry.catalystActivities}">
    <div class="grid_23  padding">
        <h5>Catalyst Activity</h5>
        <table>
            <thead>
            <tr class="tableHead">
                <td>PhysicalEntity</td>
                <td>Activity</td>
                <td>Active Units</td>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="catalystActivity" items="${entry.catalystActivities}">
                <tr>
                    <c:if test="${not empty catalystActivity.physicalEntity}">
                        <td><a href="../detail/${catalystActivity.physicalEntity.dbId}" class="" title="show Reactome ${catalystActivity.physicalEntity.dbId}" rel="nofollow">${catalystActivity.physicalEntity.name}</a></td>
                    </c:if>
                    <c:if test="${not empty catalystActivity.activity}">
                        <td><a href="${catalystActivity.activity.database.url}" class=""  title="show ${catalystActivity.activity.database.name}" rel="nofollow">${catalystActivity.activity.name} (${catalystActivity.activity.accession})</a></td>
                    </c:if>
                    <c:if test="${not empty catalystActivity.activeUnit}">
                        <td>
                            <ul class="list overflowList">
                                <c:forEach var="activeUnit" items="${catalystActivity.activeUnit}">
                                    <li><a href="../detail/${activeUnit.dbId}" class="" title="show Reactome ${activeUnit.dbId}" rel="nofollow">${activeUnit.name}</a></li>
                                </c:forEach>
                            </ul>
                        </td>
                    </c:if>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </div>
</c:if>

<c:if test="${not empty entry.referenceEntity}">

    <c:if test="${not empty entry.referenceEntity.referenceGenes}">
        <div class="grid_23  padding">
            <h5>Reference Genes</h5>
            <table class="fixedTable">
                <thead>
                <tr class="tableHead">
                    <td>Database</td>
                    <td>Identifier</td>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="referenceGenes" items="${entry.referenceEntity.referenceGenes}">
                    <tr>
                        <td><strong>${referenceGenes.key}</strong></td>
                        <td>
                            <c:forEach var="value" items="${referenceGenes.value}" varStatus="loop">
                                <a href="${value.database.url}" title="show ${value.database.name}" rel="nofollow">${value.identifier}</a><c:if test="${!loop.last}">, </c:if>
                            </c:forEach>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>
    </c:if>

    <c:if test="${not empty entry.referenceEntity.referenceTranscript}">
        <div class="grid_23  padding">
            <h5>Reference Transcripts</h5>
            <table  class="fixedTable">
                <thead>
                <tr class="tableHead">
                    <td>Database</td>
                    <td>Identifier</td>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="referenceTranscript" items="${entry.referenceEntity.referenceTranscript}">
                    <tr>
                        <td><strong>${referenceTranscript.key}</strong></td>
                        <td>
                            <c:forEach var="value" items="${referenceTranscript.value}" varStatus="loop">
                                <a href="${value.database.url}" title="show ${value.database.name}" rel="nofollow">${value.identifier}</a><c:if test="${!loop.last}">, </c:if>
                            </c:forEach>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>
    </c:if>
</c:if>

<c:if test="${not empty entry.crossReferences}">
    <div class="grid_23  padding">
        <h5>Cross References</h5>
        <table class="fixedTable">
            <thead>
            <tr class="tableHead">
                <td>Database</td>
                <td>Identifier</td>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="crossReference" items="${entry.crossReferences}">
                <tr>
                    <td><strong>${crossReference.key}</strong></td>
                    <td>
                        <c:forEach var="value" items="${crossReference.value}" varStatus="loop">
                            <a href="${value.database.url}" title="show ${value.database.name}" rel="nofollow">${value.identifier}</a><c:if test="${!loop.last}">, </c:if>
                        </c:forEach>
                    </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </div>
</c:if>

<c:if test="${not empty entry.diseases}">

    <div class="grid_23  padding">
        <h5>Diseases</h5>
        <table>
            <thead>
            <tr class="tableHead">
                <td>Name</td>
                <td>Identifier</td>
                <td>Synonyms</td>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="diseases" items="${entry.diseases}">
                <c:if test="${not empty diseases.name}">
                    <tr>
                        <td><a href="${diseases.database.url}" class=""  title="Show Details" rel="nofollow">${diseases.name} </a></td>
                        <td><c:if test="${not empty diseases.identifier}">${diseases.identifier}</c:if></td>
                        <td><c:if test="${not empty diseases.synonyms}">${diseases.synonyms}</c:if></td>
                    </tr>
                </c:if>
            </c:forEach>
            </tbody>
        </table>
    </div>
</c:if>

<c:if test="${not empty entry.regulatedEvents}">

    <div class="grid_23  padding">
        <h5>This entry is regulated by</h5>
        <table class="fixedTable">
            <thead>
            <tr class="tableHead">
                <td>Regulation type</td>
                <td>Name</td>
            </tr>
            </thead>
            <tbody>
            <c:forEach var="regulation" items="${entry.regulatedEvents}">
            <tr>
                <td><strong>${regulation.key}</strong></td>
                <td>
                    <ul class="list overflowList">
                    <c:forEach var="value" items="${regulation.value}" varStatus="loop">
                        <li><c:if test="${not empty value.regulator.dbId}"><a href="../detail/${value.regulator.dbId}" class="" title="Show Details" rel="nofollow">${value.regulator.name}<c:if test="${not empty value.regulator.species}"> (${value.regulator.species})</c:if></a></c:if></li>
                    </c:forEach>
                    </ul>
                </td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </div>
</c:if>

<c:if test="${not empty entry.regulatingEntities}">
    <div class="grid_23  padding">
        <h5>This entity regulates</h5>
        <table class="fixedTable">
            <thead>
            <tr class="tableHead">
                <td>Regulation type</td>
                <td>Name</td>

            </tr>
            </thead>
            <tbody>
            <c:forEach var="regulation" items="${entry.regulatingEntities}">
                <tr>
                    <td><strong>${regulation.key}</strong></td>
                    <td>
                        <ul class="list overflowList">
                            <c:forEach var="value" items="${regulation.value}" varStatus="loop">
                                <li><c:if test="${not empty value.regulatedEntity.dbId}"><a href="../detail/${value.regulatedEntity.dbId}" class="" title="Show Details" rel="nofollow">${value.regulatedEntity.name}<c:if test="${not empty value.regulatedEntity.species}"> (${value.regulatedEntity  .species})</c:if></a></c:if></li>
                            </c:forEach>
                        </ul>
                    </td>
                </tr>
            </c:forEach>
            </tbody>

        </table>
    </div>
</c:if>

<c:if test="${not empty entry.regulation}">
    <c:set var="regulation" value="${entry.regulation}"/>
    <div class="grid_23  padding">
        <h5>Regulation participants</h5>
        <table>
            <thead>
            <tr class="tableHead">
                <td></td>
                <td></td>
            </tr>
            </thead>
            <tbody>
            <c:if test="${not empty regulation.regulationType}">
                <tr>
                    <td><strong>regulation type</strong></td>
                    <td>${regulation.regulationType}</td>
                </tr>
            </c:if>
            <c:if test="${not empty regulation.regulatedEntity}">
                <tr>
                    <td><strong>Regulated entity</strong></td>
                    <td><a href="../detail/${regulation.regulatedEntity.dbId}" class="" title="Show Details" rel="nofollow">${regulation.regulatedEntity.name}</a></td>
                </tr>
            </c:if>
            <c:if test="${not empty regulation.regulator}">
                <tr>
                    <td><strong>Regulator</strong></td>
                    <td><a href="../detail/${regulation.regulator.dbId}" class="" title="Show Details" rel="nofollow">${regulation.regulator.name}</a></td>
                </tr>
            </c:if>
            </tbody>
        </table>
    </div>
</c:if>

<c:if test="${not empty entry.modifiedResidues}">
    <div class="grid_23  padding">
        <h5>ModifiedResidues</h5>
        <div class="paddingleft">
            <table>
                <thead>
                <tr class="tableHead">
                    <td>Name</td>
                    <td>Coordinate</td>
                    <td>Modification</td>
                    <td>PsiMod Name</td>
                    <td>PsiMod Identifier</td>
                    <td>PsiMod Definition</td>
                </tr>
                </thead>
                <tbody>
                <c:forEach var="modifiedResidue" items="${entry.modifiedResidues}">
                <tr>
                    <td><c:if test="${not empty modifiedResidue.name}">${modifiedResidue.name}</c:if></td>
                    <td><c:if test="${not empty modifiedResidue.coordinate}">${modifiedResidue.coordinate}</c:if></td>
                    <td><c:if test="${not empty modifiedResidue.modification.name}"><a href="../detail/${modifiedResidue.modification.dbId}" class="" title="Show Details" rel="nofollow">${modifiedResidue.modification.name}</a></c:if></td>
                    <td><c:if test="${not empty modifiedResidue.psiMod.name}"><a href="${modifiedResidue.psiMod.database.url}" class=""  title="Show Details" rel="nofollow">${modifiedResidue.psiMod.name} </a></c:if></td>
                    <td><c:if test="${not empty modifiedResidue.psiMod.identifier}">${modifiedResidue.psiMod.identifier}</c:if></td>
                    <td><c:if test="${not empty modifiedResidue.psiMod.definition}">${modifiedResidue.psiMod.definition}</c:if></td>
                </tr>
                </c:forEach>
            </table>
        </div>
    </div>
</c:if>

<c:if test="${not empty entry.literature}">
    <div class="grid_23  padding">
        <h5>Literature References</h5>
        <table>
            <thead>
            <tr class="tableHead">
                <td>pubMedId</td>
                <td>Title</td>
                <td>Journal</td>
                <td>Year</td>
            </tr>
            </thead>
            <tbody class="tableBody">
            <c:forEach var="literature" items="${entry.literature}">
                <tr>
                    <td><c:if test="${not empty literature.pubMedIdentifier}">${literature.pubMedIdentifier}</c:if></td>
                    <td><c:if test="${not empty literature.title}"><a href="${literature.url}" class=""  title="show Pubmed" rel="nofollow"> ${literature.title}</a></c:if></td>
                    <td><c:if test="${not empty literature.journal}">${literature.journal}</c:if></td>
                    <td><c:if test="${not empty literature.year}">${literature.year}</c:if></td>
                </tr>
            </c:forEach>
            </tbody>
        </table>
    </div>
</c:if>

<c:if test="${not empty entry.inferredFrom || not empty entry.inferredTo || not empty entry.orthologousEvents}">
    <div class="grid_23  padding">
        <h5>Inferred Entries</h5>
        <table  class="fixedTable">
            <thead>
            <tr class="tableHead">
                <td></td>
                <td></td>
            </tr>
            </thead>
            <tbody>
            <c:if test="${not empty entry.inferredFrom}">
                <tr>
                    <td><strong>Inferred From</strong></td>
                    <td>
                        <ul class="list overflowList">
                        <c:forEach var="inferredFrom" items="${entry.inferredFrom}">
                            <li><a href="../detail/${inferredFrom.dbId}" class="" title="Show Details" rel="nofollow">${inferredFrom.name} (${inferredFrom.species})</a></li>
                        </c:forEach>
                        </ul>
                    </td>
                </tr>
            </c:if>
            <c:if test="${not empty entry.inferredTo}">
                <tr>
                    <td><strong>Inferred to</strong></td>
                    <td>
                        <ul class="list overflowList">
                        <c:forEach var="inferredTo" items="${entry.inferredTo}">
                            <li><a href="../detail/${inferredTo.dbId}" class="" title="Show Details" rel="nofollow">${inferredTo.name} (${inferredTo.species})</a></li>
                        </c:forEach>
                        </ul>
                    </td>
                </tr>
            </c:if>
            <c:if test="${not empty entry.orthologousEvents}">
                <tr>
                    <td><strong>Orthologous events</strong></td>
                    <td>
                        <ul class="list overflowList">
                        <c:forEach var="orthologousEvents" items="${entry.orthologousEvents}">
                            <li><a href="../detail/${orthologousEvents.dbId}" class="" title="Show Details" rel="nofollow">${orthologousEvents.name} (${orthologousEvents.species})</a></li>
                        </c:forEach>
                        </ul>
                    </td>
                </tr>
            </c:if>
            </tbody>
        </table>
    </div>
</c:if>

</div>
<div class="clear"></div>

</div>            <%--A weird thing to avoid problems--%>
<c:import url="footer.jsp"/>