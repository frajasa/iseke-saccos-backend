package tz.co.iseke.dto;

import lombok.Builder;
import lombok.Data;
import tz.co.iseke.entity.Member;

import java.util.List;

@Data
@Builder
public class MemberPage {
    private List<Member> content;
    private int totalElements;
    private int totalPages;
    private int currentPage;
    private int pageSize;
}