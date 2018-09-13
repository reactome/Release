function validateDBIds(e) {
    e.preventDefault();
        
    var db_ids = document.getElementById("db_ids").value;
        
    var is_list_of_numbers = /^([0-9]+(\r?\n)?)+$/;
    if (is_list_of_numbers.test(db_ids)) {
        document.getElementById("export_form").submit();
    } else {
        alert("Db_ids must be numeric");
    }
};

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
    return document.getElementById("email_error");
}
