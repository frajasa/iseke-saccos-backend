package tz.co.iseke.service;

import tz.co.iseke.entity.Member;
import tz.co.iseke.enums.MemberStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tz.co.iseke.inputs.CreateMemberInput;
import tz.co.iseke.inputs.UpdateMemberInput;

import java.util.UUID;

public interface MemberService {

    /**
     * Create a new member
     */
    Member createMember(CreateMemberInput input);

    /**
     * Update an existing member
     */
    Member updateMember(UUID memberId, UpdateMemberInput input);

    /**
     * Get member by ID
     */
    Member getMemberById(UUID memberId);

    /**
     * Find member by ID (for GraphQL)
     */
    Member findById(UUID id);

    /**
     * Get member by member number
     */
    Member getMemberByNumber(String memberNumber);

    /**
     * Find member by member number (for GraphQL)
     */
    Member findByMemberNumber(String memberNumber);

    /**
     * Get all members with pagination
     */
    Page<Member> getAllMembers(Pageable pageable);

    /**
     * Find members with pagination and optional status filter
     */
    Page<Member> findMembers(Pageable pageable, MemberStatus status);

    /**
     * Search members by name, phone, or email
     */
    Page<Member> searchMembers(String searchTerm, Pageable pageable);

    /**
     * Activate a member
     */
    Member activateMember(UUID memberId);

    /**
     * Deactivate a member
     */
    Member deactivateMember(UUID memberId);

    /**
     * Generate unique member number
     */
    String generateMemberNumber();

    /**
     * Check if member number exists
     */
    boolean memberNumberExists(String memberNumber);

    /**
     * Check if national ID exists
     */
    boolean nationalIdExists(String nationalId);

    Member updateMemberPhoto(UUID memberId, String photoPath);
    Member updateMemberSignature(UUID memberId, String signaturePath);
    Member updateMemberFingerprint(UUID memberId, String fingerprintPath);

    Member registerProspect(CreateMemberInput input);
    Member submitApplication(UUID memberId);
    Member approveMembership(UUID memberId);
}