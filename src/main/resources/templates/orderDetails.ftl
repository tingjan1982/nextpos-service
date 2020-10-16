<#-- @ftlvariable name="client" type="io.nextpos.client.data.Client" -->
<#-- @ftlvariable name="order" type="io.nextpos.ordermanagement.data.Order" -->
<#-- @ftlvariable name="orderTransaction" type="io.nextpos.ordertransaction.data.OrderTransaction" -->
<#-- reference: https://freemarker.apache.org/docs/dgui_misc_whitespace.html-->
<@compress single_line=true>
<s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/">
    <s:Body>
        <epos-print xmlns="http://www.epson-pos.com/schemas/2011/03/epos-print">
            <text align="center"/>
            <text lang="zh-tw"/>
            <text font="font_b"/>
            <text width="2" height="2"/>
            <text>交易明細&#10;</text>
            <feed line="1"/>
            <text align="left"/>
            <text width="1" height="1"/>
            <text linespc="30"/>

            <text>營業人統編: ${client.attributes["UBN"]!"NA"}&#10;</text>
            <text>公司名稱: ${client.clientName!"NA"}&#10;</text>
            <text>營業人地址: ${client.attributes["ADDRESS"]!"NA"}&#10;</text>
            <text>訂單號碼: ${order.serialId!"NA"}&#10;</text>
            <#if orderTransaction??>
                <text>交易序號: ${orderTransaction.id[orderTransaction.id?length - 6..]!"NA"}&#10;</text>
                <text>交易日期: ${orderTransaction.createdDate?string('MM/dd/yyyy HH:mm:ss')!"NA"}&#10;</text>
            </#if>
            <feed line="1"/>
            <text linespc="40"/>
            <#list order.orderLineItems as li>
                <text width="2" height="2"/>
                <text>${li.productSnapshot.name}&#10;</text>
                <text width="1" height="1"/>
                <text x="250">x ${li.quantity}</text>
                <text x="310">$${li.lineItemSubTotal}&#10;</text>
            </#list>
            <text linespc="30"/>
            <feed line="1"/>
            <text width="1" height="1"/>
            <text>合計:</text><text x="310">$${order.orderTotal!"NA"}&#10;</text>
            <#if orderTransaction??>
                <text>發票金額:</text><text x="310">$${orderTransaction.settleAmount!"NA"}&#10;</text>
            </#if>
            <cut type="feed"/>
        </epos-print>
    </s:Body>
</s:Envelope>
</@compress>