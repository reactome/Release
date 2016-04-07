// Load the Visualization API and the piechart package.
google.load('visualization', '1', {'packages':['corechart']});

function drawChart0() {

    var report_data_url = 'apache.json';

    var json = $.ajax({
        url: report_data_url,
        dataType:"text",
        async: false
    }).responseText;

    data = $.parseJSON(json);
    data.reverse();
    data = data.slice(0,72);
    data.reverse();

    var counts = data[data.length - 1][1];
    var sites = {};
    var min_count = 200;

    for (var i = 0; i < data.length; i++) {
        counts = data[i][1];
        keys = Object.keys(counts);

        for(var j = 0; j < keys.length; j++) {
            if (counts[keys[j]] > min_count) {
                sites[keys[j]] = 1;
            }
        }
    }
    var components = Object.keys(sites);


    var chart_data = new google.visualization.DataTable();
    chart_data.addColumn('string', 'Date');


    for (var i = 0; i < components.length; i++) {
        label = components[i] == '/' ? 'reactome.org' : components[i];
        chart_data.addColumn('number',label);
    }

    for (var i = 0; i < data.length; i++) {
        date = data[i][0];
        site_counts = data[i][1];

        var stuff = date.split(' ');
        //times = stuff[4].split(':');
        times = stuff.pop().split(':');
	times.pop();
        time  = times.join(':');
        date = stuff[0] + ' ' + time;

        var row = [date];
        for(var j = 0; j < components.length; j++) {
            count = site_counts[components[j]] || undefined;
            if (count > 1500) {
                count = 1500
            }
            row.push(count);
        }
        chart_data.addRow(row);
    }

    var options = {
        hAxis: {slantedText: true, slantedTextAngle: 45},
        legend: { position: 'top'},
        width: 1000,
	height: 400,
        pointSize: 2,
        theme: 'material',
        chartArea:{left:50,top:50,height:'70%'}
    };

    $('#chart0').empty();
    var chart = new google.visualization.LineChart(document.getElementById('chart0'));
    chart.draw(chart_data, options);
}

function drawChart00() {

    var report_data_url = 'mysql.json';

    var json = $.ajax({
        url: report_data_url,
        dataType:"text",
        async: false
    }).responseText;

    data = $.parseJSON(json);
    data.reverse();
    data = data.slice(0,72);
    data.reverse();

    var chart_data = new google.visualization.DataTable();
    chart_data.addColumn('string', 'Date');
    chart_data.addColumn('number', '% CPU');
    chart_data.addColumn('number', 'connections');
    chart_data.addColumn('number', 'restarts');

    for (var i = 0; i < data.length; i++) {
        date = data[i][0];
        var stuff = date.split(' ');
        //times = stuff[3].split(':');
        times = stuff.pop().split(':');
	times.pop();
        time  = times.join(':');
        date = stuff[0] + ' ' + time;

        cpu  = parseFloat(data[i][1]);
        conn = parseInt(data[i][2]);
	if (cpu == 0 && conn == 150) {
	    chart_data.addRow([date,null,null,150]);
	}
	else {
            chart_data.addRow([date,cpu,conn,null]);
	}
    }

    var options = {
        vAxes: {0: {viewWindowMode:'explicit',
                    gridlines: {color: 'transparent'},
                    format: 'decimal',
		    viewWindow: {min: 0, max: 50}
                   },
                1: {
                    gridlines: {color: 'transparent'},
                },
               },
        series: {0:{targetAxisIndex:0, color: 'blue'},
                 1:{targetAxisIndex:1, pointShape: 'circle', color: 'green'},
                 2:{targetAxisIndex:1, pointShape: 'star', pointSize: 15, lineWidth: 0, color: 'red'},
                },
        hAxis: {slantedText: true, slantedTextAngle: 45},
        legend: { position: 'top'},
        width: 1000,
        height: 400,
        pointSize: 5,
        theme: 'material',
        chartArea:{left:50,top:50,height:'70%'}
    };

    $('#chart00').empty();
    var chart = new google.visualization.LineChart(document.getElementById('chart00'));
    chart.draw(chart_data, options);
}


