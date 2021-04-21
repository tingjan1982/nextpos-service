<#-- @ftlvariable name="electronicInvoice" type="io.nextpos.einvoice.common.invoice.ElectronicInvoice" -->
<#-- @ftlvariable name="client" type="io.nextpos.client.data.Client" -->
<#-- @ftlvariable name="order" type="io.nextpos.ordermanagement.data.Order" -->
<#-- @ftlvariable name="orderTransaction" type="io.nextpos.ordertransaction.data.OrderTransaction" -->
<#-- reference: https://freemarker.apache.org/docs/dgui_misc_whitespace.html-->
<@compress single_line=true>
<s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/">
    <s:Body>
        <epos-print xmlns="http://www.epson-pos.com/schemas/2011/03/epos-print">
            <#setting time_zone="Asia/Taipei">
            <text align="center"/>
            <text lang="zh-tw"/>
            <text font="font_b"/>
            <text width="2" height="2"/>
            <text>${client.clientName!"NA"}&#10;</text>
            <feed line="1"/>
            <text>營業人銷貨退回，進貨退出或折讓證明單&#10;</text>
            <feed line="1"/>
            <#assign currentDate = .now>
            <text width="3" height="2"/>
            <text>${currentDate?string('yyyy-MM-dd')}&#10;</text>
            <feed line="1"/>
            <text align="left"/>
            <text width="1" height="1"/>
            <text linespc="30"/>

            <text>賣方統編: ${client.attributes["UBN"]!"NA"}&#10;</text>
            <text>賣方名稱: ${client.attributes["COMPANY_NAME"]!"NA"}&#10;</text>
            <text>發票開立日期: ${currentDate?string('yyyy-MM-dd')}&#10;</text>
            <text align="center"/>
            <text width="2" height="2"/>
            <feed line="1"/>
            <text>${electronicInvoice.internalInvoiceNumber}&#10;</text>
            <feed line="1"/>
            <text align="left"/>
            <text width="1" height="1"/>
            <#if electronicInvoice.buyerUbn?has_content>
                <text>買方統編: ${electronicInvoice.buyerUbn}&#10;</text>
                <text>買方名稱: ${electronicInvoice.buyerName}&#10;</text>
            </#if>
            <feed line="2"/>
            <text linespc="40"/>
            <#list order.orderLineItems as li>
                <text width="2" height="2"/>
                <text>${li.productSnapshot.name}&#10;</text>
                <text width="1" height="1"/>
                <text x="100">x ${li.quantity} (${li.productSnapshot.productPriceWithOptions})</text>
                <text x="310">$${li.lineItemSubTotal} TX&#10;</text>
            </#list>
            <text linespc="30"/>
            <feed line="1"/>
            <text width="1" height="1"/>
            <text>營業稅額合計:</text>
            <text x="310">${order.orderTotalTax}&#10;</text>
            <text>金額(不含稅之進貨額)合計:</text>
            <text x="310">${order.orderTotalWithoutTax}&#10;</text>
            <text>簽收人&#10;</text>
            <feed line="4"/>
            <cut type="feed"/>
        </epos-print>
    </s:Body>
</s:Envelope>
</@compress>