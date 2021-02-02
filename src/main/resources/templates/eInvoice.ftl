<#-- @ftlvariable name="client" type="io.nextpos.client.data.Client" -->
<#-- @ftlvariable name="order" type="io.nextpos.ordermanagement.data.Order" -->
<#-- @ftlvariable name="orderTransaction" type="io.nextpos.ordertransaction.data.OrderTransaction" -->
<#-- @ftlvariable name="electronicInvoice" type="io.nextpos.einvoice.common.invoice.ElectronicInvoice" -->
<#-- reference: https://freemarker.apache.org/docs/dgui_misc_whitespace.html-->
<@compress single_line=true>
    <s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/">
        <s:Body>
            <epos-print xmlns="http://www.epson-pos.com/schemas/2011/03/epos-print">
                <text lang="zh-tw"/>
                <text align="center"/>
                <text font="font_a"/>
                <text width="2" height="2"/>
                <text>${client.clientName!"NA"}&#10;</text>
                <feed line="1"/>
                <text dw="true" dh="true" linespc="60"/>
                <text>電子發票證明聯&#10;</text>
                <text>${electronicInvoice.formattedInvoiceDate}&#10;</text>
                <text>${electronicInvoice.invoiceNumber}&#10;</text>
                <text align="left"/>
                <text width="1" height="1" linespc="30"/>
                <text>${electronicInvoice.invoiceCreatedDate?string('MM/dd/yyyy HH:mm:ss')}&#10;</text>
                <text>隨機碼:${electronicInvoice.randomNumber}</text>
                <text x="210"/>
                <text>總計:${electronicInvoice.salesAmount}&#10;</text>
                <text>賣方:${client.attributes["UBN"]!"NA"}</text>
                <#if electronicInvoice.buyerUbn?has_content>
                    <text>  買方:${electronicInvoice.buyerUbn}</text>
                </#if>
                <text>&#10;</text>
                <barcode type="code39" align="center" width="1" height="45">${electronicInvoice.barcodeContent}</barcode>
                <page>
                    <area x="0" y="0" width="420" height="200"/>
                    <direction dir="left_to_right"/>
                    <text x="15" y="160"/>
                    <image width="160" height="160" color="color_1" mode="mono">${electronicInvoice.qrCode1ImageBinary}</image>
<#--                    <command>1D286B040031413200</command>-->
<#--                    <command>1D286B0300314303</command>-->
<#--                    <command>1D286B0300314531</command>-->
<#--                    <command>${electronicInvoice.qrCode1ContentAsHex}</command>-->
<#--                    <command>1D286B0300315130</command>-->
<#--                    <symbol type="qrcode_model_2" level="level_l" width="3">${electronicInvoice.qrCode1Content}</symbol>-->
                    <text x="170" y="160"/>
                    <image width="160" height="160" color="color_1" mode="mono">${electronicInvoice.qrCode2ImageBinary}</image>
<#--                    <command>1D286B040031413200</command>-->
<#--                    <command>1D286B0300314303</command>-->
<#--                    <command>1D286B0300314531</command>-->
<#--                    <command>${electronicInvoice.qrCode2ContentAsHex}</command>-->
<#--                    <command>1D286B0300315130</command>-->
<#--                    <symbol type="qrcode_model_2" level="level_l" width="3">${electronicInvoice.qrCode2Content}</symbol>-->
                </page>
                <text align="left"/>
                <text>訂單號碼：${order.serialId!"NA"}&#10;</text>
                <text>卡號：${orderTransaction.paymentDetails.values['LAST_FOUR_DIGITS']!""}&#10;</text>
                <cut type="feed"/>
            </epos-print>
        </s:Body>
    </s:Envelope>
</@compress>