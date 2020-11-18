<#-- @ftlvariable name="order" type="io.nextpos.ordermanagement.data.Order" -->
<#-- reference: https://freemarker.apache.org/docs/dgui_misc_whitespace.html-->
<@compress single_line=true>
<s:Envelope xmlns:s="http://schemas.xmlsoap.org/soap/envelope/">
    <s:Body>
        <epos-print xmlns="http://www.epson-pos.com/schemas/2011/03/epos-print">
            <text align="center"/>
            <text lang="zh-tw"/>
            <text font="font_b"/>
            <text width="2" height="2"/>
            <text>工作單&#10;</text>
            <feed line="1"/>
            <text align="left"/>
            <text width="2" height="1"/>
            <text linespc="45"/>
            <text>${order.createdDate?string('MM/dd/yyyy HH:mm:ss')!"NA"}&#10;</text>
            <text>訂單: ${order.serialId!"NA"}&#10;</text>
            <text>桌號: ${order.oneTableInfo.displayName!"外帶"}&#10;</text>
            <feed line="1"/>
            <text width="3" height="2"/>
            <#list lineItems as li>
                <text linespc="80"/>
                <text>${li.productSnapshot.name}</text><text x="310">${li.quantity}&#10;</text>
                <text linespc="60"/>
                <#list li.productSnapshot.productOptions as option>
                    <text> &gt; ${option.optionName}: ${option.optionValue}&#10;</text>
                </#list>
            </#list>
            <feed line="1"/>
            <cut type="feed"/>
        </epos-print>
    </s:Body>
</s:Envelope>
</@compress>