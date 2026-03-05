package tz.co.iseke.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tz.co.iseke.dto.*;
import tz.co.iseke.entity.LoanRepaymentSchedule;
import tz.co.iseke.entity.Transaction;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PdfReportService {

    private final ReportService reportService;
    private final LoanAccountService loanAccountService;
    private final TransactionService transactionService;

    @Value("${saccos.name:ISEKE SACCOS}")
    private String saccosName;

    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 16, Font.BOLD, Color.DARK_GRAY);
    private static final Font HEADER_FONT = new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE);
    private static final Font BODY_FONT = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.BLACK);
    private static final Font SUBTITLE_FONT = new Font(Font.HELVETICA, 12, Font.BOLD, Color.DARK_GRAY);
    private static final Color HEADER_BG = new Color(0, 59, 115);

    public byte[] generateMemberStatement(UUID memberId, UUID accountId, LocalDate startDate, LocalDate endDate) {
        MemberStatementDTO statement = reportService.getMemberStatement(memberId, accountId, startDate, endDate);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            addHeader(document, "Member Statement");
            document.add(new Paragraph("Member: " + statement.getMember().getFullName(), SUBTITLE_FONT));
            document.add(new Paragraph("Period: " + statement.getPeriod(), BODY_FONT));
            document.add(new Paragraph("Opening Balance: TZS " + formatAmount(statement.getOpeningBalance()), BODY_FONT));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2f, 2f, 1.5f, 1.5f, 1.5f});

            addHeaderCell(table, "Date");
            addHeaderCell(table, "Description");
            addHeaderCell(table, "Type");
            addHeaderCell(table, "Amount");
            addHeaderCell(table, "Balance");

            for (Transaction txn : statement.getTransactions()) {
                addBodyCell(table, txn.getTransactionDate().toString());
                addBodyCell(table, txn.getDescription() != null ? txn.getDescription() : "");
                addBodyCell(table, txn.getTransactionType().name());
                addBodyCell(table, formatAmount(txn.getAmount()));
                addBodyCell(table, txn.getBalanceAfter() != null ? formatAmount(txn.getBalanceAfter()) : "");
            }

            document.add(table);
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Closing Balance: TZS " + formatAmount(statement.getClosingBalance()), SUBTITLE_FONT));

            document.close();
        } catch (Exception e) {
            log.error("Error generating member statement PDF: {}", e.getMessage());
            throw new RuntimeException("Failed to generate PDF", e);
        }

        return out.toByteArray();
    }

    public byte[] generateTransactionReceipt(UUID transactionId) {
        Transaction txn = transactionService.findById(transactionId);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A5);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            addHeader(document, "Transaction Receipt");
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Transaction #: " + txn.getTransactionNumber(), SUBTITLE_FONT));
            document.add(new Paragraph("Date: " + txn.getTransactionDate(), BODY_FONT));
            document.add(new Paragraph("Type: " + txn.getTransactionType().name(), BODY_FONT));
            document.add(new Paragraph("Amount: TZS " + formatAmount(txn.getAmount()), BODY_FONT));
            document.add(new Paragraph("Member: " + txn.getMember().getFullName(), BODY_FONT));
            document.add(new Paragraph("Payment Method: " + txn.getPaymentMethod().name(), BODY_FONT));
            document.add(new Paragraph("Status: " + txn.getStatus().name(), BODY_FONT));
            if (txn.getReferenceNumber() != null) {
                document.add(new Paragraph("Reference: " + txn.getReferenceNumber(), BODY_FONT));
            }
            if (txn.getDescription() != null) {
                document.add(new Paragraph("Description: " + txn.getDescription(), BODY_FONT));
            }

            document.close();
        } catch (Exception e) {
            log.error("Error generating transaction receipt PDF: {}", e.getMessage());
            throw new RuntimeException("Failed to generate PDF", e);
        }

        return out.toByteArray();
    }

    public byte[] generateRepaymentSchedule(UUID loanId) {
        List<LoanRepaymentSchedule> schedules = loanAccountService.getRepaymentSchedule(loanId);
        var loan = loanAccountService.findById(loanId);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            addHeader(document, "Loan Repayment Schedule");
            document.add(new Paragraph("Loan #: " + loan.getLoanNumber(), SUBTITLE_FONT));
            document.add(new Paragraph("Member: " + loan.getMember().getFullName(), BODY_FONT));
            document.add(new Paragraph("Principal: TZS " + formatAmount(loan.getPrincipalAmount()), BODY_FONT));
            document.add(new Paragraph("Interest Rate: " + loan.getInterestRate() + "%", BODY_FONT));
            document.add(new Paragraph("Term: " + loan.getTermMonths() + " months", BODY_FONT));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);

            addHeaderCell(table, "#");
            addHeaderCell(table, "Due Date");
            addHeaderCell(table, "Principal");
            addHeaderCell(table, "Interest");
            addHeaderCell(table, "Total Due");
            addHeaderCell(table, "Status");

            for (LoanRepaymentSchedule s : schedules) {
                addBodyCell(table, String.valueOf(s.getInstallmentNumber()));
                addBodyCell(table, s.getDueDate().toString());
                addBodyCell(table, formatAmount(s.getPrincipalDue()));
                addBodyCell(table, formatAmount(s.getInterestDue()));
                addBodyCell(table, formatAmount(s.getTotalDue()));
                addBodyCell(table, s.getStatus().name());
            }

            document.add(table);
            document.close();
        } catch (Exception e) {
            log.error("Error generating repayment schedule PDF: {}", e.getMessage());
            throw new RuntimeException("Failed to generate PDF", e);
        }

        return out.toByteArray();
    }

    public byte[] generateDelinquencyReport(LocalDate date, UUID branchId) {
        DelinquencyReportDTO report = reportService.getDelinquencyReport(branchId, date);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            addHeader(document, "Delinquency Report");
            document.add(new Paragraph("As at: " + date, SUBTITLE_FONT));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(4);
            table.setWidthPercentage(100);
            addHeaderCell(table, "Range");
            addHeaderCell(table, "Number of Loans");
            addHeaderCell(table, "Outstanding Amount");
            addHeaderCell(table, "Percentage");

            for (var range : report.getRanges()) {
                addBodyCell(table, range.getRange());
                addBodyCell(table, String.valueOf(range.getNumberOfLoans()));
                addBodyCell(table, "TZS " + formatAmount(range.getOutstandingAmount()));
                addBodyCell(table, formatAmount(range.getPercentage()) + "%");
            }
            document.add(table);

            document.add(new Paragraph(" "));
            document.add(new Paragraph("Total Outstanding: TZS " + formatAmount(report.getTotalOutstanding()), SUBTITLE_FONT));
            document.add(new Paragraph("Total At Risk: TZS " + formatAmount(report.getTotalAtRisk()), SUBTITLE_FONT));

            document.close();
        } catch (Exception e) {
            log.error("Error generating delinquency report PDF: {}", e.getMessage());
            throw new RuntimeException("Failed to generate PDF", e);
        }

        return out.toByteArray();
    }

    public byte[] generateFinancialStatements(LocalDate date, UUID branchId) {
        FinancialStatementsDTO fs = reportService.getFinancialStatements(date, branchId);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4);

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            addHeader(document, "Financial Statements");
            document.add(new Paragraph("As at: " + date, SUBTITLE_FONT));
            document.add(new Paragraph(" "));

            // Balance Sheet
            document.add(new Paragraph("Balance Sheet", SUBTITLE_FONT));
            document.add(new Paragraph("Total Assets: TZS " + formatAmount(fs.getBalanceSheet().getAssets()), BODY_FONT));
            document.add(new Paragraph("Total Liabilities: TZS " + formatAmount(fs.getBalanceSheet().getLiabilities()), BODY_FONT));
            document.add(new Paragraph("Total Equity: TZS " + formatAmount(fs.getBalanceSheet().getEquity()), BODY_FONT));
            document.add(new Paragraph(" "));

            // Income Statement
            document.add(new Paragraph("Income Statement", SUBTITLE_FONT));
            document.add(new Paragraph("Revenue: TZS " + formatAmount(fs.getIncomeStatement().getRevenue()), BODY_FONT));
            document.add(new Paragraph("Expenses: TZS " + formatAmount(fs.getIncomeStatement().getExpenses()), BODY_FONT));
            document.add(new Paragraph("Net Income: TZS " + formatAmount(fs.getIncomeStatement().getNetIncome()), SUBTITLE_FONT));

            document.close();
        } catch (Exception e) {
            log.error("Error generating financial statements PDF: {}", e.getMessage());
            throw new RuntimeException("Failed to generate PDF", e);
        }

        return out.toByteArray();
    }

    private void addHeader(Document document, String title) throws DocumentException {
        Paragraph header = new Paragraph(saccosName, TITLE_FONT);
        header.setAlignment(Element.ALIGN_CENTER);
        document.add(header);

        Paragraph subtitle = new Paragraph(title, SUBTITLE_FONT);
        subtitle.setAlignment(Element.ALIGN_CENTER);
        document.add(subtitle);

        Paragraph dateLine = new Paragraph("Generated: " + LocalDate.now().format(DateTimeFormatter.ISO_DATE), BODY_FONT);
        dateLine.setAlignment(Element.ALIGN_CENTER);
        document.add(dateLine);
        document.add(new Paragraph(" "));
    }

    private void addHeaderCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, HEADER_FONT));
        cell.setBackgroundColor(HEADER_BG);
        cell.setPadding(5);
        table.addCell(cell);
    }

    private void addBodyCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, BODY_FONT));
        cell.setPadding(4);
        table.addCell(cell);
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) return "0.00";
        return String.format("%,.2f", amount);
    }
}
