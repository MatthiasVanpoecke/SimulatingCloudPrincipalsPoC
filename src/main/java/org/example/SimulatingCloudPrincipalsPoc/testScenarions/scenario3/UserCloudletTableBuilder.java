package org.example.SimulatingCloudPrincipalsPoc.testScenarions.scenario3;

import org.cloudsimplus.builders.tables.CloudletsTableBuilder;
import org.cloudsimplus.builders.tables.Table;
import org.cloudsimplus.cloudlets.Cloudlet;

import java.util.List;

/**
 * This class is used to create a table with the list of UserCloudlets to also show the user id.
 */
public class UserCloudletTableBuilder extends CloudletsTableBuilder {
    public UserCloudletTableBuilder(List<? extends Cloudlet> list) {
        super(list);
    }

    public UserCloudletTableBuilder(List<? extends Cloudlet> list, Table table) {
        super(list, table);
    }

    @Override
    protected void createTableColumns() {
        super.createTableColumns();
        addColumn(getTable().newColumn("User", "ID"), this::getUserId);
    }

    @Override
    public CloudletsTableBuilder setTimeFormat(String timeFormat) {
        return super.setTimeFormat(timeFormat);
    }

    @Override
    public CloudletsTableBuilder setLengthFormat(String lengthFormat) {
        return super.setLengthFormat(lengthFormat);
    }

    @Override
    public CloudletsTableBuilder setIdFormat(String idFormat) {
        return super.setIdFormat(idFormat);
    }

    @Override
    public CloudletsTableBuilder setPeFormat(String peFormat) {
        return super.setPeFormat(peFormat);
    }


    private String getUserId(Cloudlet cloudlet) {
        if(cloudlet instanceof UserCloudlet) {
            return ((UserCloudlet) cloudlet).getUserId();
        }
        return "/";
    }
}
