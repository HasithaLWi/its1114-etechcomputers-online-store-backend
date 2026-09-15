package lk.ijse.etechbackend.dto.profile;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LegalPolicyDTO {
    private String id;
    private String title;
    private String subtitle;
    private String lastUpdated;
    private List<PolicySectionDTO> sections;
    private Map<String, String> policySections;
    private LocalDateTime updatedAt;
}
