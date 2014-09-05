function to_reactome(rQuery) {
    $.ajax("http://www.reactome.org/AnalysisService/identifiers/projection",{
        data: rQuery,
        processData: false,
        type: "POST",
        contentType: "application/json",
        success: function(response,status,jqXHR) {
          var token = response.summary.token;
          var url = "/PathwayBrowser/#DTAB=AN&TOOL=AT&ANALYSIS=" + token;
	  window.location = url;
        },
        error: function(jqXHR,status,errorThrown){
          alert("Failed to push data to Reactome");
        },
        complete: function(jqXHR, status) {
        }
      });
}
