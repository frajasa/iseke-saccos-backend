package tz.co.iseke.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import tz.co.iseke.dto.*;
import tz.co.iseke.service.EndOfDayService;
import tz.co.iseke.service.InterestAccrualService;
import tz.co.iseke.service.LoanProvisioningService;
import tz.co.iseke.service.ReportService;

import java.time.LocalDate;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class ReportResolver {

    private final ReportService reportService;
    private final EndOfDayService endOfDayService;
    private final InterestAccrualService interestAccrualService;
    private final LoanProvisioningService loanProvisioningService;

    @QueryMapping
    public PortfolioSummaryDTO portfolioSummary(@Argument UUID branchId,
                                                @Argument LocalDate startDate,
                                                @Argument LocalDate endDate) {
        return reportService.getPortfolioSummary(branchId, startDate, endDate);
    }

    @QueryMapping
    public DelinquencyReportDTO delinquencyReport(@Argument UUID branchId,
                                                  @Argument LocalDate date) {
        return reportService.getDelinquencyReport(branchId, date);
    }

    @QueryMapping
    public FinancialStatementsDTO financialStatements(@Argument LocalDate date,
                                                      @Argument UUID branchId) {
        return reportService.getFinancialStatements(date, branchId);
    }

    @QueryMapping
    public CashFlowStatementDTO cashFlowStatement(@Argument LocalDate startDate,
                                                   @Argument LocalDate endDate,
                                                   @Argument UUID branchId) {
        return reportService.getCashFlowStatement(startDate, endDate, branchId);
    }

    @QueryMapping
    public LoanProvisionReportDTO loanProvisionReport(@Argument LocalDate date,
                                                       @Argument UUID branchId) {
        return loanProvisioningService.runProvisioning(date, branchId);
    }

    @QueryMapping
    public MemberStatementDTO memberStatement(@Argument UUID memberId,
                                               @Argument UUID accountId,
                                               @Argument LocalDate startDate,
                                               @Argument LocalDate endDate) {
        return reportService.getMemberStatement(memberId, accountId, startDate, endDate);
    }

    @QueryMapping
    public DailyTransactionSummaryDTO dailyTransactionSummary(@Argument LocalDate date,
                                                               @Argument UUID branchId) {
        return reportService.getDailyTransactionSummary(date, branchId);
    }

    @MutationMapping
    public Boolean runEndOfDay() {
        return endOfDayService.runEndOfDay();
    }

    @MutationMapping
    public Boolean runInterestAccrual() {
        interestAccrualService.runDailyAccrual();
        return true;
    }

    @MutationMapping
    public Boolean runLoanProvisioning() {
        loanProvisioningService.runProvisioning(LocalDate.now(), null);
        return true;
    }
}
