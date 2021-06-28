<#-- @ftlvariable name="shift" type="io.nextpos.ordermanagement.data.ShiftReport" -->
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
                <text>${shift.clientName}&#10;</text>
                <feed line="1"/>
                <text>關帳紀錄&#10;</text>
                <feed line="1"/>
                <text align="left" width="1" height="1"/>
                <text linespc="30"/>

                <text>開帳期間&#10;</text>
                <text>開始: ${((shift.startDate?datetime)?iso_local_nz)}&#10;</text>
                <text>結束: ${((shift.endDate?datetime)?iso_local_nz)}&#10;</text>
                <feed line="1"/>
                <text>總營業額:</text>
                <text x="${indent}">${shift.orderSummary.orderTotal}&#10;</text>
                <text>總折扣:</text>
                <text x="${indent}">${shift.orderSummary.discount}&#10;</text>
                <text>總服務費:</text>
                <text x="${indent}">${shift.orderSummary.serviceCharge}&#10;</text>
                <text>開帳現金:</text>
                <text x="${indent}">${shift.startBalance}&#10;</text>
                <feed line="1"/>

                <#list shift.summaries as summary>
                    <text align="center" width="2" height="2"/>
                    <text>${summary.paymentMethodDisplayName}&#10;</text>
                    <feed line="1"/>
                    <text align="left" width="1" height="1"/>

                    <text>${summary.paymentMethodDisplayName}營業額:</text>
                    <text x="${indent}">${summary.total}&#10;</text>
                    <text>關帳金額:</text>
                    <text x="${indent}">${summary.closingBalance}&#10;</text>
                    <text>差額:</text>
                    <text x="${indent}">${summary.difference}&#10;</text>
                    <text>差異原因: ${summary.unbalanceReason!""}&#10;</text>
                    <feed line="1"/>
                </#list>

                <!-- order count -->
                <text align="center" width="2" height="2"/>
                <text>訂單總覽&#10;</text>
                <feed line="1"/>
                <text align="left" width="1" height="1"/>
                <text>訂單數:</text>
                <text x="${indent}">${shift.totalOrderCount}&#10;</text>
                <#assign stateTranslation = { "DELETED": "刪", "COMPLETED": "完成" }>
                <#list shift.orderCounts as state, count>
                    <text>${stateTranslation[state]}單數:</text>
                    <text x="${indent}">${count.orderCount!0}&#10;</text>
                </#list>
                <feed line="1"/>

                <!-- deleted line items -->
                <text align="center" width="2" height="2"/>
                <text>刪除品項紀錄&#10;</text>
                <feed line="1"/>
                <text align="left" width="1" height="1"/>
                <#list shift.deletedLineItems as item>
                    <text>${item.productName} x ${item.quantity}</text>
                    <text x="${indent}">${item.total}&#10;</text>
                </#list>
                <feed line="1"/>
                <text>刪除總數:</text>
                <text x="${indent}">${shift.deletedLineItems?size}&#10;</text>
                <text>刪除金額:</text>
                <text x="${indent}">${shift.deletedLineItemsTotal!0}&#10;</text>
                <text>關帳備註: ${shift.closingRemark}</text>
                <feed line="2"/>
                <cut type="feed"/>
            </epos-print>
        </s:Body>
    </s:Envelope>
</@compress>