package com.example.demo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

@RestController
public class HomeController {

    private static final List<Expense> EXPENSES = new CopyOnWriteArrayList<>();
    private static final AtomicLong NEXT_ID = new AtomicLong(1);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static volatile Account account;

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public String home() {
        return renderPage(null);
    }

    @PostMapping("/account")
    public ResponseEntity<String> saveAccount(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email) {
        if (name == null || name.isBlank() || name.trim().length() > 80) {
            return invalid("Enter a name of 1 to 80 characters.");
        }
        if (email == null || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            return invalid("Enter a valid email address.");
        }
        account = new Account(name.trim(), email.trim());
        return redirectHome();
    }

    @PostMapping("/expenses")
    public ResponseEntity<String> addExpense(
            @RequestParam(required = false) String amount,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String description) {

        if (account == null) {
            return invalid("Set up the account before adding an expense.");
        }
        if (amount == null || amount.isBlank()
                || date == null || date.isBlank()
                || category == null || category.isBlank()
                || description == null || description.isBlank()) {
            return invalid("Please complete all expense fields.");
        }
        if (category.trim().length() > 60 || description.trim().length() > 200) {
            return invalid("Category must be at most 60 characters and description at most 200.");
        }

        BigDecimal parsedAmount;
        try {
            parsedAmount = new BigDecimal(amount.trim());
        } catch (NumberFormatException exception) {
            return invalid("Enter a valid amount greater than zero.");
        }
        if (parsedAmount.compareTo(BigDecimal.ZERO) <= 0 || parsedAmount.scale() > 2) {
            return invalid("Enter a positive amount with no more than two decimal places.");
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
        return redirectHome();
    }

    @GetMapping(value = "/expenses/export.csv", produces = "text/csv")
    public ResponseEntity<String> exportCsv() {
        StringBuilder csv = new StringBuilder("Receipt ID,Date,Category,Description,Amount\r\n");
        for (Expense expense : EXPENSES) {
            csv.append(csvCell("EXP-" + expense.id())).append(',')
                    .append(csvCell(expense.date().toString())).append(',')
                    .append(csvCell(expense.category())).append(',')
                    .append(csvCell(expense.description())).append(',')
                    .append(csvCell(expense.amount().toPlainString())).append("\r\n");
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=expense-summary.csv")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(csv.toString());
    }

    @GetMapping(value = "/expenses/{id}/receipt", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> downloadReceipt(@PathVariable long id) {
        Expense expense = EXPENSES.stream().filter(item -> item.id() == id).findFirst().orElse(null);
        if (expense == null) {
            return ResponseEntity.notFound().build();
        }
        Account currentAccount = account;
        String receipt = """
                PERSONAL EXPENSE MANAGER - RECEIPT
                Receipt: EXP-%d
                Account: %s
                Email: %s
                Date: %s
                Category: %s
                Description: %s
                Amount: %s
                """.formatted(expense.id(), currentAccount == null ? "Not set" : currentAccount.name(),
                currentAccount == null ? "Not set" : currentAccount.email(), expense.date(), expense.category(),
                expense.description(), expense.amount().toPlainString());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=expense-receipt-" + id + ".txt")
                .contentType(MediaType.parseMediaType("text/plain;charset=UTF-8"))
                .body(receipt);
    }

    private ResponseEntity<String> invalid(String message) {
        return ResponseEntity.badRequest()
                .contentType(MediaType.TEXT_HTML)
                .body(renderPage(message));
    }

    private ResponseEntity<String> redirectHome() {
        return ResponseEntity.status(HttpStatus.SEE_OTHER)
                .header(HttpHeaders.LOCATION, "/")
                .build();
    }

    private String renderPage(String error) {
        Account currentAccount = account;
        StringBuilder html = new StringBuilder("""
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>Personal Expense Manager</title>
                  <style>
                    body{font-family:Arial,sans-serif;max-width:1000px;margin:36px auto;padding:0 18px;color:#222}
                    h1{color:#174ea6} form{display:grid;gap:12px;max-width:460px;padding:18px;background:#f4f7fb;border-radius:10px}
                    label{display:grid;gap:5px} input{padding:9px;border:1px solid #aaa;border-radius:5px}
                    button{padding:10px 14px;background:#1769aa;color:white;border:0;border-radius:5px;cursor:pointer}
                    .error{color:#a40000;background:#fff0f0;padding:10px;border-radius:5px}
                    .card{padding:14px;background:#f4f7fb;border-radius:8px;margin:14px 0}
                    table{border-collapse:collapse;width:100%;margin-top:14px} th,td{border:1px solid #ddd;padding:9px;text-align:left}
                    th{background:#f4f7fb} a{color:#174ea6}
                  </style>
                </head>
                <body>
                  <h1>Personal Expense Manager</h1>
                  <p>Manage a demo account and review recorded expenses.</p>
                """);
        if (error != null) {
            html.append("<p class=\"error\" role=\"alert\">")
                    .append(HtmlUtils.htmlEscape(error)).append("</p>");
        }

        html.append("<h2>Account setup</h2>");
        if (currentAccount != null) {
            html.append("<p class=\"card\"><strong>Current account:</strong> ")
                    .append(HtmlUtils.htmlEscape(currentAccount.name())).append(" (")
                    .append(HtmlUtils.htmlEscape(currentAccount.email())).append(")</p>");
        } else {
            html.append("<p>Create the single demo profile before entering expenses.</p>");
        }
        html.append("""
                  <form action="/account" method="post">
                    <label>Name<input name="name" type="text" maxlength="80" required></label>
                    <label>Email<input name="email" type="email" maxlength="254" required></label>
                    <button type="submit">Save account</button>
                  </form>
                """);

        if (currentAccount != null) {
            html.append("""
                  <h2>Add expense</h2>
                  <form action="/expenses" method="post">
                    <label>Amount<input name="amount" type="number" min="0.01" step="0.01" required></label>
                    <label>Date<input name="date" type="date" required></label>
                    <label>Category<input name="category" type="text" maxlength="60" required></label>
                    <label>Description<input name="description" type="text" maxlength="200" required></label>
                    <button type="submit">Add expense</button>
                  </form>
                  <h2>Spending summary</h2>
                """);
            appendSummary(html);
            html.append("<p><a href=\"/expenses/export.csv\">Download expense summary (CSV)</a></p>")
                    .append("<h2>Recorded expenses</h2>");
            appendExpenseTable(html);
        } else {
            html.append("<p>Expense entry, summary, and downloads are available after account setup.</p>");
        }
        return html.append("<p><small>Demo limitation: account and expense data are held in memory and reset when the app restarts.</small></p></body></html>").toString();
    }

    private void appendSummary(StringBuilder html) {
        if (EXPENSES.isEmpty()) {
            html.append("<p>No expenses yet. Add an expense to see totals.</p>");
            return;
        }
        BigDecimal total = EXPENSES.stream().map(Expense::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, BigDecimal> totalsByCategory = new LinkedHashMap<>();
        EXPENSES.stream().sorted(Comparator.comparing(Expense::category, String.CASE_INSENSITIVE_ORDER))
                .forEach(expense -> totalsByCategory.merge(expense.category(), expense.amount(), BigDecimal::add));
        html.append("<p class=\"card\"><strong>Total expenses:</strong> ").append(total.toPlainString())
                .append(" &nbsp; <strong>Transactions:</strong> ").append(EXPENSES.size()).append("</p>")
                .append("<table><thead><tr><th>Category</th><th>Total</th></tr></thead><tbody>");
        totalsByCategory.forEach((category, amount) -> html.append("<tr><td>")
                .append(HtmlUtils.htmlEscape(category)).append("</td><td>")
                .append(amount.toPlainString()).append("</td></tr>"));
        html.append("</tbody></table>");
    }

    private void appendExpenseTable(StringBuilder html) {
        if (EXPENSES.isEmpty()) {
            html.append("<p>No expenses have been recorded yet.</p>");
            return;
        }
        html.append("<table><thead><tr><th>Receipt</th><th>Amount</th><th>Date</th><th>Category</th><th>Description</th></tr></thead><tbody>");
        for (Expense expense : EXPENSES) {
            html.append("<tr><td><a href=\"/expenses/").append(expense.id())
                    .append("/receipt\">EXP-").append(expense.id()).append("</a></td><td>")
                    .append(expense.amount().toPlainString()).append("</td><td>").append(expense.date())
                    .append("</td><td>").append(HtmlUtils.htmlEscape(expense.category()))
                    .append("</td><td>").append(HtmlUtils.htmlEscape(expense.description())).append("</td></tr>");
        }
        html.append("</tbody></table>");
    }

    private String csvCell(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private record Account(String name, String email) {
    }

    private record Expense(long id, BigDecimal amount, LocalDate date, String category, String description) {
    }
}
