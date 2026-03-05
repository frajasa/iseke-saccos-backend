package tz.co.iseke.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;
import tz.co.iseke.inputs.CreateMemberInput;
import tz.co.iseke.inputs.UpdateMemberInput;
import tz.co.iseke.entity.Member;
import tz.co.iseke.enums.MemberStatus;
import tz.co.iseke.service.MemberService;
import tz.co.iseke.service.NotificationService;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class MemberController {

    private final MemberService memberService;
    private final NotificationService notificationService;

    @QueryMapping
    public Member member(@Argument UUID id) {
        log.info("Fetching member by ID: {}", id);
        return memberService.getMemberById(id);
    }

    @QueryMapping
    public Member memberByNumber(@Argument String memberNumber) {
        log.info("Fetching member by number: {}", memberNumber);
        return memberService.getMemberByNumber(memberNumber);
    }

    @QueryMapping
    public Page<Member> members(@Argument Integer page,
                               @Argument Integer size,
                               @Argument MemberStatus status) {
        log.info("Fetching members - page: {}, size: {}, status: {}", page, size, status);

        int pageNumber = page != null ? page : 0;
        int pageSize = size != null ? size : 20;
        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        if (status != null) {
            return memberService.findMembers(pageable, status);
        }
        return memberService.getAllMembers(pageable);
    }

    @QueryMapping
    public Page<Member> searchMembers(@Argument String searchTerm, 
                                     @Argument Integer page, 
                                     @Argument Integer size) {
        log.info("Searching members with term: {}", searchTerm);
        
        int pageNumber = page != null ? page : 0;
        int pageSize = size != null ? size : 20;
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        
        return memberService.searchMembers(searchTerm, pageable);
    }

    @MutationMapping
    public Member createMember(@Argument CreateMemberInput input) {
        log.info("Creating new member: {} {}", input.getFirstName(), input.getLastName());
        return memberService.createMember(input);
    }

    @MutationMapping
    public Member updateMember(@Argument UUID id, @Argument UpdateMemberInput input) {
        log.info("Updating member: {}", id);
        return memberService.updateMember(id, input);
    }

    @MutationMapping
    public Member activateMember(@Argument UUID id) {
        log.info("Activating member: {}", id);
        return memberService.activateMember(id);
    }

    @MutationMapping
    public Member deactivateMember(@Argument UUID id) {
        log.info("Deactivating member: {}", id);
        return memberService.deactivateMember(id);
    }

    @MutationMapping
    public Member registerProspect(@Argument CreateMemberInput input) {
        return memberService.registerProspect(input);
    }

    @MutationMapping
    public Member submitApplication(@Argument UUID memberId) {
        return memberService.submitApplication(memberId);
    }

    @MutationMapping
    public Member approveMembership(@Argument UUID memberId) {
        return memberService.approveMembership(memberId);
    }

    @MutationMapping
    public Member updateMemberPhoto(@Argument UUID memberId, @Argument String photoPath) {
        return memberService.updateMemberPhoto(memberId, photoPath);
    }

    @MutationMapping
    public Member updateMemberSignature(@Argument UUID memberId, @Argument String signaturePath) {
        return memberService.updateMemberSignature(memberId, signaturePath);
    }

    @MutationMapping
    public Member updateMemberFingerprint(@Argument UUID memberId, @Argument String fingerprintPath) {
        return memberService.updateMemberFingerprint(memberId, fingerprintPath);
    }

    @MutationMapping
    public Boolean sendNotification(@Argument String phoneNumber, 
                                   @Argument String email, 
                                   @Argument String message,
                                   @Argument String subject) {
        log.info("Sending notification to phone: {}, email: {}", phoneNumber, email);
        
        try {
            if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
                notificationService.sendSms(phoneNumber, message);
            }
            
            if (email != null && !email.trim().isEmpty()) {
                notificationService.sendEmail(email, subject != null ? subject : "Notification", message);
            }
            
            return true;
        } catch (Exception e) {
            log.error("Failed to send notification: {}", e.getMessage());
            return false;
        }
    }
}