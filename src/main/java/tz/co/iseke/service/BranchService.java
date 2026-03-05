package tz.co.iseke.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.co.iseke.inputs.CreateBranchInput;
import tz.co.iseke.inputs.UpdateBranchInput;
import tz.co.iseke.entity.Branch;
import tz.co.iseke.enums.BranchStatus;
import tz.co.iseke.exception.BusinessException;
import tz.co.iseke.exception.ResourceNotFoundException;
import tz.co.iseke.repository.BranchRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class BranchService {

    private final BranchRepository branchRepository;

    public Branch findById(UUID id) {
        return branchRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found"));
    }

    public List<Branch> findBranches(BranchStatus status) {
        if (status != null) {
            return branchRepository.findByStatus(status);
        }
        return branchRepository.findAll();
    }

    public Branch createBranch(CreateBranchInput input) {
        // Validate branch code uniqueness
        branchRepository.findByBranchCode(input.getBranchCode()).ifPresent(b -> {
            throw new BusinessException("Branch code already exists: " + input.getBranchCode());
        });

        Branch branch = Branch.builder()
                .branchCode(input.getBranchCode())
                .branchName(input.getBranchName())
                .address(input.getAddress())
                .phoneNumber(input.getPhoneNumber())
                .email(input.getEmail())
                .managerName(input.getManagerName())
                .openingDate(input.getOpeningDate())
                .status(BranchStatus.ACTIVE)
                .build();

        return branchRepository.save(branch);
    }

    public Branch updateBranch(UUID id, UpdateBranchInput input) {
        Branch branch = findById(id);

        if (input.getBranchName() != null) branch.setBranchName(input.getBranchName());
        if (input.getAddress() != null) branch.setAddress(input.getAddress());
        if (input.getPhoneNumber() != null) branch.setPhoneNumber(input.getPhoneNumber());
        if (input.getEmail() != null) branch.setEmail(input.getEmail());
        if (input.getManagerName() != null) branch.setManagerName(input.getManagerName());
        if (input.getStatus() != null) branch.setStatus(input.getStatus());

        branch.setUpdatedAt(LocalDateTime.now());

        return branchRepository.save(branch);
    }
}