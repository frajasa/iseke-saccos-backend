package tz.co.iseke.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import tz.co.iseke.inputs.CreateBranchInput;
import tz.co.iseke.inputs.UpdateBranchInput;
import tz.co.iseke.entity.Branch;
import tz.co.iseke.enums.BranchStatus;
import tz.co.iseke.service.BranchService;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class BranchResolver {

    private final BranchService branchService;

    @QueryMapping
    public Branch branch(@Argument UUID id) {
        return branchService.findById(id);
    }

    @QueryMapping
    public List<Branch> branches(@Argument BranchStatus status) {
        return branchService.findBranches(status);
    }

    @MutationMapping
    public Branch createBranch(@Argument CreateBranchInput input) {
        return branchService.createBranch(input);
    }

    @MutationMapping
    public Branch updateBranch(@Argument UUID id, @Argument UpdateBranchInput input) {
        return branchService.updateBranch(id, input);
    }
}