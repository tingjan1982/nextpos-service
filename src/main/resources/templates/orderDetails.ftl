<#-- @ftlvariable name="client" type="io.nextpos.client.data.Client" -->
<#-- @ftlvariable name="orderTransaction" type="io.nextpos.ordertransaction.data.OrderTransaction" -->
<#-- reference: https://freemarker.apache.org/docs/dgui_misc_whitespace.html-->
<@compress single_line=true>
<s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/">
    <s:Body>
        <epos-print xmlns="http://www.epson-pos.com/schemas/2011/03/epos-print">
            <text align="center"/>
            <text lang="zh-tw"/>
            <text font="font_a"/>
            <text width="2" height="2"/>
            <text linespc="45"/>
            <text>交易明細&#10;</text>
            <text linespc="35"/>
            <text align="left"/>
            <text width="1" height="1"/>
            <text linespc="35"/>
            <text>營業人統編: ${client.attributes["UBN"]!"NA"}&#10;</text>
            <text>公司名稱: ${client.clientName!"NA"}&#10;</text>
            <text>營業人地址: ${client.attributes["ADDRESS"]!"NA"}&#10;</text>
            <text>交易序號: ${orderTransaction.id!"NA"}&#10;</text>
            <text linespc="65"/>
            <text>${orderTransaction.createdDate?string('MM/dd/yyyy HH:mm:ss')!"NA"}&#10;</text>
            <text linespc="35"/>
            <#list orderTransaction.billDetails.billLineItems as li>
                <text>${li.name}</text><text x="250">${li.quantity}</text><text x="300">${li.subTotal}&#10;</text>
            </#list>
            <text>合計:</text><text x="300">${orderTransaction.settleAmount!"NA"}&#10;</text>
            <text>發票金額:</text><text x="300">${orderTransaction.settleAmount!"NA"}&#10;</text>
            <cut type="feed"/>
        </epos-print>
    </s:Body>
</s:Envelope>
</@compress>