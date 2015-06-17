/**
 * Created by maximiliankoch on 22/01/15.
 */
var chartURL = "./logcharts/";

jQuery('document').ready(function () {
    datepicker();
    drawCharts();
    jQuery('#apply').prop('disabled', true);

    jQuery('#apply').on('click', function () {
        checkDate();
        drawCharts();
        jQuery('#apply').prop('disabled', true);
    });
    jQuery('#granularity').on('change', function () {
        checkDate();
        drawCharts();
    });
    jQuery('#chartSize').on('change', function () {
        checkDate();
        drawCharts();
    });
    jQuery('#subchart').on('change', function () {
        checkDate();
        drawCharts();
    });
    jQuery('#xAxisTicks').on('change', function () {
        checkDate();
        drawCharts();
    });
    jQuery('.datepicker').on('change', function () {
        jQuery('#apply').prop('disabled', false);
    });
});

var createURL = function (startDate, endDate, granularity, chartSize, subChart, culling) {
    var urlParams = startDate + "/" + endDate + "/?";
    if (granularity) {
        urlParams += "granularity=" + granularity + "&";
    }
    if (subChart) {
        urlParams += "subchart=" + subChart + "&";
    }
    if (culling) {
        urlParams += "culling=" + culling + "&";
    }
    return urlParams;
};
var drawRequestComparisonChart = function (urlParams) {
    jQuery.ajax({
        url: chartURL + "chart1/" + urlParams,
        dataType: "json"
    }).done(function (data) {
        cleanInfos();
        jQuery(".requestComparison.head").append("<h2>" + data.chartInformation.title + "</h2>").append("<p>" + data.chartInformation.description + "<p>");
        c3.generate(data.chart);
        for (var i in data.dataInformation) {
            jQuery('.requestComparison.info').append("<td>" + "<div class='info_name'>" + "<h2>" + data.dataInformation[i].name + "<h2>" + "</div>" + "<div class='info_result'>" + data.dataInformation[i].result + "</div>" + "</td>");
        }
        drawFileSizeAnalysis(urlParams);
    });
};

var drawFileSizeAnalysis = function (urlParams) {
    jQuery.ajax({
        url: chartURL + "chart4/" + urlParams,
        dataType: "json"
    }).done(function (data) {
        checkDate();
        jQuery(".analysisComparison.head").append("<h2>" + data.chartInformation.title + "</h2>").append("<p>" + data.chartInformation.description + "<p>");
        console.log(data);
        c3.generate(data.chart);
        for (var i in data.dataInformation) {
            jQuery('.analysisComparison.info').append("<td>" + "<div class='info_name'>" + "<h2>" + data.dataInformation[i].name + "<h2>" + "</div>" + "<div class='info_result'>" + data.dataInformation[i].result + "</div>" + "</td>");
        }
        drawProcessingTimeComparison(urlParams);
    });
};

var drawProcessingTimeComparison = function (urlParams) {
    jQuery.ajax({
        url: chartURL + "chart3/" + urlParams,
        dataType: "json"
    }).done(function (data) {
        jQuery(".processingTimeComparison.head").append("<h2>" + data.chartInformation.title + "</h2>").append("<p>" + data.chartInformation.description + "<p>");
        console.log(data);
        c3.generate(data.chart);
        for (var i in data.dataInformation) {
            jQuery('.processingTimeComparison.info').append("<td>" + "<div class='info_name'>" + "<h2>" + data.dataInformation[i].name + "<h2>" + "</div>" + "<div class='info_result'>" + data.dataInformation[i].result + "</div>" + "</td>");
        }
        drawAnalysisComparison(urlParams);
    });
};

var drawAnalysisComparison = function (urlParams) {
    jQuery.ajax({
        url: chartURL + "chart2/" + urlParams,
        dataType: "json"
    }).done(function (data) {
        jQuery(".dateSetSizeAnalysis.head").append("<h2>" + data.chartInformation.title + "</h2>").append("<p>" + data.chartInformation.description + "<p>");
        c3.generate(data.chart);
        for (var i in data.dataInformation) {
            jQuery('.dateSetSizeAnalysis.info').append("<td>" + "<div class='info_name'>" + "<h2>" + data.dataInformation[i].name + "<h2>" + "</div>" + "<div class='info_result'>" + data.dataInformation[i].result + "</div>" + "</td>");
        }
    });
};

function cleanInfos() {
    jQuery(".head, .info").empty();
}


function getStartDate() {
    return jQuery('#startDate').val().replace(/\-/g, '');
}

function getEndDate() {
    return jQuery('#endDate').val().replace(/\-/g, '');
}

function getGranularity() {
    return jQuery('#granularity').val();
}

function getChartSize() {
    return jQuery('#chartSize').val();
}

function getSubChart() {
    return jQuery('#subchart').val();
}

function getCulling() {
    return jQuery('#xAxisTicks').val();
}

var drawCharts = function () {
    var urlParams = createURL(getStartDate(), getEndDate(), getGranularity(), getChartSize(), getSubChart(), getCulling());
    drawRequestComparisonChart(urlParams);
};

var getDateDifference = function () {
    var startDate = new Date(jQuery('#startDate').val());
    var endDate = new Date(jQuery('#endDate').val());
    var dayInMS = 1000 * 60 * 60 * 24;
    return Math.round((endDate.getTime() - startDate.getTime()) / dayInMS);
};

var checkDate = function () {
    console.log(jQuery('#granularity').val());
    if (getDateDifference() > 5) {
        if (jQuery('#granularity').val() === "hours") {
            jQuery('#granularity').val("days");
        }
        jQuery('#granularity > #hours').prop('disabled', true);
    } else {
        jQuery('#granularity > #hours').prop('disabled', false);

    }
    if (getDateDifference() > 120) {
        if (jQuery('#granularity').val() === "days") {
            jQuery('#granularity').val("months");
        }
        jQuery('#granularity > #days').prop('disabled', true);
    } else {
        jQuery('#granularity > #days').prop('disabled', false);

    }
    if (getDateDifference() > 365 * 2) {
        if (jQuery('#granularity').val() === "months") {
            jQuery('#granularity').val("years");
        }
        jQuery('#granularity > #months').prop('disabled', true);
    } else {
        jQuery('#granularity > #months').prop('disabled', false);
    }
};
