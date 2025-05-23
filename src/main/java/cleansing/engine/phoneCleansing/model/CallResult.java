package cleansing.engine.phoneCleansing.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "call_result")
public class CallResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(name = "result_call")
    private String resultCall;

    @UpdateTimestamp
    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;
}