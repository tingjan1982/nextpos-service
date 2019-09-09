<#-- @ftlvariable name="order" type="io.nextpos.ordermanagement.data.Order" -->
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
            <text>工作單&#10;</text>
            <text linespc="35"/>
            <text align="left"/>
            <text width="1" height="1"/>
            <text linespc="35"/>
            <text>${order.createdDate?string('MM/dd/yyyy HH:mm:ss')!"NA"}&#10;</text>
            <text>訂單: ${order.id!"NA"}&#10;</text>
            <text>桌號: ${order.tableId!"NA"}&#10;</text>
            <text linespc="65"/>
            <#list lineItems as li>
                <text>${li.productSnapshot.name}</text><text x="250">${li.quantity}&#10;</text>
                <#list li.productSnapshot.productOptions as option>
                    <text>${option.optionName}: ${option.optionValue}&#10;</text>
                </#list>
            </#list>
            <text linespc="35"/>
            <cut type="feed"/>
        </epos-print>
    </s:Body>
</s:Envelope>
</@compress>