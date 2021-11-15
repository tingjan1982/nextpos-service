package io.nextpos.subscription.service;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.canvas.draw.DashedLine;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.property.HorizontalAlignment;
import com.itextpdf.layout.property.TextAlignment;
import io.nextpos.client.data.Client;
import io.nextpos.client.service.ClientService;
import io.nextpos.einvoice.common.invoice.ElectronicInvoice;
import io.nextpos.notification.data.DynamicEmailDetails;
import io.nextpos.notification.data.NotificationDetails;
import io.nextpos.notification.service.NotificationService;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordermanagement.data.OrderLineItem;
import io.nextpos.ordermanagement.data.OrderSettings;
import io.nextpos.ordermanagement.data.ProductSnapshot;
import io.nextpos.ordermanagement.service.OrderService;
import io.nextpos.ordermanagement.service.ShiftService;
import io.nextpos.ordertransaction.data.OrderTransaction;
import io.nextpos.ordertransaction.service.OrderTransactionService;
import io.nextpos.settings.data.CountrySettings;
import io.nextpos.shared.exception.GeneralApplicationException;
import io.nextpos.shared.exception.ObjectNotFoundException;
import io.nextpos.shared.service.annotation.ChainedTransaction;
import io.nextpos.shared.util.DateTimeUtil;
import io.nextpos.shared.util.ImageCodeUtil;
import io.nextpos.subscription.data.ClientSubscriptionInvoice;
import io.nextpos.subscription.data.ClientSubscriptionInvoiceRepository;
import io.nextpos.subscription.data.SubscriptionPlan;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.Binary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.concurrent.CompletableFuture;

