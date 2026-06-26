package com.nenestore.api.service;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.nenestore.api.dto.InventoryResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.List;

@Service
public class CatalogService {

    private final InventoryService inventoryService;

    @Value("${storage.images-path}")
    private String imagesPath;

    // Brand colors
    private static final DeviceRgb PURPLE_DARK = new DeviceRgb(0x1a, 0x0a, 0x2e);
    private static final DeviceRgb PURPLE_VIB = new DeviceRgb(0x7c, 0x3a, 0xed);
    private static final DeviceRgb SMOKE = new DeviceRgb(0xf5, 0xf5, 0xf5);
    private static final DeviceRgb GRAY = new DeviceRgb(0x3d, 0x3d, 0x3d);

    public CatalogService(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    public byte[] generateCatalog(String gender, String status,
            String size, String search) throws Exception {

        List<InventoryResponse> items = inventoryService.getInventory(search, status, gender, size);

        if (items.isEmpty()) {
            throw new com.nenestore.api.exception.BadRequestException(
                    "No items found with the given filters");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(out);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);
        document.setMargins(30, 30, 30, 30);

        // ── Header ────────────────────────────────────────────────────────────
        Paragraph header = new Paragraph("NENESTORE — Product Catalog")
                .setFontSize(20)
                .setBold()
                .setFontColor(PURPLE_DARK)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(header);

        // filter summary line
        StringBuilder filterLine = new StringBuilder("Filters: ");
        if (gender != null)
            filterLine.append("Gender=").append(gender).append("  ");
        if (status != null)
            filterLine.append("Status=").append(status).append("  ");
        if (size != null)
            filterLine.append("Size=").append(size).append("  ");
        filterLine.append("| Total items: ").append(items.size());

        document.add(new Paragraph(filterLine.toString())
                .setFontSize(9)
                .setFontColor(GRAY)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(15));

        // ── Table ─────────────────────────────────────────────────────────────
        // columns: image | SKU | product | size | color | gender | price
        float[] columnWidths = { 80f, 100f, 200f, 50f, 100f, 50f, 80f };
        Table table = new Table(UnitValue.createPointArray(columnWidths));
        table.setWidth(UnitValue.createPercentValue(100));

        // header row
        String[] headers = { "Image", "SKU", "Product", "Size", "Color", "Gender", "Price MXN" };
        for (String h : headers) {
            table.addHeaderCell(new Cell()
                    .add(new Paragraph(h).setBold().setFontSize(9).setFontColor(ColorConstants.WHITE))
                    .setBackgroundColor(PURPLE_DARK)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE)
                    .setPadding(6));
        }

        // data rows
        boolean alternate = false;
        for (InventoryResponse item : items) {
            DeviceRgb rowBg = alternate ? SMOKE : new DeviceRgb(255, 255, 255);
            alternate = !alternate;

            // image cell
            Cell imageCell = new Cell().setBackgroundColor(rowBg).setPadding(4);
            try {
                String imagePath = item.getImageUrl() != null
                        ? item.getImageUrl().replace("/images/items/", "")
                        : null;

                if (imagePath != null) {
                    File imageFile = new File(imagesPath + imagePath);
                    if (imageFile.exists()) {
                        Image img = new Image(ImageDataFactory.create(imageFile.getAbsolutePath()));
                        img.setWidth(70);
                        img.setHeight(70);
                        imageCell.add(img);
                    } else {
                        imageCell.add(new Paragraph("No image").setFontSize(7).setFontColor(GRAY));
                    }
                }
            } catch (Exception e) {
                imageCell.add(new Paragraph("No image").setFontSize(7).setFontColor(GRAY));
            }
            table.addCell(imageCell);

            // text cells
            table.addCell(textCell(item.getSku(), 8, rowBg, true));
            table.addCell(textCell(item.getProduct(), 8, rowBg, false));
            table.addCell(textCell(item.getSize(), 8, rowBg, false));
            table.addCell(textCell(item.getColor(), 8, rowBg, false));
            table.addCell(textCell(item.getGender(), 8, rowBg, false));
            table.addCell(textCell(
                    item.getSalePriceMxn() != null
                            ? "$" + item.getSalePriceMxn().toPlainString()
                            : "—",
                    8, rowBg, false));
        }

        document.add(table);

        // ── Footer ────────────────────────────────────────────────────────────
        document.add(new Paragraph("Generated by Nenestore Internal System")
                .setFontSize(7)
                .setFontColor(GRAY)
                .setTextAlignment(TextAlignment.RIGHT)
                .setMarginTop(10));

        document.close();
        return out.toByteArray();
    }

    private Cell textCell(String text, float fontSize,
            DeviceRgb bg, boolean bold) {
        Paragraph p = new Paragraph(text != null ? text : "—")
                .setFontSize(fontSize)
                .setFontColor(GRAY);
        if (bold)
            p.setBold();
        return new Cell()
                .add(p)
                .setBackgroundColor(bg)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setPadding(5);
    }
}