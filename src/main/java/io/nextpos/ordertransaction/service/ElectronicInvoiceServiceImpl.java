package io.nextpos.ordertransaction.service;

import io.nextpos.client.data.Client;
import io.nextpos.ordermanagement.data.Order;
import io.nextpos.ordertransaction.data.ElectronicInvoice;
import io.nextpos.ordertransaction.data.ElectronicInvoiceRepository;
import io.nextpos.ordertransaction.data.OrderTransaction;
import io.nextpos.ordertransaction.util.InvoiceQRCodeEncryptor;
import io.nextpos.shared.exception.GeneralApplicationException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ElectronicInvoiceServiceImpl implements ElectronicInvoiceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ElectronicInvoiceServiceImpl.class);

    private final ElectronicInvoiceRepository electronicInvoiceRepository;

    private final RestTemplate restTemplate;

    @Autowired
    public ElectronicInvoiceServiceImpl(final ElectronicInvoiceRepository electronicInvoiceRepository) {
        this.electronicInvoiceRepository = electronicInvoiceRepository;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public ElectronicInvoice createElectronicInvoice(final Client client, final Order order, final OrderTransaction orderTransaction) {

        final AuthenticateResponse authenticateResponse = authenticate();

        String createInvoiceUrl = "http://e00008550.skinapi.com/eInvoice/B2C/create/7408";

        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-deva-locale", "zh");
        headers.set("x-deva-token", authenticateResponse.getToken().getToken());

        final List<CreateInvoiceRequest.InvoiceProduct> prodList = order.getOrderLineItems().stream()
                .map(li -> new CreateInvoiceRequest.InvoiceProduct(li.getProductSnapshot().getId(),
                        li.getProductSnapshot().getName(),
                        li.getQuantity(),
                        li.getLineItemSubTotal()))
                .collect(Collectors.toList());

        final CreateInvoiceRequest requestBody = new CreateInvoiceRequest(10,
                prodList,
                orderTransaction.getPaymentDetailsByKey(OrderTransaction.PaymentDetailsKey.LAST_FOUR_DIGITS));
        final HttpEntity<CreateInvoiceRequest> request = new HttpEntity<>(requestBody, headers);

        final ResponseEntity<CreateInvoiceResponse> response = restTemplate.postForEntity(createInvoiceUrl, request, CreateInvoiceResponse.class);

        if (response.getStatusCode() == HttpStatus.OK && response.hasBody()) {
            final CreateInvoiceResponse invoiceResponse = response.getBody();

            final String getInvoiceDetailsUrl = "http://e00008550.skinapi.com/eInvoice/B2C/viewByStore/" + invoiceResponse.getValue().getInvID();

            final HttpEntity<?> invoiceDetailsRequest = new HttpEntity<>(headers);
            final ResponseEntity<GetInvoiceDetails> getInvoiceDetailsResponse = restTemplate.exchange(getInvoiceDetailsUrl, HttpMethod.GET, invoiceDetailsRequest, GetInvoiceDetails.class);

            if (getInvoiceDetailsResponse.getStatusCode() == HttpStatus.OK && getInvoiceDetailsResponse.hasBody()) {
                final GetInvoiceDetails getInvoiceDetails = getInvoiceDetailsResponse.getBody();
                final GetInvoiceDetails.InvoiceDetailsValue value = getInvoiceDetails.getValue();

                final ElectronicInvoice electronicInvoice = new ElectronicInvoice(
                        order.getId(),
                        value.getInvID(),
                        value.getStatus(),
                        value.getInvNo(),
                        new ElectronicInvoice.InvoicePeriod(value.getYear(), value.getStrMonth(), value.getEndMonth()),
                        value.getRandNo(),
                        value.getCreateTime(),
                        value.getSalesAmount(),
                        value.getTaxAmount(),
                        client.getAttribute(Client.ClientAttributes.UBN.name()));

                if (value.getCarrier() != null) {
                    electronicInvoice.setBuyerUbn(value.getCarrier().getInvNID());
                }

                final InvoiceQRCodeEncryptor encryptor = createInvoiceQRCodeEncryptor(client);
                electronicInvoice.generateBarcodeContent();
                electronicInvoice.generateQrCode1Content(encryptor, order);
                electronicInvoice.generateQrCode2Content(order);

                return electronicInvoiceRepository.save(electronicInvoice);
            }
        }

        throw new GeneralApplicationException("Error while getting electronic invoice details");
    }

    private InvoiceQRCodeEncryptor createInvoiceQRCodeEncryptor(Client client) {
        final String aesKey = client.getAttribute(Client.ClientAttributes.AES_KEY.name());

        return new InvoiceQRCodeEncryptor(aesKey);
    }

    private AuthenticateResponse authenticate() {

        final String authenticateUrl = "http://e00008550.skinapi.com/@admin/user/login";

        final HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-deva-locale", "zh");
        headers.set("x-deva-appkey", "fc95e3e5a4c75f39ddbd14b2291c7cb2");

        final AuthenticateRequest requestBody = new AuthenticateRequest("00008550", "2f4c74524962fcd9b33457be1255385783878b02");
        final HttpEntity<AuthenticateRequest> request = new HttpEntity<>(requestBody, headers);
        final ResponseEntity<AuthenticateResponse> response = restTemplate.postForEntity(authenticateUrl, request, AuthenticateResponse.class);

        LOGGER.info("{}", response.getBody());

        if (response.getStatusCode() == HttpStatus.OK) {
            return response.getBody();
        }

        throw new GeneralApplicationException("Authentication failed against external e-invoice partner");
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class AuthenticateRequest {

        private String accName;

        private String passwd;
    }

    @Data
    @NoArgsConstructor
    private static class AuthenticateResponse {

        private AuthToken token;

        @Data
        private static class AuthToken {

            private String token;

            private long validTo;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class CreateInvoiceRequest {

        private int carrierType;

        private List<InvoiceProduct> prodList;

        private String cdc4;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        private static class InvoiceProduct {

            private String prodNo;

            private String title;

            private int qty;

            private BigDecimal price;
        }
    }

    @Data
    @NoArgsConstructor
    private static class CreateInvoiceResponse {

        private InvoiceValue value;

        @Data
        @NoArgsConstructor
        private static class InvoiceValue {

            private String poNo;

            private String poID;

            private String invID;

            private String invNo;
        }
    }

    @Data
    @NoArgsConstructor
    private static class GetInvoiceDetails {

        private InvoiceDetailsValue value;

        @Data
        @NoArgsConstructor
        private static class InvoiceDetailsValue {

            private String invID;

            private String status;

            private String poID;

            private String poNo;

            private String invNo;

            private String randNo;

            private String createTime;

            private String dspName;

            private BigDecimal salesAmount;

            private BigDecimal taxAmount;

            private String year;

            private String strMonth;

            private String endMonth;

            private CarrierValue carrier;

            @Data
            @NoArgsConstructor
            private static class CarrierValue {

                /**
                 * Buyer UBN.
                 */
                private String invNID;

            }
        }
    }
}
