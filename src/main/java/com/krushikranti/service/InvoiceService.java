package com.krushikranti.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.krushikranti.exception.ResourceNotFoundException;
import com.krushikranti.model.BulkOrder;
import com.krushikranti.model.Order;
import com.krushikranti.model.User;
import com.krushikranti.repository.BulkOrderRepository;
import com.krushikranti.repository.OrderRepository;
import com.krushikranti.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {

    private final OrderRepository orderRepository;
    private final BulkOrderRepository bulkOrderRepository;
    private final UserRepository userRepository;

    @Value("${app.invoice.storage-path:./invoices}")
    private String invoiceStoragePath;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    @Transactional
    public String generateInvoiceForOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        Path filePath = buildInvoicePath("order", order.getId(), order.getOrderNumber());
        writeOrderPdf(order, filePath);

        order.setInvoicePath(filePath.toString());
        orderRepository.save(order);
        return filePath.toString();
    }

    @Transactional
    public String generateInvoiceForBulkOrder(Long bulkOrderId) {
        BulkOrder order = bulkOrderRepository.findById(bulkOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("BulkOrder", "id", bulkOrderId));

        Path filePath = buildInvoicePath("bulk", order.getId(), "BULK-" + order.getId());
        writeBulkOrderPdf(order, filePath);

        order.setInvoicePath(filePath.toString());
        bulkOrderRepository.save(order);
        return filePath.toString();
    }

    @Transactional
    public Resource getInvoiceResourceForUser(Long orderId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order != null) {
            ensureOrderAccess(order, user);
            ensureInvoiceExistsForOrder(order);
            return loadAsResource(Path.of(order.getInvoicePath()));
        }

        BulkOrder bulkOrder = bulkOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        ensureBulkOrderAccess(bulkOrder, user);
        ensureInvoiceExistsForBulkOrder(bulkOrder);
        return loadAsResource(Path.of(bulkOrder.getInvoicePath()));
    }

    private void ensureOrderAccess(Order order, User user) {
        boolean isAdmin = user.getRole() == User.Role.ROLE_ADMIN;
        boolean isBuyer = order.getUser() != null && order.getUser().getId().equals(user.getId());
        boolean isFarmer = order.getProduct() != null
                && order.getProduct().getFarmer() != null
                && order.getProduct().getFarmer().getId().equals(user.getId());

        if (!isAdmin && !isBuyer && !isFarmer) {
            throw new AccessDeniedException("You do not have access to this invoice");
        }
    }

    private void ensureBulkOrderAccess(BulkOrder order, User user) {
        boolean isAdmin = user.getRole() == User.Role.ROLE_ADMIN;
        boolean isWholesaler = order.getWholesaler() != null && order.getWholesaler().getId().equals(user.getId());
        boolean isFarmer = order.getFarmer() != null && order.getFarmer().getId().equals(user.getId());

        if (!isAdmin && !isWholesaler && !isFarmer) {
            throw new AccessDeniedException("You do not have access to this invoice");
        }
    }

    private void ensureInvoiceExistsForOrder(Order order) {
        if (order.getInvoicePath() == null || order.getInvoicePath().isBlank()) {
            generateInvoiceForOrder(order.getId());
        }
    }

    private void ensureInvoiceExistsForBulkOrder(BulkOrder order) {
        if (order.getInvoicePath() == null || order.getInvoicePath().isBlank()) {
            generateInvoiceForBulkOrder(order.getId());
        }
    }

    private Resource loadAsResource(Path filePath) {
        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists()) {
                throw new ResourceNotFoundException("Invoice", "path", filePath.toString());
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid invoice path", e);
        }
    }

    private Path buildInvoicePath(String prefix, Long id, String reference) {
        try {
            Path dir = Paths.get(invoiceStoragePath);
            Files.createDirectories(dir);
            String fileName = String.format("%s-%d-%s.pdf", prefix, id, reference == null ? "invoice" : reference)
                    .replaceAll("[^a-zA-Z0-9._-]", "_");
            return dir.resolve(fileName);
        } catch (IOException e) {
            throw new RuntimeException("Unable to prepare invoice storage", e);
        }
    }

    private void writeOrderPdf(Order order, Path filePath) {
        try {
            PdfWriter writer = new PdfWriter(filePath.toString());
            PdfDocument pdfDocument = new PdfDocument(writer);
            Document document = new Document(pdfDocument);

            PdfFont bold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont normal = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            addHeader(document, bold);
            addOrderInfoSection(document, order, bold, normal);

            Table productTable = new Table(new float[]{4, 1, 2, 2}).useAllAvailableWidth();
            addTableHeader(productTable, "Product", "Qty", "Unit Price", "Total", bold);
            addTableRow(
                    productTable,
                    order.getProduct().getName(),
                    String.valueOf(order.getQuantity()),
                    formatCurrency(order.getProduct().getRetailPrice()),
                    formatCurrency(order.getTotalAmount()),
                    normal
            );
            document.add(productTable);

            BigDecimal subtotal = order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount();
            BigDecimal platformFee = order.getAdminCommission() == null ? BigDecimal.ZERO : order.getAdminCommission();
            BigDecimal tax = BigDecimal.ZERO;
            BigDecimal grandTotal = subtotal.add(tax);
            addSummary(document, subtotal, platformFee, tax, grandTotal, bold, normal);
            addFooter(document, normal);

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate order invoice PDF", e);
        }
    }

    private void writeBulkOrderPdf(BulkOrder order, Path filePath) {
        try {
            PdfWriter writer = new PdfWriter(filePath.toString());
            PdfDocument pdfDocument = new PdfDocument(writer);
            Document document = new Document(pdfDocument);

            PdfFont bold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont normal = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            addHeader(document, bold);

            Paragraph sectionTitle = new Paragraph("Order Information")
                    .setFont(bold)
                    .setFontSize(13)
                    .setMarginTop(16)
                    .setMarginBottom(8);
            document.add(sectionTitle);

            document.add(new Paragraph("Order ID: " + order.getId()).setFont(normal).setFontSize(10));
            document.add(new Paragraph("Order Date: " + formatDate(order.getCreatedAt())).setFont(normal).setFontSize(10));
            document.add(new Paragraph("Customer Name: " + safe(order.getWholesaler().getFirstName() + " " + order.getWholesaler().getLastName())).setFont(normal).setFontSize(10));
            document.add(new Paragraph("Customer Email: " + safe(order.getWholesaler().getEmail())).setFont(normal).setFontSize(10));

                document.add(new Paragraph("Farmer Details").setFont(bold).setFontSize(11).setMarginTop(10));
                document.add(new Paragraph("Farmer Name: " + safe(order.getFarmer().getFirstName() + " " + order.getFarmer().getLastName())).setFont(normal).setFontSize(10));
                document.add(new Paragraph("Farm Location: " + safe(order.getBulkProduct().getLocation())).setFont(normal).setFontSize(10));

            document.add(new Paragraph("Shipping Address: " + safe(order.getShippingAddress()) + ", " + safe(order.getShippingCity()) + ", " + safe(order.getShippingState()) + " - " + safe(order.getShippingPincode()))
                    .setFont(normal).setFontSize(10));

            Table productTable = new Table(new float[]{4, 1, 2, 2}).useAllAvailableWidth();
            addTableHeader(productTable, "Product", "Qty", "Unit Price", "Total", bold);
            BigDecimal unitPrice = order.getDealOffer().getPricePerUnit() == null
                    ? BigDecimal.ZERO
                    : order.getDealOffer().getPricePerUnit();
            addTableRow(
                    productTable,
                    order.getBulkProduct().getName(),
                    String.valueOf(order.getDealOffer().getQuantity()),
                    formatCurrency(unitPrice),
                    formatCurrency(order.getTotalAmount()),
                    normal
            );
            document.add(productTable);

            BigDecimal subtotal = valueOrZero(order.getTotalAmount());
            BigDecimal platformFee = valueOrZero(order.getPlatformFee());
            BigDecimal tax = BigDecimal.ZERO;
            BigDecimal grandTotal = subtotal.add(tax);
            addSummary(document, subtotal, platformFee, tax, grandTotal, bold, normal);
            addFooter(document, normal);

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate bulk order invoice PDF", e);
        }
    }

    private void addHeader(Document document, PdfFont bold) {
        Paragraph brand = new Paragraph("KrushiKranti")
                .setFont(bold)
                .setFontSize(22)
                .setFontColor(ColorConstants.GREEN)
                .setMarginBottom(2);
        document.add(brand);

        Paragraph title = new Paragraph("TAX INVOICE")
                .setFont(bold)
                .setFontSize(16)
                .setMarginBottom(12);
        document.add(title);
    }

    private void addOrderInfoSection(Document document, Order order, PdfFont bold, PdfFont normal) {
        Paragraph sectionTitle = new Paragraph("Order Information")
                .setFont(bold)
                .setFontSize(13)
                .setMarginTop(8)
                .setMarginBottom(8);
        document.add(sectionTitle);

        document.add(new Paragraph("Order ID: " + order.getId()).setFont(normal).setFontSize(10));
        document.add(new Paragraph("Order Date: " + formatDate(order.getCreatedAt())).setFont(normal).setFontSize(10));
        document.add(new Paragraph("Customer Name: " + safe(order.getUser().getFirstName() + " " + order.getUser().getLastName())).setFont(normal).setFontSize(10));
        document.add(new Paragraph("Customer Email: " + safe(order.getUser().getEmail())).setFont(normal).setFontSize(10));

        document.add(new Paragraph("Farmer Details").setFont(bold).setFontSize(11).setMarginTop(10));
        document.add(new Paragraph("Farmer Name: " + safe(order.getProduct().getFarmer().getFirstName() + " " + order.getProduct().getFarmer().getLastName())).setFont(normal).setFontSize(10));
        document.add(new Paragraph("Farm Location: " + safe(order.getProduct().getLocation())).setFont(normal).setFontSize(10));

        document.add(new Paragraph("Shipping Address: " + safe(order.getShippingAddress()) + ", " + safe(order.getShippingCity()) + ", " + safe(order.getShippingState()) + " - " + safe(order.getShippingPincode()))
                .setFont(normal).setFontSize(10));
    }

    private void addTableHeader(Table table, String c1, String c2, String c3, String c4, PdfFont bold) {
        table.addHeaderCell(buildHeaderCell(c1, bold));
        table.addHeaderCell(buildHeaderCell(c2, bold));
        table.addHeaderCell(buildHeaderCell(c3, bold));
        table.addHeaderCell(buildHeaderCell(c4, bold));
    }

    private Cell buildHeaderCell(String label, PdfFont bold) {
        return new Cell()
                .add(new Paragraph(label).setFont(bold).setFontSize(10))
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setBorder(new SolidBorder(ColorConstants.GRAY, 0.7f));
    }

    private void addTableRow(Table table, String product, String qty, String unitPrice, String total, PdfFont normal) {
        table.addCell(buildDataCell(product, normal));
        table.addCell(buildDataCell(qty, normal).setTextAlignment(TextAlignment.RIGHT));
        table.addCell(buildDataCell(unitPrice, normal).setTextAlignment(TextAlignment.RIGHT));
        table.addCell(buildDataCell(total, normal).setTextAlignment(TextAlignment.RIGHT));
    }

    private Cell buildDataCell(String value, PdfFont normal) {
        return new Cell()
                .add(new Paragraph(value).setFont(normal).setFontSize(10))
                .setBorder(new SolidBorder(ColorConstants.GRAY, 0.5f));
    }

    private void addSummary(Document document, BigDecimal subtotal, BigDecimal platformFee, BigDecimal tax,
                            BigDecimal grandTotal, PdfFont bold, PdfFont normal) {
        Table summary = new Table(new float[]{6, 2}).useAllAvailableWidth();
        summary.setMarginTop(12);

        addSummaryRow(summary, "Subtotal", formatCurrency(subtotal), normal, false);
        addSummaryRow(summary, "Platform Fee", formatCurrency(platformFee), normal, false);
        addSummaryRow(summary, "Tax", formatCurrency(tax), normal, false);
        addSummaryRow(summary, "Grand Total", formatCurrency(grandTotal), bold, true);

        document.add(summary);
    }

    private void addSummaryRow(Table summary, String label, String amount, PdfFont font, boolean emphasize) {
        Cell labelCell = new Cell().add(new Paragraph(label).setFont(font).setFontSize(10));
        Cell valueCell = new Cell()
                .add(new Paragraph(amount).setFont(font).setFontSize(10))
                .setTextAlignment(TextAlignment.RIGHT);

        if (emphasize) {
            labelCell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
            valueCell.setBackgroundColor(ColorConstants.LIGHT_GRAY);
        }

        summary.addCell(labelCell);
        summary.addCell(valueCell);
    }

    private void addFooter(Document document, PdfFont normal) {
        document.add(new Paragraph("\nThank you for shopping with KrushiKranti.")
                .setFont(normal)
                .setFontSize(10)
                .setFontColor(ColorConstants.DARK_GRAY));
        document.add(new Paragraph("Support: support@krushikranti.com")
                .setFont(normal)
                .setFontSize(9)
                .setFontColor(ColorConstants.GRAY));
    }

    private String formatCurrency(BigDecimal value) {
        return "INR " + valueOrZero(value).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String formatDate(java.time.LocalDateTime dateTime) {
        return dateTime == null ? "-" : dateTime.format(DATE_TIME_FORMATTER);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
