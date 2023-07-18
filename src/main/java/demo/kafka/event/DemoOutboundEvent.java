package demo.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DemoOutboundEvent {

    private String firstName;
    private String middleName;
    private String lastName;
}
