package lk.ijse.etechbackend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicySections {

    @Id
    private String id;


    private String sectionTitle;
    @Column(columnDefinition = "TEXT")
    private String sectionContent;

    @Column(columnDefinition = "TEXT")
    private String bulletPoints;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "legal_policy_id")
    private LegalPolicy legalPolicy;
}
