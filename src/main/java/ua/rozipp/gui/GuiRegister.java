package ua.rozipp.gui;

import java.util.HashMap;

public class GuiRegister {

    public static HashMap<String, GuiAction> guiActionList = new HashMap<>();

    private static void registerGuiAction(GuiAction gIA){
        String name = gIA.getClass().getName();
        guiActionList.put(name, gIA);
    }

}
