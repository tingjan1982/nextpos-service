<#-- @ftlvariable name="activationStatus" type="io.nextpos.client.service.ClientActivationServiceImpl.ActivationStatus" -->
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
    <link rel="stylesheet" href="https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css"
          integrity="sha384-ggOyR0iXCbMQv3Xipma34MD+dH/1fQ784/j6cY/iJTQUOhcWr7x9JvoRxT2MZw1T" crossorigin="anonymous">
    <title>Account Activation</title>
</head>
<style type="text/css">

</style>
<body>

<div class="container-fluid">
    <nav class="navbar navbar-dark bg-primary">
        <span class="navbar-brand mb-0 h1">Rain Account Activation</span>
    </nav>
    <div class="jumbotron jumbotron-fluid">
        <div class="container">
            <#if activationStatus == "ACTIVATED">
                <h1 class="display-4">Thank you for choosing Rain.</h1>
                <p class="lead">Your account has been activated.</p>
            <#elseif activationStatus == "EXPIRED">
                <p class="lead">The activation link has expired. Please request for another one.</p>
            <#else>
                <p class="lead">Activation has failed. Please contact support.</p>
            </#if>
        </div>
    </div>
</div>

<script src="https://code.jquery.com/jquery-3.3.1.slim.min.js"
        integrity="sha384-q8i/X+965DzO0rT7abK41JStQIAqVgRVzpbzo5smXKp4YfRvH+8abtTE1Pi6jizo" crossorigin="anonymous"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.14.7/umd/popper.min.js"
        integrity="sha384-UO2eT0CpHqdSJQ6hJty5KVphtPhzWj9WO1clHTMGa3JDZwrnQq4sF86dIHNDz0W1" crossorigin="anonymous"></script>
<script src="https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/js/bootstrap.min.js"
        integrity="sha384-JjSmVgyd0p3pXB1rRibZUAYoIIy6OrQ6VrjIEaFf/nJGzIxFDsf4x0xIM+B07jRM" crossorigin="anonymous"></script>
</body>
</html>