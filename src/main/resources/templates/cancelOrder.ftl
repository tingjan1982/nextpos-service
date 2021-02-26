<#-- @ftlvariable name="electronicInvoice" type="io.nextpos.einvoice.common.invoice.ElectronicInvoice" -->
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
            <text>營業人銷貨退回，進貨退出或折讓證明單&#10;</text>
            <feed line="1"/>
            <text align="left"/>
            <text width="1" height="1"/>
            <text linespc="30"/>

            <#assign currentDate = .now>
            <text>作廢日期: ${currentDate?string('MM/dd/yyyy HH:mm:ss')}&#10;</text>
            <text>賣方統編: ${client.attributes["UBN"]!"NA"}&#10;</text>
            <text>賣方名稱: ${client.attributes["COMPANY_NAME"]!"NA"}&#10;</text>
            <#if electronicInvoice.buyerUbn?has_content>
                <text>買方統編:${electronicInvoice.buyerUbn}</text>
            </#if>
            <text>訂單號碼: ${order.serialId!"NA"}&#10;</text>
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
                <text>總金額:</text><text x="310">$${orderTransaction.settleAmount!"NA"}&#10;</text>
            </#if>
            <cut type="feed"/>
        </epos-print>
    </s:Body>
</s:Envelope>
</@compress>