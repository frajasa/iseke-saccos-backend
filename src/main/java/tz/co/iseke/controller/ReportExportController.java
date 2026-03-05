package tz.co.iseke.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tz.co.iseke.service.ExcelReportService;
import tz.co.iseke.service.PdfReportService;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportExportController {

    private final PdfReportService pdfReportService;
    private final ExcelReportService excelReportService;

    @GetMapping("/member-statement/{memberId}")
    public ResponseEntity<byte[]> memberStatement(
            @PathVariable UUID memberId,
            @RequestParam(required = false) UUID accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "pdf") String format) {

        if ("excel".equalsIgnoreCase(format)) {
            byte[] data = excelReportService.exportTransactionReport(memberId, startDate, endDate);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=member-statement.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(data);
        }

        byte[] data = pdfReportService.generateMemberStatement(memberId, accountId, startDate, endDate);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=member-statement.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }

    @GetMapping("/transaction-receipt/{transactionId}")
    public ResponseEntity<byte[]> transactionReceipt(@PathVariable UUID transactionId) {
        byte[] data = pdfReportService.generateTransactionReceipt(transactionId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=receipt.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }

    @GetMapping("/repayment-schedule/{loanId}")
    public ResponseEntity<byte[]> repaymentSchedule(@PathVariable UUID loanId) {
        byte[] data = pdfReportService.generateRepaymentSchedule(loanId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=repayment-schedule.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }

    @GetMapping("/financial-statements")
    public ResponseEntity<byte[]> financialStatements(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) UUID branchId,
            @RequestParam(defaultValue = "pdf") String format) {

        byte[] data = pdfReportService.generateFinancialStatements(date, branchId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=financial-statements.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }

    @GetMapping("/delinquency")
    public ResponseEntity<byte[]> delinquencyReport(
            @RequestParam(required = false) UUID branchId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "pdf") String format) {

        if ("excel".equalsIgnoreCase(format)) {
            byte[] data = excelReportService.exportDelinquencyReport(branchId, date);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=delinquency-report.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(data);
        }

        byte[] data = pdfReportService.generateDelinquencyReport(date, branchId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=delinquency-report.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(data);
    }

    @GetMapping("/members")
    public ResponseEntity<byte[]> memberListExport() {
        byte[] data = excelReportService.exportMemberList();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=member-list.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }
}
