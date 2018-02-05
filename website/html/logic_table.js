$(document).ready(function() {
    $("#export_form").submit(function(e) {
        e.preventDefault();
        
        var db_ids = $("#db_ids").val();
        
        var list_of_numbers_pattern = /^([0-9]+(\r?\n)?)+$/;
        
        if (list_of_numbers_pattern.test(db_ids)) {
            $("#export_form")[0].submit();
        } else {
            alert("Db_ids must be numeric");
        }
    });
});