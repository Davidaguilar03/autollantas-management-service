package com.autollantas.gestion.reporting.service;

import com.autollantas.gestion.purchases.model.Purchase;
import com.autollantas.gestion.purchases.model.PurchaseDetail;
import com.autollantas.gestion.purchases.service.PurchasesService;
import com.autollantas.gestion.sales.model.Sale;
import com.autollantas.gestion.sales.model.SaleDetail;
import com.autollantas.gestion.sales.service.SalesService;
import com.autollantas.gestion.treasury.model.OccasionalIncome;
import com.autollantas.gestion.treasury.model.OperationalExpense;
import com.autollantas.gestion.treasury.service.TreasuryService;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private static final String EMPRESA = "AUTOLLANTAS A&C";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ─── PDF palette ──────────────────────────────────────────────────────────
    private static final DeviceRgb C_DARK   = new DeviceRgb(0x1A, 0x1A, 0x2E);
    private static final DeviceRgb C_YELLOW = new DeviceRgb(0xF5, 0xA6, 0x23);
    private static final DeviceRgb C_ALT    = new DeviceRgb(0xF8, 0xF8, 0xF8);
    private static final DeviceRgb C_TOTAL  = new DeviceRgb(0xEE, 0xEE, 0xEE);
    private static final DeviceRgb C_GREEN  = new DeviceRgb(0xE8, 0xF5, 0xE9);
    private static final DeviceRgb C_RED    = new DeviceRgb(0xFF, 0xEB, 0xEE);

    // ─── Excel palette ────────────────────────────────────────────────────────
    private static final byte[] XL_DARK   = {(byte) 0x1A, (byte) 0x1A, (byte) 0x2E};
    private static final byte[] XL_YELLOW = {(byte) 0xF5, (byte) 0xA6, (byte) 0x23};
    private static final byte[] XL_ALT    = {(byte) 0xF8, (byte) 0xF8, (byte) 0xF8};

    private final SalesService salesService;
    private final PurchasesService purchasesService;
    private final TreasuryService treasuryService;

    public ReportService(SalesService salesService, PurchasesService purchasesService,
                         TreasuryService treasuryService) {
        this.salesService = salesService;
        this.purchasesService = purchasesService;
        this.treasuryService = treasuryService;
    }

    private String formatCOP(Double amount) {
        double v = amount != null ? amount : 0.0;
        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("es", "CO"));
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return "$ " + nf.format(v);
    }

    // ─── Resumen Financiero PDF ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public byte[] generateResumenFinancieroPDF(LocalDate from, LocalDate to) {
        ResumenData d = buildResumen(from, to);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfFont bold   = pdfFont(StandardFonts.HELVETICA_BOLD);
            PdfFont normal = pdfFont(StandardFonts.HELVETICA);

            PdfDocument pdfDoc = new PdfDocument(new PdfWriter(baos));
            addFooterHandler(pdfDoc, normal);
            Document doc = new Document(pdfDoc);
            pdfTitle(doc, "Resumen Financiero", from, to, bold, normal);

            // 1. Ingresos
            doc.add(pdfSectionTitle("1. INGRESOS DEL PERÍODO", bold));
            Table t1 = twoColTable();
            int ri = 0;
            pdfRow(t1, "Ventas al contado",      formatCOP(d.ventasContado),        normal, ri++);
            pdfRow(t1, "Ventas crédito cobrado",  formatCOP(d.ventasCreditoCobrado), normal, ri++);
            pdfRow(t1, "Ingresos ocasionales",    formatCOP(d.ingresosOcasionales),  normal, ri);
            pdfTotal(t1, "TOTAL INGRESOS", formatCOP(d.totalIngresos()), bold);
            doc.add(t1);
            doc.add(spacer());

            // 2. Costos
            doc.add(pdfSectionTitle("2. COSTOS DEL PERÍODO", bold));
            Table t2 = twoColTable();
            ri = 0;
            for (Map.Entry<String, Double> e : d.comprasPorProveedor.entrySet()) {
                pdfRow(t2, e.getKey(), formatCOP(e.getValue()), normal, ri++);
            }
            pdfTotal(t2, "TOTAL COSTOS", formatCOP(d.totalCostos()), bold);
            doc.add(t2);
            doc.add(spacer());

            // 3. Gastos operativos
            doc.add(pdfSectionTitle("3. GASTOS OPERATIVOS DEL PERÍODO", bold));
            Table t3 = twoColTable();
            ri = 0;
            for (OperationalExpense g : d.gastos) {
                pdfRow(t3, g.getConcept() != null ? g.getConcept() : "",
                       formatCOP(g.getAmount()), normal, ri++);
            }
            pdfTotal(t3, "TOTAL GASTOS OPERATIVOS", formatCOP(d.totalGastos()), bold);
            doc.add(t3);
            doc.add(spacer());

            // 4. Resultado neto
            doc.add(pdfSectionTitle("4. RESULTADO NETO", bold));
            Table t4 = twoColTable();
            ri = 0;
            pdfRow(t4, "Ingresos",             formatCOP(d.totalIngresos()), normal, ri++);
            pdfRow(t4, "(-) Costos",            formatCOP(d.totalCostos()),   normal, ri++);
            pdfRow(t4, "(-) Gastos operativos", formatCOP(d.totalGastos()),   normal, ri);
            pdfNetResult(t4, "RESULTADO NETO", formatCOP(d.resultadoNeto()), bold, d.resultadoNeto() >= 0);
            doc.add(t4);
            doc.add(spacer());

            // 5. Cartera por cobrar
            doc.add(pdfSectionTitle("5. CARTERA POR COBRAR", bold));
            Table t5 = twoColTable();
            ri = 0;
            for (Sale s : d.cartera) {
                String cliente = s.getCustomer() != null ? s.getCustomer().getName() : "Sin cliente";
                String ref     = s.getInvoiceNumber() != null ? " (" + s.getInvoiceNumber() + ")" : "";
                pdfRow(t5, cliente + ref, formatCOP(s.getPendingBalance()), normal, ri++);
            }
            pdfTotal(t5, "TOTAL POR COBRAR", formatCOP(d.totalCartera()), bold);
            doc.add(t5);
            doc.add(spacer());

            // 6. Deudas por pagar
            doc.add(pdfSectionTitle("6. DEUDAS POR PAGAR", bold));
            Table t6 = twoColTable();
            ri = 0;
            for (Purchase p : d.deudas) {
                String prov = p.getSupplier() != null ? p.getSupplier().getName() : "Sin proveedor";
                String ref  = p.getInvoiceNumber() != null ? " (" + p.getInvoiceNumber() + ")" : "";
                pdfRow(t6, prov + ref, formatCOP(p.getPendingBalance()), normal, ri++);
            }
            pdfTotal(t6, "TOTAL POR PAGAR", formatCOP(d.totalDeudas()), bold);
            doc.add(t6);

            doc.close();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error generando PDF Resumen Financiero", e);
        }
    }

    // ─── Resumen Financiero Excel ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public byte[] generateResumenFinancieroExcel(LocalDate from, LocalDate to) {
        ResumenData d = buildResumen(from, to);
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Resumen Financiero");

            CellStyle sectionSt = xlMakeStyle(wb, XL_DARK,   true,  (short) 0, true,  null);
            CellStyle dataSt    = xlMakeStyle(wb, null,       false, (short) 0, false, BorderStyle.THIN);
            CellStyle altSt     = xlMakeStyle(wb, XL_ALT,    false, (short) 0, false, BorderStyle.THIN);
            CellStyle numSt     = xlNumericStyle(wb, null);
            CellStyle altNumSt  = xlNumericStyle(wb, XL_ALT);
            CellStyle totalSt   = xlTotalStyle(wb);

            int r = xlHeader(sheet, wb, "Resumen Financiero", from, to, 0, 2);
            int rowIdx;

            r = xlSection(sheet, sectionSt, "1. INGRESOS DEL PERÍODO", r, 2);
            rowIdx = 0;
            r = xlRow(sheet, dataSt, altSt, numSt, altNumSt, "Ventas al contado",         d.ventasContado,         r, rowIdx++);
            r = xlRow(sheet, dataSt, altSt, numSt, altNumSt, "Ventas crédito cobrado",     d.ventasCreditoCobrado,  r, rowIdx++);
            r = xlRow(sheet, dataSt, altSt, numSt, altNumSt, "Ingresos ocasionales",       d.ingresosOcasionales,   r, rowIdx);
            r = xlTotal(sheet, totalSt, "TOTAL INGRESOS", d.totalIngresos(), r);
            r++;

            r = xlSection(sheet, sectionSt, "2. COSTOS DEL PERÍODO", r, 2);
            rowIdx = 0;
            for (Map.Entry<String, Double> e : d.comprasPorProveedor.entrySet()) {
                r = xlRow(sheet, dataSt, altSt, numSt, altNumSt, e.getKey(), e.getValue(), r, rowIdx++);
            }
            r = xlTotal(sheet, totalSt, "TOTAL COSTOS", d.totalCostos(), r);
            r++;

            r = xlSection(sheet, sectionSt, "3. GASTOS OPERATIVOS", r, 2);
            rowIdx = 0;
            for (OperationalExpense g : d.gastos) {
                r = xlRow(sheet, dataSt, altSt, numSt, altNumSt,
                          g.getConcept() != null ? g.getConcept() : "", g.getAmount(), r, rowIdx++);
            }
            r = xlTotal(sheet, totalSt, "TOTAL GASTOS OPERATIVOS", d.totalGastos(), r);
            r++;

            r = xlSection(sheet, sectionSt, "4. RESULTADO NETO", r, 2);
            r = xlTotal(sheet, totalSt, "RESULTADO NETO", d.resultadoNeto(), r);
            r++;

            r = xlSection(sheet, sectionSt, "5. CARTERA POR COBRAR", r, 2);
            rowIdx = 0;
            for (Sale s : d.cartera) {
                String cliente = s.getCustomer() != null ? s.getCustomer().getName() : "Sin cliente";
                String ref     = s.getInvoiceNumber() != null ? " (" + s.getInvoiceNumber() + ")" : "";
                r = xlRow(sheet, dataSt, altSt, numSt, altNumSt, cliente + ref, s.getPendingBalance(), r, rowIdx++);
            }
            r = xlTotal(sheet, totalSt, "TOTAL POR COBRAR", d.totalCartera(), r);
            r++;

            r = xlSection(sheet, sectionSt, "6. DEUDAS POR PAGAR", r, 2);
            rowIdx = 0;
            for (Purchase p : d.deudas) {
                String prov = p.getSupplier() != null ? p.getSupplier().getName() : "Sin proveedor";
                String ref  = p.getInvoiceNumber() != null ? " (" + p.getInvoiceNumber() + ")" : "";
                r = xlRow(sheet, dataSt, altSt, numSt, altNumSt, prov + ref, p.getPendingBalance(), r, rowIdx++);
            }
            xlTotal(sheet, totalSt, "TOTAL POR PAGAR", d.totalDeudas(), r);

            autoSizeCols(sheet, 3);
            wb.write(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error generando Excel Resumen Financiero", e);
        }
    }

    // ─── Ventas por Producto PDF ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public byte[] generateVentasPorProductoPDF(LocalDate from, LocalDate to) {
        List<ProductRow> rows = buildVentasPorProducto(from, to);
        return productPdf("Ventas por Producto", from, to,
                new String[]{"Código", "Descripción", "Categoría", "Cant. Vendida", "Precio Prom.", "Total Vendido"},
                rows);
    }

    // ─── Ventas por Producto Excel ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public byte[] generateVentasPorProductoExcel(LocalDate from, LocalDate to) {
        List<ProductRow> rows = buildVentasPorProducto(from, to);
        return productExcel("Ventas por Producto", from, to,
                new String[]{"Código", "Descripción", "Categoría", "Cant. Vendida", "Precio Prom. ($)", "Total Vendido ($)"},
                rows);
    }

    // ─── Compras por Producto PDF ──────────────────────────────────────────────

    @Transactional(readOnly = true)
    public byte[] generateComprasPorProductoPDF(LocalDate from, LocalDate to) {
        List<ProductRow> rows = buildComprasPorProducto(from, to);
        return productPdf("Compras por Producto", from, to,
                new String[]{"Código", "Descripción", "Categoría", "Cant. Comprada", "Precio Prom.", "Total Comprado"},
                rows);
    }

    // ─── Compras por Producto Excel ────────────────────────────────────────────

    @Transactional(readOnly = true)
    public byte[] generateComprasPorProductoExcel(LocalDate from, LocalDate to) {
        List<ProductRow> rows = buildComprasPorProducto(from, to);
        return productExcel("Compras por Producto", from, to,
                new String[]{"Código", "Descripción", "Categoría", "Cant. Comprada", "Precio Prom. ($)", "Total Comprado ($)"},
                rows);
    }

    // ─── Data builders ────────────────────────────────────────────────────────

    private ResumenData buildResumen(LocalDate from, LocalDate to) {
        ResumenData d = new ResumenData();

        for (Sale s : salesService.findSalesByDateBetween(from, to)) {
            if ("ANULADA".equalsIgnoreCase(s.getStatus())) continue;
            double total = s.getTotal() != null ? s.getTotal() : 0.0;
            if ("Contado".equalsIgnoreCase(s.getPaymentType())) {
                d.ventasContado += total;
            } else {
                double pending = s.getPendingBalance() != null ? s.getPendingBalance() : total;
                d.ventasCreditoCobrado += Math.max(0.0, total - pending);
            }
        }

        for (OccasionalIncome i : treasuryService.findIncomesByDateBetween(from, to)) {
            d.ingresosOcasionales += i.getAmount() != null ? i.getAmount() : 0.0;
        }

        for (Purchase p : purchasesService.findPurchasesByDateBetween(from, to)) {
            if ("ANULADA".equalsIgnoreCase(p.getStatus())) continue;
            String prov = p.getSupplier() != null ? p.getSupplier().getName() : "Sin proveedor";
            double total = p.getTotal() != null ? p.getTotal() : 0.0;
            d.comprasPorProveedor.merge(prov, total, Double::sum);
        }

        d.gastos = treasuryService.findExpensesByDateBetween(from, to);

        d.cartera = salesService.findAllSales().stream()
                .filter(s -> "PENDIENTE".equalsIgnoreCase(s.getStatus()))
                .filter(s -> s.getPendingBalance() != null && s.getPendingBalance() > 0)
                .sorted((a, b) -> {
                    String na = a.getCustomer() != null && a.getCustomer().getName() != null
                            ? a.getCustomer().getName() : "";
                    String nb = b.getCustomer() != null && b.getCustomer().getName() != null
                            ? b.getCustomer().getName() : "";
                    return na.compareTo(nb);
                })
                .collect(Collectors.toList());

        d.deudas = purchasesService.findAllPurchases().stream()
                .filter(p -> "PENDIENTE".equalsIgnoreCase(p.getStatus()))
                .filter(p -> p.getPendingBalance() != null && p.getPendingBalance() > 0)
                .sorted((a, b) -> {
                    String na = a.getSupplier() != null && a.getSupplier().getName() != null
                            ? a.getSupplier().getName() : "";
                    String nb = b.getSupplier() != null && b.getSupplier().getName() != null
                            ? b.getSupplier().getName() : "";
                    return na.compareTo(nb);
                })
                .collect(Collectors.toList());

        return d;
    }

    private List<ProductRow> buildVentasPorProducto(LocalDate from, LocalDate to) {
        Map<Integer, ProductRow> map = new LinkedHashMap<>();
        for (Sale s : salesService.findSalesByDateBetween(from, to)) {
            if ("ANULADA".equalsIgnoreCase(s.getStatus())) continue;
            for (SaleDetail det : salesService.findSaleDetailsBySale(s)) {
                if (det.getProduct() == null) continue;
                Integer pid = det.getProduct().getId();
                ProductRow row = map.computeIfAbsent(pid, k -> {
                    var p = det.getProduct();
                    String cat = p.getCategory() != null ? p.getCategory().getName() : "";
                    return new ProductRow(p.getCode(), p.getDescription(), cat);
                });
                int qty      = det.getQuantity() != null ? det.getQuantity() : 0;
                double price = det.getSalePrice() != null ? det.getSalePrice() : 0.0;
                double sub   = det.getSubtotal() != null ? det.getSubtotal() : price * qty;
                row.totalQty += qty;
                row.totalAmount += sub;
                row.priceWeightedSum += price * qty;
            }
        }
        List<ProductRow> result = new ArrayList<>(map.values());
        for (ProductRow row : result)
            row.avgPrice = row.totalQty > 0 ? row.priceWeightedSum / row.totalQty : 0.0;
        result.sort((a, b) -> Integer.compare(b.totalQty, a.totalQty));
        return result;
    }

    private List<ProductRow> buildComprasPorProducto(LocalDate from, LocalDate to) {
        Map<Integer, ProductRow> map = new LinkedHashMap<>();
        for (Purchase p : purchasesService.findPurchasesByDateBetween(from, to)) {
            if ("ANULADA".equalsIgnoreCase(p.getStatus())) continue;
            for (PurchaseDetail det : purchasesService.findDetailsByPurchase(p)) {
                if (det.getProduct() == null) continue;
                Integer pid = det.getProduct().getId();
                ProductRow row = map.computeIfAbsent(pid, k -> {
                    var prod = det.getProduct();
                    String cat = prod.getCategory() != null ? prod.getCategory().getName() : "";
                    return new ProductRow(prod.getCode(), prod.getDescription(), cat);
                });
                int qty      = det.getQuantity() != null ? det.getQuantity() : 0;
                double price = det.getUnitPrice() != null ? det.getUnitPrice() : 0.0;
                double sub   = det.getSubtotal() != null ? det.getSubtotal() : price * qty;
                row.totalQty += qty;
                row.totalAmount += sub;
                row.priceWeightedSum += price * qty;
            }
        }
        List<ProductRow> result = new ArrayList<>(map.values());
        for (ProductRow row : result)
            row.avgPrice = row.totalQty > 0 ? row.priceWeightedSum / row.totalQty : 0.0;
        result.sort((a, b) -> Integer.compare(b.totalQty, a.totalQty));
        return result;
    }

    // ─── PDF shared helpers ────────────────────────────────────────────────────

    private byte[] productPdf(String title, LocalDate from, LocalDate to,
                               String[] headers, List<ProductRow> rows) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfFont bold   = pdfFont(StandardFonts.HELVETICA_BOLD);
            PdfFont normal = pdfFont(StandardFonts.HELVETICA);
            PdfDocument pdfDoc = new PdfDocument(new PdfWriter(baos));
            addFooterHandler(pdfDoc, normal);
            Document doc = new Document(pdfDoc);
            pdfTitle(doc, title, from, to, bold, normal);

            Table table = new Table(UnitValue.createPercentArray(new float[]{10, 30, 15, 10, 15, 20}))
                    .useAllAvailableWidth();
            for (String h : headers) {
                table.addHeaderCell(new Cell()
                        .add(new Paragraph(h).setFont(bold).setFontColor(ColorConstants.WHITE))
                        .setBackgroundColor(C_DARK));
            }

            double grandTotal = 0;
            int grandQty = 0;
            int rowIdx = 0;
            for (ProductRow r : rows) {
                DeviceRgb bg = (rowIdx++ % 2 == 1) ? C_ALT : null;
                table.addCell(altCell(r.code        != null ? r.code        : "", normal, bg));
                table.addCell(altCell(r.description != null ? r.description : "", normal, bg));
                table.addCell(altCell(r.category    != null ? r.category    : "", normal, bg));
                table.addCell(altCellRight(String.valueOf(r.totalQty), normal, bg));
                table.addCell(altCellRight(formatCOP(r.avgPrice),      normal, bg));
                table.addCell(altCellRight(formatCOP(r.totalAmount),   normal, bg));
                grandTotal += r.totalAmount;
                grandQty   += r.totalQty;
            }

            table.addCell(new Cell(1, 4)
                    .add(new Paragraph("TOTAL").setFont(bold))
                    .setBackgroundColor(C_TOTAL));
            table.addCell(new Cell()
                    .add(new Paragraph(String.valueOf(grandQty)).setFont(bold)
                            .setTextAlignment(TextAlignment.RIGHT))
                    .setBackgroundColor(C_TOTAL));
            table.addCell(new Cell()
                    .add(new Paragraph(formatCOP(grandTotal)).setFont(bold)
                            .setTextAlignment(TextAlignment.RIGHT))
                    .setBackgroundColor(C_TOTAL));

            doc.add(table);
            doc.close();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error generando PDF " + title, e);
        }
    }

    private void pdfTitle(Document doc, String title, LocalDate from, LocalDate to,
                           PdfFont bold, PdfFont normal) {
        Table header = new Table(UnitValue.createPercentArray(new float[]{30, 70}))
                .useAllAvailableWidth();

        Cell leftCell = new Cell().setBorder(Border.NO_BORDER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
        try {
            try (InputStream is = getClass().getResourceAsStream(
                    "/com/autollantas/gestion/images/Logo Blanco.png")) {
                if (is != null) {
                    Image logo = new Image(ImageDataFactory.create(is.readAllBytes()));
                    logo.scaleToFit(200f, 100f);
                    leftCell.add(new Paragraph().add(logo));
                }
            }
        } catch (Exception ignored) {}
        header.addCell(leftCell);

        Cell rightCell = new Cell().setBorder(Border.NO_BORDER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
        rightCell.add(new Paragraph(EMPRESA).setFont(bold).setFontSize(16)
                .setTextAlignment(TextAlignment.RIGHT).setFontColor(C_DARK));
        rightCell.add(new Paragraph(title).setFont(bold).setFontSize(13)
                .setTextAlignment(TextAlignment.RIGHT));
        rightCell.add(new Paragraph("Período: " + from.format(FMT) + " — " + to.format(FMT))
                .setFont(normal).setFontSize(10).setTextAlignment(TextAlignment.RIGHT)
                .setFontColor(ColorConstants.GRAY));
        header.addCell(rightCell);

        doc.add(header);

        SolidLine sl = new SolidLine(3f);
        sl.setColor(C_YELLOW);
        doc.add(new LineSeparator(sl).setMarginTop(4f).setMarginBottom(6f));
    }

    private Paragraph pdfSectionTitle(String text, PdfFont bold) {
        return new Paragraph(text).setFont(bold).setFontSize(11)
                .setBackgroundColor(C_DARK)
                .setFontColor(ColorConstants.WHITE)
                .setPadding(4f)
                .setMarginTop(5f).setMarginBottom(2f);
    }

    private Table twoColTable() {
        return new Table(UnitValue.createPercentArray(new float[]{70, 30})).useAllAvailableWidth();
    }

    private void pdfRow(Table t, String label, String value, PdfFont font, int rowIdx) {
        DeviceRgb bg = (rowIdx % 2 == 1) ? C_ALT : null;
        t.addCell(altCell(label, font, bg));
        t.addCell(altCellRight(value, font, bg));
    }

    private void pdfTotal(Table t, String label, String value, PdfFont bold) {
        t.addCell(new Cell().add(new Paragraph(label).setFont(bold)).setBackgroundColor(C_TOTAL));
        t.addCell(new Cell().add(new Paragraph(value).setFont(bold)
                .setTextAlignment(TextAlignment.RIGHT)).setBackgroundColor(C_TOTAL));
    }

    private void pdfNetResult(Table t, String label, String value, PdfFont bold, boolean positive) {
        DeviceRgb bg = positive ? C_GREEN : C_RED;
        t.addCell(new Cell().add(new Paragraph(label).setFont(bold)).setBackgroundColor(bg));
        t.addCell(new Cell().add(new Paragraph(value).setFont(bold)
                .setTextAlignment(TextAlignment.RIGHT)).setBackgroundColor(bg));
    }

    private Cell altCell(String text, PdfFont font, DeviceRgb bg) {
        Cell c = new Cell().add(new Paragraph(text != null ? text : "").setFont(font));
        if (bg != null) c.setBackgroundColor(bg);
        return c;
    }

    private Cell altCellRight(String text, PdfFont font, DeviceRgb bg) {
        Cell c = new Cell().add(new Paragraph(text != null ? text : "").setFont(font)
                .setTextAlignment(TextAlignment.RIGHT));
        if (bg != null) c.setBackgroundColor(bg);
        return c;
    }

    private Paragraph spacer() {
        return new Paragraph(" ");
    }

    private PdfFont pdfFont(String name) {
        try {
            return PdfFontFactory.createFont(name);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create PDF font: " + name, e);
        }
    }

    private void addFooterHandler(PdfDocument pdfDoc, PdfFont font) {
        pdfDoc.addEventHandler(PdfDocumentEvent.END_PAGE, (Event ev) -> {
            PdfDocumentEvent docEvent = (PdfDocumentEvent) ev;
            PdfPage page = docEvent.getPage();
            Rectangle ps = page.getPageSize();
            int pageNum = docEvent.getDocument().getPageNumber(page);
            String date = LocalDate.now().format(FMT);
            try {
                PdfCanvas pdfCanvas = new PdfCanvas(
                        page.newContentStreamAfter(), page.getResources(), docEvent.getDocument());
                try (Canvas canvas = new Canvas(pdfCanvas,
                        new Rectangle(36, 10, ps.getWidth() - 72, 20))) {
                    canvas.add(new Paragraph(
                            "Generado el " + date + " — Autollantas A&C  |  Pág. " + pageNum)
                            .setFont(font).setFontSize(8)
                            .setTextAlignment(TextAlignment.CENTER));
                }
                pdfCanvas.release();
            } catch (Exception ignored) {}
        });
    }

    // ─── Excel shared helpers ──────────────────────────────────────────────────

    private byte[] productExcel(String sheetName, LocalDate from, LocalDate to,
                                 String[] headers, List<ProductRow> rows) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet(sheetName);

            CellStyle colHdrSt = xlMakeStyle(wb, XL_DARK,   true,  (short) 0, true,  BorderStyle.THIN);
            CellStyle dataSt   = xlMakeStyle(wb, null,       false, (short) 0, false, BorderStyle.THIN);
            CellStyle altSt    = xlMakeStyle(wb, XL_ALT,    false, (short) 0, false, BorderStyle.THIN);
            CellStyle numSt    = xlNumericStyle(wb, null);
            CellStyle altNumSt = xlNumericStyle(wb, XL_ALT);
            CellStyle totalSt  = xlTotalStyle(wb);

            int ri = xlHeader(sheet, wb, sheetName, from, to, 0, headers.length);

            Row hRow = sheet.createRow(ri++);
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell c = hRow.createCell(i + 1);
                c.setCellValue(headers[i]);
                c.setCellStyle(colHdrSt);
            }

            double grandTotal = 0;
            int grandQty = 0;
            int rowIdx = 0;
            for (ProductRow r : rows) {
                boolean alt = (rowIdx++ % 2 == 1);
                CellStyle tSt = alt ? altSt : dataSt;
                CellStyle nSt = alt ? altNumSt : numSt;
                Row row = sheet.createRow(ri++);

                org.apache.poi.ss.usermodel.Cell c0 = row.createCell(1);
                c0.setCellValue(r.code        != null ? r.code        : "");
                c0.setCellStyle(tSt);

                org.apache.poi.ss.usermodel.Cell c1 = row.createCell(2);
                c1.setCellValue(r.description != null ? r.description : "");
                c1.setCellStyle(tSt);

                org.apache.poi.ss.usermodel.Cell c2 = row.createCell(3);
                c2.setCellValue(r.category    != null ? r.category    : "");
                c2.setCellStyle(tSt);

                org.apache.poi.ss.usermodel.Cell c3 = row.createCell(4);
                c3.setCellValue(r.totalQty);
                c3.setCellStyle(tSt);

                org.apache.poi.ss.usermodel.Cell c4 = row.createCell(5);
                c4.setCellValue(r.avgPrice);
                c4.setCellStyle(nSt);

                org.apache.poi.ss.usermodel.Cell c5 = row.createCell(6);
                c5.setCellValue(r.totalAmount);
                c5.setCellStyle(nSt);

                grandTotal += r.totalAmount;
                grandQty   += r.totalQty;
            }

            Row tRow = sheet.createRow(ri);
            for (int i = 1; i <= headers.length; i++) tRow.createCell(i).setCellStyle(totalSt);
            tRow.getCell(1).setCellValue("TOTAL");
            tRow.getCell(4).setCellValue(grandQty);
            tRow.getCell(6).setCellValue(grandTotal);

            autoSizeCols(sheet, headers.length + 1);
            wb.write(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Error generando Excel " + sheetName, e);
        }
    }

    private int xlHeader(Sheet sheet, Workbook wb, String title,
                          LocalDate from, LocalDate to, int startRow, int numCols) {
        int last = Math.max(numCols - 1, 1);
        int r = startRow;

        CellStyle s1 = xlMakeStyle(wb, XL_DARK, true, (short) 16, true, null);
        Row row0 = sheet.createRow(r);
        org.apache.poi.ss.usermodel.Cell c0 = row0.createCell(1);
        c0.setCellValue(EMPRESA);
        c0.setCellStyle(s1);
        sheet.addMergedRegion(new CellRangeAddress(r, r, 1, Math.max(last + 1, 4)));
        r++;

        CellStyle s2 = xlMakeStyle(wb, XL_DARK, true, (short) 13, true, null);
        Row row1 = sheet.createRow(r);
        org.apache.poi.ss.usermodel.Cell c1 = row1.createCell(1);
        c1.setCellValue(title);
        c1.setCellStyle(s2);
        sheet.addMergedRegion(new CellRangeAddress(r, r, 1, Math.max(last + 1, 4)));
        r++;

        CellStyle s3 = xlMakeStyle(wb, XL_YELLOW, false, (short) 0, false, null);
        Row row2 = sheet.createRow(r);
        org.apache.poi.ss.usermodel.Cell c2 = row2.createCell(1);
        c2.setCellValue("Período: " + from.format(FMT) + " — " + to.format(FMT));
        c2.setCellStyle(s3);
        sheet.addMergedRegion(new CellRangeAddress(r, r, 1, Math.max(last + 1, 4)));
        r++;

        sheet.createRow(r++);
        return r;
    }

    private int xlSection(Sheet sheet, CellStyle style, String label, int r, int numCols) {
        Row row = sheet.createRow(r);
        org.apache.poi.ss.usermodel.Cell c = row.createCell(1);
        c.setCellValue(label);
        c.setCellStyle(style);
        try { sheet.addMergedRegion(new CellRangeAddress(r, r, 1, numCols)); }
        catch (Exception ignored) {}
        return r + 1;
    }

    private int xlRow(Sheet sheet,
                       CellStyle labelSt, CellStyle altLabelSt,
                       CellStyle numSt, CellStyle altNumSt,
                       String label, Double value, int r, int rowIdx) {
        boolean alt = (rowIdx % 2 == 1);
        Row row = sheet.createRow(r++);
        org.apache.poi.ss.usermodel.Cell c0 = row.createCell(1);
        c0.setCellValue(label);
        c0.setCellStyle(alt ? altLabelSt : labelSt);
        org.apache.poi.ss.usermodel.Cell c1 = row.createCell(2);
        c1.setCellValue(value != null ? value : 0.0);
        c1.setCellStyle(alt ? altNumSt : numSt);
        return r;
    }

    private int xlTotal(Sheet sheet, CellStyle style, String label, double value, int r) {
        Row row = sheet.createRow(r++);
        org.apache.poi.ss.usermodel.Cell c0 = row.createCell(1);
        c0.setCellValue(label);
        c0.setCellStyle(style);
        org.apache.poi.ss.usermodel.Cell c1 = row.createCell(2);
        c1.setCellValue(value);
        c1.setCellStyle(style);
        return r;
    }

    private XSSFCellStyle xlMakeStyle(Workbook wb, byte[] bgRgb, boolean bold,
                                       short fontSize, boolean whiteText,
                                       BorderStyle borderStyle) {
        XSSFCellStyle s = (XSSFCellStyle) wb.createCellStyle();
        if (bgRgb != null) {
            s.setFillForegroundColor(new XSSFColor(bgRgb, null));
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        Font f = wb.createFont();
        f.setBold(bold);
        if (fontSize > 0) f.setFontHeightInPoints(fontSize);
        if (whiteText) f.setColor(IndexedColors.WHITE.getIndex());
        s.setFont(f);
        if (borderStyle != null) {
            s.setBorderTop(borderStyle);
            s.setBorderBottom(borderStyle);
            s.setBorderLeft(borderStyle);
            s.setBorderRight(borderStyle);
        }
        return s;
    }

    private CellStyle xlNumericStyle(Workbook wb, byte[] bgRgb) {
        XSSFCellStyle s = (XSSFCellStyle) wb.createCellStyle();
        if (bgRgb != null) {
            s.setFillForegroundColor(new XSSFColor(bgRgb, null));
            s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        s.setBorderTop(BorderStyle.THIN);
        s.setBorderBottom(BorderStyle.THIN);
        s.setBorderLeft(BorderStyle.THIN);
        s.setBorderRight(BorderStyle.THIN);
        s.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
        return s;
    }

    private CellStyle xlTotalStyle(Workbook wb) {
        XSSFCellStyle s = (XSSFCellStyle) wb.createCellStyle();
        s.setFillForegroundColor(new XSSFColor(XL_YELLOW, null));
        s.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font f = wb.createFont();
        f.setBold(true);
        s.setFont(f);
        s.setBorderTop(BorderStyle.MEDIUM);
        s.setBorderBottom(BorderStyle.MEDIUM);
        s.setBorderLeft(BorderStyle.MEDIUM);
        s.setBorderRight(BorderStyle.MEDIUM);
        s.setDataFormat(wb.createDataFormat().getFormat("#,##0.00"));
        return s;
    }

    private void autoSizeCols(Sheet sheet, int numCols) {
        for (int i = 0; i < numCols; i++) {
            sheet.autoSizeColumn(i);
            if (sheet.getColumnWidth(i) < 3000) sheet.setColumnWidth(i, 3000);
        }
    }

    // ─── Inner types ──────────────────────────────────────────────────────────

    private static class ResumenData {
        double ventasContado = 0;
        double ventasCreditoCobrado = 0;
        double ingresosOcasionales = 0;
        Map<String, Double> comprasPorProveedor = new LinkedHashMap<>();
        List<OperationalExpense> gastos = new ArrayList<>();
        List<Sale> cartera = new ArrayList<>();
        List<Purchase> deudas = new ArrayList<>();

        double totalIngresos() { return ventasContado + ventasCreditoCobrado + ingresosOcasionales; }

        double totalCostos() {
            return comprasPorProveedor.values().stream().mapToDouble(Double::doubleValue).sum();
        }

        double totalGastos() {
            return gastos.stream().mapToDouble(g -> g.getAmount() != null ? g.getAmount() : 0.0).sum();
        }

        double resultadoNeto() { return totalIngresos() - totalCostos() - totalGastos(); }

        double totalCartera() {
            return cartera.stream().mapToDouble(s -> s.getPendingBalance() != null ? s.getPendingBalance() : 0.0).sum();
        }

        double totalDeudas() {
            return deudas.stream().mapToDouble(p -> p.getPendingBalance() != null ? p.getPendingBalance() : 0.0).sum();
        }
    }

    private static class ProductRow {
        String code;
        String description;
        String category;
        int totalQty = 0;
        double totalAmount = 0;
        double priceWeightedSum = 0;
        double avgPrice = 0;

        ProductRow(String code, String description, String category) {
            this.code = code;
            this.description = description;
            this.category = category;
        }
    }
}
