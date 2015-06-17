/**
 * Created by maximiliankoch on 26/01/15.
 */
var datepicker = function(){
    jQuery("#startDate").datepicker({
        dateFormat: "yy-mm-dd",
        autoSize: true,
        onClose: function (selectedDate) {
            jQuery("#endDate").datepicker("option", "minDate", selectedDate);
        }
    }).datepicker("setDate", getFirstOfTheMonth());
    jQuery("#endDate").datepicker({
        dateFormat: "yy-mm-dd",
        autoSize: true,
        onClose: function (selectedDate) {
            $("#startDate").datepicker("option", "maxDate", selectedDate);
        }
    }).datepicker("setDate", getCurrentDate());
};

function getCurrentDate() {
    return new Date();
}

function getFirstOfTheMonth() {
    return new Date(getCurrentDate().getFullYear(), getCurrentDate().getMonth(), 1);
}