function drawChart1() {
    
    var report_data_url = 'mysql.json';

    var json = $.ajax({
        url: report_data_url,
        dataType:"text",
        async: false
    }).responseText;

    // last 25 hours
    data = $.parseJSON(json);
    data.reverse();
    data = data.slice(0,288);
    data.reverse();

    var chart_data = new google.visualization.DataTable();
    chart_data.addColumn('string', 'Date');
    chart_data.addColumn('number', '% CPU');
    chart_data.addColumn('number', 'connections');
    chart_data.addColumn('number', 'restarts');

    for (var i = 0; i < data.length; i++) {
        date = data[i][0];
        var stuff = date.split(' ');

        times = stuff.pop().split(':');
        times.pop();
        time  = times.join(':');
        date = stuff[0] + ' ' + time;

        cpu  = parseFloat(data[i][1]);
        conn = parseInt(data[i][2]);
        if (cpu == 0 && conn == 150) {
            chart_data.addRow([date,null,null,150]);
        }
        else {
            chart_data.addRow([date,cpu,conn,null]);
        }
    }

    var options = {
        vAxes: {0: {viewWindowMode:'explicit',
                    gridlines: {color: 'transparent'},
                    format: 'decimal',
                    viewWindow: {min: 0, max: 50}
                   },
                1: {
                    gridlines: {color: 'transparent'},
                },
               },
        series: {0:{targetAxisIndex:0, color: 'blue'},
                 1:{targetAxisIndex:1, pointShape: 'circle', color: 'green'},
                 2:{targetAxisIndex:1, pointShape: 'star', pointSize: 15, lineWidth: 0, color: 'red'},
                },
        hAxis: {slantedText: true, slantedTextAngle: 45},
        legend: { position: 'top'},
        width: 1000,
        height: 400,
        pointSize: 5,
        theme: 'material',
        chartArea:{left:50,top:50,height:'70%'}
    };

    $('#chart1').empty();
    var chart = new google.visualization.LineChart(document.getElementById('chart1'));
    chart.draw(chart_data, options);
}

function drawChart3() {

    var report_data_url = 'mysql.json';

    var json = $.ajax({
        url: report_data_url,
        dataType:"text",
        async: false
    }).responseText;

    data = $.parseJSON(json);
    data.reverse();
    data = data.slice(0,576);
    data.reverse();

    var chart_data = new google.visualization.DataTable();
    chart_data.addColumn('string', 'Date');
    chart_data.addColumn('number', '% CPU');
    chart_data.addColumn('number', 'connections');
    chart_data.addColumn('number', 'restarts');

    for (var i = 0; i < data.length; i++) {
        date = data[i][0];
        var stuff = date.split(' ');

        times = stuff.pop().split(':');
        times.pop();
        time  = times.join(':');
        date = stuff[0] + ' ' + time;

        cpu  = parseFloat(data[i][1]);
        conn = parseInt(data[i][2]);
        if (cpu == 0 && conn == 150) {
            chart_data.addRow([date,null,null,150]);
        }
        else {
            chart_data.addRow([date,cpu,conn,null]);
        }
    }

    var options = {
        vAxes: {0: {viewWindowMode:'explicit',
                    gridlines: {color: 'transparent'},
                    format: 'decimal',
                    viewWindow: {min: 0, max: 50}
                   },
                1: {
                    gridlines: {color: 'transparent'},
                },
               },
        series: {0:{targetAxisIndex:0, color: 'blue'},
                 1:{targetAxisIndex:1, pointShape: 'circle', color: 'green'},
                 2:{targetAxisIndex:1, pointShape: 'star', pointSize: 15, lineWidth: 0, color: 'red'},
                },
        hAxis: {slantedText: true, slantedTextAngle: 45},
        legend: { position: 'top'},
        width: 1000,
        height: 400,
        pointSize: 5,
        theme: 'material',
        chartArea:{left:50,top:50,height:'70%'}
    };

    $('#chart3').empty();
    var chart = new google.visualization.LineChart(document.getElementById('chart3'));
    chart.draw(chart_data, options);
}

