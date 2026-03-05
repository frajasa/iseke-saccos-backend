package tz.co.iseke.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import tz.co.iseke.entity.CreditScore;
import tz.co.iseke.entity.DividendRun;
import tz.co.iseke.service.CreditScoringService;
import tz.co.iseke.service.DividendService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class CreditScoringResolver {

    private final CreditScoringService creditScoringService;
    private final DividendService dividendService;

    @QueryMapping
    public CreditScore creditScore(@Argument UUID memberId) {
        return creditScoringService.getLatestScore(memberId);
    }

    @QueryMapping
    public List<DividendRun> dividendRuns() {
        return dividendService.findAll();
    }

    @MutationMapping
    public CreditScore calculateCreditScore(@Argument UUID memberId) {
        return creditScoringService.calculateCreditScore(memberId);
    }

    @MutationMapping
    public DividendRun calculateDividends(@Argument Integer year, @Argument String method, @Argument BigDecimal rate) {
        return dividendService.calculateDividends(year, method, rate);
    }

    @MutationMapping
    public DividendRun postDividends(@Argument UUID dividendRunId) {
        return dividendService.postDividends(dividendRunId);
    }
}
