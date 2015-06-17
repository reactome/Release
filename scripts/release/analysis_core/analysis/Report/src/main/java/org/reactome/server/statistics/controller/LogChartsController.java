package org.reactome.server.statistics.controller;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */

import com.wordnik.swagger.annotations.ApiParam;
import org.apache.log4j.Logger;
import org.reactome.server.statistics.model.Model;
import org.reactome.server.statistics.properties.ChartTypes;
import org.springframework.web.bind.annotation.*;

@RestController
public class LogChartsController {
    private static Logger logger = Logger.getLogger(LogChartsController.class.getName());


    @RequestMapping(value = "logcharts/{chartType}/{startdate}/{enddate}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    public Model logcharts(
            @ApiParam(name = "chartType", required = true, value = "")
            @PathVariable String chartType,
            @ApiParam(name = "startdate", required = true, value = "")
            @PathVariable String startdate,
            @ApiParam(name = "enddate", required = true, value = "")
            @PathVariable String enddate,
            @ApiParam(name = "granularity", value = "granularity", required = false, defaultValue = "months")
            @RequestParam(required = false, defaultValue = "months") String granularity,
            @ApiParam(name = "height", value = "chart height", defaultValue = "400")
            @RequestParam(required = false, defaultValue = "400") Integer chartHeight,
            @ApiParam(name = "width", value = "chart width", defaultValue = "600")
            @RequestParam(required = false, defaultValue = "600") Integer chartWidth,
            @ApiParam(name = "subchart", value = "subchart", defaultValue = "false")
            @RequestParam(required = false, defaultValue = "false") boolean subchart,
            @ApiParam(name = "culling", value = "culling", defaultValue = "5")
            @RequestParam(required = false, defaultValue = "5") Integer culling
    ) {
        if (chartType.equals(ChartTypes.REQUEST_COMPARISON.getChartType())) {
            logger.info("REQUEST" + "\t" + chartType + "\t" + startdate + "\t" + enddate + "\t" + granularity + "\t" + chartHeight + "\t" + chartWidth + "\t" + subchart + "\t" + culling + "\t");
            return RESTHandler.getRequestComparisonData(startdate, enddate, granularity, chartHeight, chartWidth, subchart, culling);
        }
        if (chartType.equals(ChartTypes.DATA_SET_SIZE_COMPARISON.getChartType())) {
            logger.info("REQUEST" + "\t" + chartType + "\t" + startdate + "\t" + enddate + "\t" + granularity + "\t" + chartHeight + "\t" + chartWidth + "\t" + subchart + "\t" + culling + "\t");
            return RESTHandler.getDataSetSizeAnalysisData(startdate, enddate, granularity, chartHeight, chartWidth, subchart, culling);
        }
        if (chartType.equals(ChartTypes.PROCESSING_TIME_COMPARISON.getChartType())) {
            logger.info("REQUEST" + "\t" + chartType + "\t" + startdate + "\t" + enddate + "\t" + granularity + "\t" + chartHeight + "\t" + chartWidth + "\t" + subchart + "\t" + culling + "\t");
            return RESTHandler.getProcessingTimeComparisonData(startdate, enddate, granularity, chartHeight, chartWidth, subchart, culling);
        }
        if (chartType.equals(ChartTypes.ANALYSIS_COMPARISON.getChartType())) {
            logger.info("REQUEST" + "\t" + chartType + "\t" + startdate + "\t" + enddate + "\t" + granularity + "\t" + chartHeight + "\t" + chartWidth + "\t" + subchart + "\t" + culling + "\t");
            return RESTHandler.getAnalysisComparisonData(startdate, enddate, granularity, chartHeight, chartWidth, subchart, culling);
        }
        return null;
    }
}