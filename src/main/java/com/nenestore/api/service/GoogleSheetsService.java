package com.nenestore.api.service;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.nenestore.api.dto.SheetItem;
import com.nenestore.api.dto.SheetOrder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GoogleSheetsService {

    private final Sheets sheetsService;

    @Value("${google.sheets.spreadsheet-id}")
    private String spreadsheetId;

    public GoogleSheetsService(Sheets sheetsService) {
        this.sheetsService = sheetsService;
    }

    // ── Orders ────────────────────────────────────────────────────────────────
    public List<SheetOrder> getOrders() throws IOException {
        ValueRange response = sheetsService.spreadsheets().values()
                .get(spreadsheetId, "orders!A1:D")
                .execute();

        List<List<Object>> rows = response.getValues();
        if (rows == null || rows.size() < 2)
            return new ArrayList<>();

        // First row = headers
        Map<String, Integer> colMap = buildColumnMap(rows.get(0));

        List<SheetOrder> orders = new ArrayList<>();
        for (int i = 1; i < rows.size(); i++) {
            List<Object> row = rows.get(i);
            if (row.isEmpty())
                continue;
            orders.add(new SheetOrder(
                    str(row, colMap, "order_number"),
                    str(row, colMap, "order_date"),
                    toDouble(row, colMap, "total_price"),
                    toInt(row, colMap, "total_items")));
        }
        return orders;
    }

    // ── Items ─────────────────────────────────────────────────────────────────
    public List<SheetItem> getItems() throws IOException {
        ValueRange response = sheetsService.spreadsheets().values()
                .get(spreadsheetId, "items!A1:G")
                .execute();

        List<List<Object>> rows = response.getValues();
        if (rows == null || rows.size() < 2)
            return new ArrayList<>();

        // First row = headers
        Map<String, Integer> colMap = buildColumnMap(rows.get(0));

        List<SheetItem> items = new ArrayList<>();
        for (int i = 1; i < rows.size(); i++) {
            List<Object> row = rows.get(i);
            if (row.isEmpty())
                continue;
            items.add(new SheetItem(
                    str(row, colMap, "order_id"),
                    str(row, colMap, "product"),
                    str(row, colMap, "image_url"),
                    toInt(row, colMap, "quantity"),
                    str(row, colMap, "color"),
                    str(row, colMap, "size"),
                    toDouble(row, colMap, "purchase_price_usd")));
        }
        return items;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Map<String, Integer> buildColumnMap(List<Object> headerRow) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < headerRow.size(); i++) {
            map.put(headerRow.get(i).toString().trim().toLowerCase(), i);
        }
        return map;
    }

    private String str(List<Object> row, Map<String, Integer> colMap, String colName) {
        Integer idx = colMap.get(colName);
        if (idx == null || idx >= row.size() || row.get(idx) == null)
            return "";
        return row.get(idx).toString().trim();
    }

    private Double toDouble(List<Object> row, Map<String, Integer> colMap, String colName) {
        try {
            return Double.parseDouble(str(row, colMap, colName));
        } catch (Exception e) {
            return 0.0;
        }
    }

    private Integer toInt(List<Object> row, Map<String, Integer> colMap, String colName) {
        try {
            return Integer.parseInt(str(row, colMap, colName));
        } catch (Exception e) {
            return 0;
        }
    }
}