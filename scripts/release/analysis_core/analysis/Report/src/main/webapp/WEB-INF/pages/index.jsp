<%--suppress XmlPathReference HtmlUnknownTarget --%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<c:import url="header.jsp"/>

<div class="wrapper">
    <div id="analysis-charts">
        <div id="analysis-charts-control">
            <table>
                <tbody>
                <tr>
                    <td>
                        <label for="startDate">From: </label>
                    </td>
                    <td>
                        <input type="text" class="datepicker" id="startDate" name="startDate"><br>
                    </td>
                </tr>
                <tr>
                    <td>
                        <label for="endDate">To: </label>
                    </td>
                    <td>
                        <input type="text" class="datepicker" id="endDate" name="endDate"><br>

                    </td>
                </tr>
                <tr>
                    <td>

                    </td>
                    <td>
                        <input type="button" id="apply" value="Apply"><br><br>
                    </td>
                </tr>
                <tr>
                    <td>
                        <label for="granularity">Granularity: </label>
                    </td>
                    <td>
                        <select id="granularity">
                            <option id="hours" value="hours">hours</option>
                            <option id="days" value="days" selected>days</option>
                            <option id="months" value="months">months</option>
                            <option id="years" value="years">years</option>
                        </select>
                    </td>
                </tr>
                <tr>
                    <td>
                        <label for="subchart">Show Subchart:</label>

                    </td>
                    <td>
                        <select id="subchart">
                            <option value="true">show</option>
                            <option value="false" selected>hide</option>
                        </select>
                    </td>
                </tr>
                <tr>
                    <td>
                        <label for="xAxisTicks">Amount of Ticks:</label>
                    </td>
                    <td>
                        <select id="xAxisTicks">
                            <option value="" selected>default</option>
                            <option value="2">2</option>
                            <option value="4">4</option>
                            <option value="5">5</option>
                            <option value="8">8</option>
                            <option value="10">10</option>
                            <option value="15">15</option>
                            <option value="20">20</option>
                        </select>
                    </td>
                </tr>
                </tbody>
            </table>
        </div>
        <div id="analysis-charts-view">
            <table>
                <tbody>
                <tr><div class="requestComparison head"></div></tr>
                <tr><div class="requestComparison chart"></div></tr>
                <tr><div class="requestComparison info"></div></tr>
                <tr><div class="analysisComparison head"></div></tr>
                <tr><div class="analysisComparison chart"></div></tr>
                <tr><div class="analysisComparison info"></div></tr>
                <tr><div class="processingTimeComparison head"></div></tr>
                <tr><div class="processingTimeComparison chart"></div></tr>
                <tr><div class="processingTimeComparison info"></div></tr>
                <tr><div class="dateSetSizeAnalysis head"></div></tr>
                <tr><div class="dateSetSizeAnalysis chart"></div></tr>
                <tr><div class="dateSetSizeAnalysis info"></div></tr>
                </tbody>
            </table>
        </div>
    </div>
    <div class="clear"></div>
</div>

</div>            <%--A weird thing to avoid problems--%>
<c:import url="footer.jsp"/>