function drawChart2() {

    var report_data_url = 'apache.json';

    var json = $.ajax({
        url: report_data_url,
        dataType:"text",
        async: false
    }).responseText;

    data = $.parseJSON(json);
    data.reverse();
    data = data.slice(0,288);
    data.reverse();

    var counts = data[data.length - 1][1];
    var sites = {};
    var min_count = 200;
    
    // Only use web site components with > min_hits.
    for (var i = 0; i < data.length; i++) {
	counts = data[i][1];
	keys = Object.keys(counts);

	for(var j = 0; j < keys.length; j++) {
	    if (counts[keys[j]] > min_count) {
		sites[keys[j]] = 1;
	    }
	}
    }
    var components = Object.keys(sites);
	       
	
    var chart_data = new google.visualization.DataTable();
    chart_data.addColumn('string', 'Date');

	
    for (var i = 0; i < components.length; i++) {
	label = components[i] == '/' ? 'reactome.org' : components[i];
	chart_data.addColumn('number',label);
    }

    for (var i = 0; i < data.length; i++) {
        date = data[i][0];
	site_counts = data[i][1];

        var stuff = date.split(' ');
        //times = stuff[4].split(':');
        times = stuff.pop().split(':');
	times.pop();
        time  = times.join(':');
        date = stuff[0] + ' ' + time;

	var row = [date];
	for(var j = 0; j < components.length; j++) { 
	    count = site_counts[components[j]] || undefined;
	    if (count > 1500) { 
		count = 1500 
	    }
	    row.push(count);
	}
	chart_data.addRow(row);
    }

    var options = {
        //title: 'Web site components',
        vAxis: {title:'hits/10 min', viewWindow: {min: 0, max: 1500}},
        hAxis: {slantedText: true, slantedTextAngle: 45},
        legend: { position: 'top'},
        width: 1000,
        height: 325,
        pointSize: 5,
        theme: 'material',
        chartArea:{left:50,top:50,height:'65%'}
    };

    $('#chart2').empty();
    var chart = new google.visualization.LineChart(document.getElementById('chart2'));
    chart.draw(chart_data, options);
}


function drawChart4() {

    var report_data_url = 'apache.json';

    var json = $.ajax({
        url: report_data_url,
        dataType:"text",
        async: false
    }).responseText;

    data = $.parseJSON(json);
    data.reverse();
    data = data.slice(0,576);
    data.reverse();

    var counts = data[data.length - 1][1];
    var sites = {};
    var min_count = 200;

    // Only use web site components with > min_hits.                                                                                                                                                                                                                                                                                                                 
    for (var i = 0; i < data.length; i++) {
        counts = data[i][1];
        keys = Object.keys(counts);

        for(var j = 0; j < keys.length; j++) {
            if (counts[keys[j]] > min_count) {
                sites[keys[j]] = 1;
            }
        }
    }
    var components = Object.keys(sites);


    var chart_data = new google.visualization.DataTable();
    chart_data.addColumn('string', 'Date');


    for (var i = 0; i < components.length; i++) {
        label = components[i] == '/' ? 'reactome.org' : components[i];
        chart_data.addColumn('number',label);
    }

    for (var i = 0; i < data.length; i++) {
        date = data[i][0];
        site_counts = data[i][1];

        var stuff = date.split(' ');
        //times = stuff[4].split(':');
        times = stuff.pop().split(':');
        time  = times.join(':');
        date = stuff[0] + ' ' + time;


        var row = [date];
        for(var j = 0; j < components.length; j++) {
            count = site_counts[components[j]] || undefined;
            if (count > 1500) {
                count = 1500
            }
            row.push(count);
        }
        chart_data.addRow(row);
    }

    var options = {
        //title: 'Web site components',                                                                                                                                                                                                                                                                                                                              
        vAxis: {title:'hits/10 min', viewWindow: {min: 0, max: 1500}},
        hAxis: {slantedText: true, slantedTextAngle: 45},
        legend: { position: 'top'},
        width: 1000,
        height: 325,
        pointSize: 2,
        theme: 'material',
        chartArea:{left:50,top:50,height:'65%'}
    };

    $('#chart4').empty();
    var chart = new google.visualization.LineChart(document.getElementById('chart4'));
    chart.draw(chart_data, options);
}

