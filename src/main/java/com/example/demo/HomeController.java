package com.example.demo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
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
    public String home(@RequestParam(required = false) String saved) {
        return renderPage(null, saved);
    }

    @PostMapping("/account")
    public ResponseEntity<String> saveAccount(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email) {
        if (name == null || name.isBlank() || name.trim().length() > 80) {
            return invalid("Enter a name of 1 to 80 characters.");
        }
        if (email == null || email.length() > 254 || !EMAIL_PATTERN.matcher(email.trim()).matches()) {
            return invalid("Enter a valid email address (up to 254 characters).");
        }
        account = new Account(name.trim(), email.trim());
        return redirectHome("account");
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

        EXPENSES.add(new Expense(NEXT_ID.getAndIncrement(), parsedAmount, parsedDate,
                category.trim(), description.trim()));
        return redirectHome("expense");
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
                Amount: INR %s
                """.formatted(expense.id(), currentAccount == null ? "Not set" : currentAccount.name(),
                currentAccount == null ? "Not set" : currentAccount.email(), expense.date(), expense.category(),
                expense.description(), money(expense.amount()));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=expense-receipt-" + id + ".txt")
                .contentType(MediaType.parseMediaType("text/plain;charset=UTF-8"))
                .body(receipt);
    }

    private ResponseEntity<String> invalid(String message) {
        return ResponseEntity.badRequest().contentType(MediaType.TEXT_HTML).body(renderPage(message, null));
    }

    private ResponseEntity<String> redirectHome(String saved) {
        return ResponseEntity.status(HttpStatus.SEE_OTHER)
                .header(HttpHeaders.LOCATION, "/?saved=" + saved)
                .build();
    }

    private String renderPage(String error, String saved) {
        Account currentAccount = account;
        StringBuilder html = new StringBuilder("""
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>Spendwise | Personal Expense Manager</title>
                  <style>
                    :root{--ink:#17243a;--muted:#66758b;--line:#e6ebf2;--blue:#315efb;--mint:#28b889;--paper:#f5f7fb;--white:#fff}
                    *{box-sizing:border-box}
                    body{margin:0;background:var(--paper);color:var(--ink);font-family:Inter,"Segoe UI",Arial,sans-serif}
                    .topbar{height:76px;background:#fff;border-bottom:1px solid var(--line);display:flex;align-items:center;justify-content:space-between;padding:0 max(24px,calc((100vw - 1120px)/2))}
                    .brand{display:flex;align-items:center;gap:12px;font-weight:800;font-size:18px;letter-spacing:-.3px}
                    .brand-mark{width:38px;height:38px;display:grid;place-items:center;background:#eaf0ff;color:var(--blue);border-radius:12px;font-size:20px}
                    .brand small{display:block;color:var(--muted);font-size:11px;font-weight:600;letter-spacing:1px;text-transform:uppercase;margin-top:2px}
                    .top-user{display:flex;align-items:center;gap:10px;color:var(--muted);font-size:13px}
                    .avatar{width:34px;height:34px;border-radius:50%;display:grid;place-items:center;background:#e7f7f1;color:#13785b;font-weight:800}
                    main{max-width:1120px;margin:36px auto;padding:0 22px}
                    .hero{display:flex;justify-content:space-between;align-items:end;gap:20px;margin-bottom:24px}
                    .eyebrow{color:var(--blue);font-size:11px;font-weight:800;letter-spacing:1.4px;text-transform:uppercase}
                    h1{font-size:32px;letter-spacing:-1px;margin:8px 0}
                    h2{font-size:18px;letter-spacing:-.3px;margin:0}
                    .subtle{color:var(--muted);font-size:14px;margin:0}
                    .today{color:var(--muted);font-size:13px;padding-bottom:5px}
                    .notice{padding:12px 16px;border-radius:12px;margin:0 0 18px}
                    .notice.error{color:#a3313d;background:#fff0f0;border:1px solid #ffd5d9}
                    .notice.success{color:#12694d;background:#eaf8f2;border:1px solid #c7eddd}
                    .metrics{display:grid;grid-template-columns:repeat(4,1fr);gap:14px;margin-bottom:18px}
                    .metric,.panel{background:var(--white);border:1px solid var(--line);border-radius:16px;box-shadow:0 5px 20px rgba(28,50,83,.035)}
                    .metric{padding:18px 20px}
                    .metric-label{color:var(--muted);font-size:12px;font-weight:700}
                    .metric-value{font-size:25px;font-weight:800;letter-spacing:-.7px;margin-top:10px}
                    .metric-caption{color:#8b97a8;font-size:11px;margin-top:4px}
                    .layout{display:grid;grid-template-columns:minmax(0,1.5fr) minmax(285px,1fr);gap:16px;align-items:start}
                    .panel{padding:21px}
                    .panel-head{display:flex;justify-content:space-between;gap:12px;align-items:center;margin-bottom:17px}
                    .panel-kicker{display:block;color:var(--muted);font-size:11px;font-weight:700;margin-top:4px}
                    form{display:grid;gap:13px}
                    .form-grid{display:grid;grid-template-columns:1fr 1fr;gap:12px}
                    label{display:grid;gap:6px;color:#45546a;font-size:12px;font-weight:700}
                    input{width:100%;padding:11px 12px;border:1px solid #dce3ed;border-radius:10px;background:#fff;color:var(--ink);font:inherit;font-size:14px;outline:none}
                    input:focus{border-color:var(--blue);box-shadow:0 0 0 3px rgba(49,94,251,.12)}
                    .btn{border:0;border-radius:10px;background:var(--blue);color:#fff;padding:12px 15px;font:inherit;font-size:13px;font-weight:800;cursor:pointer}
                    .btn:hover{background:#244de0}
                    .btn-light{border:1px solid var(--line);background:#fff;color:var(--ink);padding:9px 12px;border-radius:9px;font:inherit;font-size:12px;font-weight:700;cursor:pointer}
                    .profile{display:flex;align-items:center;gap:12px;padding:14px;background:#f8faff;border:1px solid #edf1f8;border-radius:13px;margin-bottom:14px}
                    .profile .avatar{width:42px;height:42px;font-size:14px}
                    .profile-name{font-weight:800}
                    .profile-email{font-size:12px;color:var(--muted);margin-top:4px;word-break:break-word}
                    details{border-top:1px solid var(--line);padding-top:12px}
                    summary{color:var(--blue);font-size:12px;font-weight:800;cursor:pointer}
                    details form{margin-top:13px}
                    .category-list{display:grid;gap:14px}
                    .category-row{display:grid;grid-template-columns:1fr auto;gap:8px 12px;font-size:13px}
                    .category-name{font-weight:700}
                    .category-amount{font-weight:800}
                    .bar{grid-column:1/-1;height:7px;border-radius:10px;background:#edf1f7;overflow:hidden}
                    .bar span{height:100%;display:block;border-radius:10px;background:linear-gradient(90deg,var(--blue),#6f8cff)}
                    .empty{padding:18px;border:1px dashed #d8e0eb;border-radius:12px;color:var(--muted);font-size:13px;text-align:center}
                    .activity{margin-top:16px}
                    .table-wrap{overflow:auto}
                    table{border-collapse:collapse;width:100%;min-width:640px}
                    th{padding:11px 10px;text-align:left;color:#7d899a;font-size:10px;letter-spacing:.8px;text-transform:uppercase;border-bottom:1px solid var(--line)}
                    td{padding:13px 10px;border-bottom:1px solid #f0f2f6;font-size:13px}
                    .amount{font-weight:800;white-space:nowrap}
                    .receipt{color:var(--blue);text-decoration:none;font-weight:800}
                    .actions{display:flex;align-items:center;gap:8px}
                    .download{display:inline-flex;align-items:center;gap:7px;text-decoration:none;color:#2348c9;font-size:12px;font-weight:800;background:#eef2ff;padding:10px 12px;border-radius:9px}
                    .hint{color:#8793a2;font-size:11px;line-height:1.5;margin-top:14px}
                    footer{max-width:1120px;margin:20px auto 32px;padding:0 22px;color:#8793a2;font-size:11px}
                    @media(max-width:850px){.metrics{grid-template-columns:repeat(2,1fr)}.layout{grid-template-columns:1fr}}
                    @media(max-width:540px){main{margin-top:24px;padding:0 14px}.topbar{padding:0 14px}.hero{align-items:start;flex-direction:column}h1{font-size:27px}.metrics{gap:9px}.metric{padding:14px}.metric-value{font-size:21px}.form-grid{grid-template-columns:1fr}.panel{padding:17px}}
                  </style>
                </head>
                <body>
                  <header class="topbar">
                    <div class="brand"><span class="brand-mark">₹</span><span>Spendwise<small>PERSONAL FINANCE</small></span></div>
                """);
        html.append("<div class=\"top-user\">");
        if (currentAccount == null) {
            html.append("<span>Demo workspace</span><span class=\"avatar\">₹</span>");
        } else {
            html.append("<span>").append(HtmlUtils.htmlEscape(currentAccount.name()))
                    .append("</span><span class=\"avatar\">").append(HtmlUtils.htmlEscape(initials(currentAccount.name())))
                    .append("</span>");
        }
        html.append("</div></header><main>");

        if (error != null) {
            html.append("<div class=\"notice error\" role=\"alert\">").append(HtmlUtils.htmlEscape(error)).append("</div>");
        } else if ("account".equals(saved)) {
            html.append("<div class=\"notice success\" role=\"status\">Account profile saved successfully.</div>");
        } else if ("expense".equals(saved)) {
            html.append("<div class=\"notice success\" role=\"status\">Expense recorded. Your summary is up to date.</div>");
        }

        html.append("<section class=\"hero\"><div><span class=\"eyebrow\">Expense dashboard</span><h1>")
                .append(currentAccount == null ? "Set up your account" : "Your spending, at a glance")
                .append("</h1><p class=\"subtle\">")
                .append(currentAccount == null
                        ? "Create your demo profile to start tracking expenses."
                        : "Record purchases and keep your monthly spending in view.")
                .append("</p></div><div class=\"today\">Today · ").append(LocalDate.now()).append("</div></section>");

        if (currentAccount == null) {
            appendSetupPanel(html);
            html.append("<p class=\"hint\">This is a single-profile learning MVP. Account data is kept in memory and resets when the application restarts.</p>");
        } else {
            appendMetrics(html);
            html.append("<section class=\"layout\">");
            appendExpensePanel(html);
            appendProfileAndCategoryPanels(html, currentAccount);
            html.append("</section>");
            html.append("<section class=\"panel activity\"><div class=\"panel-head\"><div><h2>Recent expenses</h2><span class=\"panel-kicker\">Your latest recorded transactions</span></div>")
                    .append("<a class=\"download\" href=\"/expenses/export.csv\">↓ &nbsp; Export CSV</a></div>");
            appendExpenseTable(html);
            html.append("</section><p class=\"hint\">Demo limitation: profile and expenses are held in memory. They reset when the app restarts; authentication and database storage are planned for later work.</p>");
        }
        return html.append("</main><footer>Spendwise · Personal Expense Management System · MVP demo</footer></body></html>").toString();
    }

    private void appendSetupPanel(StringBuilder html) {
        html.append("""
                <section class="panel" style="max-width:620px">
                  <div class="panel-head"><div><h2>Create your profile</h2><span class="panel-kicker">Your details appear on downloadable receipts</span></div></div>
                  <form action="/account" method="post">
                    <div class="form-grid">
                      <label>Full name<input name="name" type="text" maxlength="80" autocomplete="name" placeholder="e.g. Kavya Telavane" required></label>
                      <label>Email address<input name="email" type="email" maxlength="254" autocomplete="email" placeholder="you@example.com" required></label>
                    </div>
                    <button class="btn" type="submit">Create demo account</button>
                  </form>
                </section>
                """);
    }

    private void appendMetrics(StringBuilder html) {
        BigDecimal total = EXPENSES.stream().map(Expense::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        YearMonth currentMonth = YearMonth.now();
        BigDecimal monthTotal = EXPENSES.stream()
                .filter(expense -> YearMonth.from(expense.date()).equals(currentMonth))
                .map(Expense::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        long categoryCount = EXPENSES.stream().map(Expense::category).distinct().count();
        html.append("<section class=\"metrics\">");
        appendMetric(html, "TOTAL SPENDING", money(total), "Across all recorded expenses");
        appendMetric(html, "THIS MONTH", money(monthTotal), currentMonth.toString());
        appendMetric(html, "TRANSACTIONS", Long.toString(EXPENSES.size()), "Recorded expense entries");
        appendMetric(html, "CATEGORIES", Long.toString(categoryCount), "Distinct spending groups");
        html.append("</section>");
    }

    private void appendMetric(StringBuilder html, String label, String value, String caption) {
        html.append("<article class=\"metric\"><div class=\"metric-label\">").append(label)
                .append("</div><div class=\"metric-value\">").append(HtmlUtils.htmlEscape(value))
                .append("</div><div class=\"metric-caption\">").append(HtmlUtils.htmlEscape(caption))
                .append("</div></article>");
    }

    private void appendExpensePanel(StringBuilder html) {
        html.append("""
                <section class="panel">
                  <div class="panel-head"><div><h2>Add an expense</h2><span class="panel-kicker">Fields marked below are required</span></div></div>
                  <form action="/expenses" method="post">
                    <div class="form-grid">
                      <label>Amount (INR)<input name="amount" type="number" min="0.01" step="0.01" placeholder="0.00" inputmode="decimal" required></label>
                      <label>Date<input name="date" type="date" value="%DATE%" required></label>
                    </div>
                    <div class="form-grid">
                      <label>Category<input name="category" type="text" maxlength="60" list="categories" placeholder="Food, travel, bills..." required>
                        <datalist id="categories"><option>Food</option><option>Transport</option><option>Housing</option><option>Bills</option><option>Health</option><option>Shopping</option><option>Other</option></datalist>
                      </label>
                      <label>Description<input name="description" type="text" maxlength="200" placeholder="What was this expense for?" required></label>
                    </div>
                    <button class="btn" type="submit">+ &nbsp; Record expense</button>
                  </form>
                </section>
                """.replace("%DATE%", LocalDate.now().toString()));
    }

    private void appendProfileAndCategoryPanels(StringBuilder html, Account currentAccount) {
        html.append("<section class=\"panel\"><div class=\"panel-head\"><div><h2>Account profile</h2><span class=\"panel-kicker\">Manage your demo account details</span></div></div>")
                .append("<div class=\"profile\"><span class=\"avatar\">").append(HtmlUtils.htmlEscape(initials(currentAccount.name())))
                .append("</span><div><div class=\"profile-name\">").append(HtmlUtils.htmlEscape(currentAccount.name()))
                .append("</div><div class=\"profile-email\">").append(HtmlUtils.htmlEscape(currentAccount.email()))
                .append("</div></div></div>")
                .append("<details><summary>Edit profile details</summary><form action=\"/account\" method=\"post\">")
                .append("<label>Full name<input name=\"name\" type=\"text\" maxlength=\"80\" autocomplete=\"name\" value=\"")
                .append(HtmlUtils.htmlEscape(currentAccount.name())).append("\" required></label>")
                .append("<label>Email address<input name=\"email\" type=\"email\" maxlength=\"254\" autocomplete=\"email\" value=\"")
                .append(HtmlUtils.htmlEscape(currentAccount.email())).append("\" required></label>")
                .append("<button class=\"btn-light\" type=\"submit\">Save profile changes</button></form></details>")
                .append("<p class=\"hint\">Your name and email are added to each downloadable receipt.</p></section>");

        html.append("<section class=\"panel\" style=\"margin-top:16px\"><div class=\"panel-head\"><div><h2>Spending by category</h2><span class=\"panel-kicker\">Compare where your money goes</span></div></div>");
        Map<String, BigDecimal> categoryTotals = categoryTotals();
        if (categoryTotals.isEmpty()) {
            html.append("<div class=\"empty\">Category totals will appear after your first expense.</div>");
        } else {
            BigDecimal max = categoryTotals.values().stream().max(BigDecimal::compareTo).orElse(BigDecimal.ONE);
            html.append("<div class=\"category-list\">");
            categoryTotals.forEach((category, amount) -> {
                int percent = amount.multiply(BigDecimal.valueOf(100)).divide(max, 0, java.math.RoundingMode.HALF_UP).intValue();
                html.append("<div class=\"category-row\"><span class=\"category-name\">")
                        .append(HtmlUtils.htmlEscape(category)).append("</span><span class=\"category-amount\">")
                        .append(money(amount)).append("</span><div class=\"bar\"><span style=\"width:")
                        .append(Math.max(percent, 4)).append("%\"></span></div></div>");
            });
            html.append("</div>");
        }
        html.append("</section>");
    }

    private Map<String, BigDecimal> categoryTotals() {
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        EXPENSES.stream()
                .sorted(Comparator.comparing(Expense::category, String.CASE_INSENSITIVE_ORDER))
                .forEach(expense -> totals.merge(expense.category(), expense.amount(), BigDecimal::add));
        return totals;
    }

    private void appendExpenseTable(StringBuilder html) {
        if (EXPENSES.isEmpty()) {
            html.append("<div class=\"empty\">No expenses recorded yet. Add your first expense above.</div>");
            return;
        }
        html.append("<div class=\"table-wrap\"><table><thead><tr><th>Receipt</th><th>Date</th><th>Category</th><th>Description</th><th>Amount</th></tr></thead><tbody>");
        EXPENSES.stream().sorted(Comparator.comparing(Expense::date).reversed().thenComparing(Comparator.comparingLong(Expense::id).reversed()))
                .forEach(expense -> html.append("<tr><td><a class=\"receipt\" href=\"/expenses/")
                        .append(expense.id()).append("/receipt\">EXP-").append(expense.id())
                        .append("</a></td><td>").append(expense.date())
                        .append("</td><td>").append(HtmlUtils.htmlEscape(expense.category()))
                        .append("</td><td>").append(HtmlUtils.htmlEscape(expense.description()))
                        .append("</td><td class=\"amount\">").append(money(expense.amount())).append("</td></tr>"));
        html.append("</tbody></table></div>");
    }

    private String money(BigDecimal amount) {
        return "₹" + amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private String initials(String name) {
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase();
        }
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
    }

    private String csvCell(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private record Account(String name, String email) {
    }

    private record Expense(long id, BigDecimal amount, LocalDate date, String category, String description) {
    }
}

