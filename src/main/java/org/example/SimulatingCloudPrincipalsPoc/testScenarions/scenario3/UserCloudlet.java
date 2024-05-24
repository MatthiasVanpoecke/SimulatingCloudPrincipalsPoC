package org.example.SimulatingCloudPrincipalsPoc.testScenarions.scenario3;

import lombok.Getter;
import lombok.Setter;
import org.cloudsimplus.cloudlets.CloudletSimple;
import org.cloudsimplus.utilizationmodels.UtilizationModel;

public class UserCloudlet extends CloudletSimple {

    @Getter @Setter
    String userId = "";

    public UserCloudlet(long length, int pesNumber, UtilizationModel utilizationModel) {
        super(length, pesNumber, utilizationModel);

    }

    public UserCloudlet(long length, int pesNumber) {
        super(length, pesNumber);
    }

    public UserCloudlet(long length, long pesNumber) {
        super(length, pesNumber);
    }

    public UserCloudlet(long id, long length, long pesNumber) {
        super(id, length, pesNumber);
    }

    public UserCloudlet(long length, int pesNumber, UtilizationModel utilizationModel, String userId) {
        super(length, pesNumber, utilizationModel);
        this.userId = userId;
    }

    public UserCloudlet(long length, int pesNumber, String userId) {
        super(length, pesNumber);
        this.userId = userId;
    }

    public UserCloudlet(long length, long pesNumber, String userId) {
        super(length, pesNumber);
        this.userId = userId;
    }

    public UserCloudlet(long id, long length, long pesNumber, String userId) {
        super(id, length, pesNumber);
        this.userId = userId;
    }
}
