var hideSubmissionTimeout;
function validateAndSubmitLogicTableForm(e) {
    e.preventDefault();
    if (!DBIdsAreValid()) {
        showDBIDInputError();
        return;
    }
    
    hideDBIDInputError();
    document.getElementById("export_form").submit();
    clearTimeout(hideSubmissionTimeout);
    showSubmissionReceived("Submission received -- your download will begin shortly!");
    hideSubmissionTimeout = setTimeout(hideSubmissionReceived, 4000);
    
}

function validateDBIds(e) {
    e.preventDefault();
    !getDBIds() || DBIdsAreValid() ? hideDBIDInputError() : showDBIDInputError();
}

function getDBIds() {
    return document.getElementById("db_ids").value;
}

function DBIdsAreValid() {
    var isListOfNumbers = /^([0-9]+(\r?\n)?)+$/;
    return isListOfNumbers.test(getDBIds());
}

// Taken from https://gist.github.com/dreamstarter/9231254
function emailValid(email) {
    var emailRegex = /^([A-Za-z0-9_\-.+])+@([A-Za-z0-9_\-.])+\.([A-Za-z]{2,})$/;
    return emailRegex.test(email);
}
    
function validateEmailAddress(e) {
    e.preventDefault();
        
    var email = document.getElementById("emailaddress").value;
    
    if (!email) {
        showEmailInputError("Please enter an e-mail address");
        return;
    }
    
    if (!emailValid(email)) {
        showEmailInputError("E-mail address provided is invalid");
        return;
    }
    
    hideEmailInputError();
    document.getElementById("export_mapping_file_form").submit();
}

function showEmailInputError(message) {
    var emailErrorSpan = getEmailErrorSpanElement();
    emailErrorSpan.style = "color:red";
    emailErrorSpan.innerHTML = message;
}

function hideEmailInputError() {
    var emailErrorSpan = getEmailErrorSpanElement();
    emailErrorSpan.style = "";
    emailErrorSpan.innerHTML = "";    
}

function getEmailErrorSpanElement() {
    return document.getElementsByClassName("email_error")[0];
}

function showDBIDInputError(message) {
    var dbIDErrorSpan = getDBIDErrorSpanElement();
    dbIDErrorSpan.style = "color:red";
    dbIDErrorSpan.innerHTML = message || "Db_ids must be numeric";
}

function hideDBIDInputError() {
    var dbIDErrorSpan = getDBIDErrorSpanElement();
    dbIDErrorSpan.style = "";
    dbIDErrorSpan.innerHTML = "";    
}

function getDBIDErrorSpanElement() {
    return document.getElementsByClassName("db_id_error")[0];
}

function showSubmissionReceived(message) {
    var submissionReceivedSpan = getSubmissionReceivedSpanElement();
    submissionReceivedSpan.style = "color:green";
    submissionReceivedSpan.innerHTML = message;   
}

function hideSubmissionReceived() {
    var submissionReceivedSpan = getSubmissionReceivedSpanElement();
    submissionReceivedSpan.style = "";
    submissionReceivedSpan.innerHTML = "";    
}

function getSubmissionReceivedSpanElement() {
    return document.getElementsByClassName("submission_received")[0];
}

function hide_logic_table_messages() {
    hideDBIDInputError();
    hideSubmissionReceived();
}