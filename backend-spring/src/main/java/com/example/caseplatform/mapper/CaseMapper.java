package com.example.caseplatform.mapper;

import com.example.caseplatform.domain.Case;
import com.example.caseplatform.dto.CaseResponse;
import org.springframework.stereotype.Component;

@Component
public class CaseMapper {

    public CaseResponse toResponse(Case entity) {
        CaseResponse response = new CaseResponse();
        response.setId(entity.getId());
        response.setVersion(entity.getVersion());
        response.setTitle(entity.getTitle());
        response.setDescription(entity.getDescription());
        response.setStatus(entity.getStatus());
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }
}
