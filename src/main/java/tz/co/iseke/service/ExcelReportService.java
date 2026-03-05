package tz.co.iseke.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import tz.co.iseke.dto.*;
import tz.co.iseke.entity.Member;
import tz.co.iseke.entity.Transaction;
import tz.co.iseke.repository.MemberRepository;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExcelReportService {

    private final MemberRepository memberRepository;
    private final ReportService reportService;

    public byte[] exportMemberList() {
        List<Member> members = memberRepository.findAll();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Members");
            CellStyle headerStyle = createHeaderStyle(workbook);

            String[] headers = {"Member #", "First Name", "Last Name", "Gender", "Phone", "Email",
                    "National ID", "Status", "Membership Date"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (Member m : members) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(m.getMemberNumber());
                row.createCell(1).setCellValue(m.getFirstName());
                row.createCell(2).setCellValue(m.getLastName());
                row.createCell(3).setCellValue(m.getGender() != null ? m.getGender().name() : "");
                row.createCell(4).setCellValue(m.getPhoneNumber() != null ? m.getPhoneNumber() : "");
                row.createCell(5).setCellValue(m.getEmail() != null ? m.getEmail() : "");
                row.createCell(6).setCellValue(m.getNationalId() != null ? m.getNationalId() : "");
                row.createCell(7).setCellValue(m.getStatus().name());
                row.createCell(8).setCellValue(m.getMembershipDate() != null ? m.getMembershipDate().toString() : "");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Error generating member list Excel: {}", e.getMessage());
            throw new RuntimeException("Failed to generate Excel", e);
        }
    }

    public byte[] exportTransactionReport(UUID memberId, LocalDate startDate, LocalDate endDate) {
        MemberStatementDTO statement = reportService.getMemberStatement(memberId, null, startDate, endDate);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Transactions");
            CellStyle headerStyle = createHeaderStyle(workbook);

            String[] headers = {"Date", "Txn #", "Type", "Amount", "Balance Before", "Balance After", "Description", "Status"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (Transaction t : statement.getTransactions()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(t.getTransactionDate().toString());
                row.createCell(1).setCellValue(t.getTransactionNumber());
                row.createCell(2).setCellValue(t.getTransactionType().name());
                row.createCell(3).setCellValue(t.getAmount().doubleValue());
                row.createCell(4).setCellValue(t.getBalanceBefore() != null ? t.getBalanceBefore().doubleValue() : 0);
                row.createCell(5).setCellValue(t.getBalanceAfter() != null ? t.getBalanceAfter().doubleValue() : 0);
                row.createCell(6).setCellValue(t.getDescription() != null ? t.getDescription() : "");
                row.createCell(7).setCellValue(t.getStatus().name());
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Error generating transaction report Excel: {}", e.getMessage());
            throw new RuntimeException("Failed to generate Excel", e);
        }
    }

    public byte[] exportDelinquencyReport(UUID branchId, LocalDate date) {
        DelinquencyReportDTO report = reportService.getDelinquencyReport(branchId, date);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Delinquency Report");
            CellStyle headerStyle = createHeaderStyle(workbook);

            String[] headers = {"Range", "Number of Loans", "Outstanding Amount", "Percentage %"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (DelinquencyRangeDTO range : report.getRanges()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(range.getRange());
                row.createCell(1).setCellValue(range.getNumberOfLoans());
                row.createCell(2).setCellValue(range.getOutstandingAmount().doubleValue());
                row.createCell(3).setCellValue(range.getPercentage().doubleValue());
            }

            Row totalRow = sheet.createRow(rowNum);
            totalRow.createCell(0).setCellValue("TOTAL AT RISK");
            totalRow.createCell(2).setCellValue(report.getTotalAtRisk().doubleValue());

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Error generating delinquency report Excel: {}", e.getMessage());
            throw new RuntimeException("Failed to generate Excel", e);
        }
    }

    public byte[] exportTrialBalance(LocalDate date) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Trial Balance");
            CellStyle headerStyle = createHeaderStyle(workbook);

            String[] headers = {"Account Code", "Account Name", "Debit Balance", "Credit Balance"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Note: Actual data would come from AccountingService.getTrialBalance
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Error generating trial balance Excel: {}", e.getMessage());
            throw new RuntimeException("Failed to generate Excel", e);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }
}
