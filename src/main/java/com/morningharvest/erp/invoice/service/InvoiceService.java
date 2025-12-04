package com.morningharvest.erp.invoice.service;

import com.morningharvest.erp.common.exception.ResourceNotFoundException;
import com.morningharvest.erp.invoice.client.InvoiceServiceClient;
import com.morningharvest.erp.invoice.client.dto.*;
import com.morningharvest.erp.invoice.dto.*;
import com.morningharvest.erp.invoice.entity.Invoice;
import com.morningharvest.erp.invoice.entity.InvoiceAllowance;
import com.morningharvest.erp.invoice.entity.InvoiceItem;
import com.morningharvest.erp.invoice.repository.InvoiceAllowanceRepository;
import com.morningharvest.erp.invoice.repository.InvoiceItemRepository;
import com.morningharvest.erp.invoice.repository.InvoiceRepository;
import com.morningharvest.erp.order.entity.Order;
import com.morningharvest.erp.order.entity.OrderItem;
import com.morningharvest.erp.order.entity.SingleOrderItem;
import com.morningharvest.erp.order.entity.ComboOrderItem;
import com.morningharvest.erp.order.repository.OrderItemRepository;
import com.morningharvest.erp.order.repository.OrderRepository;
import com.morningharvest.erp.payment.entity.PaymentTransaction;
import com.morningharvest.erp.payment.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final InvoiceAllowanceRepository invoiceAllowanceRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final InvoiceServiceClient invoiceServiceClient;

    // 稅率 5%
    private static final BigDecimal TAX_RATE = new BigDecimal("0.05");

    /**
     * 開立發票
     */
    @Transactional
    public InvoiceResult issueInvoice(IssueInvoiceRequest request) {
        log.info("開立發票, orderId: {}, invoiceType: {}", request.getOrderId(), request.getInvoiceType());

        // 1. 驗證訂單存在
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("訂單不存在: " + request.getOrderId()));

        // 2. 驗證付款交易存在
        PaymentTransaction payment = paymentTransactionRepository.findById(request.getPaymentTransactionId())
                .orElseThrow(() -> new ResourceNotFoundException("付款交易不存在: " + request.getPaymentTransactionId()));

        // 3. 檢查是否已開立過發票
        if (invoiceRepository.existsByOrderId(request.getOrderId())) {
            throw new IllegalArgumentException("該訂單已開立發票");
        }

        // 4. 計算金額 (含稅金額拆分)
        BigDecimal totalAmount = order.getTotalAmount();
        BigDecimal taxAmount = totalAmount.multiply(TAX_RATE)
                .divide(BigDecimal.ONE.add(TAX_RATE), 0, RoundingMode.HALF_UP);
        BigDecimal salesAmount = totalAmount.subtract(taxAmount);

        // 5. 建立發票記錄
        Invoice invoice = Invoice.builder()
                .orderId(request.getOrderId())
                .paymentTransactionId(request.getPaymentTransactionId())
                .invoiceType(request.getInvoiceType())
                .issueType(request.getIssueType())
                .buyerIdentifier(request.getBuyerIdentifier())
                .buyerName(request.getBuyerName())
                .carrierType(request.getCarrierType())
                .carrierValue(request.getCarrierValue())
                .isDonated(request.getIsDonated() != null ? request.getIsDonated() : false)
                .donateCode(request.getDonateCode())
                .salesAmount(salesAmount)
                .taxAmount(taxAmount)
                .totalAmount(totalAmount)
                .status("ISSUED")
                .build();

        Invoice savedInvoice = invoiceRepository.save(invoice);

        // 6. 建立發票明細
        List<OrderItem> orderItems = orderItemRepository.findByOrderIdOrderByIdAsc(request.getOrderId());
        List<InvoiceItem> invoiceItems = createInvoiceItems(savedInvoice.getId(), orderItems);
        invoiceItemRepository.saveAll(invoiceItems);

        // 7. 呼叫外部發票服務
        IssueInvoiceExternalRequest externalRequest = buildExternalRequest(savedInvoice, invoiceItems);
        IssueInvoiceExternalResponse externalResponse = invoiceServiceClient.issueInvoice(externalRequest);

        // 8. 更新發票資訊
        if (Boolean.TRUE.equals(externalResponse.getSuccess())) {
            savedInvoice.setInvoiceNumber(externalResponse.getInvoiceNumber());
            savedInvoice.setInvoiceDate(externalResponse.getInvoiceDate());
            savedInvoice.setInvoicePeriod(externalResponse.getInvoicePeriod());
            savedInvoice.setExternalInvoiceId(externalResponse.getExternalId());
            savedInvoice.setStatus("ISSUED");
            savedInvoice.setIssuedAt(LocalDateTime.now());
        } else {
            savedInvoice.setStatus("FAILED");
        }
        savedInvoice.setIssueResultCode(externalResponse.getResultCode());
        savedInvoice.setIssueResultMessage(externalResponse.getResultMessage());

        invoiceRepository.save(savedInvoice);

        log.info("發票開立完成, invoiceId: {}, invoiceNumber: {}, status: {}",
                savedInvoice.getId(), savedInvoice.getInvoiceNumber(), savedInvoice.getStatus());

        return InvoiceResult.builder()
                .invoiceId(savedInvoice.getId())
                .invoiceNumber(savedInvoice.getInvoiceNumber())
                .status(savedInvoice.getStatus())
                .message(savedInvoice.getIssueResultMessage())
                .build();
    }

    /**
     * 作廢發票
     */
    @Transactional
    public InvoiceResult voidInvoice(VoidInvoiceRequest request) {
        log.info("作廢發票, invoiceId: {}, reason: {}", request.getInvoiceId(), request.getReason());

        Invoice invoice = invoiceRepository.findById(request.getInvoiceId())
                .orElseThrow(() -> new ResourceNotFoundException("發票不存在: " + request.getInvoiceId()));

        // 驗證發票狀態
        if (!"ISSUED".equals(invoice.getStatus())) {
            throw new IllegalArgumentException("只有已開立的發票才能作廢");
        }

        // 檢查是否為當月發票 (只有當月才能作廢)
        if (!isSameMonth(invoice.getInvoiceDate(), LocalDate.now())) {
            throw new IllegalArgumentException("只能作廢當月發票，跨月請使用折讓");
        }

        // 呼叫外部發票服務作廢
        VoidInvoiceExternalRequest externalRequest = VoidInvoiceExternalRequest.builder()
                .requestId(String.valueOf(invoice.getId()))
                .invoiceNumber(invoice.getInvoiceNumber())
                .reason(request.getReason())
                .build();

        VoidInvoiceExternalResponse externalResponse = invoiceServiceClient.voidInvoice(externalRequest);

        // 更新發票狀態
        if (Boolean.TRUE.equals(externalResponse.getSuccess())) {
            invoice.setStatus("VOID");
            invoice.setIsVoided(true);
            invoice.setVoidedAt(LocalDateTime.now());
            invoice.setVoidReason(request.getReason());
        }
        invoice.setIssueResultCode(externalResponse.getResultCode());
        invoice.setIssueResultMessage(externalResponse.getResultMessage());

        invoiceRepository.save(invoice);

        log.info("發票作廢完成, invoiceId: {}, status: {}", invoice.getId(), invoice.getStatus());

        return InvoiceResult.builder()
                .invoiceId(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .status(invoice.getStatus())
                .message(externalResponse.getResultMessage())
                .build();
    }

    /**
     * 開立折讓
     */
    @Transactional
    public InvoiceAllowanceDTO createAllowance(CreateAllowanceRequest request) {
        log.info("開立折讓, invoiceId: {}, amount: {}, reason: {}",
                request.getInvoiceId(), request.getAmount(), request.getReason());

        Invoice invoice = invoiceRepository.findById(request.getInvoiceId())
                .orElseThrow(() -> new ResourceNotFoundException("發票不存在: " + request.getInvoiceId()));

        // 驗證發票狀態
        if (!"ISSUED".equals(invoice.getStatus())) {
            throw new IllegalArgumentException("只有已開立的發票才能折讓");
        }

        // 計算折讓金額 (含稅金額拆分)
        BigDecimal totalAmount = request.getAmount();
        BigDecimal taxAmount = totalAmount.multiply(TAX_RATE)
                .divide(BigDecimal.ONE.add(TAX_RATE), 0, RoundingMode.HALF_UP);
        BigDecimal salesAmount = totalAmount.subtract(taxAmount);

        // 建立折讓記錄
        InvoiceAllowance allowance = InvoiceAllowance.builder()
                .invoiceId(invoice.getId())
                .salesAmount(salesAmount)
                .taxAmount(taxAmount)
                .totalAmount(totalAmount)
                .reason(request.getReason())
                .status("ISSUED")
                .build();

        InvoiceAllowance savedAllowance = invoiceAllowanceRepository.save(allowance);

        // 呼叫外部發票服務開立折讓
        AllowanceExternalRequest externalRequest = AllowanceExternalRequest.builder()
                .requestId(String.valueOf(savedAllowance.getId()))
                .invoiceNumber(invoice.getInvoiceNumber())
                .salesAmount(salesAmount)
                .taxAmount(taxAmount)
                .totalAmount(totalAmount)
                .reason(request.getReason())
                .build();

        AllowanceExternalResponse externalResponse = invoiceServiceClient.createAllowance(externalRequest);

        // 更新折讓資訊
        if (Boolean.TRUE.equals(externalResponse.getSuccess())) {
            savedAllowance.setAllowanceNumber(externalResponse.getAllowanceNumber());
            savedAllowance.setAllowanceDate(externalResponse.getAllowanceDate());
            savedAllowance.setExternalAllowanceId(externalResponse.getExternalId());
            savedAllowance.setStatus("ISSUED");
        } else {
            savedAllowance.setStatus("FAILED");
        }
        savedAllowance.setResultCode(externalResponse.getResultCode());
        savedAllowance.setResultMessage(externalResponse.getResultMessage());

        invoiceAllowanceRepository.save(savedAllowance);

        log.info("折讓開立完成, allowanceId: {}, allowanceNumber: {}, status: {}",
                savedAllowance.getId(), savedAllowance.getAllowanceNumber(), savedAllowance.getStatus());

        return InvoiceAllowanceDTO.from(savedAllowance);
    }

    /**
     * 記錄列印
     */
    @Transactional
    public InvoiceDTO recordPrint(Long invoiceId) {
        log.info("記錄發票列印, invoiceId: {}", invoiceId);

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("發票不存在: " + invoiceId));

        invoice.setIsPrinted(true);
        invoice.setPrintCount(invoice.getPrintCount() + 1);
        invoice.setLastPrintedAt(LocalDateTime.now());

        Invoice saved = invoiceRepository.save(invoice);
        List<InvoiceItem> items = invoiceItemRepository.findByInvoiceIdOrderBySequenceAsc(invoiceId);

        return InvoiceDTO.from(saved, items);
    }

    /**
     * 查詢發票詳情
     */
    @Transactional(readOnly = true)
    public InvoiceDTO getInvoiceById(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("發票不存在: " + invoiceId));

        List<InvoiceItem> items = invoiceItemRepository.findByInvoiceIdOrderBySequenceAsc(invoiceId);
        return InvoiceDTO.from(invoice, items);
    }

    /**
     * 依訂單查詢發票
     */
    @Transactional(readOnly = true)
    public InvoiceDTO getInvoiceByOrderId(Long orderId) {
        Invoice invoice = invoiceRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到該訂單的發票: " + orderId));

        List<InvoiceItem> items = invoiceItemRepository.findByInvoiceIdOrderBySequenceAsc(invoice.getId());
        return InvoiceDTO.from(invoice, items);
    }

    /**
     * 查詢發票列表
     */
    @Transactional(readOnly = true)
    public Page<InvoiceDTO> listInvoices(LocalDate startDate, LocalDate endDate, String status, Pageable pageable) {
        Page<Invoice> invoices;

        if (status != null && startDate != null && endDate != null) {
            invoices = invoiceRepository.findByInvoiceDateBetweenAndStatus(startDate, endDate, status, pageable);
        } else if (startDate != null && endDate != null) {
            invoices = invoiceRepository.findByInvoiceDateBetween(startDate, endDate, pageable);
        } else if (status != null) {
            invoices = invoiceRepository.findByStatus(status, pageable);
        } else {
            invoices = invoiceRepository.findAll(pageable);
        }

        return invoices.map(invoice -> {
            List<InvoiceItem> items = invoiceItemRepository.findByInvoiceIdOrderBySequenceAsc(invoice.getId());
            return InvoiceDTO.from(invoice, items);
        });
    }

    /**
     * 查詢發票的折讓記錄
     */
    @Transactional(readOnly = true)
    public List<InvoiceAllowanceDTO> getAllowancesByInvoiceId(Long invoiceId) {
        return invoiceAllowanceRepository.findByInvoiceIdOrderByIdDesc(invoiceId)
                .stream()
                .map(InvoiceAllowanceDTO::from)
                .toList();
    }

    // === POS 專用方法 (依訂單 ID 操作) ===

    /**
     * 依訂單作廢發票 (POS 用)
     */
    @Transactional
    public InvoiceResult voidInvoiceByOrderId(Long orderId, String reason) {
        log.info("依訂單作廢發票, orderId: {}, reason: {}", orderId, reason);

        Invoice invoice = invoiceRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到該訂單的發票: " + orderId));

        VoidInvoiceRequest request = VoidInvoiceRequest.builder()
                .invoiceId(invoice.getId())
                .reason(reason)
                .build();

        return voidInvoice(request);
    }

    /**
     * 依訂單開立折讓 (POS 用)
     */
    @Transactional
    public InvoiceAllowanceDTO createAllowanceByOrderId(Long orderId, BigDecimal amount, String reason) {
        log.info("依訂單開立折讓, orderId: {}, amount: {}, reason: {}", orderId, amount, reason);

        Invoice invoice = invoiceRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到該訂單的發票: " + orderId));

        CreateAllowanceRequest request = CreateAllowanceRequest.builder()
                .invoiceId(invoice.getId())
                .amount(amount)
                .reason(reason)
                .build();

        return createAllowance(request);
    }

    /**
     * 依訂單記錄列印 (POS 用)
     */
    @Transactional
    public InvoiceDTO recordPrintByOrderId(Long orderId) {
        log.info("依訂單記錄發票列印, orderId: {}", orderId);

        Invoice invoice = invoiceRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("找不到該訂單的發票: " + orderId));

        return recordPrint(invoice.getId());
    }

    // === 私有方法 ===

    private List<InvoiceItem> createInvoiceItems(Long invoiceId, List<OrderItem> orderItems) {
        List<InvoiceItem> items = new ArrayList<>();
        int sequence = 1;

        for (OrderItem orderItem : orderItems) {
            if (orderItem instanceof SingleOrderItem single) {
                items.add(InvoiceItem.builder()
                        .invoiceId(invoiceId)
                        .sequence(sequence++)
                        .description(single.getProductName())
                        .quantity(BigDecimal.valueOf(single.getQuantity()))
                        .unitPrice(single.getUnitPrice())
                        .amount(single.getSubtotal())
                        .note(single.getOptions())
                        .build());
            } else if (orderItem instanceof ComboOrderItem combo) {
                items.add(InvoiceItem.builder()
                        .invoiceId(invoiceId)
                        .sequence(sequence++)
                        .description(combo.getComboName())
                        .quantity(BigDecimal.ONE)
                        .unitPrice(combo.getComboPrice())
                        .amount(combo.getComboPrice())
                        .build());
            }
            // ComboItemOrderItem 不單獨列入發票明細，因為已包含在套餐價格中
        }

        return items;
    }

    private IssueInvoiceExternalRequest buildExternalRequest(Invoice invoice, List<InvoiceItem> items) {
        return IssueInvoiceExternalRequest.builder()
                .requestId(String.valueOf(invoice.getId()))
                .invoiceType(invoice.getInvoiceType())
                .issueType(invoice.getIssueType())
                .buyer(IssueInvoiceExternalRequest.Buyer.builder()
                        .identifier(invoice.getBuyerIdentifier())
                        .name(invoice.getBuyerName())
                        .build())
                .carrier(IssueInvoiceExternalRequest.Carrier.builder()
                        .type(invoice.getCarrierType())
                        .value(invoice.getCarrierValue())
                        .build())
                .donation(IssueInvoiceExternalRequest.Donation.builder()
                        .enabled(invoice.getIsDonated())
                        .code(invoice.getDonateCode())
                        .build())
                .amounts(IssueInvoiceExternalRequest.Amounts.builder()
                        .salesAmount(invoice.getSalesAmount())
                        .taxAmount(invoice.getTaxAmount())
                        .totalAmount(invoice.getTotalAmount())
                        .build())
                .items(items.stream()
                        .map(item -> IssueInvoiceExternalRequest.Item.builder()
                                .description(item.getDescription())
                                .quantity(item.getQuantity())
                                .unitPrice(item.getUnitPrice())
                                .amount(item.getAmount())
                                .build())
                        .toList())
                .build();
    }

    private boolean isSameMonth(LocalDate date1, LocalDate date2) {
        if (date1 == null || date2 == null) {
            return false;
        }
        return date1.getYear() == date2.getYear() && date1.getMonth() == date2.getMonth();
    }
}
