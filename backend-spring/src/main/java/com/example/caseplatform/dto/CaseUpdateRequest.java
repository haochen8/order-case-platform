package com.example.caseplatform.dto;

import com.example.caseplatform.domain.enums.CaseStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public class CaseUpdateRequest {

    @NotNull
    @PositiveOrZero
    private Long version;

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    @Pattern(regexp = "(?s).*\\S.*", message = "must not be blank")
    @Size(max = 255)
    private String title;

    @Size(max = 4000)
    private String description;

    private CaseStatus status;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public CaseStatus getStatus() {
        return status;
    }

    public void setStatus(CaseStatus status) {
        this.status = status;
    }
}
