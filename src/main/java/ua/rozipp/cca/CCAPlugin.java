package ua.rozipp.cca;

import lombok.Getter;
import ua.rozipp.abstractplugin.APlugin;
import ua.rozipp.abstractplugin.exception.InvalidConfiguration;
import ua.rozipp.cca.database.ADatabaseMaster;
import ua.rozipp.cca.objects.Town;

import java.util.Arrays;

public class CCAPlugin extends APlugin {

    @Getter
    private static CCAPlugin instance;
    @Getter
    private ADatabaseMaster databaseMaster;

    @Override
    public void onEnable() {
        super.onEnable();
        try {
            databaseMaster = new ADatabaseMaster(this.getSetting(), this.getLogger(),
                    Arrays.asList(Town.class,
                            Town.class));
        } catch (InvalidConfiguration e) {
            getLogger().severe(e.getMessage());
            return;
        }



    }
}
