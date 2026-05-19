package com.nenestore.api.service;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.nenestore.api.dto.SheetItem;
import com.nenestore.api.dto.SheetOrder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class GoogleSheetsService {

    private final Sheets sheetsService;

    @Value("${google.sheets.spreadsheet-id}")
    private String spreadsheetId;

    public GoogleSheetsService(Sheets sheetsService) {
        this.sheetsService = sheetsService;
    }

    public List<SheetOrder> getOrders() throws IOException {
        ValueRange response = sheetsService.spreadsheets().values()
                .get(spreadsheetId, "orders!A2:D")
                .execute();

        List<SheetOrder> orders = new ArrayList<>();
        List<List<Object>> rows = response.getValues();

        if (rows == null || rows.isEmpty())
            return orders;

        for (List<Object> row : rows) {
            if (row.size() < 4)
                continue;
            orders.add(new SheetOrder(
                    str(row, 0),
                    str(row, 1),
                    toDouble(row, 2),
                    toInt(row, 3)));
        }
        return orders;
    }

    public List<SheetItem> getItems() throws IOException {
        ValueRange response = sheetsService.spreadsheets().values()
                .get(spreadsheetId, "items!A2:G")
                .execute();

        List<SheetItem> items = new ArrayList<>();
        List<List<Object>> rows = response.getValues();

        if (rows == null || rows.isEmpty())
            return items;

        for (List<Object> row : rows) {
            if (row.size() < 7)
                continue;
            items.add(new SheetItem(
                    str(row, 0),
                    str(row, 1),
                    str(row, 2),
                    toInt(row, 3),
                    str(row, 4),
                    str(row, 5),
                    toDouble(row, 6)));
        }
        return items;
    }

    private String str(List<Object> row, int i) {
        return row.get(i) != null ? row.get(i).toString().trim() : "";
    }

    private Double toDouble(List<Object> row, int i) {
        try {
            return Double.parseDouble(str(row, i));
        } catch (Exception e) {
            return 0.0;
        }
    }

    private Integer toInt(List<Object> row, int i) {
        try {
            return Integer.parseInt(str(row, i));
        } catch (Exception e) {
            return 0;
        }
    }
}