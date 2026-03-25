package com.ebikes.iam.dtos.requests.filters;

import com.ebikes.iam.enums.ContactSourceType;
import com.ebikes.iam.enums.ContactStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ContactFilter extends BaseFilter {
    private String branchId;
    private LocalDate createdFrom;
    private LocalDate createdTo;
    private LocalDate expiresFrom;
    private LocalDate expiresTo;
    private String organizationId;
    private String phoneNumber;
    private String sourceReference;
    private ContactSourceType sourceType;
    private ContactStatus status;
}
