<#-- @ftlvariable name="clientFound" type="java.lang.Boolean" -->
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
    <h1 class="mt-5">Rain App帳號啟用</h1>
    <#if clientFound>
        <p class="lead">已重新寄送啟用郵件到您的電子信箱。</p>
    <#else>
        <p class="lead">找不到您的帳號，請聯絡客服。</p>
    </#if>
</main>

<footer class="footer">
    <div class="container">
        <span class="text-muted">Rain App 2020 Copyright</span>
    </div>
</footer>
</body>
</html>
