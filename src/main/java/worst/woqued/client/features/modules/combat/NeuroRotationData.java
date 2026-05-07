package worst.woqued.client.features.modules.combat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NeuroRotationData {
    private float deltaYaw;
    private float deltaPitch;
    private float distance;
    private float targetSpeed;
    private long timestamp;
}