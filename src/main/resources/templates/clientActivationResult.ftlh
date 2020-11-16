<#-- @ftlvariable name="activationStatus" type="io.nextpos.client.data.ClientActivationResult.ActivationStatus" -->
<!doctype html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
    <meta name="description" content="">
    <meta name="author" content="">
    <link rel="icon" href="/docs/4.0/assets/img/favicons/favicon.ico">

    <title>Rain App Client Activation</title>

    <link rel="canonical" href="https://getbootstrap.com/docs/4.0/examples/sticky-footer/">

    <!-- Bootstrap core CSS -->
    <link href="https://getbootstrap.com/docs/4.0/dist/css/bootstrap.min.css" rel="stylesheet">

    <!-- Custom styles for this template -->
    <link href="https://getbootstrap.com/docs/4.0/examples/sticky-footer/sticky-footer.css" rel="stylesheet">
</head>

<body>

<!-- Begin page content -->
<main role="main" class="container">
    <h1 class="mt-5">Rain App帳號驗證狀態</h1>
    <#if activationStatus == "ACTIVATED">
        <p class="lead">您的帳戶已驗證成功。</p>
    <#elseif activationStatus == "EXPIRED">
        <p class="lead">啟用連結已失效，請重新申請。</p>
        <p class="lead">
            <a href="${clientActivationLink}" class="btn btn-lg btn-secondary">重新申請驗證</a>
        </p>
    <#else>
        <p class="lead">帳號驗證失敗，請聯絡您的客服。</p>
    </#if>
</main>

<footer class="footer">
    <div class="container">
        <span class="text-muted">Rain App 2020 Copyright</span>
    </div>
</footer>
</body>
</html>
