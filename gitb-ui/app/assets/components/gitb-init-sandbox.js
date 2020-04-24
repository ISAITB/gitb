$(document).ready(function() {
    var importFinished = false
    // Handler definitions.
    var fileUploadHandler = function () {
        $("#fileInputControl").trigger("click")
    }
    var submitHandler = function (event) {
        event.preventDefault()
        if (!$("#buttonImport").prop("disabled")) {
            $("#initFormAlerts").html("")
            $("#initDataSpinner").show()
            var dataToSend = new FormData()
            dataToSend.append("file", $("#fileInputControl")[0].files[0])
            dataToSend.append("password", getPasswordField().val())
            $("#buttonImport").prop("disabled", true)
            $("#buttonNoImport").prop("disabled", true)
            $.ajax({
                url: "/initdata",
                type: "POST",
                data: dataToSend,
                enctype: 'multipart/form-data',
                processData: false,
                contentType: false,
                cache: false,
                success: function (e) {
                    importFinished = true
                    $("#initFormAlerts").html("<div class='alert alert-success' role='alert'>Archive successfully imported. Close this dialog to proceed.</div>")
                    $("#initDataSpinner").hide()
                    $("#buttonNoImport").prop("disabled", false)
                },
                error: function (e) {
                    var message = "An error occurred while importing the archive."
                    if (e && e.responseJSON && e.responseJSON.error_description) {
                        message = e.responseJSON.error_description
                    }
                    $("#initFormAlerts").html("<div class='alert alert-danger' role='alert'>"+message+"</div>")
                    $("#initDataSpinner").hide()
                    $("#buttonImport").prop("disabled", false)
                    $("#buttonNoImport").prop("disabled", false)
                }
            })
        }
    }
    // Method definitions.
    var getPasswordField = function () {
        if ($("#encryptionPasswordVisible").is(":hidden")) {
            return $("#encryptionPassword")
        } else {
            return $("#encryptionPasswordVisible")
        }
    }
    var checkFormValid = function () {
        var valid = false
        var fileInputControl = $("#fileInputControl")
        if ((fileInputControl.length > 0 && fileInputControl[0].files && fileInputControl[0].files.length > 0) && getPasswordField().val().trim() != "") {
            valid = true
        }
        $("#buttonImport").prop("disabled", importFinished || !valid)
    }
    // Setup of handlers.
    $("#fileInputButton").on("click", fileUploadHandler)
    $("#fileInputText").on("click", fileUploadHandler)
    $("#fileInputControl").on("change", function(e) {
        if (e && e.target && e.target.files && e.target.files.length > 0 && e.target.files[0].name) {
            $("#fileInputText").val(e.target.files[0].name)
        } else {
            $("#fileInputText").val("")
        }
        $("#fileInputText").blur()
        checkFormValid()
    })
    $("#encryptionPassword").on("input", function(e) {
        checkFormValid()
    })
    $("#encryptionPasswordVisible").on("input", function(e) {
        checkFormValid()
    })
    $("#encryptionPasswordCheck").on("change", function(e) {
        if ($("#encryptionPasswordCheck").prop("checked")) {
            $("#encryptionPasswordVisible").val($("#encryptionPassword").val())
            $("#encryptionPasswordVisible").show()
            $("#encryptionPassword").hide()
        } else {
            $("#encryptionPassword").val($("#encryptionPasswordVisible").val())
            $("#encryptionPasswordVisible").hide()
            $("#encryptionPassword").show()
        }
    })
    $("#initForm").on("submit", submitHandler)
    $("#buttonImport").on("click", submitHandler)
    $("#buttonNoImport").on("click", function() {
        $("#sandboxModal").modal("hide")
    })
    // Initialisation of modal content.
    $('#sandboxModal').on('show.bs.modal', function (e) {
        importFinished = false
        $("[data-toggle='tooltip']").tooltip()
        $("#initDataSpinner").hide()
        $("#encryptionPasswordVisible").hide()
        $("#fileInputControl").val("")
        $("#fileInputText").val("")
        checkFormValid()
    })
    // Activation of modal.
    $("#sandboxModal").modal({
        backdrop: 'static',
        keyboard: false
    })
})