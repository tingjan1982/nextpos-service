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
                <text>總計:${orderTransaction.settleAmount}&#10;</text>
                <text>賣方:${client.attributes["UBN"]!"NA"}&#10;</text>
                <barcode type="code39" align="center" width="1" height="45">${electronicInvoice.barcodeContent}</barcode>
                <page>
                    <area x="0" y="0" width="420" height="220"/>
                    <direction dir="left_to_right"/>
                    <text x="5"/>
                    <symbol type="qrcode_model_2" level="level_l" width="4">${electronicInvoice.qrCode1Content}</symbol>
                    <text x="230"/>
                    <symbol type="qrcode_model_2" level="level_l" width="6">${electronicInvoice.qrCode2Content}</symbol>
                </page>
                <text align="left"/>
                <text>訂單號碼：${order.serialId!"NA"}&#10;</text>
                <text>卡號：${orderTransaction.paymentDetails.values['LAST_FOUR_DIGITS']!""}&#10;</text>
                <cut type="feed"/>
            </epos-print>
        </s:Body>
    </s:Envelope>
</@compress>