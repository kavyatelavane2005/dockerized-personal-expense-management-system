package com.example.demo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

@RestController
public class HomeController {

    private static final List<Expense> EXPENSES = new CopyOnWriteArrayList<>();
    private static final AtomicLong NEXT_ID = new AtomicLong(1);

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public String home() {
        return renderPage(null);
    }

    @PostMapping("/expenses")
    public ResponseEntity<String> addExpense(
            @RequestParam(required = false) String amount,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String description) {

        if (amount == null || amount.isBlank()
                || date == null || date.isBlank()
                || category == null || category.isBlank()
                || description == null || description.isBlank()) {
            return invalid("Please complete all fields.");
        }

        BigDecimal parsedAmount;
        try {
            parsedAmount = new BigDecimal(amount.trim());
        } catch (NumberFormatException exception) {
            return invalid("Enter a valid amount greater than zero.");
        }
        if (parsedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return invalid("Enter an amount greater than zero.");
        }

        LocalDate parsedDate;
        try {
            parsedDate = LocalDate.parse(date.trim());
        } catch (DateTimeParseException exception) {
            return invalid("Enter a valid date.");
        }

        EXPENSES.add(new Expense(
                NEXT_ID.getAndIncrement(),
                parsedAmount,
                parsedDate,
                category.trim(),
                description.trim()));

        return ResponseEntity.status(HttpStatus.SEE_OTHER)
                .header(HttpHeaders.LOCATION, "/")
                .build();
    }

    private ResponseEntity<String> invalid(String message) {
        return ResponseEntity.badRequest()
                .contentType(MediaType.TEXT_HTML)
                .body(renderPage(message));
    }

    private String renderPage(String error) {
        StringBuilder html = new StringBuilder("""
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>Personal Expense Manager</title>
                  <style>
                    body{font-family:Arial,sans-serif;max-width:850px;margin:40px auto;padding:0 18px;color:#222}
                    h1{color:#174ea6} form{display:grid;gap:12px;max-width:460px;padding:20px;background:#f4f7fb;border-radius:10px}
                    label{display:grid;gap:5px} input{padding:9px;border:1px solid #aaa;border-radius:5px}
                    button{padding:10px 14px;background:#1769aa;color:white;border:0;border-radius:5px;cursor:pointer}
                    .error{color:#a40000;background:#fff0f0;padding:10px;border-radius:5px}
                    table{border-collapse:collapse;width:100%;margin-top:18px} th,td{border:1px solid #ddd;padding:9px;text-align:left}
                    th{background:#f4f7fb}
                  </style>
                </head>
                <body>
                  <h1>Personal Expense Manager</h1>
                  <p>Record an expense using the form below.</p>
                """);
        if (error != null) {
            html.append("<p class=\"error\" role=\"alert\">")
                    .append(HtmlUtils.htmlEscape(error)).append("</p>");
        }
        html.append("""
                  <form action="/expenses" method="post">
                    <label>Amount<input name="amount" type="number" min="0.01" step="0.01" required></label>
                    <label>Date<input name="date" type="date" required></label>
                    <label>Category<input name="category" type="text" maxlength="60" required></label>
                    <label>Description<input name="description" type="text" maxlength="200" required></label>
                    <button type="submit">Add expense</button>
                  </form>
                  <h2>Recorded expenses</h2>
                """);
        if (EXPENSES.isEmpty()) {
            html.append("<p>No expenses have been recorded yet.</p>");
        } else {
            html.append("<table><thead><tr><th>Amount</th><th>Date</th><th>Category</th><th>Description</th></tr></thead><tbody>");
            for (Expense expense : EXPENSES) {
                html.append("<tr><td>").append(expense.amount().toPlainString())
                        .append("</td><td>").append(expense.date())
                        .append("</td><td>").append(HtmlUtils.htmlEscape(expense.category()))
                        .append("</td><td>").append(HtmlUtils.htmlEscape(expense.description()))
                        .append("</td></tr>");
            }
            html.append("</tbody></table>");
        }
        return html.append("</body></html>").toString();
    }

    private record Expense(long id, BigDecimal amount, LocalDate date, String category, String description) {
    }
}
