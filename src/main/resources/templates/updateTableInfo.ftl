<#-- @ftlvariable name="from" type="java.lang.String" -->
<#-- @ftlvariable name="to" type="java.lang.String" -->
<#-- reference: https://freemarker.apache.org/docs/dgui_misc_whitespace.html-->
<@compress single_line=true>
    <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/">
        <s:Body>
            <epos-print xmlns="http://www.epson-pos.com/schemas/2011/03/epos-print">
                <#setting time_zone="Asia/Taipei">
                <#assign indent="200"/>
                <text lang="zh-tw"/>
                <text font="font_b"/>
                <text align="center" width="2" height="2"/>
                <text>換桌通知&#10;</text>
                <feed line="2"/>
                <text align="left" width="2" height="2"/>
                <text linespc="30"/>

                <text>原始桌位: ${from}&#10;</text>
                <text>新桌位: ${to}&#10;</text>

                <feed line="2"/>
                <cut type="feed"/>
            </epos-print>
        </s:Body>
    </s:Envelope>
</@compress>