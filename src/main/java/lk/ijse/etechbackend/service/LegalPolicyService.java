package lk.ijse.etechbackend.service;

import lk.ijse.etechbackend.dto.profile.LegalPolicyDTO;
import lk.ijse.etechbackend.dto.profile.PolicySectionDTO;

import java.util.List;

public interface LegalPolicyService {
    List<LegalPolicyDTO> getAllPolicies();
    LegalPolicyDTO getPolicyBySlug(String slug);
    LegalPolicyDTO createPolicy(LegalPolicyDTO request);
    LegalPolicyDTO updatePolicy(String slug, LegalPolicyDTO request);
    void deletePolicy(String slug);
    LegalPolicyDTO addSection(String slug, PolicySectionDTO sectionDTO);
    LegalPolicyDTO deleteSection(String slug, String sectionId);
}
