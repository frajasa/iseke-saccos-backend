package tz.co.iseke.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.co.iseke.inputs.CreateMemberInput;
import tz.co.iseke.inputs.UpdateMemberInput;
import tz.co.iseke.entity.Branch;
import tz.co.iseke.entity.Member;
import tz.co.iseke.enums.MemberStage;
import tz.co.iseke.enums.MemberStatus;
import tz.co.iseke.repository.BranchRepository;
import tz.co.iseke.repository.MemberRepository;
import tz.co.iseke.service.AuditService;
import tz.co.iseke.service.MemberService;
import tz.co.iseke.service.NotificationService;

import java.time.LocalDate;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class MemberServiceImpl implements MemberService {

    private final MemberRepository memberRepository;
    private final BranchRepository branchRepository;
    private final NotificationService notificationService;
    private final AuditService auditService;

    @Override
    public Member createMember(CreateMemberInput input) {
        log.info("Creating new member: {} {}", input.getFirstName(), input.getLastName());

        // Validate input
        validateCreateMemberInput(input);

        // Check if member already exists
        if (StringUtils.isNotBlank(input.getNationalId()) && 
            memberRepository.existsByNationalId(input.getNationalId())) {
            throw new IllegalArgumentException("Member with National ID already exists");
        }

        if (StringUtils.isNotBlank(input.getEmail()) && 
            memberRepository.existsByEmail(input.getEmail())) {
            throw new IllegalArgumentException("Member with email already exists");
        }

        // Get branch if specified
        Branch branch = null;
        if (input.getBranchId() != null) {
            branch = branchRepository.findById(input.getBranchId())
                    .orElseThrow(() -> new IllegalArgumentException("Branch not found"));
        }

        // Generate member number
        String memberNumber = generateMemberNumber();

        // Create member entity
        Member member = Member.builder()
                .memberNumber(memberNumber)
                .firstName(input.getFirstName())
                .middleName(input.getMiddleName())
                .lastName(input.getLastName())
                .dateOfBirth(input.getDateOfBirth())
                .gender(input.getGender())
                .nationalId(input.getNationalId())
                .phoneNumber(input.getPhoneNumber())
                .email(input.getEmail())
                .address(input.getAddress())
                .occupation(input.getOccupation())
                .employer(input.getEmployer())
                .monthlyIncome(input.getMonthlyIncome())
                .maritalStatus(input.getMaritalStatus())
                .nextOfKinName(input.getNextOfKinName())
                .nextOfKinPhone(input.getNextOfKinPhone())
                .nextOfKinRelationship(input.getNextOfKinRelationship())
                .membershipDate(LocalDate.now())
                .status(MemberStatus.ACTIVE)
                .branch(branch)
                .build();

        // Save member
        Member savedMember = memberRepository.save(member);

        // Send welcome notification
        if (StringUtils.isNotBlank(savedMember.getPhoneNumber())) {
            try {
                notificationService.sendWelcomeSms(
                        savedMember.getPhoneNumber(),
                        savedMember.getFullName(),
                        savedMember.getMemberNumber()
                );
            } catch (Exception e) {
                log.warn("Failed to send welcome SMS to new member {}: {}", 
                        savedMember.getMemberNumber(), e.getMessage());
            }
        }

        // SEND NOTIFICATIONS
        // TODO: Implement sendMemberRegistrationNotifications method in NotificationService
        // notificationService.sendMemberRegistrationNotifications(savedMember);

        auditService.logAction("MEMBER_CREATED", "Member", savedMember.getId(),
                null, "Member: " + savedMember.getMemberNumber());

        log.info("Member created successfully: {}", savedMember.getMemberNumber());
        return savedMember;
    }

    @Override
    public Member updateMember(UUID memberId, UpdateMemberInput input) {
        log.info("Updating member: {}", memberId);

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        // Update fields
        if (StringUtils.isNotBlank(input.getFirstName())) {
            member.setFirstName(input.getFirstName());
        }
        if (StringUtils.isNotBlank(input.getMiddleName())) {
            member.setMiddleName(input.getMiddleName());
        }
        if (StringUtils.isNotBlank(input.getLastName())) {
            member.setLastName(input.getLastName());
        }
        if (input.getDateOfBirth() != null) {
            member.setDateOfBirth(input.getDateOfBirth());
        }
        if (input.getGender() != null) {
            member.setGender(input.getGender());
        }
        if (StringUtils.isNotBlank(input.getPhoneNumber())) {
            member.setPhoneNumber(input.getPhoneNumber());
        }
        if (StringUtils.isNotBlank(input.getEmail())) {
            member.setEmail(input.getEmail());
        }
        if (StringUtils.isNotBlank(input.getAddress())) {
            member.setAddress(input.getAddress());
        }
        if (StringUtils.isNotBlank(input.getOccupation())) {
            member.setOccupation(input.getOccupation());
        }
        if (StringUtils.isNotBlank(input.getEmployer())) {
            member.setEmployer(input.getEmployer());
        }
        if (input.getMonthlyIncome() != null) {
            member.setMonthlyIncome(input.getMonthlyIncome());
        }
        if (input.getMaritalStatus() != null) {
            member.setMaritalStatus(input.getMaritalStatus());
        }
        if (StringUtils.isNotBlank(input.getNextOfKinName())) {
            member.setNextOfKinName(input.getNextOfKinName());
        }
        if (StringUtils.isNotBlank(input.getNextOfKinPhone())) {
            member.setNextOfKinPhone(input.getNextOfKinPhone());
        }
        if (StringUtils.isNotBlank(input.getNextOfKinRelationship())) {
            member.setNextOfKinRelationship(input.getNextOfKinRelationship());
        }
        if (input.getBranchId() != null) {
            Branch branch = branchRepository.findById(input.getBranchId())
                    .orElseThrow(() -> new IllegalArgumentException("Branch not found"));
            member.setBranch(branch);
        }

        Member updatedMember = memberRepository.save(member);
        auditService.logAction("MEMBER_UPDATED", "Member", updatedMember.getId());

        log.info("Member updated successfully: {}", updatedMember.getMemberNumber());
        return updatedMember;
    }

    @Override
    @Transactional(readOnly = true)
    public Member getMemberById(UUID memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Member getMemberByNumber(String memberNumber) {
        return memberRepository.findByMemberNumber(memberNumber)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Member> getAllMembers(Pageable pageable) {
        return memberRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Member> searchMembers(String searchTerm, Pageable pageable) {
        return memberRepository.searchMembers(searchTerm, pageable);
    }

    @Override
    public Member activateMember(UUID memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        member.setStatus(MemberStatus.ACTIVE);
        Member updatedMember = memberRepository.save(member);

        log.info("Member activated: {}", updatedMember.getMemberNumber());
        return updatedMember;
    }

    @Override
    public Member deactivateMember(UUID memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        member.setStatus(MemberStatus.INACTIVE);
        Member updatedMember = memberRepository.save(member);

        log.info("Member deactivated: {}", updatedMember.getMemberNumber());
        return updatedMember;
    }

    @Override
    public String generateMemberNumber() {
        String currentYear = String.valueOf(LocalDate.now().getYear());
        String prefix = "MEM" + currentYear;
        
        long count = memberRepository.countByMemberNumberPrefix(prefix);
        String sequence = String.format("%06d", count + 1);
        
        return prefix + sequence;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean memberNumberExists(String memberNumber) {
        return memberRepository.existsByMemberNumber(memberNumber);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean nationalIdExists(String nationalId) {
        return memberRepository.existsByNationalId(nationalId);
    }

    private void validateCreateMemberInput(CreateMemberInput input) {
        if (StringUtils.isBlank(input.getFirstName())) {
            throw new IllegalArgumentException("First name is required");
        }
        if (StringUtils.isBlank(input.getLastName())) {
            throw new IllegalArgumentException("Last name is required");
        }
        if (input.getDateOfBirth() == null) {
            throw new IllegalArgumentException("Date of birth is required");
        }
        if (input.getGender() == null) {
            throw new IllegalArgumentException("Gender is required");
        }
        
        // Age validation (must be at least 18 years old)
        if (input.getDateOfBirth().isAfter(LocalDate.now().minusYears(18))) {
            throw new IllegalArgumentException("Member must be at least 18 years old");
        }
        
        // Phone number validation
        if (StringUtils.isNotBlank(input.getPhoneNumber()) && 
            memberRepository.existsByPhoneNumber(input.getPhoneNumber())) {
            throw new IllegalArgumentException("Phone number already exists");
        }
    }

    @Override
    public Member findById(UUID id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Member not found with ID: " + id));
    }

    @Override
    public Member findByMemberNumber(String memberNumber) {
        return memberRepository.findByMemberNumber(memberNumber)
                .orElseThrow(() -> new IllegalArgumentException("Member not found with number: " + memberNumber));
    }

    @Override
    public Page<Member> findMembers(Pageable pageable, MemberStatus status) {
        if (status != null) {
            return memberRepository.findByStatus(status, pageable);
        }
        return memberRepository.findAll(pageable);
    }

    @Override
    public Member updateMemberPhoto(UUID memberId, String photoPath) {
        Member member = getMemberById(memberId);
        member.setPhotoPath(photoPath);
        member.setUpdatedAt(java.time.LocalDateTime.now());
        auditService.logAction("MEMBER_PHOTO_UPDATED", "Member", memberId);
        return memberRepository.save(member);
    }

    @Override
    public Member updateMemberSignature(UUID memberId, String signaturePath) {
        Member member = getMemberById(memberId);
        member.setSignaturePath(signaturePath);
        member.setUpdatedAt(java.time.LocalDateTime.now());
        auditService.logAction("MEMBER_SIGNATURE_UPDATED", "Member", memberId);
        return memberRepository.save(member);
    }

    @Override
    public Member updateMemberFingerprint(UUID memberId, String fingerprintPath) {
        Member member = getMemberById(memberId);
        member.setFingerprintPath(fingerprintPath);
        member.setUpdatedAt(java.time.LocalDateTime.now());
        auditService.logAction("MEMBER_FINGERPRINT_UPDATED", "Member", memberId);
        return memberRepository.save(member);
    }

    @Override
    public Member registerProspect(CreateMemberInput input) {
        // Check for duplicates
        java.util.List<Member> duplicates = memberRepository.findPotentialDuplicates(
                input.getFirstName(), input.getLastName(), input.getDateOfBirth());
        if (!duplicates.isEmpty()) {
            log.warn("Potential duplicate members found for {} {}: {} matches",
                    input.getFirstName(), input.getLastName(), duplicates.size());
        }

        validateCreateMemberInput(input);

        Branch branch = null;
        if (input.getBranchId() != null) {
            branch = branchRepository.findById(input.getBranchId())
                    .orElseThrow(() -> new IllegalArgumentException("Branch not found"));
        }

        String memberNumber = generateMemberNumber();

        Member member = Member.builder()
                .memberNumber(memberNumber)
                .firstName(input.getFirstName())
                .middleName(input.getMiddleName())
                .lastName(input.getLastName())
                .dateOfBirth(input.getDateOfBirth())
                .gender(input.getGender())
                .nationalId(input.getNationalId())
                .phoneNumber(input.getPhoneNumber())
                .email(input.getEmail())
                .address(input.getAddress())
                .occupation(input.getOccupation())
                .employer(input.getEmployer())
                .monthlyIncome(input.getMonthlyIncome())
                .maritalStatus(input.getMaritalStatus())
                .nextOfKinName(input.getNextOfKinName())
                .nextOfKinPhone(input.getNextOfKinPhone())
                .nextOfKinRelationship(input.getNextOfKinRelationship())
                .membershipDate(LocalDate.now())
                .status(MemberStatus.INACTIVE)
                .stage(MemberStage.POTENTIAL)
                .branch(branch)
                .build();

        Member saved = memberRepository.save(member);
        auditService.logAction("PROSPECT_REGISTERED", "Member", saved.getId(),
                null, "Prospect: " + saved.getMemberNumber());
        return saved;
    }

    @Override
    public Member submitApplication(UUID memberId) {
        Member member = getMemberById(memberId);
        if (member.getStage() != MemberStage.POTENTIAL) {
            throw new IllegalArgumentException("Only prospects can submit applications");
        }
        member.setStage(MemberStage.APPLICANT);
        member.setUpdatedAt(java.time.LocalDateTime.now());
        Member saved = memberRepository.save(member);
        auditService.logAction("APPLICATION_SUBMITTED", "Member", saved.getId(),
                "Stage: POTENTIAL", "Stage: APPLICANT");
        return saved;
    }

    @Override
    public Member approveMembership(UUID memberId) {
        Member member = getMemberById(memberId);
        if (member.getStage() != MemberStage.APPLICANT) {
            throw new IllegalArgumentException("Only applicants can be approved");
        }
        member.setStage(MemberStage.ACTIVE);
        member.setStatus(MemberStatus.ACTIVE);
        member.setUpdatedAt(java.time.LocalDateTime.now());
        Member saved = memberRepository.save(member);
        auditService.logAction("MEMBERSHIP_APPROVED", "Member", saved.getId(),
                "Stage: APPLICANT", "Stage: ACTIVE");
        return saved;
    }
}