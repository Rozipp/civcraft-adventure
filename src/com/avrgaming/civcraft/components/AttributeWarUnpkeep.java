
package com.avrgaming.civcraft.components;

import com.avrgaming.civcraft.components.Component;
import com.avrgaming.civcraft.construct.Construct;

public class AttributeWarUnpkeep
extends Component {
    public double value;

    @Override
    public void createComponent(Construct constr, boolean async) {
        super.createComponent(constr, async);
        this.value = this.getDouble("value");
    }
}