@Service
@ChainedTransaction
public class ClientSubscriptionOrderServiceImpl implements ClientSubscriptionOrderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientSubscriptionServiceImpl.class);

    private final ClientService clientService;

    private final OrderService orderService;

    private final OrderTransactionService orderTransactionService;

    private final ClientSubscriptionInvoiceRepository clientSubscriptionInvoiceRepository;

    private final ShiftService shiftService;

    private final NotificationService notificationService;

    private final ImageCodeUtil imageCodeUtil;

    private final CountrySettings defaultCountrySettings;

    private final byte[] fontBytes;

    @Autowired
    public ClientSubscriptionOrderServiceImpl(ClientService clientService, OrderService orderService, OrderTransactionService orderTransactionService, ClientSubscriptionInvoiceRepository clientSubscriptionInvoiceRepository, ShiftService shiftService, NotificationService notificationService, ImageCodeUtil imageCodeUtil, CountrySettings defaultCountrySettings,
                                              @Value("classpath:fonts/bkai00mp.ttf") Resource fontResource) throws Exception {
        this.clientService = clientService;
        this.orderService = orderService;
        this.orderTransactionService = orderTransactionService;
        this.clientSubscriptionInvoiceRepository = clientSubscriptionInvoiceRepository;
        this.shiftService = shiftService;
        this.notificationService = notificationService;
        this.imageCodeUtil = imageCodeUtil;
        this.defaultCountrySettings = defaultCountrySettings;
        this.fontBytes = IOUtils.toByteArray(fontResource.getInputStream());
    }

    @Override
    public void sendClientSubscriptionOrder(ClientSubscriptionInvoice clientSubscriptionInvoice, String overrideEmail) {

        if (clientSubscriptionInvoice.getElectronicInvoice() == null) {
            clientService.getClientByUsername("rain.io.app@gmail.com").ifPresent(c -> {
                final OrderSettings orderSettings = new OrderSettings(defaultCountrySettings, true, BigDecimal.ZERO);
                final Order order = Order.newOrder(c.getId(), Order.OrderType.ONLINE, orderSettings);
                order.addOrderLineItem(new OrderLineItem(createProductSnapshot(clientSubscriptionInvoice), 1, orderSettings));
                order.setState(Order.OrderState.DELIVERED);

                shiftService.openShift(c.getId(), BigDecimal.ZERO);
                orderService.createOrder(order);

                final OrderTransaction orderTransaction = orderTransactionService.createOrderTransaction(c, createOrderTransaction(clientSubscriptionInvoice, order));
                orderService.performOrderAction(order.getId(), Order.OrderAction.COMPLETE);

                orderTransaction.getElectronicInvoice().ifPresent(inv -> {
                        clientSubscriptionInvoice.setElectronicInvoice(inv);
                        clientSubscriptionInvoiceRepository.save(clientSubscriptionInvoice);
                });
            });
        }

        try {
            sendNotification(clientSubscriptionInvoice, overrideEmail).get();

        } catch (Exception e) {
            LOGGER.error("Error while creating and sending client order: {}", e.getMessage(), e);
        }
    }

    private ProductSnapshot createProductSnapshot(ClientSubscriptionInvoice clientSubscriptionInvoice) {

        return new ProductSnapshot(clientSubscriptionInvoice.getId(),
                clientSubscriptionInvoice.getClientSubscription().getSubscriptionPlanSnapshot().getPlanName(),
                clientSubscriptionInvoice.getDueAmount().getAmount());
    }

    private OrderTransaction createOrderTransaction(ClientSubscriptionInvoice invoice, Order order) {

        final OrderTransaction orderTransaction = new OrderTransaction(order,
                OrderTransaction.PaymentMethod.CARD,
                OrderTransaction.BillType.SINGLE,
                null);

        final Client client = clientService.getClientOrThrows(invoice.getClientSubscription().getClientId());
        final String ubn = client.getAttribute(Client.ClientAttributes.UBN);
        orderTransaction.updateInvoiceDetails(ubn, null, null, null, false);

        return orderTransaction;
    }

    private CompletableFuture<NotificationDetails> sendNotification(ClientSubscriptionInvoice subscriptionInvoice, String overrideEmail) {

        final Client client = clientService.getClient(subscriptionInvoice.getClientSubscription().getClientId()).orElseThrow(() -> {
            throw new ObjectNotFoundException(subscriptionInvoice.getClientSubscription().getClientId(), Client.class);
        });

        String emailToUse = client.getNotificationEmail(overrideEmail);

        final DynamicEmailDetails dynamicEmailDetails = new DynamicEmailDetails(client.getId(), emailToUse, "d-e574bf79c5534e52a86c80f25a762ba5");
        dynamicEmailDetails.addTemplateData("client", client.getClientName());
        SubscriptionPlan subscriptionPlanSnapshot = subscriptionInvoice.getClientSubscription().getSubscriptionPlanSnapshot();
        final CountrySettings.RoundingAmountHelper helper = defaultCountrySettings.roundingAmountHelper();

        dynamicEmailDetails.addTemplateData("subscriptionPlan", subscriptionPlanSnapshot.getPlanName());
        dynamicEmailDetails.addTemplateData("subscriptionPlanPeriod", subscriptionInvoice.getSubscriptionPeriod(client.getZoneId()));
        dynamicEmailDetails.addTemplateData("subscriptionAmount", helper.roundAmountAsString(() -> subscriptionInvoice.getDueAmount().getAmountWithoutTax()));
        dynamicEmailDetails.addTemplateData("subscriptionTax", helper.roundAmountAsString(() -> subscriptionInvoice.getDueAmount().getTax()));
        dynamicEmailDetails.addTemplateData("subscriptionAmountWithTax", helper.roundAmountAsString(() -> subscriptionInvoice.getDueAmount().getAmountWithTax()));
        dynamicEmailDetails.addTemplateData("invoiceIdentifier", subscriptionInvoice.getInvoiceIdentifier());

        final ElectronicInvoice electronicInvoice = subscriptionInvoice.getElectronicInvoice();

        if (electronicInvoice != null) {
            final byte[] pdf = generateElectronicInvoicePdf(electronicInvoice);
            dynamicEmailDetails.setAttachment(new Binary(pdf));
            dynamicEmailDetails.setContentType("application/pdf");
            dynamicEmailDetails.setFilename("einvoice.pdf");
        }

        return notificationService.sendNotification(dynamicEmailDetails);
    }

    private byte[] generateElectronicInvoicePdf(ElectronicInvoice electronicInvoice) {

        try (ByteArrayOutputStream pdfOs = new ByteArrayOutputStream()) {
            final PdfFont chineseFont = PdfFontFactory.createFont(fontBytes, PdfEncodings.IDENTITY_H, true);
            final PdfDocument pdf = new PdfDocument(new PdfWriter(pdfOs));
            final PdfPage pdfPage = pdf.addNewPage(PageSize.A4);
            final PdfCanvas pdfCanvas = new PdfCanvas(pdfPage);
            final Rectangle rectangle = new Rectangle(20, PageSize.A4.getHeight() - 255 - 20, 164, 255);
            pdfCanvas.rectangle(rectangle);
            new DashedLine(1).draw(pdfCanvas, rectangle);

            final Canvas canvas = new Canvas(pdfCanvas, rectangle);

            canvas.add(new Paragraph(electronicInvoice.getSellerName()).setFont(chineseFont).setFontSize(10).setTextAlignment(TextAlignment.CENTER).setFixedLeading(6).setMarginTop(20));
            canvas.add(new Paragraph("電子發票證明聯").setFont(chineseFont).setFontSize(16).setTextAlignment(TextAlignment.CENTER).setFixedLeading(6));
            canvas.add(new Paragraph(electronicInvoice.getFormattedInvoiceDate()).setFont(chineseFont).setFontSize(16).setTextAlignment(TextAlignment.CENTER).setFixedLeading(6));
            canvas.add(new Paragraph(electronicInvoice.getInvoiceNumber()).setFont(chineseFont).setFontSize(16).setTextAlignment(TextAlignment.CENTER).setFixedLeading(6));

            final String invoiceDate = DateTimeUtil.formatDate(ZoneId.of("Asia/Taipei"), electronicInvoice.getInvoiceCreatedDate());

            canvas.add(new Paragraph(invoiceDate).setFont(chineseFont).setFontSize(8).setMarginLeft(5).setFixedLeading(3));
            canvas.add(new Paragraph("隨機碼: ").add(electronicInvoice.getRandomNumber()).add("  ").add("總計: ").add(electronicInvoice.getSalesAmount().toString()).setFont(chineseFont).setFontSize(8).setMarginLeft(5).setFixedLeading(3));
            final Paragraph ubns = new Paragraph("賣方: ").add(electronicInvoice.getSellerUbn());

            if (StringUtils.isNotBlank(electronicInvoice.getBuyerUbn())) {
                ubns.add("  ").add("買方: ").add(electronicInvoice.getBuyerUbn());
            }
            canvas.add(ubns.setFont(chineseFont).setFontSize(8).setMarginLeft(5).setFixedLeading(3));

            final BufferedImage qrCode = imageCodeUtil.generateBarCode(electronicInvoice.getBarcodeContent());
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            ImageIO.write(qrCode, "png", bo);
            canvas.add(new Image(ImageDataFactory.create(bo.toByteArray())).scaleAbsolute(150, 15).setMarginTop(6).setMarginBottom(10).setHorizontalAlignment(HorizontalAlignment.CENTER));

            final Table table = new Table(2).setWidth(150).setHorizontalAlignment(HorizontalAlignment.CENTER);

            final BufferedImage qrCode1 = imageCodeUtil.generateQRCode(electronicInvoice.getQrCode1Content());
            ByteArrayOutputStream bo1 = new ByteArrayOutputStream();
            ImageIO.write(qrCode1, "png", bo1);
            final Image image1 = new Image(ImageDataFactory.create(bo1.toByteArray()));
            image1.scaleAbsolute(60, 60);
            table.addCell(new Cell().add(image1).setBorder(Border.NO_BORDER));

            final BufferedImage qrCode2 = imageCodeUtil.generateQRCode(electronicInvoice.getQrCode2Content());
            ByteArrayOutputStream bo2 = new ByteArrayOutputStream();
            ImageIO.write(qrCode2, "png", bo2);
            final Image image2 = new Image(ImageDataFactory.create(bo2.toByteArray()));
            image2.scaleAbsolute(60, 60);
            table.addCell(new Cell().add(image2).setBorder(Border.NO_BORDER));

            canvas.add(table);

            canvas.close();
            pdf.close();

            return pdfOs.toByteArray();
        } catch (Exception e) {
            throw new GeneralApplicationException("Error while generating electronic invoice PDF: " + e.getMessage(), e);
        }
    }
}
