package lk.ijse.etechbackend.service.impl;

import lk.ijse.etechbackend.dto.profile.LegalPolicyDTO;
import lk.ijse.etechbackend.dto.profile.PolicySectionDTO;
import lk.ijse.etechbackend.entity.LegalPolicy;
import lk.ijse.etechbackend.entity.PolicySections;
import lk.ijse.etechbackend.exception.BadRequestException;
import lk.ijse.etechbackend.exception.ResourceNotFoundException;
import lk.ijse.etechbackend.repository.LegalPolicyRepository;
import lk.ijse.etechbackend.repository.PolicySectionRepository;
import lk.ijse.etechbackend.service.LegalPolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class LegalPolicyServiceImpl implements LegalPolicyService {

    private final LegalPolicyRepository policyRepository;
    private final PolicySectionRepository policySectionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<LegalPolicyDTO> getAllPolicies() {
        log.info("Fetching all legal policies");
        return policyRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public LegalPolicyDTO getPolicyBySlug(String slug) {
        log.info("Fetching legal policy by slug/ID: {}", slug);
        LegalPolicy policy = policyRepository.findById(slug.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("Policy document not found for: " + slug));
        return toDTO(policy);
    }

    @Override
    public LegalPolicyDTO createPolicy(LegalPolicyDTO request) {
        if (request.getId() == null || request.getId().trim().isEmpty()) {
            throw new BadRequestException("Policy ID / slug is required");
        }
        String slug = request.getId().trim().toLowerCase();
        if (policyRepository.existsById(slug)) {
            throw new BadRequestException("Policy document already exists with ID: " + slug);
        }

        log.info("Creating new legal policy for slug: {}", slug);
        LegalPolicy policy = LegalPolicy.builder()
                .id(slug)
                .title(request.getTitle() != null ? request.getTitle().trim() : slug)
                .subtitle(request.getSubtitle() != null ? request.getSubtitle().trim() : "")
                .lastUpdated(request.getLastUpdated() != null ? request.getLastUpdated().trim() : "Current")
                .build();

        if (request.getSections() != null && !request.getSections().isEmpty()) {
            for (PolicySectionDTO sec : request.getSections()) {
                String title = sec.getSectionTitle() != null ? sec.getSectionTitle() : sec.getHeading();
                String content = sec.getSectionContent() != null ? sec.getSectionContent() : sec.getContent();
                String bullets = sec.getBulletPoints();

                if (title != null && !title.trim().isEmpty()) {
                    PolicySections policySection = PolicySections.builder()
                            .id(sec.getId() != null && !sec.getId().trim().isEmpty()
                                    ? sec.getId()
                                    : (slug + "-" + title.toLowerCase().replaceAll("\\s+", "-")))
                            .sectionTitle(title.trim())
                            .sectionContent(content != null ? content.trim() : "")
                            .bulletPoints(bullets != null ? bullets.trim() : "")
                            .legalPolicy(policy)
                            .build();

                    policy.addPolicySection(policySectionRepository.save(policySection));
                }
            }
        } else if (request.getPolicySections() != null && !request.getPolicySections().isEmpty()) {
            for (Map.Entry<String, String> entry : request.getPolicySections().entrySet()) {
                String title = entry.getKey();
                String content = entry.getValue();

                PolicySections policySection = PolicySections.builder()
                        .id(slug + "-" + title.toLowerCase().replaceAll("\\s+", "-"))
                        .sectionTitle(title)
                        .sectionContent(content)
                        .bulletPoints("")
                        .legalPolicy(policy)
                        .build();

                policy.addPolicySection(policySectionRepository.save(policySection));
            }
        }

        LegalPolicy saved = policyRepository.save(policy);
        return toDTO(saved);
    }

    @Override
    public LegalPolicyDTO updatePolicy(String slug, LegalPolicyDTO request) {
        log.info("Updating legal policy for: {}", slug);
        LegalPolicy policy = policyRepository.findById(slug.toLowerCase())
                .orElseGet(() -> LegalPolicy.builder().id(slug.toLowerCase()).build());

        if (request.getTitle() != null) policy.setTitle(request.getTitle().trim());
        if (request.getSubtitle() != null) policy.setSubtitle(request.getSubtitle().trim());
        if (request.getLastUpdated() != null) policy.setLastUpdated(request.getLastUpdated().trim());

        if (request.getSections() != null && !request.getSections().isEmpty()) {
            policy.getPolicySections().clear();
            for (PolicySectionDTO sec : request.getSections()) {
                String title = sec.getSectionTitle() != null ? sec.getSectionTitle() : sec.getHeading();
                String content = sec.getSectionContent() != null ? sec.getSectionContent() : sec.getContent();
                String bullets = sec.getBulletPoints();

                if (title != null && !title.trim().isEmpty()) {
                    PolicySections policySection = PolicySections.builder()
                            .id(sec.getId() != null && !sec.getId().trim().isEmpty()
                                    ? sec.getId()
                                    : (slug.toLowerCase() + "-" + title.toLowerCase().replaceAll("\\s+", "-")))
                            .sectionTitle(title.trim())
                            .sectionContent(content != null ? content.trim() : "")
                            .bulletPoints(bullets != null ? bullets.trim() : "")
                            .legalPolicy(policy)
                            .build();

                    policy.addPolicySection(policySectionRepository.save(policySection));
                }
            }
        } else if (request.getPolicySections() != null && !request.getPolicySections().isEmpty()) {
            policy.getPolicySections().clear();
            for (Map.Entry<String, String> entry : request.getPolicySections().entrySet()) {
                String title = entry.getKey();
                String section = entry.getValue();

                PolicySections policySection = PolicySections.builder()
                        .id(slug.toLowerCase() + "-" + title.toLowerCase().replaceAll("\\s+", "-"))
                        .sectionTitle(title)
                        .sectionContent(section)
                        .bulletPoints("")
                        .legalPolicy(policy)
                        .build();

                policy.addPolicySection(policySectionRepository.save(policySection));
            }
        }

        LegalPolicy saved = policyRepository.save(policy);
        return toDTO(saved);
    }

    @Override
    public void deletePolicy(String slug) {
        log.info("Deleting legal policy for: {}", slug);
        LegalPolicy policy = policyRepository.findById(slug.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("Policy document not found for: " + slug));
        policyRepository.delete(policy);
    }

    @Override
    public LegalPolicyDTO addSection(String slug, PolicySectionDTO sectionDTO) {
        log.info("Adding section to policy: {}", slug);
        LegalPolicy policy = policyRepository.findById(slug.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("Policy document not found for: " + slug));

        String title = sectionDTO.getSectionTitle() != null ? sectionDTO.getSectionTitle() : sectionDTO.getHeading();
        if (title == null || title.trim().isEmpty()) {
            throw new BadRequestException("Section title / heading is required");
        }
        String content = sectionDTO.getSectionContent() != null ? sectionDTO.getSectionContent() : sectionDTO.getContent();
        String bullets = sectionDTO.getBulletPoints();

        PolicySections policySection = PolicySections.builder()
                .id(sectionDTO.getId() != null && !sectionDTO.getId().trim().isEmpty()
                        ? sectionDTO.getId()
                        : (slug.toLowerCase() + "-" + title.toLowerCase().replaceAll("\\s+", "-")))
                .sectionTitle(title.trim())
                .sectionContent(content != null ? content.trim() : "")
                .bulletPoints(bullets != null ? bullets.trim() : "")
                .legalPolicy(policy)
                .build();

        policy.addPolicySection(policySectionRepository.save(policySection));
        LegalPolicy saved = policyRepository.save(policy);
        return toDTO(saved);
    }

    @Override
    public LegalPolicyDTO deleteSection(String slug, String sectionId) {
        log.info("Deleting section {} from policy: {}", sectionId, slug);
        LegalPolicy policy = policyRepository.findById(slug.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("Policy document not found for: " + slug));

        if (policy.getPolicySections() != null) {
            policy.getPolicySections().removeIf(sec -> sectionId.equalsIgnoreCase(sec.getId()));
        }

        LegalPolicy saved = policyRepository.save(policy);
        return toDTO(saved);
    }
    private LegalPolicyDTO toDTO(LegalPolicy p) {
        List<PolicySectionDTO> sections = new ArrayList<>();
        Map<String, String> legacyMap = new HashMap<>();

        if (p.getPolicySections() != null && !p.getPolicySections().isEmpty()) {
            p.getPolicySections().forEach(section -> {
                sections.add(PolicySectionDTO.builder()
                        .id(section.getId())
                        .sectionTitle(section.getSectionTitle())
                        .heading(section.getSectionTitle())
                        .sectionContent(section.getSectionContent())
                        .content(section.getSectionContent())
                        .bulletPoints(section.getBulletPoints() != null ? section.getBulletPoints() : "")
                        .build());
                legacyMap.put(
                        section.getSectionTitle(),
                        section.getSectionContent()
                );
            });
        }
        return LegalPolicyDTO.builder()
                .id(p.getId())
                .title(p.getTitle())
                .subtitle(p.getSubtitle())
                .lastUpdated(p.getLastUpdated())
                .sections(sections)
                .policySections(legacyMap)
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
